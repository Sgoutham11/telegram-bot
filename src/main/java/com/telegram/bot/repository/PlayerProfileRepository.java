package com.telegram.bot.repository;

import com.telegram.bot.entity.GameSession;
import com.telegram.bot.entity.Player;
import com.telegram.bot.entity.PlayerProfile;
import com.telegram.bot.utils.BingoEnums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerProfileRepository extends JpaRepository<PlayerProfile, Long> {

    Optional<PlayerProfile> findByChatId(Long chatId);

}
