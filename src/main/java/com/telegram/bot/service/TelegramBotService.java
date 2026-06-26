package com.telegram.bot.service;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public interface TelegramBotService {

    public BingoGame startNewGame(Long chatId);
    public String buildStatusText(BingoGame game);
    public InlineKeyboardMarkup buildBoardKeyboard(BingoGame game);
    public BingoGame getGame(Long chatId);
    public void endGame(Long chatId);


}
