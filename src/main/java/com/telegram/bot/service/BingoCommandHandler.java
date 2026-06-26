package com.telegram.bot.service;


import com.telegram.bot.entity.*;
import com.telegram.bot.repository.PlayerBoardRepository;
import com.telegram.bot.repository.PlayerProfileRepository;
import com.telegram.bot.utils.BingoEnums;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@Transactional
public class BingoCommandHandler {

    @Autowired
    private BingoGameService gameService;

    @Autowired
    private BingoBoardService boardService;

    @Autowired
    private PlayerBoardRepository boardRepo;

    @Autowired
    private PlayerProfileRepository profileRepo;

    // Pending actions
    private final Map<Long, String> pendingAction = new ConcurrentHashMap<>();

    // chatId -> hostId
    private final Map<Long, String> playerGameMap = new ConcurrentHashMap<>();

    // chatId -> messageId (for single-message UI)
    private final Map<Long, Integer> playerBoardMessageId = new ConcurrentHashMap<>();

    // ==================== WELCOME MENU ====================

    public SendMessage showWelcomeMenu(Long chatId) {
        // Check if user exists
        Optional<PlayerProfile> profile = profileRepo.findByChatId(chatId);

        StringBuilder sb = new StringBuilder();
        sb.append("🎉 *Welcome to Game World* 🎮\n\n");

        if (profile.isPresent()) {
            sb.append("👋 Hello, *").append(profile.get().getPlayerName()).append("*!\n\n");
        }

        sb.append("✨ Select your game below ✨\n");
//        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText(sb.toString());
        msg.setParseMode("Markdown");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(List.of(createButton("🎯 Bingo", "bingo_play")));

        markup.setKeyboard(keyboard);
        msg.setReplyMarkup(markup);
        return msg;
    }

    // ==================== PLAY MENU ====================

    public SendMessage showPlayMenu(Long chatId) {
        StringBuilder sb = new StringBuilder();
        sb.append("🎮 *BINGO*\n\n");
//        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("Choose an option:\n");

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText(sb.toString());
        msg.setParseMode("Markdown");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(List.of(createButton("👑 Create Game", "bingo_create")));
        keyboard.add(List.of(createButton("🎯 Join Party", "bingo_join")));
        keyboard.add(List.of(createButton("🔙 Back", "bingo_back")));

        markup.setKeyboard(keyboard);
        msg.setReplyMarkup(markup);
        return msg;
    }

    // ==================== NAME REGISTRATION ====================

    public SendMessage askPlayerName(Long chatId, String nextAction) {
        pendingAction.put(chatId, "REGISTER_NAME_" + nextAction);

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("👤 *What's your name?*\n\nEnter your player name:");
        msg.setParseMode("Markdown");
        return msg;
    }

    public void registerPlayer(Long chatId, String name) {
        Optional<PlayerProfile> existing = profileRepo.findByChatId(chatId);
        if (existing.isPresent()) {
            PlayerProfile profile = existing.get();
            profile.setPlayerName(name);
            profileRepo.save(profile);
        } else {
            PlayerProfile profile = new PlayerProfile();
            profile.setChatId(chatId);
            profile.setPlayerName(name);
            profileRepo.save(profile);
        }
    }

    public String getPlayerName(Long chatId) {
        return profileRepo.findByChatId(chatId)
                .map(PlayerProfile::getPlayerName)
                .orElse(null);
    }

    // ==================== HOST CREATION ====================

    public SendMessage handleCreateHost(Long chatId) {
        String playerName = getPlayerName(chatId);

        if (playerName == null) {
            return askPlayerName(chatId, "CREATE");
        }

        return createHostGame(chatId, playerName);
    }

    public SendMessage createHostGame(Long chatId, String hostName) {
        GameSession session = gameService.createHost(chatId, hostName);
        playerGameMap.put(chatId, session.getHostId());

        return buildLobbyMessage(chatId, session);
    }

