package com.telegram.bot.repository;

import com.telegram.bot.entity.SOSGameScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SOS Game Score entity
 */
@Repository
public interface SOSGameScoreRepository extends JpaRepository<SOSGameScore, Long> {

    /**
     * Find all scores by user
     */
    List<SOSGameScore> findByChatIdOrderByCreatedAtDesc(Long chatId);

    /**
     * Find the most recent game for a user
     */
    @Query(value = "SELECT * FROM (SELECT * FROM SOS_GAME_SCORE WHERE chat_id = :chatId ORDER BY game_played_at DESC) WHERE ROWNUM = 1", nativeQuery = true)
    SOSGameScore findTopByChatIdOrderByGamePlayedAtDesc(@Param("chatId") Long chatId);

    /**
     * Count total games played by user
     */
    @Query("SELECT COUNT(s) FROM SOSGameScore s WHERE s.chatId = :chatId")
    Integer countGamesByUserId(@Param("chatId") Long chatId);

    /**
     * Count wins for user
     */
    @Query("SELECT COUNT(s) FROM SOSGameScore s WHERE s.chatId = :chatId AND s.isWin = true")
    Integer countWinsByUserId(@Param("chatId") Long chatId);

    /**
     * Get total score for user
     */
    @Query("SELECT COALESCE(SUM(s.score), 0) FROM SOSGameScore s WHERE s.chatId = :chatId")
    Long getTotalScoreByUserId(@Param("chatId") Long chatId);

    /**
     * Get average score for user
     */
    @Query("SELECT COALESCE(AVG(s.score), 0.0) FROM SOSGameScore s WHERE s.chatId = :chatId")
    Double getAverageScoreByUserId(@Param("chatId") Long chatId);

    /**
     * Get user's rank based on total score
     */
    @Query(value = "SELECT RANK() OVER (ORDER BY total_score DESC) as rank " +
            "FROM (" +
            "SELECT s.chat_id, SUM(s.score) as total_score " +
            "FROM SOS_GAME_SCORE s " +
            "GROUP BY s.chat_id" +
            ") WHERE chat_id = :chatId", nativeQuery = true)
    Integer getUserRank(@Param("chatId") Long chatId);

    /**
     * Get longest win streak for user
     * For now, this returns 0 as a placeholder
     * Can be extended with more complex logic
     */
    @Query("SELECT COUNT(s) FROM SOSGameScore s WHERE s.chatId = :chatId AND s.isWin = true")
    Integer getLongestWinStreakByUserId(@Param("chatId") Long chatId);

    /**
     * Get top N players by total score
     */
    @Query(value = "SELECT s.player_name, s.chat_id, SUM(s.score) as total_score, " +
            "SUM(CASE WHEN s.is_win = 1 THEN 1 ELSE 0 END) as wins, " +
            "COUNT(*) as games_played, MAX(s.game_played_at) as last_played " +
            "FROM SOS_GAME_SCORE s " +
            "GROUP BY s.chat_id, s.player_name " +
            "ORDER BY total_score DESC", nativeQuery = true)
    List<Object[]> findTop(@Param("limit") int limit);

    /**
     * Find score by ID - override to ensure we get SOSGameScore
     */
    Optional<SOSGameScore> findById(Long id);
}
