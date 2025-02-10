package com.dev.vault96;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class Vault96Application {

    public static void main(String[] args) {
        SpringApplication.run(Vault96Application.class, args);
    }

}
