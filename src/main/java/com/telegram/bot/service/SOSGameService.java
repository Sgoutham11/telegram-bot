package com.telegram.bot.service;

import com.telegram.bot.dto.SOSGameDTO;
import com.telegram.bot.entity.GameScore;
import com.telegram.bot.entity.PlayerProfile;
import com.telegram.bot.repository.GameScoreRepository;
import com.telegram.bot.repository.PlayerProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
public class SOSGameService {

    @Autowired
    private PlayerProfileRepository playerProfileRepository;

    @Autowired
    private GameScoreRepository gameScoreRepository;

    @Autowired
    private TelegramInitDataValidator initDataValidator;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;


    private Long extractUserIdFromAuth(String authHeader) throws Exception {
        log.info("Extract user ID -------------:=>");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new Exception("Invalid Authorization header");
        }
        String token = authHeader.substring(7);
        if (token.startsWith("web:")) {
            return extractWebUserId(token.substring(4));
        }

        // Local development: keep Telegram auth optional for direct local testing.
        if ("local".equalsIgnoreCase(activeProfile) && token.isBlank()) {
            return Math.abs(java.util.UUID.randomUUID().getMostSignificantBits());
        }

        return initDataValidator.validateAndExtractUserId(token)
                .orElseThrow(() -> new Exception("Invalid Telegram initData"));
    }

    private Long extractWebUserId(String webChatId) throws Exception {
        if (webChatId == null || !webChatId.matches("^777\\d{8,}$")) {
            throw new Exception("Invalid web chat ID");
        }
        return Long.parseLong(webChatId);
    }

    /**
     * Save game score to database
     */

    public SOSGameDTO.GameScoreResponse saveGameScore(String authHeader, SOSGameDTO.SaveScoreRequest scoreData) {

        try {

            Long userId = extractUserIdFromAuth(authHeader);
            log.info("Saving game score for user: {}", userId);
            PlayerProfile playerProfile = playerProfileRepository.findByChatId(userId)
                    .orElseGet(() -> {
                        PlayerProfile newProfile = PlayerProfile.builder()
                                .chatId(userId)
                                .playerName(scoreData.getPlayerName() != null ? scoreData.getPlayerName() : "Player_" + userId)
                                .createdAt(LocalDateTime.now())
                                .build();
                        return playerProfileRepository.save(newProfile);
                    });
            String gameCode = scoreData.getGameCode() != null && !scoreData.getGameCode().isBlank()
                    ? scoreData.getGameCode().toUpperCase()
                    : "SOS";

            GameScore gameScore = GameScore.builder()
                    .gameCode(gameCode)
                    .chatId(userId)
                    .playerName(playerProfile.getPlayerName())
                    .score(scoreData.getScore())
                    .opponentScore(scoreData.getOpponentScore())
                    .boardSize(scoreData.getBoardSize() != null ? scoreData.getBoardSize() : 5)
                    .isWin(scoreData.getIsWin() != null ? scoreData.getIsWin() : (scoreData.getScore() > scoreData.getOpponentScore()))
                    .gamePlayedAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();

            GameScore savedScore = gameScoreRepository.save(gameScore);
            log.info("Game score saved successfully. ID: {}", savedScore.getId());

            // Calculate new rank
            Long totalScoreSum = gameScoreRepository.getTotalScoreByUserId(userId);
            int newRank = gameScoreRepository.getUserRank(userId);

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


}
