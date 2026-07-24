package com.telegram.bot.service;

import com.telegram.bot.dto.LeaderboardDTO;
import com.telegram.bot.repository.GameScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaderboardService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final List<String> SUPPORTED_GAMES = List.of("SOS", "BINGO");

    private final GameScoreRepository gameScoreRepository;

    public List<LeaderboardDTO.LeaderboardPlayer> getLeaderboard(String gameCode, Integer requestedLimit) {
        int limit = normalizeLimit(requestedLimit);
        List<Object[]> rows = gameScoreRepository.findLeaderboardByGame(gameCode, PageRequest.of(0, limit));

        return IntStream.range(0, rows.size())
                .mapToObj(index -> toLeaderboardPlayer(rows.get(index), index + 1))
                .toList();
    }

    public Map<String, LeaderboardDTO.LeaderboardPlayer> getBestPlayersByGame() {
        Map<String, LeaderboardDTO.LeaderboardPlayer> result = new LinkedHashMap<>();
        for (String gameCode : SUPPORTED_GAMES) {
            Optional<LeaderboardDTO.LeaderboardPlayer> best = getLeaderboard(gameCode, 1).stream().findFirst();
            result.put(gameCode, best.orElse(null));
        }
        return result;
    }

    private int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(requestedLimit, MAX_LIMIT);
    }

    private LeaderboardDTO.LeaderboardPlayer toLeaderboardPlayer(Object[] row, int rank) {
        return LeaderboardDTO.LeaderboardPlayer.builder()
                .playerId(asLong(row[0]))
                .playerName((String) row[1])
                .gameType((String) row[2])
                .totalWins(asInt(row[3]))
                .totalLosses(asInt(row[4]))
                .totalGames(asInt(row[5]))
                .winLossRatio(roundRatio(asDouble(row[6])))
                .rank(rank)
                .build();
    }

    private Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private Integer asInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private Double asDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }

    private Double roundRatio(Double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
