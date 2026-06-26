package com.telegram.bot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs for SOS Game API requests and responses
 */
public class SOSGameDTO {

    /**
     * Request body for saving game score
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaveScoreRequest {
        private Long userId;
        private String playerName;
        private Integer score;
        private Integer opponentScore;
        private Integer boardSize;
        private Boolean isWin;
        private String timestamp;
    }

    /**
     * Response after saving score
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GameScoreResponse {
        private Boolean success;
        private String scoreId;
        private String savedAt;
        private Integer newRank;
        private String message;
    }

    /**
     * Player progress response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressResponse {
        private Long userId;
        private String playerName;
        private Integer wins;
        private Integer gamesPlayed;
        private Double averageScore;
        private Integer totalScore;
        private Integer currentRank;
        private String lastPlayedDate;
        private Integer longestWinStreak;
    }

    /**
     * Leaderboard entry
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaderboardEntry {
        private Integer rank;
        private String playerName;
        private Integer score;
        private Integer wins;
        private Integer gamesPlayed;
        private Double winRate;
        private Double averageScore;
        private String lastPlayedDate;
    }

    /**
     * Player inventory response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryResponse {
        private Long userId;
        private String playerName;
        private List<InventoryItem> items;
        private Integer totalCoins;
        private Integer totalGems;
    }

    /**
     * Inventory item
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryItem {
        private String itemId;
        private String itemName;
        private String itemType; // POWER_UP, HINT, BOOST, etc.
        private Integer quantity;
        private Long acquiredAt;
    }

    /**
     * Request for updating inventory
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateInventoryRequest {
        private List<InventoryItem> items;
        private Integer coinsSpent;
        private Integer gemsSpent;
    }

    /**
     * Generic API response wrapper
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiResponse {
        private Boolean success;
        private String message;
        private Object data;
        private LocalDateTime timestamp;
    }
}
