package com.telegram.bot.service;

import com.telegram.bot.entity.GameMove;
import com.telegram.bot.entity.GameSession;
import com.telegram.bot.entity.Player;
import com.telegram.bot.entity.PlayerBoard;
import com.telegram.bot.repository.GameMoveRepository;
import com.telegram.bot.repository.GameSessionRepository;
import com.telegram.bot.repository.PlayerBoardRepository;
import com.telegram.bot.repository.PlayerRepository;
import com.telegram.bot.utils.BingoEnums;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@Transactional
public class BingoGameService {
    @Autowired
    private GameSessionRepository gameSessionRepo;
    @Autowired
    private PlayerRepository playerRepo;
    @Autowired
    private PlayerBoardRepository boardRepo;
    @Autowired
    private GameMoveRepository moveRepo;
    @Autowired
    private BingoBoardService boardService;

    private final Random random = new Random();


    public String addBots(String hostId) {
        GameSession session = gameSessionRepo.findByHostId(hostId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        int currentPlayers = session.getPlayers().size();

        if (currentPlayers >= 6) {
            return "FULL"; // Already at max
        }

        // Count existing bots
        long botCount = session.getPlayers().stream()
                .filter(Player::getIsBot)
                .count();

        int botNumber = (int) botCount + 1;

        Player bot = new Player();
        bot.setChatId(-1L * System.currentTimeMillis()); // Unique negative ID
        bot.setPlayerName("Bot" + botNumber);
        bot.setIsBot(true);
        bot.setTurnOrder(currentPlayers);
        bot.setGameSession(session);
        bot.setStatus(BingoEnums.PlayerStatus.WAITING);
        playerRepo.save(bot);

        return "SUCCESS";
    }

    // ========== HOST CREATION ==========

    public GameSession createHost(Long chatId, String hostName) {
        // Generate unique 4-digit code
        String hostId = generateUniqueHostId();

        GameSession session = new GameSession();
        session.setHostId(hostId);
        session.setHostChatId(chatId);
        session.setStatus(BingoEnums.GameStatus.WAITING);
        session = gameSessionRepo.save(session);

        // Add host as first player
        Player host = new Player();
        host.setChatId(chatId);
        host.setPlayerName(hostName);
        host.setIsHost(true);
        host.setTurnOrder(0);
        host.setGameSession(session);
        host.setStatus(BingoEnums.PlayerStatus.WAITING);
        playerRepo.save(host);

        session.getPlayers().add(host);
        return session;
    }

    private String generateUniqueHostId() {
        String hostId;
        do {
            hostId = String.format("%04d", random.nextInt(10000));
        } while (gameSessionRepo.findByHostId(hostId).isPresent());
        return hostId;
    }

    // ========== JOIN PARTY ==========

    public String joinParty(String hostId, Long chatId, String playerName) {
        Optional<GameSession> optSession = gameSessionRepo.findByHostId(hostId);

        if (optSession.isEmpty()) return "INVALID";

        GameSession session = optSession.get();

        if (session.getStatus() != BingoEnums.GameStatus.WAITING) return "STARTED";
        if (session.getPlayers().size() >= 6) return "FULL";

        Optional<Player> existing = playerRepo.findByChatIdAndGameSession(chatId, session);
        if (existing.isPresent()) return "ALREADY_JOINED";

        Player player = new Player();
        player.setChatId(chatId);
        player.setPlayerName(playerName);
        player.setTurnOrder(session.getPlayers().size());
        player.setGameSession(session);
        player.setStatus(BingoEnums.PlayerStatus.WAITING);
        playerRepo.save(player);

        return "SUCCESS";
    }

    // ========== ADD BOT PLAYER ==========

    public Player addBotPlayer(GameSession session) {
        Player bot = new Player();
        bot.setChatId(-1L); // Bot has no real chat
        bot.setPlayerName("🤖 Bot");
        bot.setIsBot(true);
        bot.setTurnOrder(session.getPlayers().size());
        bot.setGameSession(session);
        bot.setStatus(BingoEnums.PlayerStatus.WAITING);
        return playerRepo.save(bot);
    }

    // ========== START GAME ==========

    public GameSession startGame(String hostId, int boardSize) {
        GameSession session = gameSessionRepo.findByHostId(hostId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        session.setBoardSize(boardSize);
        session.setStatus(BingoEnums.GameStatus.PLAYING);
        session.setCurrentTurnIndex(0);
        session.setTurnStartTime(LocalDateTime.now());

        for (Player player : session.getPlayers()) {
            player.setStatus(BingoEnums.PlayerStatus.PLAYING);
            player.setScore(0);
            playerRepo.save(player);

            // Check if board already exists (prevents duplicate)
            Optional<PlayerBoard> existingBoard = boardRepo.findByPlayerId(player.getId());
            if (existingBoard.isPresent()) {
                // Update existing board
                PlayerBoard playerBoard = existingBoard.get();
                int[][] board = boardService.generateBoard(boardSize);
                boolean[][] markedArr = boardService.createEmptyMarked(boardSize);
                playerBoard.setBoardArray(board);
                playerBoard.setMarkedArray(markedArr);
                playerBoard.setCompletedPatterns("");
                boardRepo.save(playerBoard);
            } else {
                // Create new board
                PlayerBoard playerBoard = new PlayerBoard();
                playerBoard.setPlayer(player);
                int[][] board = boardService.generateBoard(boardSize);
                boolean[][] markedArr = boardService.createEmptyMarked(boardSize);
                playerBoard.setBoardArray(board);
                playerBoard.setMarkedArray(markedArr);
                playerBoard.setCompletedPatterns("");
                boardRepo.save(playerBoard);
            }
        }

        return gameSessionRepo.save(session);
    }

    // ========== MAKE A MOVE ==========


    public synchronized Map<String, Object> makeMove(String hostId, Long chatId, int selectedNumber) {
        Map<String, Object> result = new HashMap<>();

        GameSession session = gameSessionRepo.findByHostId(hostId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        if (session.getStatus() != BingoEnums.GameStatus.PLAYING) {
            result.put("status", "NOT_PLAYING");
            return result;
        }

        Player currentPlayer = session.getCurrentTurnPlayer();
        if (currentPlayer == null || !currentPlayer.getChatId().equals(chatId)) {
            result.put("status", "NOT_YOUR_TURN");
            return result;
        }

        if (session.getAllSelectedNumbers().contains(selectedNumber)) {
            result.put("status", "ALREADY_SELECTED");
            return result;
        }

        int maxNumber = session.getBoardSize() * session.getBoardSize();
        if (selectedNumber < 1 || selectedNumber > maxNumber) {
            result.put("status", "INVALID_NUMBER");
            return result;
        }

        // Record move
        GameMove move = new GameMove();
        move.setGameSession(session);
        move.setPlayer(currentPlayer);
        move.setSelectedNumber(selectedNumber);
        move.setMoveOrder(session.getMoves().size() + 1);
        move.setAutoSelected(false);
        moveRepo.save(move);
        session.getMoves().add(move);

        // Mark on ALL boards and check wins
        // CRITICAL: Process current player FIRST for race condition
        List<Player> allPlayers = new ArrayList<>(session.getPlayers());

        // Sort: current player first, then others
        allPlayers.sort((a, b) -> {
            if (a.getId().equals(currentPlayer.getId())) return -1;
            if (b.getId().equals(currentPlayer.getId())) return 1;
            return 0;
        });

        int pointsToWin = BingoEnums.BoardSize.fromSize(session.getBoardSize()).getPointsToWin();
        boolean someoneJustWon = false;

        for (Player player : allPlayers) {
            PlayerBoard board = boardRepo.findByPlayerId(player.getId()).orElse(null);
            if (board == null) continue;

            boardService.markNumberOnBoard(board, selectedNumber);
            int newPoints = boardService.checkNewPatterns(board);

            if (newPoints > 0) {
                player.setScore(player.getScore() + newPoints);
                playerRepo.save(player);

                // Check win — RACE CONDITION FIX
                // If someone already won this turn, DON'T process others winning
                if (!someoneJustWon && player.getScore() >= pointsToWin
                        && player.getStatus() == BingoEnums.PlayerStatus.PLAYING) {

                    int rank = (int) session.getPlayers().stream()
                            .filter(p -> p.getStatus() == BingoEnums.PlayerStatus.WON)
                            .count() + 1;
                    player.setStatus(BingoEnums.PlayerStatus.WON);
                    player.setFinishRank(rank);
                    playerRepo.save(player);
                    someoneJustWon = true;
                }
            }
            boardRepo.save(board);
        }

        // Check game over
        List<Player> stillPlaying = session.getPlayers().stream()
                .filter(p -> p.getStatus() == BingoEnums.PlayerStatus.PLAYING)
                .toList();

        if (stillPlaying.size() <= 1) {
            if (stillPlaying.size() == 1) {
                Player loser = stillPlaying.get(0);
                loser.setStatus(BingoEnums.PlayerStatus.LOST);
                loser.setFinishRank(session.getPlayers().size());
                playerRepo.save(loser);
            }
            session.setStatus(BingoEnums.GameStatus.FINISHED);
            result.put("gameOver", true);
        } else {
            session.advanceTurn();
            result.put("gameOver", false);
        }

        gameSessionRepo.save(session);

        result.put("status", "SUCCESS");
        result.put("selectedNumber", selectedNumber);
        result.put("selectedBy", currentPlayer.getPlayerName());
        result.put("nextPlayer", session.getCurrentTurnPlayer());

        return result;
    }

    // ========== AUTO MOVE (Timer Expired) ==========

    public synchronized Map<String, Object> autoMove(String hostId) {
        GameSession session = gameSessionRepo.findByHostId(hostId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        if (session.getStatus() != BingoEnums.GameStatus.PLAYING) {
            return Map.of("status", "NOT_PLAYING");
        }

        Player currentPlayer = session.getCurrentTurnPlayer();
        if (currentPlayer == null) return Map.of("status", "NO_PLAYER");

        Set<Integer> selected = session.getAllSelectedNumbers();
        int maxNumber = session.getBoardSize() * session.getBoardSize();
        List<Integer> available = new ArrayList<>();
        for (int i = 1; i <= maxNumber; i++) {
            if (!selected.contains(i)) available.add(i);
        }

        if (available.isEmpty()) return Map.of("status", "NO_NUMBERS");

        int randomNumber = available.get(random.nextInt(available.size()));

        // Make the move
        Map<String, Object> result = makeMove(hostId, currentPlayer.getChatId(), randomNumber);

        // Mark as auto-selected using direct query (avoids null player issue)
        if ("SUCCESS".equals(result.get("status"))) {
            GameSession updatedSession = gameSessionRepo.findByHostId(hostId).orElse(null);
            if (updatedSession != null && !updatedSession.getMoves().isEmpty()) {
                GameMove lastMove = updatedSession.getMoves().stream()
                        .max(Comparator.comparingInt(GameMove::getMoveOrder))
                        .orElse(null);
                if (lastMove != null) {
                    moveRepo.markAsAutoSelected(lastMove.getId());
                }
            }
        }

        result.put("autoMove", true);
        result.put("timedOutPlayer", currentPlayer.getPlayerName());
        return result;
    }
    // ========== BOT MOVE ==========

    public Map<String, Object> botMove(String hostId) {
        return autoMove(hostId); // Same logic as auto-move
    }

    // ========== STOP GAME ==========

    public void stopGame(String hostId) {
        GameSession session = gameSessionRepo.findByHostId(hostId)
                .orElseThrow(() -> new RuntimeException("Game not found"));
        session.setStatus(BingoEnums.GameStatus.FINISHED);
        gameSessionRepo.save(session);
    }

    // ========== GET GAME ==========

    public GameSession getGame(String hostId) {
        return gameSessionRepo.findByHostId(hostId).orElse(null);
    }

    // ========== RANKINGS ==========

    public String buildRankings(GameSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("🏆 *RANKINGS*\n\n");

        List<Player> ranked = session.getPlayers().stream()
                .filter(p -> p.getFinishRank() != null)
                .sorted(Comparator.comparingInt(Player::getFinishRank))
                .toList();

        String[] medals = {"🥇", "🥈", "🥉", "4️⃣", "5️⃣", "6️⃣"};

        for (Player p : ranked) {
            String medal = p.getFinishRank() <= medals.length ?
                    medals[p.getFinishRank() - 1] : "  ";
            String status = p.getStatus() == BingoEnums.PlayerStatus.LOST ? " (💀 Loser)" : " (Winner!)";
            sb.append(medal).append(" ").append(p.getPlayerName())
                    .append(" - Score: ").append(p.getScore())
                    .append(status).append("\n");
        }

        return sb.toString();
    }

    // ========== FIND PLAYER'S ACTIVE GAME ==========

    public GameSession findActiveGame(Long chatId) {
        return playerRepo.findByChatIdAndGameSession_Status(chatId, BingoEnums.GameStatus.PLAYING)
                .map(Player::getGameSession)
                .orElse(null);
    }

    @Transactional
    public void cleanupGame(String hostId) {
        try {
            GameSession session = gameSessionRepo.findByHostId(hostId).orElse(null);
            if (session == null) return;

            Long sessionId = session.getId();

            // Delete in correct order using native queries (avoids cascade issues)
            moveRepo.deleteByGameSessionId(sessionId);
            boardRepo.deleteByPlayerGameSessionId(sessionId);
            playerRepo.deleteByGameSessionId(sessionId);
            gameSessionRepo.deleteBySessionId(sessionId);


            log.info("Cleaned up game: {}", hostId);
        } catch (Exception e) {
            log.error("Error cleaning up game: {}", hostId, e);
        }
    }
}



