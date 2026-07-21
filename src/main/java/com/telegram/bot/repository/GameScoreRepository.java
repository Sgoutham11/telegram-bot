package com.telegram.bot.repository;

import com.telegram.bot.entity.GameScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface GameScoreRepository extends JpaRepository<GameScore, Long> {

    List<GameScore> findByChatIdOrderByCreatedAtDesc(Long chatId);

    @Query(value = "SELECT * FROM (SELECT * FROM GAME_SCORE WHERE chat_id = :chatId ORDER BY game_played_at DESC) WHERE ROWNUM = 1", nativeQuery = true)
    GameScore findTopByChatIdOrderByGamePlayedAtDesc(@Param("chatId") Long chatId);

    @Query("SELECT COUNT(s) FROM GameScore s WHERE s.chatId = :chatId")
    Integer countGamesByUserId(@Param("chatId") Long chatId);

    @Query("SELECT COUNT(s) FROM GameScore s WHERE s.chatId = :chatId AND s.isWin = true")
    Integer countWinsByUserId(@Param("chatId") Long chatId);

    @Query("SELECT COALESCE(SUM(s.score), 0) FROM GameScore s WHERE s.chatId = :chatId")
    Long getTotalScoreByUserId(@Param("chatId") Long chatId);

    @Query("SELECT COALESCE(AVG(s.score), 0.0) FROM GameScore s WHERE s.chatId = :chatId")
    Double getAverageScoreByUserId(@Param("chatId") Long chatId);

    @Query(value = "SELECT RANK() OVER (ORDER BY total_score DESC) as rank " +
            "FROM (" +
            "SELECT s.chat_id, SUM(s.score) as total_score " +
            "FROM GAME_SCORE s " +
            "GROUP BY s.chat_id" +
            ") WHERE chat_id = :chatId", nativeQuery = true)
    Integer getUserRank(@Param("chatId") Long chatId);

    @Query("SELECT COUNT(s) FROM GameScore s WHERE s.chatId = :chatId AND s.isWin = true")
    Integer getLongestWinStreakByUserId(@Param("chatId") Long chatId);

    @Query(value = "SELECT s.player_name, s.chat_id, SUM(s.score) as total_score, " +
            "SUM(CASE WHEN s.is_win = 1 THEN 1 ELSE 0 END) as wins, " +
            "COUNT(*) as games_played, MAX(s.game_played_at) as last_played " +
            "FROM GAME_SCORE s " +
            "GROUP BY s.chat_id, s.player_name " +
            "ORDER BY total_score DESC " +
            "FETCH FIRST :limit ROWS ONLY", nativeQuery = true)
    List<Object[]> findTop(@Param("limit") int limit);

    Optional<GameScore> findById(Long id);
}
