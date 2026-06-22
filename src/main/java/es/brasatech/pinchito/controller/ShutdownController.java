package es.brasatech.pinchito.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShutdownController {

    private static final Logger log = LoggerFactory.getLogger(ShutdownController.class);

    private final ConfigurableApplicationContext context;

    public ShutdownController(ConfigurableApplicationContext context) {
        this.context = context;
    }

    @GetMapping("/shutdown")
    public String shutdown() {
        log.info("Programmatic shutdown requested via /shutdown endpoint");
        
        // Execute shutdown asynchronously to allow HTTP response to return to client
        new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            log.info("Initiating ApplicationContext close...");
            try {
                context.close();
                log.info("ApplicationContext closed successfully.");
            } catch (Exception e) {
                log.error("Error closing ApplicationContext", e);
            }
            log.info("Initiating system exit (SIGTERM simulation)...");
            System.exit(0);
        }).start();

        return "Apagando la aplicación y sincronizando datos... Revisa la consola.";
    }
}