    public SendMessage buildLobbyMessage(Long chatId, GameSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("👑 *GAME LOBBY*\n\n");
//        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("🔑 Host ID:").append(session.getHostId()).append("\n");
        sb.append("📋 Share this ID with friends!\n\n");
//        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        sb.append("👥 *Players* (").append(session.getPlayers().size()).append("/6):\n");

        int i = 1;
        for (Player p : session.getPlayers()) {
            String icon = p.getIsHost() ? "👑" : p.getIsBot() ? "🤖" : "👤";
            sb.append(icon).append(" ").append(i++).append(". ").append(p.getPlayerName()).append("\n");
        }

//        sb.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━");

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText(sb.toString());
        msg.setParseMode("Markdown");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(List.of(createButton("🤖 Add Bot", "bingo_addbot_" + session.getHostId())));
        keyboard.add(List.of(createButton("🔄 Refresh", "bingo_refresh_" + session.getHostId())));
        keyboard.add(List.of(createButton("🚀 Start Game", "bingo_startmenu_" + session.getHostId())));
        keyboard.add(List.of(createButton("❌ Cancel", "bingo_cancel_" + session.getHostId())));

        markup.setKeyboard(keyboard);
        msg.setReplyMarkup(markup);
        return msg;
    }

    // ==================== JOIN PARTY ====================

    public SendMessage handleJoinParty(Long chatId) {
        String playerName = getPlayerName(chatId);

        if (playerName == null) {
            return askPlayerName(chatId, "JOIN");
        }

        pendingAction.put(chatId, "WAITING_HOST_ID");

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
//        msg.setText("🎯 *Join Party*\n\n━━━━━━━━━━━━━━━━━━━━━━━━━━\nEnter the 4-digit Host ID:");
        msg.setText("🎯 *Join Party*\n\nEnter the 4-digit Host ID:");

        msg.setParseMode("Markdown");
        return msg;
    }

    public SendMessage processHostId(Long chatId, String hostId) {
        pendingAction.remove(chatId);

        String playerName = getPlayerName(chatId);
        String result = gameService.joinParty(hostId, chatId, playerName);

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());

