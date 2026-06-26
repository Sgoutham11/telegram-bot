package com.telegram.bot.repository;

import com.telegram.bot.entity.SOSMatch;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SOSMatchRepository extends JpaRepository<SOSMatch, Long> {
    List<SOSMatch> findByWinnerTelegramIdOrWinnerTelegramIdIsNullOrderByEndTimeDesc(Long telegramId, Pageable pageable);
    List<SOSMatch> findAllByOrderByEndTimeDesc(Pageable pageable);
}
