package com.tecsup.clima;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ClimaCosteroApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClimaCosteroApplication.class, args);
    }
}