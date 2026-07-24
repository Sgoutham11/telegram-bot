package com.telegram.bot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

public class LeaderboardDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaderboardPlayer {
        private Integer rank;
        private Long playerId;
        private String playerName;
        private String gameType;
        private Integer totalWins;
        private Integer totalLosses;
        private Integer totalGames;
        private Double winLossRatio;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BestByGameResponse {
        private Map<String, LeaderboardPlayer> bestPlayers;
    }
}
