package com.aios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AiosApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiosApplication.class, args);
    }
}
