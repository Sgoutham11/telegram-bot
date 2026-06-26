package com.telegram.bot.service;

import com.telegram.bot.entity.GameSession;
import com.telegram.bot.entity.Player;
import com.telegram.bot.repository.GameSessionRepository;
import com.telegram.bot.utils.BingoEnums;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

@Slf4j
@Service
public class BingoTurnService {
    @Autowired
    private BingoGameService gameService;

    @Autowired
    private GameSessionRepository gameSessionRepo;

    private final Map<String, ScheduledFuture<?>> turnTimers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    // Callback interface to notify bot
    private BiConsumer<String, Map<String, Object>> onAutoMoveCallback;

    public void setOnAutoMoveCallback(BiConsumer<String, Map<String, Object>> callback) {
        this.onAutoMoveCallback = callback;
    }

    public void startTurnTimer(String hostId) {
        cancelTurnTimer(hostId);

        ScheduledFuture<?> future = executor.schedule(() -> {
            try {
                GameSession session = gameService.getGame(hostId);
                if (session == null || session.getStatus() != BingoEnums.GameStatus.PLAYING) return;

                Player current = session.getCurrentTurnPlayer();
                if (current == null) return;

                log.info("Timer expired for player: {} in game: {}", current.getPlayerName(), hostId);

                Map<String, Object> result = gameService.autoMove(hostId);
                result.put("autoMove", true);
                result.put("timedOutPlayer", current.getPlayerName());

                if (onAutoMoveCallback != null) {
                    onAutoMoveCallback.accept(hostId, result);
                }

                // Start next timer if game continues
                if (!"true".equals(String.valueOf(result.get("gameOver")))) {
                    startTurnTimer(hostId);
                }

            } catch (Exception e) {
                log.error("Error in turn timer for game: {}", hostId, e);
            }
        }, 15, TimeUnit.SECONDS); // 15 seconds instead of 5 for Telegram (typing takes time)

        turnTimers.put(hostId, future);
    }

    public void cancelTurnTimer(String hostId) {
        ScheduledFuture<?> existing = turnTimers.remove(hostId);
        if (existing != null) {
            existing.cancel(false);
        }
    }

    public void cancelAllTimers() {
        turnTimers.values().forEach(f -> f.cancel(false));
        turnTimers.clear();
    }
}



