package com.telegram.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
//@EnableAsync
@EnableScheduling
public class TelegramBotBuddy extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(TelegramBotBuddy.class, args);
    }

}
