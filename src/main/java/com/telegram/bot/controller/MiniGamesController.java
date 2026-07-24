package com.telegram.bot.controller;

import com.telegram.bot.dto.SOSGameDTO;
import com.telegram.bot.service.LeaderboardService;
import com.telegram.bot.service.SOSGameService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for Mini Games app.
 * Handles game score tracking and leaderboards.
 */
@RestController
@RequestMapping("/api")
@Log4j2
public class MiniGamesController {

    @Autowired
    private SOSGameService sosGameService;

    @Autowired
    private LeaderboardService leaderboardService;

    // Used by both SOS and Bingo for saving the game score.
    @PostMapping("/game/score")
    public ResponseEntity<?> saveGameScore(@RequestHeader(value = "Authorization", required = false)
                                           String authHeader, @RequestBody SOSGameDTO.SaveScoreRequest scoreData) {
        log.info("Inside Saving game score-------------:=>");
        SOSGameDTO.GameScoreResponse response = sosGameService.saveGameScore(authHeader, scoreData);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/leaderboard/best-by-game")
    public ResponseEntity<?> getBestPlayersByGame() {
        return ResponseEntity.ok(leaderboardService.getBestPlayersByGame());
    }

    @GetMapping("/leaderboard/sos")
    public ResponseEntity<?> getSosLeaderboard(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(Map.of("leaderboard", leaderboardService.getLeaderboard("SOS", limit)));
    }

    @GetMapping("/leaderboard/bingo")
    public ResponseEntity<?> getBingoLeaderboard(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(Map.of("leaderboard", leaderboardService.getLeaderboard("BINGO", limit)));
    }
}
