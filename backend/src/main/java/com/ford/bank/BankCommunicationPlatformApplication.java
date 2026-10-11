package com.ford.bank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BankCommunicationPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankCommunicationPlatformApplication.class, args);
    }
}
