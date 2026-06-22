package es.brasatech.pinchito.controller;

import es.brasatech.pinchito.model.*;
import es.brasatech.pinchito.service.StorageService;
import es.brasatech.pinchito.util.PasswordHasher;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class PinchitoController {

    private static final Logger log = LoggerFactory.getLogger(PinchitoController.class);

    private final StorageService storageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public PinchitoController(StorageService storageService, SimpMessagingTemplate messagingTemplate) {
        this.storageService = storageService;
        this.messagingTemplate = messagingTemplate;
    }

    private String getLoggedInUser(HttpSession session) {
        return (String) session.getAttribute("username");
    }

    private String getLoggedInAccount(HttpSession session) {
        return (String) session.getAttribute("accountName");
    }

    @GetMapping("/")
    public String showLogin(HttpSession session, Model model) {
        if (getLoggedInUser(session) != null && getLoggedInAccount(session) != null) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam("accountName") String accountName,
                              @RequestParam("username") String username,
                              @RequestParam("password") String password,
                              HttpSession session,
                              Model model) {
        
        String safeAccountName = accountName.replaceAll("[^a-zA-Z0-9_\\-]", "").toLowerCase();
        if (safeAccountName.isEmpty()) {
            model.addAttribute("error", "Nombre de cuenta no válido.");
            return "login";
        }

        if (!storageService.accountExists(safeAccountName)) {
            log.warn("Login failed: Account '{}' does not exist.", safeAccountName);
            model.addAttribute("error", "La cuenta no existe. Puedes registrarla a continuación.");
            return "login";
        }

        try {
            log.info("Attempting login for account: '{}', user: '{}'", safeAccountName, username);
            Account account = storageService.loadAccount(safeAccountName);
            Member matchingMember = account.getMembers().stream()
                    .filter(m -> m.getUsername().equalsIgnoreCase(username))
                    .findFirst()
                    .orElse(null);

            if (matchingMember == null) {
                log.warn("Login failed: User '{}' not found in account '{}'. Existing members: {}", 
                         username, safeAccountName, account.getMembers().stream().map(Member::getUsername).collect(Collectors.toList()));
                model.addAttribute("error", "Usuario o contraseña incorrectos.");
                return "login";
            }

            if (!PasswordHasher.verifyPassword(password, matchingMember.getPassword())) {
                log.warn("Login failed: Password mismatch for user '{}' in account '{}'. Stored hash: '{}', Computed hash: '{}'", 
                         username, safeAccountName, matchingMember.getPassword(), PasswordHasher.hashPassword(password));
                model.addAttribute("error", "Usuario o contraseña incorrectos.");
                return "login";
            }

            log.info("Login successful for user '{}' in account '{}'", matchingMember.getUsername(), safeAccountName);
            session.setAttribute("accountName", safeAccountName);
            session.setAttribute("username", matchingMember.getUsername());
            return "redirect:/dashboard";
        } catch (Exception e) {
            log.error("Login exception: ", e);
            model.addAttribute("error", "Error al cargar la cuenta: " + e.getMessage());
            return "login";
        }
    }

    @GetMapping("/register")
    public String showRegister(HttpSession session) {
        if (getLoggedInUser(session) != null && getLoggedInAccount(session) != null) {
            return "redirect:/dashboard";
        }
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(@RequestParam("accountName") String accountName,
                                 @RequestParam("usernames") List<String> usernames,
                                 @RequestParam("passwords") List<String> passwords,
                                 HttpSession session,
                                 Model model) {
        
        String safeAccountName = accountName.replaceAll("[^a-zA-Z0-9_\\-]", "").toLowerCase();
        if (safeAccountName.isEmpty()) {
            model.addAttribute("error", "El nombre de la cuenta no puede estar vacío.");
            return "register";
        }

        if (storageService.accountExists(safeAccountName)) {
            model.addAttribute("error", "La cuenta '" + accountName + "' ya existe.");
            return "register";
        }

        List<Member> members = new ArrayList<>();
        for (int i = 0; i < usernames.size(); i++) {
            String u = usernames.get(i).trim();
            String p = passwords.size() > i ? passwords.get(i) : "";
            if (!u.isEmpty() && !p.isEmpty()) {
                members.add(new Member(u, PasswordHasher.hashPassword(p)));
            }
        }

        if (members.size() < 2) {
            model.addAttribute("error", "Debes especificar al menos dos miembros con contraseñas.");
            return "register";
        }

        try {
            Account account = new Account(safeAccountName);
            account.setMembers(members);
            storageService.saveAccount(account);

            session.setAttribute("accountName", safeAccountName);
            session.setAttribute("username", members.get(0).getUsername());
            return "redirect:/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Error al crear la cuenta: " + e.getMessage());
            return "register";
        }
    }

    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        String username = getLoggedInUser(session);
        String accountName = getLoggedInAccount(session);

        if (username == null || accountName == null) {
            return "redirect:/";
        }

        try {
            Account account = storageService.loadAccount(accountName);

            List<Debit> debitsIOwe = account.getDebits().stream()
                    .filter(d -> d.getFromUser().equalsIgnoreCase(username) && !"PAID".equalsIgnoreCase(d.getStatus()))
                    .collect(Collectors.toList());

            List<Debit> debitsOwedToMe = account.getDebits().stream()
                    .filter(d -> d.getToUser().equalsIgnoreCase(username) && !"PAID".equalsIgnoreCase(d.getStatus()))
                    .collect(Collectors.toList());

            List<Debit> paidHistory = account.getDebits().stream()
                    .filter(d -> "PAID".equalsIgnoreCase(d.getStatus()) && 
                            (d.getFromUser().equalsIgnoreCase(username) || d.getToUser().equalsIgnoreCase(username)))
                    .collect(Collectors.toList());

            List<Debit> oppositionDebits = debitsIOwe.stream()
                    .filter(d -> d.getPaymentRequests().stream().anyMatch(pr -> "REJECTED".equalsIgnoreCase(pr.getStatus())))
                    .collect(Collectors.toList());

            List<String> otherMembers = account.getMembers().stream()
                    .map(Member::getUsername)
                    .filter(u -> !u.equalsIgnoreCase(username))
                    .collect(Collectors.toList());

            model.addAttribute("accountName", accountName);
            model.addAttribute("username", username);
            model.addAttribute("debitsIOwe", debitsIOwe);
            model.addAttribute("debitsOwedToMe", debitsOwedToMe);
            model.addAttribute("paidHistory", paidHistory);
            model.addAttribute("oppositionDebits", oppositionDebits);
            model.addAttribute("otherMembers", otherMembers);
            model.addAttribute("debitKinds", DebitKind.values());

            return "dashboard";
        } catch (Exception e) {
            session.invalidate();
            return "redirect:/?error=Sesión+expirada+o+error+de+cuenta";
        }
    }

    @PostMapping("/debit/create")
    public String createDebit(@RequestParam("fromUser") String fromUser,
                              @RequestParam("quantity") int quantity,
                              @RequestParam("kind") DebitKind kind,
                              @RequestParam("why") String why,
                              HttpSession session) {
        String username = getLoggedInUser(session);
        String accountName = getLoggedInAccount(session);
        if (username == null || accountName == null) return "redirect:/";

        try {
            Account account = storageService.loadAccount(accountName);
            
            Debit debit = new Debit();
            debit.setId(UUID.randomUUID().toString());
            debit.setFromUser(fromUser);
            debit.setToUser(username);
            debit.setQuantity(quantity);
            debit.setKind(kind);
            debit.setWhy(why);
            debit.setStatus("PENDING");
            debit.setCreatedDate(LocalDateTime.now().format(dateFormatter));
            
            account.getDebits().add(debit);
            storageService.saveAccount(account);

            String msg = String.format("¡%s ha declarado una deuda de %d '%s' para ti!", 
                                       username, quantity, kind.getDisplayName());
            messagingTemplate.convertAndSend("/topic/account/" + accountName, 
                    new WebSocketEvent("DEBIT_CREATED", msg, username));
        } catch (Exception e) {
            // Handle error silently or log
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/debit/request-payment")
    public String requestPayment(@RequestParam("debitId") String debitId,
                                 @RequestParam("whyPaid") String whyPaid,
                                 HttpSession session) {
        String username = getLoggedInUser(session);
        String accountName = getLoggedInAccount(session);
        if (username == null || accountName == null) return "redirect:/";

        try {
            Account account = storageService.loadAccount(accountName);
            Debit debit = account.getDebits().stream()
                    .filter(d -> d.getId().equals(debitId))
                    .findFirst()
                    .orElse(null);

            if (debit != null && debit.getFromUser().equalsIgnoreCase(username)) {
                PaymentRequest req = new PaymentRequest();
                req.setId(UUID.randomUUID().toString());
                req.setWhyPaid(whyPaid);
                req.setStatus("PENDING");
                req.setRequestDate(LocalDateTime.now().format(dateFormatter));
                
                debit.getPaymentRequests().add(req);
                storageService.saveAccount(account);

                String msg = String.format("¡%s afirma haber pagado la deuda de '%s'!", 
                                           username, debit.getKind().getDisplayName());
                messagingTemplate.convertAndSend("/topic/account/" + accountName, 
                        new WebSocketEvent("PAYMENT_REQUESTED", msg, username));
            }
        } catch (Exception e) {
            // Handle error
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/debit/confirm-payment")
    public String confirmPayment(@RequestParam("debitId") String debitId,
                                 @RequestParam("requestId") String requestId,
                                 HttpSession session) {
        String username = getLoggedInUser(session);
        String accountName = getLoggedInAccount(session);
        if (username == null || accountName == null) return "redirect:/";

        try {
            Account account = storageService.loadAccount(accountName);
            Debit debit = account.getDebits().stream()
                    .filter(d -> d.getId().equals(debitId))
                    .findFirst()
                    .orElse(null);

            if (debit != null && debit.getToUser().equalsIgnoreCase(username)) {
                PaymentRequest req = debit.getPaymentRequests().stream()
                        .filter(r -> r.getId().equals(requestId))
                        .findFirst()
                        .orElse(null);

                if (req != null) {
                    req.setStatus("CONFIRMED");
                    debit.setStatus("PAID");
                    storageService.saveAccount(account);

                    String msg = String.format("¡%s ha confirmado el pago de '%s'!", 
                                               username, debit.getKind().getDisplayName());
                    messagingTemplate.convertAndSend("/topic/account/" + accountName, 
                            new WebSocketEvent("PAYMENT_CONFIRMED", msg, username));
                }
            }
        } catch (Exception e) {
            // Handle error
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/debit/reject-payment")
    public String rejectPayment(@RequestParam("debitId") String debitId,
                                @RequestParam("requestId") String requestId,
                                @RequestParam("rejectionReason") String rejectionReason,
                                HttpSession session) {
        String username = getLoggedInUser(session);
        String accountName = getLoggedInAccount(session);
        if (username == null || accountName == null) return "redirect:/";

        try {
            Account account = storageService.loadAccount(accountName);
            Debit debit = account.getDebits().stream()
                    .filter(d -> d.getId().equals(debitId))
                    .findFirst()
                    .orElse(null);

            if (debit != null && debit.getToUser().equalsIgnoreCase(username)) {
                PaymentRequest req = debit.getPaymentRequests().stream()
                        .filter(r -> r.getId().equals(requestId))
                        .findFirst()
                        .orElse(null);

                if (req != null) {
                    req.setStatus("REJECTED");
                    req.setRejectionReason(rejectionReason);
                    storageService.saveAccount(account);

                    String msg = String.format("¡%s ha rechazado tu declaración de pago para '%s'!", 
                                               username, debit.getKind().getDisplayName());
                    messagingTemplate.convertAndSend("/topic/account/" + accountName, 
                            new WebSocketEvent("PAYMENT_REJECTED", msg, username));
                }
            }
        } catch (Exception e) {
            // Handle error
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
