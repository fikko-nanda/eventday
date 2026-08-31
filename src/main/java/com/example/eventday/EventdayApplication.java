package com.example.eventday;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Mengaktifkan fitu cron job / background scheduler
public class EventdayApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventdayApplication.class, args);
    }
}