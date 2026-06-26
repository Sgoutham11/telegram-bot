package com.telegram.bot.controller;

import com.telegram.bot.dto.SOSGameDTO;
import com.telegram.bot.service.GameMatchService;
import com.telegram.bot.service.SOSGameService;
import com.telegram.bot.service.TelegramInitDataValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for SOS Game match lifecycle
 * Endpoints: player registration, match start/end/move, history, leaderboard
 */
@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameMatchController {

    private final GameMatchService gameMatchService;
    private final SOSGameService sosGameService;
    private final TelegramInitDataValidator initDataValidator;

    @PostMapping("/player")
    public ResponseEntity<?> registerPlayer(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> body) {
        try {
            Long userId = extractUserId(authHeader);
            String displayName = body.getOrDefault("displayName", "Player");
            return ResponseEntity.ok(gameMatchService.registerPlayer(userId, displayName));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/match/start")
    public ResponseEntity<?> startMatch(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body) {
        try {
            Long userId = extractUserId(authHeader);
            return ResponseEntity.ok(gameMatchService.startMatch(userId, body));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/match/end")
    public ResponseEntity<?> endMatch(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body) {
        try {
            extractUserId(authHeader);
            Long matchId = ((Number) body.get("matchId")).longValue();
            return ResponseEntity.ok(gameMatchService.endMatch(matchId, body));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/match/move")
    public ResponseEntity<?> recordMove(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body) {
        try {
            extractUserId(authHeader);
            Long matchId = ((Number) body.get("matchId")).longValue();
            return ResponseEntity.ok(gameMatchService.recordMove(matchId, body));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            Long userId = extractUserId(authHeader);
            return ResponseEntity.ok(Map.of("history", gameMatchService.getMatchHistory(userId, limit)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<?> getLeaderboard(
            @RequestParam(defaultValue = "10") int limit) {
        List<SOSGameDTO.LeaderboardEntry> leaderboard = gameMatchService.getLeaderboard(limit);
        for (int i = 0; i < leaderboard.size(); i++) {
            leaderboard.get(i).setRank(i + 1);
        }
        return ResponseEntity.ok(Map.of("leaderboard", leaderboard));
    }

    private Long extractUserId(String authHeader) throws Exception {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new Exception("Invalid Authorization header");
        }
        String initData = authHeader.substring(7);
        return initDataValidator.validateAndExtractUserId(initData)
                .orElseThrow(() -> new Exception("Invalid Telegram initData"));
    }
}
