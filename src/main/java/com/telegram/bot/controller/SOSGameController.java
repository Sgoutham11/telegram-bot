package com.telegram.bot.controller;

import com.telegram.bot.dto.SOSGameDTO;
import com.telegram.bot.service.SOSGameService;
import com.telegram.bot.service.TelegramInitDataValidator;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for SOS Game Mini App
 * Handles game score tracking, player progress, and leaderboards
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Log4j2
public class SOSGameController {

    @Autowired
    private SOSGameService sosGameService;

    @Autowired
    private TelegramInitDataValidator initDataValidator;

    /**
     * Save game score after a game ends
     * POST /api/game/score
     */
    @PostMapping("/game/score")
    public ResponseEntity<?> saveGameScore(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SOSGameDTO.SaveScoreRequest scoreData) {
        try {
            log.info("Inside Saving game score-------------:=>");
            Long userId = extractUserIdFromAuth(authHeader);
            SOSGameDTO.GameScoreResponse response = sosGameService.saveGameScore(userId, scoreData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
           log.error("Error while saving the game soure ",e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to save score: " + e.getMessage()));
        }
    }

    /**
     * Get player's game progress and statistics
     * GET /api/game/progress
     */
    @GetMapping("/game/progress")
    public ResponseEntity<?> getGameProgress(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            log.info("Inside Game Progress -------------:=>");
            Long userId = extractUserIdFromAuth(authHeader);
            SOSGameDTO.ProgressResponse progress = sosGameService.getPlayerProgress(userId);
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch progress: " + e.getMessage()));
        }
    }

    /**
     * Get top players leaderboard
     * GET /api/leaderboard?limit=10
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<?> getLeaderboard(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            log.info("Inside leaderboard-------------:=>");
            List<SOSGameDTO.LeaderboardEntry> leaderboard = sosGameService.getLeaderboard(limit);
            return ResponseEntity.ok(Map.of("leaderboard", leaderboard));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch leaderboard: " + e.getMessage()));
        }
    }

    /**
     * Get player's inventory/items
     * GET /api/inventory
     */
    @GetMapping("/inventory")
    public ResponseEntity<?> getInventory(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            log.info("Inside Inventory-------------:=>");

            Long userId = extractUserIdFromAuth(authHeader);
            SOSGameDTO.InventoryResponse inventory = sosGameService.getPlayerInventory(userId);
            return ResponseEntity.ok(inventory);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch inventory: " + e.getMessage()));
        }
    }

    /**
     * Update player's inventory/items
     * POST /api/inventory/update
     */
    @PostMapping("/inventory/update")
    public ResponseEntity<?> updateInventory(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SOSGameDTO.UpdateInventoryRequest inventoryData) {
        try {
            log.info("Inside Inventory update-------------:=>");

            Long userId = extractUserIdFromAuth(authHeader);
            SOSGameDTO.InventoryResponse inventory = sosGameService.updatePlayerInventory(userId, inventoryData);
            return ResponseEntity.ok(inventory);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to update inventory: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        try {
            log.info("Inside health -------------:=>");
            return ResponseEntity.ok(Map.of(
                "status", "OK",
                "service", "SOS Game API",
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "ERROR",
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Extract user ID from Authorization header
     * Format: Bearer <initData> where initData contains user ID
     */
    private Long extractUserIdFromAuth(String authHeader) throws Exception {
        log.info("Extract user ID -------------:=>");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new Exception("Invalid Authorization header");
        }
        
        String initData = authHeader.substring(7);
        return initDataValidator.validateAndExtractUserId(initData)
                .orElseThrow(() -> new Exception("Invalid Telegram initData"));
    }
}
