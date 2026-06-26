package com.telegram.bot.repository;

import com.telegram.bot.entity.GameSession;
import com.telegram.bot.utils.BingoEnums;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameSessionRepository extends JpaRepository<GameSession, Long> {

    Optional<GameSession> findByHostId(String hostId);

    Optional<GameSession> findByHostIdAndStatus(String hostId, BingoEnums.GameStatus status);

    List<GameSession> findByStatus(BingoEnums.GameStatus status);

    void deleteByHostId(String hostId);

    @Modifying
    @Query(value = "DELETE FROM BINGO_GAME_SESSION WHERE ID = :sessionId", nativeQuery = true)
    void deleteBySessionId(@Param("sessionId") Long sessionId);

}
