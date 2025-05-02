package com.codejam.codex.authzen;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@OpenAPIDefinition(
		info = @io.swagger.v3.oas.annotations.info.Info(
				title = "Authzen",
				version = "1.0.0",
				description = "Advanced Enterprise IAM System for Secure Access Control",

				contact = @io.swagger.v3.oas.annotations.info.Contact(
						name = "Authzen",
						email = "kavinduc.20@cse.mrt.ac.lk"
				)
		)
)
@SpringBootApplication
@ConfigurationPropertiesScan
public class AuthzenApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthzenApplication.class, args);
	}

}
