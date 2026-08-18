package com.chacha.multitenantsaas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MultitenantSaasApplication {

    public static void main(String[] args) {

        SpringApplication.run(MultitenantSaasApplication.class, args);
    }
}
