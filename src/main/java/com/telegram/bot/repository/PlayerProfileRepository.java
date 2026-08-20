package com.telegram.bot.repository;

import com.telegram.bot.entity.PlayerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PlayerProfileRepository extends JpaRepository<PlayerProfile, Long> {

    Optional<PlayerProfile> findByChatId(Long chatId);

    @Modifying
    @Query(value = """
            DECLARE
                existing_count NUMBER;
            BEGIN
                INSERT INTO PLAYER_PROFILE (ID, CHAT_ID, PLAYER_NAME, CREATED_AT)
                SELECT PLAYER_PROFILE_SEQ.NEXTVAL, :chatId, :playerName, SYSTIMESTAMP
                FROM DUAL
                WHERE NOT EXISTS (
                    SELECT 1 FROM PLAYER_PROFILE WHERE CHAT_ID = :chatId
                );
            EXCEPTION
                WHEN DUP_VAL_ON_INDEX THEN
                    SELECT COUNT(1) INTO existing_count
                    FROM PLAYER_PROFILE
                    WHERE CHAT_ID = :chatId;

                    IF existing_count = 0 THEN
                        RAISE;
                    END IF;
            END;
            """, nativeQuery = true)
    void insertIfMissing(@Param("chatId") Long chatId, @Param("playerName") String playerName);

}
