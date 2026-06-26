package com.telegram.bot.repository;

import com.telegram.bot.entity.SOSMatchMove;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SOSMatchMoveRepository extends JpaRepository<SOSMatchMove, Long> {
    List<SOSMatchMove> findByMatchMatchIdOrderByMoveTimeAsc(Long matchId);
}
