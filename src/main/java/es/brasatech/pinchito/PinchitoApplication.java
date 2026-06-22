package es.brasatech.pinchito;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

@SpringBootApplication
@ImportRuntimeHints(PinchitoRuntimeHints.class)
public class PinchitoApplication {

	public static void main(String[] args) {
		SpringApplication.run(PinchitoApplication.class, args);
	}

}
