package com.fincore.fincorebe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinCoreBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinCoreBeApplication.class, args);
    }

}

