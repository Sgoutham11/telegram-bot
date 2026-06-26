package com.telegram.bot.service;

import com.telegram.bot.dto.SOSGameDTO;
import com.telegram.bot.entity.Player;
import com.telegram.bot.entity.PlayerProfile;
import com.telegram.bot.entity.SOSGameScore;
import com.telegram.bot.repository.PlayerProfileRepository;
import com.telegram.bot.repository.PlayerRepository;
import com.telegram.bot.repository.SOSGameScoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for SOS Game backend logic
 * Handles score tracking, player progress, and leaderboards
 */
@Slf4j
@Service
@Transactional
public class SOSGameService {

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private SOSGameScoreRepository sosGameScoreRepository;

    @Autowired
    private PlayerRepository playerRepository;

    /**
     * Save game score to database
     */
    public SOSGameDTO.GameScoreResponse saveGameScore(Long userId, SOSGameDTO.SaveScoreRequest scoreData) {
        try {
            log.info("Saving game score for user: {}", userId);

            // Get or create player profile
            PlayerProfile playerProfile = playerProfileRepository.findByChatId(userId)
                    .orElseGet(() -> {
                        PlayerProfile newProfile = PlayerProfile.builder()
                                .chatId(userId)
                                .playerName(scoreData.getPlayerName() != null ? scoreData.getPlayerName() : "Player_" + userId)
                                .createdAt(LocalDateTime.now())
                                .build();
                        return playerProfileRepository.save(newProfile);
                    });

            // Create game score record
            SOSGameScore gameScore = SOSGameScore.builder()
                    .chatId(userId)
                    .playerName(playerProfile.getPlayerName())
                    .score(scoreData.getScore())
                    .opponentScore(scoreData.getOpponentScore())
                    .boardSize(scoreData.getBoardSize() != null ? scoreData.getBoardSize() : 5)
                    .isWin(scoreData.getIsWin() != null ? scoreData.getIsWin() : (scoreData.getScore() > scoreData.getOpponentScore()))
                    .gamePlayedAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();

            SOSGameScore savedScore = sosGameScoreRepository.save(gameScore);
            log.info("Game score saved successfully. ID: {}", savedScore.getId());

            // Calculate new rank
            Long totalScoreSum = sosGameScoreRepository.getTotalScoreByUserId(userId);
            int newRank = sosGameScoreRepository.getUserRank(userId);

            return SOSGameDTO.GameScoreResponse.builder()
                    .success(true)
                    .scoreId(savedScore.getId().toString())
                    .savedAt(LocalDateTime.now().toString())
                    .newRank(newRank)
                    .message("Score saved successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error saving game score: {}", e.getMessage());
            return SOSGameDTO.GameScoreResponse.builder()
                    .success(false)
                    .message("Failed to save score: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Get player's game progress and statistics
     */
    public SOSGameDTO.ProgressResponse getPlayerProgress(Long userId) {
        try {
            log.info("Fetching progress for user: {}", userId);

            // Get player profile
            PlayerProfile playerProfile = playerProfileRepository.findByChatId(userId)
                    .orElse(null);

            if (playerProfile == null) {
                // Return default progress if player doesn't exist yet
                return SOSGameDTO.ProgressResponse.builder()
                        .userId(userId)
                        .playerName("New Player")
                        .wins(0)
                        .gamesPlayed(0)
                        .averageScore(0.0)
                        .totalScore(0)
                        .currentRank(0)
                        .longestWinStreak(0)
                        .build();
            }

            // Get game statistics
            Integer gamesPlayed = sosGameScoreRepository.countGamesByUserId(userId);
            if (gamesPlayed == null) gamesPlayed = 0;
            
            Integer wins = sosGameScoreRepository.countWinsByUserId(userId);
            if (wins == null) wins = 0;
            
            Long totalScore = sosGameScoreRepository.getTotalScoreByUserId(userId);
            if (totalScore == null) totalScore = 0L;
            
            Double averageScore = gamesPlayed > 0 ? totalScore.doubleValue() / gamesPlayed : 0.0;
            
            Integer currentRank = 0;
            try {
                currentRank = sosGameScoreRepository.getUserRank(userId);
                if (currentRank == null) currentRank = 0;
            } catch (Exception e) {
                log.warn("Failed to fetch user rank: {}", e.getMessage());
                currentRank = 0;
            }
            
            Integer longestWinStreak = sosGameScoreRepository.getLongestWinStreakByUserId(userId);
            if (longestWinStreak == null) longestWinStreak = 0;

            // Get last played date
            SOSGameScore lastGame = sosGameScoreRepository.findTopByChatIdOrderByGamePlayedAtDesc(userId);
            String lastPlayedDate = lastGame != null && lastGame.getGamePlayedAt() != null ? lastGame.getGamePlayedAt().toString() : null;

            return SOSGameDTO.ProgressResponse.builder()
                    .userId(userId)
                    .playerName(playerProfile.getPlayerName())
                    .wins(wins)
                    .gamesPlayed(gamesPlayed)
                    .averageScore(Math.round(averageScore * 100.0) / 100.0)
                    .totalScore(totalScore.intValue())
                    .currentRank(currentRank)
                    .lastPlayedDate(lastPlayedDate)
                    .longestWinStreak(longestWinStreak)
                    .build();

        } catch (Exception e) {
            log.error("Error fetching player progress: {}", e.getMessage());
            return SOSGameDTO.ProgressResponse.builder()
                    .userId(userId)
                    .playerName("Unknown")
                    .wins(0)
                    .gamesPlayed(0)
                    .averageScore(0.0)
                    .totalScore(0)
                    .build();
        }
    }

    /**
     * Get top players leaderboard
     */
    public List<SOSGameDTO.LeaderboardEntry> getLeaderboard(int limit) {
        try {
            log.info("Fetching leaderboard with limit: {}", limit);

            // Get all unique players and their stats, sorted by total score descending
            List<SOSGameDTO.LeaderboardEntry> leaderboard = sosGameScoreRepository.findTop(limit)
                    .stream()
                    .map(this::mapToLeaderboardEntry)
                    .collect(Collectors.toList());

            // Add ranks
            for (int i = 0; i < leaderboard.size(); i++) {
                leaderboard.get(i).setRank(i + 1);
            }

            log.info("Leaderboard fetched successfully. Size: {}", leaderboard.size());
            return leaderboard;

        } catch (Exception e) {
            log.error("Error fetching leaderboard: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Map game score to leaderboard entry
     */
    private SOSGameDTO.LeaderboardEntry mapToLeaderboardEntry(Object[] result) {
        String playerName = (String) result[0];
        Long chatId = (Long) result[1];
        
        // Handle BigDecimal or Number types from Oracle aggregations
        Long totalScore = 0L;
        if (result[2] != null) {
            Object scoreObj = result[2];
            if (scoreObj instanceof java.math.BigDecimal) {
                totalScore = ((java.math.BigDecimal) scoreObj).longValue();
            } else if (scoreObj instanceof Long) {
                totalScore = (Long) scoreObj;
            } else if (scoreObj instanceof Number) {
                totalScore = ((Number) scoreObj).longValue();
            }
        }
        
        Long wins = 0L;
        if (result[3] != null) {
            Object winsObj = result[3];
            if (winsObj instanceof java.math.BigDecimal) {
                wins = ((java.math.BigDecimal) winsObj).longValue();
            } else if (winsObj instanceof Long) {
                wins = (Long) winsObj;
            } else if (winsObj instanceof Number) {
                wins = ((Number) winsObj).longValue();
            }
        }
        
        Long gamesPlayed = 0L;
        if (result[4] != null) {
            Object gamesObj = result[4];
            if (gamesObj instanceof java.math.BigDecimal) {
                gamesPlayed = ((java.math.BigDecimal) gamesObj).longValue();
            } else if (gamesObj instanceof Long) {
                gamesPlayed = (Long) gamesObj;
            } else if (gamesObj instanceof Number) {
                gamesPlayed = ((Number) gamesObj).longValue();
            }
        }
        
        LocalDateTime lastPlayedDate = (LocalDateTime) result[5];

        Double winRate = gamesPlayed > 0 ? (wins.doubleValue() / gamesPlayed.doubleValue()) * 100 : 0.0;
        Double averageScore = gamesPlayed > 0 ? totalScore.doubleValue() / gamesPlayed.doubleValue() : 0.0;

        return SOSGameDTO.LeaderboardEntry.builder()
                .playerName(playerName)
                .score(totalScore.intValue())
                .wins(wins.intValue())
                .gamesPlayed(gamesPlayed.intValue())
                .winRate(Math.round(winRate * 100.0) / 100.0)
                .averageScore(Math.round(averageScore * 100.0) / 100.0)
                .lastPlayedDate(lastPlayedDate != null ? lastPlayedDate.toString() : null)
                .build();
    }

    /**
     * Get player's inventory
     */
    public SOSGameDTO.InventoryResponse getPlayerInventory(Long userId) {
        try {
            log.info("Fetching inventory for user: {}", userId);

            PlayerProfile playerProfile = playerProfileRepository.findByChatId(userId)
                    .orElse(null);

            // For now, return empty inventory
            // This can be extended to store actual inventory items in a separate table
            return SOSGameDTO.InventoryResponse.builder()
                    .userId(userId)
                    .playerName(playerProfile != null ? playerProfile.getPlayerName() : "Unknown")
                    .items(new ArrayList<>())
                    .totalCoins(0)
                    .totalGems(0)
                    .build();

        } catch (Exception e) {
            log.error("Error fetching inventory: {}", e.getMessage());
            return SOSGameDTO.InventoryResponse.builder()
                    .userId(userId)
                    .items(new ArrayList<>())
                    .build();
        }
    }

    /**
     * Update player's inventory
     */
    public SOSGameDTO.InventoryResponse updatePlayerInventory(Long userId, SOSGameDTO.UpdateInventoryRequest inventoryData) {
        try {
            log.info("Updating inventory for user: {}", userId);

            PlayerProfile playerProfile = playerProfileRepository.findByChatId(userId)
                    .orElse(null);

            // For now, just return the updated inventory
            // This can be extended to actually persist inventory items
            return SOSGameDTO.InventoryResponse.builder()
                    .userId(userId)
                    .playerName(playerProfile != null ? playerProfile.getPlayerName() : "Unknown")
                    .items(inventoryData.getItems())
                    .totalCoins(0)
                    .totalGems(0)
                    .build();

        } catch (Exception e) {
            log.error("Error updating inventory: {}", e.getMessage());
            return SOSGameDTO.InventoryResponse.builder()
                    .userId(userId)
                    .items(new ArrayList<>())
                    .build();
        }
    }
}
