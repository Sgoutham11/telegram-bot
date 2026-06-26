package com.telegram.bot.service;

import com.telegram.bot.dto.SOSGameDTO;
import com.telegram.bot.entity.*;
import com.telegram.bot.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GameMatchService {

    private final PlayerProfileRepository playerProfileRepository;
    private final SOSMatchRepository matchRepository;
    private final SOSMatchMoveRepository matchMoveRepository;
    private final SOSGameScoreRepository scoreRepository;

    public Map<String, Object> registerPlayer(Long telegramId, String displayName) {
        PlayerProfile profile = playerProfileRepository.findByChatId(telegramId)
                .orElseGet(() -> playerProfileRepository.save(
                        PlayerProfile.builder()
                                .chatId(telegramId)
                                .playerName(displayName)
                                .createdAt(LocalDateTime.now())
                                .build()
                ));

        return Map.of(
                "userId", profile.getChatId(),
                "telegramId", telegramId,
                "displayName", profile.getPlayerName(),
                "createdAt", profile.getCreatedAt().toString()
        );
    }

    public Map<String, Object> startMatch(Long hostTelegramId, Map<String, Object> request) {
        String roomCode = (String) request.getOrDefault("roomCode", "LOCAL");
        Integer gridSize = (Integer) request.getOrDefault("gridSize", 6);
        Integer playerCount = (Integer) request.getOrDefault("playerCount", 2);

        SOSMatch match = matchRepository.save(SOSMatch.builder()
                .roomCode(roomCode)
                .gridSize(gridSize)
                .playerCount(playerCount)
                .startTime(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build());

        return Map.of("matchId", match.getMatchId(), "roomCode", roomCode, "status", "started");
    }

    public Map<String, Object> endMatch(Long matchId, Map<String, Object> request) {
        SOSMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        match.setEndTime(LocalDateTime.now());
        if (request.containsKey("winnerTelegramId")) {
            match.setWinnerTelegramId(((Number) request.get("winnerTelegramId")).longValue());
        }
        matchRepository.save(match);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> players = (List<Map<String, Object>>) request.getOrDefault("players", List.of());
        for (Map<String, Object> p : players) {
            Long tgId = ((Number) p.get("telegramId")).longValue();
            Integer score = ((Number) p.get("score")).intValue();
            String name = (String) p.getOrDefault("name", "Player");

            SOSMatchPlayer mp = SOSMatchPlayer.builder()
                    .match(match)
                    .telegramId(tgId)
                    .playerName(name)
                    .score(score)
                    .build();
            // Saved via cascade if configured; save score record
            scoreRepository.save(SOSGameScore.builder()
                    .chatId(tgId)
                    .playerName(name)
                    .score(score)
                    .opponentScore(0)
                    .boardSize(match.getGridSize())
                    .isWin(Objects.equals(tgId, match.getWinnerTelegramId()))
                    .gamePlayedAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        return Map.of("matchId", matchId, "status", "ended");
    }

    public Map<String, Object> recordMove(Long matchId, Map<String, Object> request) {
        SOSMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        SOSMatchMove move = matchMoveRepository.save(SOSMatchMove.builder()
                .match(match)
                .telegramId(((Number) request.get("telegramId")).longValue())
                .rowIndex(((Number) request.get("row")).intValue())
                .columnIndex(((Number) request.get("col")).intValue())
                .letter((String) request.get("letter"))
                .moveTime(LocalDateTime.now())
                .build());

        return Map.of("moveId", move.getMoveId(), "matchId", matchId);
    }

    public List<Map<String, Object>> getMatchHistory(Long telegramId, int limit) {
        return matchRepository.findAllByOrderByEndTimeDesc(PageRequest.of(0, limit))
                .stream()
                .map(m -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("matchId", m.getMatchId());
                    entry.put("roomCode", m.getRoomCode());
                    entry.put("gridSize", m.getGridSize());
                    entry.put("winnerTelegramId", m.getWinnerTelegramId());
                    entry.put("startTime", m.getStartTime() != null ? m.getStartTime().toString() : null);
                    entry.put("endTime", m.getEndTime() != null ? m.getEndTime().toString() : null);
                    return entry;
                })
                .collect(Collectors.toList());
    }

    public List<SOSGameDTO.LeaderboardEntry> getLeaderboard(int limit) {
        return scoreRepository.findTop(limit).stream()
                .map(result -> {
                    String playerName = (String) result[0];
                    Long totalScore = toLong(result[2]);
                    Long wins = toLong(result[3]);
                    Long games = toLong(result[4]);
                    Double winRate = games > 0 ? (wins.doubleValue() / games) * 100 : 0.0;
                    return SOSGameDTO.LeaderboardEntry.builder()
                            .playerName(playerName)
                            .score(totalScore.intValue())
                            .wins(wins.intValue())
                            .gamesPlayed(games.intValue())
                            .winRate(Math.round(winRate * 100.0) / 100.0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private Long toLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Number) return ((Number) obj).longValue();
        return 0L;
    }
}