        switch (result) {
            case "SUCCESS":
                playerGameMap.put(chatId, hostId);
                msg.setText("✅ *Joined Successfully!*\n\n" +
//                        "━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                "Welcome, *" + playerName + "*!\n" +
                                "⏳ Waiting for host to start...\n"
//                        "━━━━━━━━━━━━━━━━━━━━━━━━━━"
                );

                break;
            case "FULL":
                msg.setText("❌ Party is full! (Max 6 players)");
                break;
            case "ALREADY_JOINED":
                msg.setText("⚠️ You already joined this game!");
                break;
            case "INVALID":
                msg.setText("❌ *Invalid Host ID!*\n\nCheck the ID and try again.");
                break;
            case "STARTED":
                msg.setText("❌ *Game already started!*\n\nCannot join now.");
                break;
            default:
                msg.setText("❌ Could not join. Try again.");
        }
        msg.setParseMode("Markdown");
        return msg;
    }

    // ==================== BOARD SIZE SELECTION ====================

    public SendMessage showBoardSizeMenu(Long chatId, String hostId) {
        // Validate minimum players
        GameSession session = gameService.getGame(hostId);
        if (session.getPlayers().size() < 2) {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("⚠️ *Minimum 2 players required to start the game*\n\n" +
                    "Add bots or wait for more players!");
            msg.setParseMode("Markdown");
            return msg;
        }

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
//        msg.setText("📐 *Select Board Size*\n\n━━━━━━━━━━━━━━━━━━━━━━━━━━");
        msg.setText("📐 *Select Board Size*\n\n");

        msg.setParseMode("Markdown");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(List.of(
                createButton("5×5 (BINGO)", "bingo_size_5_" + hostId),
                createButton("6×6 (BINGOS)", "bingo_size_6_" + hostId)
        ));
        keyboard.add(List.of(
                createButton("7×7 (BINGOES)", "bingo_size_7_" + hostId),
                createButton("8×8 (BINGOESS)", "bingo_size_8_" + hostId)
        ));

        markup.setKeyboard(keyboard);
        msg.setReplyMarkup(markup);
        return msg;
    }

    // ==================== GAME UI (SINGLE MESSAGE) ====================

    public String buildGameUI(GameSession session, Player player) {
        BingoEnums.BoardSize bs = BingoEnums.BoardSize.fromSize(session.getBoardSize());
        String header = bs.getHeader();
        int score = player.getScore() != null ? player.getScore() : 0;

        PlayerBoard board = boardRepo.findByPlayerId(player.getId()).orElse(null);
        if (board == null) return "⚠️ Board not found!";

//        int[][] boardArray = board.getBoardArray();
//        boolean[][] marked = board.getMarkedArray();
//        int size = session.getBoardSize();
//        Set<Integer> selectedNumbers = session.getAllSelectedNumbers();

        StringBuilder sb = new StringBuilder();

        // Title
        sb.append("🎯 *BINGO GAME*\n\n");
//        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        // Turn info
        Player currentTurn = session.getCurrentTurnPlayer();
        if (currentTurn != null) {
            if (currentTurn.getChatId().equals(player.getChatId())) {
                sb.append("👑 Turn: *YOU!* Pick a number!\n");
            } else {
                sb.append("👑 Turn: *").append(currentTurn.getPlayerName()).append("*\n");
            }
        }
        sb.append("⏳ You have 15s to select\n\n");

        // Scores
        sb.append("👥 *Players:*\n");
        for (Player p : session.getPlayers()) {
            String icon;
            if (p.getStatus() == BingoEnums.PlayerStatus.WON) icon = "🏆";
            else if (p.getStatus() == BingoEnums.PlayerStatus.LOST) icon = "💀";
            else if (p.getChatId().equals(player.getChatId())) icon = "👤";
            else icon = "🤖";

            String turnMarker = "";
            if (currentTurn != null && p.getId().equals(currentTurn.getId())
                    && p.getStatus() == BingoEnums.PlayerStatus.PLAYING) {
                turnMarker = " ◀️";
            }

            sb.append(icon).append(" ").append(p.getPlayerName()).append(" ")
//                    .append(": ").append(p.getScore()).append("/").append(bs.getPointsToWin())
                    .append(turnMarker).append("\n");
        }

        sb.append("\n");

        // Header progress
        sb.append("📊 Progress: ");
        for (int i = 0; i < header.length(); i++) {
            if (i < score) {
                sb.append("🟢").append(header.charAt(i)).append(" ");
            } else {
                sb.append("⚪").append(header.charAt(i)).append(" ");
            }
        }
        sb.append("\n\n");

        // Last move
        if (!session.getMoves().isEmpty()) {
            GameMove lastMove = session.getMoves().stream()
                    .max(Comparator.comparingInt(GameMove::getMoveOrder))
                    .orElse(null);
            if (lastMove != null) {
                String selectedBy = lastMove.getPlayer().getPlayerName();
                if (Boolean.TRUE.equals(lastMove.getAutoSelected()) && !lastMove.getPlayer().getIsBot()) {

                    sb.append("👤").append(selectedBy).append(":")
                            .append("⚡ Auto-selected: *").append(lastMove.getSelectedNumber())
                            .append("*\n");
                } else {
                    sb.append("✅ ").append(selectedBy).append(" selected: *")
                            .append(lastMove.getSelectedNumber()).append("*\n");
                }
            }
        }

//        sb.append("\n🔢 *Board:*\n");
//        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        // Board display with formatting
//        sb.append("`");
//        for (int r = 0; r < size; r++) {
//            for (int c = 0; c < size; c++) {
//                int num = boardArray[r][c];
//                if (marked[r][c]) {
//                    sb.append(String.format("[%2d]", num));
//                } else {
//                    sb.append(String.format(" %2d ", num));
//                }
//            }
//            sb.append("\n");
//        }
//        sb.append("`");

//        sb.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
//        sb.append("✅ = Selected  |  Pick unmarked numbers!");

        return sb.toString();
    }

    public InlineKeyboardMarkup buildGameKeyboard(GameSession session, Player player) {
        int size = session.getBoardSize();
        Set<Integer> selectedNumbers = session.getAllSelectedNumbers();

        PlayerBoard board = boardRepo.findByPlayerId(player.getId()).orElse(null);
        if (board == null) return new InlineKeyboardMarkup();

        boolean[][] marked = board.getMarkedArray();
        int[][] boardArray = board.getBoardArray();

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (int r = 0; r < size; r++) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            for (int c = 0; c < size; c++) {
                int num = boardArray[r][c];
                InlineKeyboardButton btn = new InlineKeyboardButton();

                if (marked[r][c]) {
                    btn.setText("✅" + num);
                    btn.setCallbackData("bingo_noop");
                } else {
                    btn.setText(String.valueOf(num));
                    btn.setCallbackData("bingo_select_" + num + "_" + session.getHostId());
                }

                row.add(btn);
            }
            keyboard.add(row);
        }

        // Quit button
        List<InlineKeyboardButton> actionRow = new ArrayList<>();
        actionRow.add(createButton("❌ Quit Game", "bingo_quit_" + session.getHostId()));
        keyboard.add(actionRow);

        markup.setKeyboard(keyboard);
        return markup;
    }

    // ==================== GAME OVER UI ====================

    public String buildGameOverUI(GameSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("🏁 *GAME OVER!*\n");
//        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        // Winners
        sb.append("🏆 *Winners:*\n");
        String[] medals = {"🥇", "🥈", "🥉", "4️⃣", "5️⃣"};

        List<Player> winners = session.getPlayers().stream()
                .filter(p -> p.getStatus() == BingoEnums.PlayerStatus.WON)
                .sorted(Comparator.comparingInt(Player::getFinishRank))
                .toList();

        for (Player p : winners) {
            int idx = p.getFinishRank() - 1;
            String medal = idx < medals.length ? medals[idx] : "🏅";
            sb.append(medal).append(" ").append(p.getPlayerName())
                    .append(" (Score: ").append(p.getScore()).append(")\n");
        }

        // Loser
        sb.append("\n💀 *Loser:*\n");
        session.getPlayers().stream()
                .filter(p -> p.getStatus() == BingoEnums.PlayerStatus.LOST)
                .forEach(p -> sb.append("😢 ").append(p.getPlayerName()).append("\n"));

//        sb.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("Send /bingo to play again! 🎮");

        return sb.toString();
    }

    public InlineKeyboardMarkup buildGameOverKeyboard(GameSession session, Player player) {
        int size = session.getBoardSize();

        PlayerBoard board = boardRepo.findByPlayerId(player.getId()).orElse(null);
        if (board == null) return new InlineKeyboardMarkup();

        boolean[][] marked = board.getMarkedArray();
        int[][] boardArray = board.getBoardArray();

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Board rows — all buttons show "game_over" callback
        for (int r = 0; r < size; r++) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            for (int c = 0; c < size; c++) {
                int num = boardArray[r][c];
                InlineKeyboardButton btn = new InlineKeyboardButton();

                if (marked[r][c]) {
                    btn.setText("✅" + num);
                } else {
                    btn.setText(String.valueOf(num));
                }

                // ALL buttons point to game_over callback
                btn.setCallbackData("bingo_gameover");
                row.add(btn);
            }
            keyboard.add(row);
        }

        // New game button
        List<InlineKeyboardButton> actionRow = new ArrayList<>();
        actionRow.add(createButton("🎮 New Game", "bingo_newgame"));
        keyboard.add(actionRow);

        markup.setKeyboard(keyboard);
        return markup;
    }

    // ==================== TEXT INPUT HANDLER ====================

    public SendMessage handleTextInput(Long chatId, String text) {
        String action = pendingAction.get(chatId);
        if (action == null) return null;

        if (action.startsWith("REGISTER_NAME_")) {
            String nextAction = action.replace("REGISTER_NAME_", "");
            pendingAction.remove(chatId);
            registerPlayer(chatId, text);

            if (nextAction.equals("CREATE")) {
                return createHostGame(chatId, text);
            } else if (nextAction.equals("JOIN")) {
                pendingAction.put(chatId, "WAITING_HOST_ID");
                SendMessage msg = new SendMessage();
                msg.setChatId(chatId.toString());
                msg.setText("✅ Name saved: *" + text + "*\n\n" +
                        "🎯 Now enter the 4-digit Host ID:");
                msg.setParseMode("Markdown");
                return msg;
            }
        }

        if (action.equals("WAITING_HOST_ID")) {
            return processHostId(chatId, text);
        }

        return null;
    }

    // ==================== HELPERS ====================

    public boolean hasPendingAction(Long chatId) {
        return pendingAction.containsKey(chatId);
    }

    public String getPlayerGameId(Long chatId) {
        return playerGameMap.get(chatId);
    }

    public void setPlayerGameId(Long chatId, String hostId) {
        playerGameMap.put(chatId, hostId);
    }

    public void setPlayerBoardMsgId(Long chatId, Integer messageId) {
        playerBoardMessageId.put(chatId, messageId);
    }

    public Integer getPlayerBoardMsgId(Long chatId) {
        return playerBoardMessageId.get(chatId);
    }

    public void clearPlayerBoardMsgId(Long chatId) {
        playerBoardMessageId.remove(chatId);
    }

    public void clearAllPlayerData(GameSession session) {
        for (Player p : session.getPlayers()) {
            playerBoardMessageId.remove(p.getChatId());
            playerGameMap.remove(p.getChatId());
        }
    }

    private InlineKeyboardButton createButton(String text, String data) {
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText(text);
        btn.setCallbackData(data);
        return btn;
    }

    public SendMessage handleBingoCommand(Long chatId) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("🎰 *Welcome to BINGO!*\n\nChoose an option:");
        msg.setParseMode("Markdown");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(List.of(createButton("🎮 Play Bingo", "bingo_play")));
