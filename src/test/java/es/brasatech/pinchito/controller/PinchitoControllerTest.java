package es.brasatech.pinchito.controller;

import es.brasatech.pinchito.model.Account;
import es.brasatech.pinchito.model.Member;
import es.brasatech.pinchito.service.StorageService;
import es.brasatech.pinchito.util.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PinchitoControllerTest {

    private PinchitoController controller;
    private StorageService storageService;
    private MockHttpSession session;
    private Model model;

    @BeforeEach
    void setUp() {
        storageService = mock(StorageService.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        controller = new PinchitoController(storageService, messagingTemplate);
        session = new MockHttpSession();
        model = new ConcurrentModel();
    }

    @Test
    void testRegisterAndLogin() {
        // 1. Test Registration
        when(storageService.accountExists("our_space")).thenReturn(false);

        List<String> usernames = Arrays.asList("Alice", "Bob");
        List<String> passwords = Arrays.asList("password123", "password456");

        String regResult = controller.handleRegister("our_space", usernames, passwords, session, model);

        assertEquals("redirect:/dashboard", regResult);
        verify(storageService, times(1)).saveAccount(any(Account.class));
        assertEquals("our_space", session.getAttribute("accountName"));
        assertEquals("Alice", session.getAttribute("username"));

        // 2. Test Login
        Account account = new Account("our_space");
        List<Member> members = new ArrayList<>();
        members.add(new Member("Alice", PasswordHasher.hashPassword("password123")));
        members.add(new Member("Bob", PasswordHasher.hashPassword("password456")));
        account.setMembers(members);

        when(storageService.accountExists("our_space")).thenReturn(true);
        when(storageService.loadAccount("our_space")).thenReturn(account);

        session.clearAttributes();

        String loginResult = controller.handleLogin("our_space", "Alice", "password123", session, model);

        assertEquals("redirect:/dashboard", loginResult);
        assertEquals("our_space", session.getAttribute("accountName"));
        assertEquals("Alice", session.getAttribute("username"));
    }

    @Test
    void testLoginFailure() {
        Account account = new Account("our_space");
        List<Member> members = new ArrayList<>();
        members.add(new Member("Alice", PasswordHasher.hashPassword("password123")));
        account.setMembers(members);

        when(storageService.accountExists("our_space")).thenReturn(true);
        when(storageService.loadAccount("our_space")).thenReturn(account);

        // Test wrong password
        String resultWrongPass = controller.handleLogin("our_space", "Alice", "wrongpass", session, model);
        assertEquals("login", resultWrongPass);
        assertNotNull(model.getAttribute("error"));
        assertNull(session.getAttribute("username"));

        // Test non-existent user
        String resultWrongUser = controller.handleLogin("our_space", "Unknown", "password123", session, model);
        assertEquals("login", resultWrongUser);
        assertNull(session.getAttribute("username"));
    }
}
