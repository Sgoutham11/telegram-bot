package com.telegram.bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotServiceImpl implements TelegramBotService {

    // Store active games per chat
    private final Map<Long, BingoGame> activeGames = new ConcurrentHashMap<>();

    public BingoGame startNewGame(Long chatId) {
        BingoGame game = new BingoGame();
        activeGames.put(chatId, game);
        return game;
    }

    public BingoGame getGame(Long chatId) {
        return activeGames.get(chatId);
    }

    public void endGame(Long chatId) {
        activeGames.remove(chatId);
    }

    public InlineKeyboardMarkup buildBoardKeyboard(BingoGame game) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        String[] headers = {"B", "I", "N", "G", "O"};
        int[][] board = game.getBoard();
        boolean[][] marked = game.getMarked();

        // Header row
        List<InlineKeyboardButton> headerRow = new ArrayList<>();
        for (String h : headers) {
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText("🔵 " + h);
            btn.setCallbackData("bingo_header");
            headerRow.add(btn);
        }
        keyboard.add(headerRow);

        // Number rows
        for (int row = 0; row < 5; row++) {
            List<InlineKeyboardButton> rowButtons = new ArrayList<>();
            for (int col = 0; col < 5; col++) {
                InlineKeyboardButton btn = new InlineKeyboardButton();

                if (row == 2 && col == 2) {
                    btn.setText("⭐");  // FREE space
                    btn.setCallbackData("bingo_free");
                } else if (marked[row][col]) {
                    btn.setText("✅ " + board[row][col]);
                    btn.setCallbackData("bingo_marked");
                } else if (game.getCalledNumbers().contains(board[row][col])) {
                    btn.setText("🟡 " + board[row][col]);  // Available to mark
                    btn.setCallbackData("bingo_mark_" + row + "_" + col);
                } else {
                    btn.setText(String.valueOf(board[row][col]));
                    btn.setCallbackData("bingo_notcalled_" + row + "_" + col);
                }

                rowButtons.add(btn);
            }
            keyboard.add(rowButtons);
        }

        // Action buttons
        List<InlineKeyboardButton> actionRow = new ArrayList<>();

        InlineKeyboardButton callBtn = new InlineKeyboardButton();
        callBtn.setText("🎱 Call Number");
        callBtn.setCallbackData("bingo_call");
        actionRow.add(callBtn);

        InlineKeyboardButton bingoBtn = new InlineKeyboardButton();
        bingoBtn.setText("🎉 BINGO!");
        bingoBtn.setCallbackData("bingo_check");
        actionRow.add(bingoBtn);

        keyboard.add(actionRow);

        // Quit button
        List<InlineKeyboardButton> quitRow = new ArrayList<>();
        InlineKeyboardButton quitBtn = new InlineKeyboardButton();
        quitBtn.setText("❌ Quit Game");
        quitBtn.setCallbackData("bingo_quit");
        quitRow.add(quitBtn);
        keyboard.add(quitRow);

        markup.setKeyboard(keyboard);
        return markup;
    }

    public String buildStatusText(BingoGame game) {
        StringBuilder sb = new StringBuilder();
        sb.append("🎰 *BINGO GAME* 🎰\n\n");
        sb.append("Last Called: *").append(game.getLastCalledNumber()).append("*\n");
        sb.append("Numbers Called: ").append(game.getCalledNumbers().size()).append("/75\n\n");
        sb.append("🟡 = Called (tap to mark)\n");
        sb.append("✅ = Marked\n");
        sb.append("⭐ = FREE space\n\n");
        sb.append("Tap 🎱 to call the next number!");
        return sb.toString();
    }

}
