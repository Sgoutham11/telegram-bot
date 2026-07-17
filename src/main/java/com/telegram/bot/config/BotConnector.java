package com.telegram.bot.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
@Slf4j
public class BotConnector {

    @Autowired
    private MyTelegramBot myTelegramBot;

    /*
     * Legacy Telegram command/callback Bingo bot is disabled.
     * Bingo gameplay now lives in the Phaser mini app.
     */
//    @Autowired
//    private TelegramGameBot telegramGameBot;

    @Value("${telegram.bot2.status}")
    private boolean bot2Flag;

    @Value("${telegram.bot1.status}")
    private boolean bot1Flag;


    @PostConstruct
    public void startBot() {
        new Thread(() -> {
            while (true) {
                try {
                    if (bot1Flag) {
                        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
                        botsApi.registerBot(myTelegramBot);
                    }
//                    if (bot2Flag) {
//                        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
//                        telegramBotsApi.registerBot(telegramGameBot);
//                    }

                    log.info("✅ Bot started successfully");
                    return;
                } catch (Exception e) {
                    log.warn("❌ Bot start failed, retrying in 10s: {}", e.getMessage());
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException ie) {
                        return;
                    }
                }
            }
        }).start();
    }
}
