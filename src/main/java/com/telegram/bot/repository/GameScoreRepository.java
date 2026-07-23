package com.telegram.bot.repository;

import com.telegram.bot.entity.GameScore;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameScoreRepository extends JpaRepository<GameScore, Long> {


    @Query("SELECT COALESCE(SUM(s.score), 0) FROM GameScore s WHERE s.chatId = :chatId")
    Long getTotalScoreByUserId(@Param("chatId") Long chatId);


    @Query(value = "SELECT RANK() OVER (ORDER BY total_score DESC) as rank " +
            "FROM (" +
            "SELECT s.chat_id, SUM(s.score) as total_score " +
            "FROM GAME_SCORE s " +
            "GROUP BY s.chat_id" +
            ") WHERE chat_id = :chatId", nativeQuery = true)
    Integer getUserRank(@Param("chatId") Long chatId);

    @Query("""
            SELECT
                s.chatId,
                MIN(s.playerName),
                s.gameCode,
                SUM(CASE WHEN s.isWin = true THEN 1 ELSE 0 END),
                SUM(CASE WHEN s.isWin = false THEN 1 ELSE 0 END),
                COUNT(s),
                CASE
                    WHEN SUM(CASE WHEN s.isWin = false THEN 1.0 ELSE 0.0 END) = 0.0
                        THEN SUM(CASE WHEN s.isWin = true THEN 1.0 ELSE 0.0 END)
                    ELSE SUM(CASE WHEN s.isWin = true THEN 1.0 ELSE 0.0 END)
                        / SUM(CASE WHEN s.isWin = false THEN 1.0 ELSE 0.0 END)
                END
            FROM GameScore s
            WHERE UPPER(s.gameCode) = UPPER(:gameCode)
              AND s.isWin IS NOT NULL
            GROUP BY s.chatId, s.gameCode
            HAVING COUNT(s) > 0
            ORDER BY
                CASE
                    WHEN SUM(CASE WHEN s.isWin = false THEN 1.0 ELSE 0.0 END) = 0.0
                        THEN SUM(CASE WHEN s.isWin = true THEN 1.0 ELSE 0.0 END)
                    ELSE SUM(CASE WHEN s.isWin = true THEN 1.0 ELSE 0.0 END)
                        / SUM(CASE WHEN s.isWin = false THEN 1.0 ELSE 0.0 END)
                END DESC,
                SUM(CASE WHEN s.isWin = true THEN 1 ELSE 0 END) DESC,
                COUNT(s) DESC,
                MIN(s.playerName) ASC
            """)
    List<Object[]> findLeaderboardByGame(@Param("gameCode") String gameCode, Pageable pageable);

}
