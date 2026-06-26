package com.telegram.bot.repository;

import com.telegram.bot.entity.GameSession;
import com.telegram.bot.entity.Player;
import com.telegram.bot.utils.BingoEnums;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByChatIdAndGameSession(Long chatId, GameSession gameSession);

    List<Player> findByGameSessionAndStatus(GameSession session, BingoEnums.PlayerStatus status);

    Optional<Player> findByChatIdAndGameSession_Status(Long chatId, BingoEnums.GameStatus status);


    @Modifying
    @Query(value = "DELETE FROM BINGO_PLAYER WHERE GAME_SESSION_ID = :sessionId", nativeQuery = true)
    void deleteByGameSessionId(@Param("sessionId") Long sessionId);

}
