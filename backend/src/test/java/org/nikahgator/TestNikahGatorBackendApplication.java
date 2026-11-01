package org.nikahgator;

import org.springframework.boot.SpringApplication;

public class TestNikahGatorBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(NikahGatorBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
