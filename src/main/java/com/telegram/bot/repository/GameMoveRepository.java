package com.telegram.bot.repository;

import com.telegram.bot.entity.GameMove;
import com.telegram.bot.entity.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GameMoveRepository extends JpaRepository<GameMove, Long> {

    List<GameMove> findByGameSessionOrderByMoveOrderAsc(GameSession session);

    @Modifying
    @Query("UPDATE GameMove m SET m.autoSelected = true WHERE m.id = :moveId")
    void markAsAutoSelected(@Param("moveId") Long moveId);

    @Modifying
    @Query(value = "DELETE FROM BINGO_GAME_MOVE WHERE GAME_SESSION_ID = :sessionId", nativeQuery = true)
    void deleteByGameSessionId(@Param("sessionId") Long sessionId);

}