//        keyboard.add(List.of(createButton("💬 Random Talk", "bingo_chat")));

        markup.setKeyboard(keyboard);
        msg.setReplyMarkup(markup);
        return msg;
    }

    /*
    @Autowired
    private BingoGameService gameService;
    @Autowired
    private BingoBoardService boardService;
    @Autowired
    private BingoTurnService turnService;
//    @Autowired
//    private PlayerBoardRepository boardRepository;

    // Store pending states (waiting for user input)
    private final Map<Long, String> pendingAction = new ConcurrentHashMap<>();
    // chatId -> hostId mapping for active players
    private final Map<Long, String> playerGameMap = new ConcurrentHashMap<>();

    // ========== MAIN MENU ==========

    public SendMessage handleBingoCommand(Long chatId) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("🎰 *Welcome to BINGO!*\n\nChoose an option:");
        msg.setParseMode("Markdown");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(List.of(createButton("🎮 Play Bingo", "bingo_play")));
//        keyboard.add(List.of(createButton("💬 Random Talk", "bingo_chat")));

        markup.setKeyboard(keyboard);
        msg.setReplyMarkup(markup);
        return msg;
    }

    // ========== PLAY MENU ==========

    public SendMessage handlePlayMenu(Long chatId) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("🎮 *Game Mode*\n\nCreate a new game or join an existing one:");
        msg.setParseMode("Markdown");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(List.of(createButton("👑 Create Host", "bingo_create")));
        keyboard.add(List.of(createButton("🎯 Join Party", "bingo_join")));
        keyboard.add(List.of(createButton("🔙 Back", "bingo_back")));

        markup.setKeyboard(keyboard);
        msg.setReplyMarkup(markup);
        return msg;
    }

    // ========== HOST CREATION ==========

    public SendMessage handleCreateHost(Long chatId) {
        pendingAction.put(chatId, "WAITING_HOST_NAME");

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("👑 *Create New Game*\n\nEnter your player name:");
        msg.setParseMode("Markdown");
        return msg;
    }

    public SendMessage processHostName(Long chatId, String hostName) {
        pendingAction.remove(chatId);

        GameSession session = gameService.createHost(chatId, hostName);
        playerGameMap.put(chatId, session.getHostId());

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("👑 *Game Created!*\n\n" +
                "🔑 Host ID: *" + session.getHostId() + "*\n" +
                "Share this ID with friends to join!\n\n" +
                "👥 Players:\n" +
                "1. " + hostName + " (Host)\n\n" +
                "Waiting for players to join...");
        msg.setParseMode("Markdown");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(List.of(createButton("🤖 Play with Bot", "bingo_addbot_" + session.getHostId())));
        keyboard.add(List.of(createButton("🔄 Refresh Players", "bingo_refresh_" + session.getHostId())));
        keyboard.add(List.of(createButton("🚀 Start Game", "bingo_startmenu_" + session.getHostId())));
        keyboard.add(List.of(createButton("❌ Cancel", "bingo_cancel_" + session.getHostId())));

        markup.setKeyboard(keyboard);
        msg.setReplyMarkup(markup);
        return msg;
    }

    // ========== JOIN PARTY ==========

    public SendMessage handleJoinParty(Long chatId) {
        pendingAction.put(chatId, "WAITING_HOST_ID");

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("🎯 *Join Party*\n\nEnter the 4-digit Host ID:");
        msg.setParseMode("Markdown");
        return msg;
    }

    public SendMessage processHostId(Long chatId, String hostId) {
        GameSession session = gameService.getGame(hostId);

        if (session == null) {
            pendingAction.remove(chatId);
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("❌ *Invalid Host ID!*\n\nPlease check the ID and try again.");
            msg.setParseMode("Markdown");
            return msg;
        }

        if (session.getStatus() != BingoEnums.GameStatus.WAITING) {
            pendingAction.remove(chatId);
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("❌ *Game already started!*\n\nCannot join a game in progress.");
            msg.setParseMode("Markdown");
            return msg;
        }

        pendingAction.put(chatId, "WAITING_PLAYER_NAME_" + hostId);

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("✅ *Game Found!*\n\nEnter your player name:");
        msg.setParseMode("Markdown");
        return msg;
    }

    public SendMessage processPlayerName(Long chatId, String playerName, String hostId) {
        pendingAction.remove(chatId);

        String result = gameService.joinParty(hostId, chatId, playerName);

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());

        switch (result) {
            case "SUCCESS":
                playerGameMap.put(chatId, hostId);
                msg.setText("✅ *Joined Successfully!*\n\n" +
                        "Welcome, *" + playerName + "*!\n" +
                        "Waiting for host to start the game...");
                msg.setParseMode("Markdown");
                break;
            case "FULL":
                msg.setText("❌ Party is full! (Max 6 players)");
                break;
            case "ALREADY_JOINED":
                msg.setText("⚠️ You already joined this game!");
                break;
            default:
                msg.setText("❌ Could not join. Try again.");
        }
        return msg;
    }

    // ========== BOARD SIZE SELECTION ==========

    public SendMessage showBoardSizeMenu(Long chatId, String hostId) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("📐 *Select Board Size*");
        msg.setParseMode("Markdown");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(List.of(
                createButton("5×5", "bingo_size_5_" + hostId),
                createButton("6×6", "bingo_size_6_" + hostId)
        ));
        keyboard.add(List.of(
                createButton("7×7", "bingo_size_7_" + hostId),
                createButton("8×8", "bingo_size_8_" + hostId)
        ));

        markup.setKeyboard(keyboard);
        msg.setReplyMarkup(markup);
        return msg;
    }

    // ========== GAME BOARD KEYBOARD ==========

    public InlineKeyboardMarkup buildGameKeyboard(GameSession session, Player player,PlayerBoardRepository boardRepo) {
        int size = session.getBoardSize();
        Set<Integer> selectedNumbers = session.getAllSelectedNumbers();

        // Fetch board explicitly instead of player.getPlayerBoard()
        PlayerBoard board = boardRepo.findByPlayerId(player.getId()).orElse(null);
        if (board == null) return new InlineKeyboardMarkup(); // return empty keyboard

        boolean[][] marked = board.getMarkedArray();
        int[][] boardArray = board.getBoardArray();

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Board rows
        for (int r = 0; r < size; r++) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            for (int c = 0; c < size; c++) {
                int num = boardArray[r][c];
                InlineKeyboardButton btn = new InlineKeyboardButton();

                if (marked[r][c]) {
                    btn.setText("✅" + num);
                } else {
                    btn.setText(String.valueOf(num));
                }

                // Player can only select numbers on their turn
                btn.setCallbackData("bingo_select_" + num + "_" + session.getHostId());
                row.add(btn);
            }
            keyboard.add(row);
        }

        // Action row
        List<InlineKeyboardButton> actionRow = new ArrayList<>();
        actionRow.add(createButton("❌ Quit", "bingo_quit_" + session.getHostId()));
        keyboard.add(actionRow);

        markup.setKeyboard(keyboard);
        return markup;
    }

    public String buildGameStatus(GameSession session, Player player, PlayerBoardRepository boardRepo) {
        BingoEnums.BoardSize bs = BingoEnums.BoardSize.fromSize(session.getBoardSize());
        String header = bs.getHeader();


        StringBuilder sb = new StringBuilder();
        sb.append("🎰 *BINGO* - Game: ").append(session.getHostId()).append("\n\n");

        // Header progress
        sb.append("  ");
        for (int i = 0; i < header.length(); i++) {
            if (i < player.getScore()) {
                sb.append("🟢").append(header.charAt(i)).append(" ");
            } else {
                sb.append("⚪").append(header.charAt(i)).append(" ");
            }
        }
        sb.append("\n\n");

        // Current turn
        Player currentTurn = session.getCurrentTurnPlayer();
        if (currentTurn != null) {
            if (currentTurn.getChatId().equals(player.getChatId())) {
                sb.append("👉 *YOUR TURN!* Pick a number!\n\n");
            } else {
                sb.append("⏳ Waiting for *").append(currentTurn.getPlayerName()).append("*...\n\n");
            }
        }

        // Scores
        sb.append("📊 *Scores:*\n");
        for (Player p : session.getPlayers()) {
            String icon = p.getChatId().equals(player.getChatId()) ? "👤" : "👥";
            if (p.getStatus() == BingoEnums.PlayerStatus.WON) icon = "🏆";
            if (p.getStatus() == BingoEnums.PlayerStatus.LOST) icon = "💀";
            sb.append(icon).append(" ").append(p.getPlayerName())
                    .append(": ").append(p.getScore()).append("/").append(bs.getPointsToWin())
                    .append("\n");
        }

        // Last move
        if (!session.getMoves().isEmpty()) {
            GameMove lastMove = session.getMoves().get(session.getMoves().size() - 1);
            sb.append("\n📢 Last: *").append(lastMove.getSelectedNumber())
                    .append("* by ").append(lastMove.getPlayer().getPlayerName());
        }

        return sb.toString();
    }

    // ========== TEXT INPUT HANDLER ==========

    public SendMessage handleTextInput(Long chatId, String text) {
        String action = pendingAction.get(chatId);
        if (action == null) return null;

        if (action.equals("WAITING_HOST_NAME")) {
            return processHostName(chatId, text);
        }
        if (action.equals("WAITING_HOST_ID")) {
            return processHostId(chatId, text);
        }
        if (action.startsWith("WAITING_PLAYER_NAME_")) {
            String hostId = action.replace("WAITING_PLAYER_NAME_", "");
            return processPlayerName(chatId, text, hostId);
        }

        return null;
    }

    public boolean hasPendingAction(Long chatId) {
        return pendingAction.containsKey(chatId);
    }

    public String getPlayerGameId(Long chatId) {
        return playerGameMap.get(chatId);
    }

    // ========== HELPER ==========

    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText(text);
        btn.setCallbackData(callbackData);
        return btn;
    }
    private final Map<Long, Integer> playerBoardMessageId = new ConcurrentHashMap<>();

    public void setPlayerBoardMessageId(Long chatId, Integer messageId) {
        playerBoardMessageId.put(chatId, messageId);
    }

    public Integer getPlayerBoardMessageId(Long chatId) {
        return playerBoardMessageId.get(chatId);
    }

    public void clearPlayerBoardMessageId(Long chatId) {
        playerBoardMessageId.remove(chatId);
    }


     */
}



