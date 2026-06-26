package com.telegram.bot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Entity
@Builder
@Table(name = "TELEGRAM_BOT")
@NoArgsConstructor
@AllArgsConstructor
public class TelegramBot {

    @Id
    @Column(name = "CHAT_ID")
    private Long chatId;

    @Column(name = "SENDER_USER_NAME")
    private String senderUserName;

    @Column(name = "NIC_NAME")
    private String nicName;




}
