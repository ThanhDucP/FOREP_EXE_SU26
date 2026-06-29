package com.forep.exe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ForepExeApplication {
    public static void main(String[] args) {
        SpringApplication.run(ForepExeApplication.class, args);
    }
}
