package com.banking.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class UserApplication {

    static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }

}
