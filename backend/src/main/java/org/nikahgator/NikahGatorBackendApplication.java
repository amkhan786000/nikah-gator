package org.nikahgator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
public class NikahGatorBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(NikahGatorBackendApplication.class, args);
	}

}
