package com.telegram.bot.repository;

import com.telegram.bot.entity.Player;
import com.telegram.bot.entity.PlayerBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PlayerBoardRepository extends JpaRepository<PlayerBoard, Long> {

    Optional<PlayerBoard> findByPlayer(Player player);
    Optional<PlayerBoard> findByPlayerId(Long playerId);

    @Modifying
    @Query(value = "DELETE FROM BINGO_PLAYER_BOARD WHERE PLAYER_ID IN (SELECT ID FROM BINGO_PLAYER WHERE GAME_SESSION_ID = :sessionId)", nativeQuery = true)
    void deleteByPlayerGameSessionId(@Param("sessionId") Long sessionId);

}
