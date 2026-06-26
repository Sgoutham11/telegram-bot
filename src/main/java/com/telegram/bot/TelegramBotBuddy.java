package com.telegram.bot;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
//@EnableAsync
@EnableScheduling
public class TelegramBotBuddy extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(TelegramBotBuddy.class, args);
    }
//    @Bean
//    public TelegramBotsApi telegramBotsApi(MyTelegramBot bot) throws TelegramApiException {
//        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
//        api.registerBot(bot);
//        System.out.println(">>> BOT REGISTERED: " + bot.getBotUsername());
//        return api;
//    }

    @PostConstruct
    public void init() {
        // Set default time zone to Arabian Standard Time
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Riyadh"));
    }

}
