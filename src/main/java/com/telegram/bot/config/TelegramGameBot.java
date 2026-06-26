package com.telegram.bot.config;

import com.telegram.bot.entity.GameSession;
import com.telegram.bot.entity.Player;
import com.telegram.bot.repository.TelegramBotRepository;
import com.telegram.bot.service.BingoCommandHandler;
import com.telegram.bot.service.BingoGameService;
import com.telegram.bot.service.BingoTurnService;
import com.telegram.bot.utils.BingoEnums;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class TelegramGameBot extends TelegramLongPollingBot {

    @Value("${telegram.bot2.username}")
    private String botUsername;

    @Autowired
    TelegramBotRepository telegramBotRepository;

    @Autowired
    private BingoCommandHandler bingoHandler;
    @Autowired
    private BingoGameService bingoGameService;
    @Autowired
    private BingoTurnService bingoTurnService;

    public TelegramGameBot(@Value("${telegram.bot2.token}") String botToken) {
        super(botToken);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
            return;
        }
        if (update.hasMessage()) {
            Message message = update.getMessage();
            Long chatId = message.getChatId();

            // Check for pending bingo input first
            if (message.hasText() && bingoHandler.hasPendingAction(chatId)) {
                try {
                    SendMessage response = bingoHandler.handleTextInput(chatId, message.getText());
                    if (response != null) {
                        execute(response);
                        return;
                    }
                } catch (Exception e) {
                    log.error("Error handling bingo input", e);
                }
            }
            String activeHostId = bingoHandler.getPlayerGameId(chatId);
            GameSession activeSession = null;

            if (activeHostId != null) {
                activeSession = bingoGameService.getGame(activeHostId);
                if (activeSession == null || activeSession.getStatus() == BingoEnums.GameStatus.FINISHED) {
                    activeSession = null;
                }
            }

            // Handle /bingo command
            if (activeSession == null && message.hasText() && message.getText().equals("/bingo")) {
                try {
                    execute(bingoHandler.showWelcomeMenu(chatId));
                } catch (Exception e) {
                    log.error("Error", e);
                }
                return;
            }

            if (message.hasText()) {
                try {

                    // Only show hint if NOT in an active game
                    if (activeSession == null) {
                        SendMessage msg = new SendMessage();
                        msg.setChatId(chatId.toString());
                        msg.setText("🎮 Send /bingo to start a game!");
                        execute(msg);
                    }
                } catch (Exception e) {
                    log.error("Error", e);
                }
            }

        }
    }

    private void handleCallbackQuery(CallbackQuery query) {
        String data = query.getData();
        Long chatId = query.getMessage().getChatId();
        Integer messageId = query.getMessage().getMessageId();

        try {
            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(query.getId());

            // ===== GAME CALLBACKS (need validation) =====

            // Extract hostId from callback data
            String hostId = extractHostId(data);

            if (hostId != null && isGameExpired(hostId, chatId, answer)) {
                return;
            }

            switch (data.split("_")[1]) {

                case "noop":
                    answer.setText("Already marked!");
                    execute(answer);
                    return;

                case "play":
                    execute(answer);
                    execute(bingoHandler.showPlayMenu(chatId));
                    return;

                case "back":
                    execute(answer);
                    execute(bingoHandler.showWelcomeMenu(chatId));
                    return;

                case "create":
                    execute(answer);
                    execute(bingoHandler.handleCreateHost(chatId));
                    return;

                case "join":
                    execute(answer);
                    execute(bingoHandler.handleJoinParty(chatId));
                    return;

                case "gameover":
                    answer.setText("🏁 Game Over! Send /bingo to start new game!");
                    answer.setShowAlert(true);
                    execute(answer);
                    return;

                case "newgame":
                    execute(answer);
                    execute(bingoHandler.showWelcomeMenu(chatId));
                    return;

                case "addbot":
                    hostId = data.replace("bingo_addbot_", "");

                    String result = bingoGameService.addBots(hostId);

                    if ("FULL".equals(result)) {
                        answer.setText("❌ Party is full! (Max 6)");
                    } else {
                        answer.setText("🤖 Bot added!");
                    }




                    execute(answer);

                    GameSession session = bingoGameService.getGame(hostId);
                    editLobbyMessage(chatId, messageId, session);
                    break;

                case "refresh":
                    hostId = data.replace("bingo_refresh_", "");

                    execute(answer);

                    GameSession refreshSession = bingoGameService.getGame(hostId);
                    editLobbyMessage(chatId, messageId, refreshSession);
                    break;

                case "startmenu":
                    hostId = data.replace("bingo_startmenu_", "");

                    execute(answer);
                    execute(bingoHandler.showBoardSizeMenu(chatId, hostId));
                    break;

                case "size":
                    String[] sizeParts = data.split("_");

                    int size = Integer.parseInt(sizeParts[2]);
                    hostId = sizeParts[3];

                    execute(answer);
                    startBingoGame(hostId, size);
                    break;

                case "select":
                    String[] selectParts = data.split("_");

                    int number = Integer.parseInt(selectParts[2]);
                    hostId = selectParts[3];

                    handleBingoMove(chatId, hostId, number, messageId, answer);
                    break;

                case "quit":
                    hostId = data.replace("bingo_quit_", "");

                    answer.setText("Game ended!");
                    execute(answer);

                    bingoGameService.stopGame(hostId);

                    GameSession quitSession = bingoGameService.getGame(hostId);

                    sendToAllPlayersEdit(
                            hostId,
                            "🛑 *Game stopped!*\n\nSend /bingo to play again! 🎮"
                    );

                    bingoHandler.clearAllPlayerData(quitSession);
                    bingoGameService.cleanupGame(hostId);
                    break;

                case "cancel":
                    hostId = data.replace("bingo_cancel_", "");

                    answer.setText("Game cancelled!");
                    execute(answer);

                    GameSession cancelSession = bingoGameService.getGame(hostId);

                    if (cancelSession != null) {
                        bingoHandler.clearAllPlayerData(cancelSession);
                        bingoGameService.cleanupGame(hostId);
                    }

                    // sending confirmation
                    SendMessage msg = new SendMessage();
                    msg.setChatId(chatId.toString());
                    msg.setText("❌ Game cancelled!\n\nSend /bingo to start a new game! 🎮");

                    execute(msg);
                    break;

                default:
                    execute(answer);
                    break;
            }


        } catch (Exception e) {
            log.error("Error handling callback", e);
        }
    }

    private String extractHostId(String callbackData) {
        try {
            if (callbackData.startsWith("bingo_addbot_")) return callbackData.replace("bingo_addbot_", "");
            if (callbackData.startsWith("bingo_refresh_")) return callbackData.replace("bingo_refresh_", "");
            if (callbackData.startsWith("bingo_startmenu_")) return callbackData.replace("bingo_startmenu_", "");
            if (callbackData.startsWith("bingo_cancel_")) return callbackData.replace("bingo_cancel_", "");
            if (callbackData.startsWith("bingo_quit_")) return callbackData.replace("bingo_quit_", "");
            if (callbackData.startsWith("bingo_size_")) {
                String[] parts = callbackData.split("_");
                return parts[3];
            }
            if (callbackData.startsWith("bingo_select_")) {
                String[] parts = callbackData.split("_");
                return parts[3];
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private void sendToAllPlayersEdit(String hostId, String text) {
        GameSession session = bingoGameService.getGame(hostId);
        if (session == null) return;

        for (Player player : session.getPlayers()) {
            if (player.getIsBot()) continue;
            Integer msgId = bingoHandler.getPlayerBoardMsgId(player.getChatId());
            try {
                if (msgId != null) {
                    EditMessageText edit = new EditMessageText();
                    edit.setChatId(player.getChatId().toString());
                    edit.setMessageId(msgId);
                    edit.setText(text);
                    edit.setParseMode("Markdown");
                    execute(edit);
                } else {
                    SendMessage msg = new SendMessage();
                    msg.setChatId(player.getChatId().toString());
                    msg.setText(text);
                    msg.setParseMode("Markdown");
                    execute(msg);
                }
            } catch (Exception e) {
                log.error("Error sending to: {}", player.getPlayerName(), e);
            }
        }
    }

    private void editLobbyMessage(Long chatId, Integer messageId, GameSession session) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("👑 *GAME LOBBY*\n\n");
//            sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            sb.append("🔑 Host ID: `").append(session.getHostId()).append("`\n\n");
//            sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
            sb.append("👥 *Players* (").append(session.getPlayers().size()).append("/6):\n");

            int i = 1;
            for (Player p : session.getPlayers()) {
                String icon = p.getIsHost() ? "👑" : p.getIsBot() ? "🤖" : "👤";
                sb.append(icon).append(" ").append(i++).append(". ").append(p.getPlayerName()).append("\n");
            }

            EditMessageText edit = new EditMessageText();
            edit.setChatId(chatId.toString());
            edit.setMessageId(messageId);
            edit.setText(sb.toString());
            edit.setParseMode("Markdown");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            keyboard.add(List.of(createButton("🤖 Add Bot", "bingo_addbot_" + session.getHostId())));
            keyboard.add(List.of(createButton("🔄 Refresh", "bingo_refresh_" + session.getHostId())));
            keyboard.add(List.of(createButton("🚀 Start Game", "bingo_startmenu_" + session.getHostId())));
            keyboard.add(List.of(createButton("❌ Cancel", "bingo_cancel_" + session.getHostId())));
            markup.setKeyboard(keyboard);
            edit.setReplyMarkup(markup);

            execute(edit);
        } catch (Exception e) {
            if (!e.getMessage().contains("message is not modified"))
                log.error("Error editing lobby", e);
            else log.info("Error editing lobby---Skipped error----------:=>");
        }
    }

    private void startBingoGame(String hostId, int size) {
        try {
            GameSession session = bingoGameService.startGame(hostId, size);

            bingoTurnService.setOnAutoMoveCallback((hId, result) -> {
                try {
                    handleAutoMove(hId, result);
                } catch (Exception e) {
                    log.error("Error in auto move", e);
                }
            });

            // Clear old message IDs
            for (Player player : session.getPlayers()) {
                if (!player.getIsBot()) {
                    bingoHandler.clearPlayerBoardMsgId(player.getChatId());
                }
            }

            // Send initial board to all human players
            for (Player player : session.getPlayers()) {
                if (player.getIsBot()) continue;
                sendBoardToPlayer(session, player);
            }

            bingoTurnService.startTurnTimer(hostId);

            // Handle bot's first turn
            Player currentPlayer = session.getCurrentTurnPlayer();
            if (currentPlayer != null && currentPlayer.getIsBot()) {
                handleBotTurn(hostId);
            }

        } catch (Exception e) {
            log.error("Error starting game", e);
        }
    }

    private void handleBingoMove(Long chatId, String hostId, int number, Integer messageId, AnswerCallbackQuery answer) {
        try {
            Map<String, Object> result = bingoGameService.makeMove(hostId, chatId, number);
            String status = (String) result.get("status");

            switch (status) {
                case "NOT_YOUR_TURN":
                    answer.setText("⏳ Not your turn!");
                    execute(answer);
                    return;
                case "ALREADY_SELECTED":
                    answer.setText("⚠️ Already selected!");
                    execute(answer);
                    return;
                case "NOT_PLAYING":
                    answer.setText("Game not active!");
                    execute(answer);
                    return;
                default:
                    answer.setText("✅ " + number);
                    execute(answer);
            }

            bingoTurnService.cancelTurnTimer(hostId);
            GameSession session = bingoGameService.getGame(hostId);

            if (Boolean.TRUE.equals(result.get("gameOver"))) {
                handleGameOver(hostId, session);
            } else {
                // Update all human players' boards
                for (Player player : session.getPlayers()) {
                    if (player.getIsBot()) continue;
                    sendBoardToPlayer(session, player);
                }

                bingoTurnService.startTurnTimer(hostId);

                Player nextPlayer = session.getCurrentTurnPlayer();
                if (nextPlayer != null && nextPlayer.getIsBot()) {
                    handleBotTurn(hostId);
                }
            }

        } catch (Exception e) {
            log.error("Error handling move", e);
        }
    }

    private void handleBotTurn(String hostId) {
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                bingoTurnService.cancelTurnTimer(hostId);

                Map<String, Object> result = bingoGameService.botMove(hostId);
                GameSession session = bingoGameService.getGame(hostId);

                if (Boolean.TRUE.equals(result.get("gameOver"))) {
                    handleGameOver(hostId, session);
                } else {
                    for (Player player : session.getPlayers()) {
                        if (player.getIsBot()) continue;
                        sendBoardToPlayer(session, player);
                    }

                    bingoTurnService.startTurnTimer(hostId);

                    Player next = session.getCurrentTurnPlayer();
                    if (next != null && next.getIsBot()) {
                        handleBotTurn(hostId);
                    }
                }
            } catch (Exception e) {
                log.error("Error in bot turn", e);
            }
        }).start();
    }

    private void handleAutoMove(String hostId, Map<String, Object> result) {
        try {
            GameSession session = bingoGameService.getGame(hostId);

            if (Boolean.TRUE.equals(result.get("gameOver"))) {
                handleGameOver(hostId, session);
            } else {
                // Update all boards (timeout info is in the game UI via last move)
                for (Player player : session.getPlayers()) {
                    if (player.getIsBot()) continue;
                    sendBoardToPlayer(session, player);
                }

                Player next = session.getCurrentTurnPlayer();
                if (next != null && next.getIsBot()) {
                    handleBotTurn(hostId);
                }
            }
        } catch (Exception e) {
            log.error("Error handling auto move", e);
        }
    }

    private void sendBoardToPlayer(GameSession session, Player player) {
        try {
            String ui = bingoHandler.buildGameUI(session, player);
            InlineKeyboardMarkup keyboard = bingoHandler.buildGameKeyboard(session, player);

            Integer existingMsgId = bingoHandler.getPlayerBoardMsgId(player.getChatId());

            if (existingMsgId != null) {
                // EDIT existing message
                try {
                    EditMessageText edit = new EditMessageText();
                    edit.setChatId(player.getChatId().toString());
                    edit.setMessageId(existingMsgId);
                    edit.setText(ui);
                    edit.setParseMode("Markdown");
                    edit.setReplyMarkup(keyboard);
                    execute(edit);
                    return;
                } catch (Exception e) {
                    log.warn("Edit failed, sending new: {}", e.getMessage());
                }
            }

            // Send NEW message (first time only)
            SendMessage msg = new SendMessage();
            msg.setChatId(player.getChatId().toString());
            msg.setText(ui);
            msg.setParseMode("Markdown");
            msg.setReplyMarkup(keyboard);
            Message sent = execute(msg);

            // Store message ID
            bingoHandler.setPlayerBoardMsgId(player.getChatId(), sent.getMessageId());

        } catch (Exception e) {
            log.error("Error sending board to: {}", player.getPlayerName(), e);
        }
    }


    private InlineKeyboardButton createButton(String text, String data) {
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText(text);
        btn.setCallbackData(data);
        return btn;
    }

    private void handleGameOver(String hostId, GameSession session) {
        try {
            bingoTurnService.cancelTurnTimer(hostId);

            String gameOverUI = bingoHandler.buildGameOverUI(session);

            // Edit all players' board messages with final result
            for (Player player : session.getPlayers()) {
                if (player.getIsBot()) continue;
                Integer msgId = bingoHandler.getPlayerBoardMsgId(player.getChatId());
                InlineKeyboardMarkup gameOverBoard = bingoHandler.buildGameOverKeyboard(session, player);

                if (msgId != null) {
                    try {
                        EditMessageText edit = new EditMessageText();
                        edit.setChatId(player.getChatId().toString());
                        edit.setMessageId(msgId);
                        edit.setText(gameOverUI);
                        edit.setReplyMarkup(gameOverBoard);
                        edit.setParseMode("Markdown");
                        execute(edit);
                    } catch (Exception e) {
                        // If edit fails, send new
                        SendMessage msg = new SendMessage();
                        msg.setChatId(player.getChatId().toString());
                        msg.setText(gameOverUI);
                        msg.setParseMode("Markdown");
                        execute(msg);
                    }
                }
            }

            // Cleanup
            bingoHandler.clearAllPlayerData(session);
            bingoGameService.cleanupGame(hostId);

        } catch (Exception e) {
            log.error("Error handling game over", e);
        }
    }

    private boolean isGameExpired(String hostId, Long chatId, AnswerCallbackQuery answer) {
        try {
            if (hostId == null || hostId.isEmpty()) {
                answer.setText("⚠️ Session expired! Send /bingo to start.");
                execute(answer);
                return true;
            }

            GameSession session = bingoGameService.getGame(hostId);

            if (session == null || session.getStatus() == BingoEnums.GameStatus.FINISHED) {
                answer.setText("⚠️ Session expired! Send /bingo to start.");
                execute(answer);
                return true;
            }

            return false;
        } catch (Exception e) {
            return true;
        }
    }

}
