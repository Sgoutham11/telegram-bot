package com.telegram.bot.repository;

import com.telegram.bot.entity.TelegramBot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramBotRepository extends JpaRepository<TelegramBot, Long> {

    @Query("select g.chatId from TelegramBot as g where g.senderUserName=:userName or g.nicName=:userName")
    Long getChatId(String userName);

//    @Query("SELECT CONCAT(g.exchange, ':', g.tradingSymbol) FROM StockList g WHERE (g.exchange='NSE' OR g.exchange='BSE' ) ")
//    List<String> getExchangeSymbols();




}