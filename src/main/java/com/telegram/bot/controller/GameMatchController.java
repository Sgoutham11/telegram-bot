//package com.telegram.bot.controller;
//
//import com.telegram.bot.dto.SOSGameDTO;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/game")
//@RequiredArgsConstructor
//public class GameMatchController {
//
//    private final GameMatchService gameMatchService;
//    @PostMapping("/player")
//    public ResponseEntity<?> registerPlayer(@RequestHeader(value = "Authorization", required = false)
//                                            String authHeader, @RequestBody Map<String, String> body) {
//
//        return ResponseEntity.ok(gameMatchService.registerPlayer(authHeader, body));
//
//    }
//
//    @PostMapping("/match/start")
//    public ResponseEntity<?> startMatch(@RequestHeader(value = "Authorization", required = false)
//                                        String authHeader, @RequestBody Map<String, Object> body) {
//
//        return ResponseEntity.ok(gameMatchService.startMatch(authHeader, body));
//
//    }
//
//    @PostMapping("/match/end")
//    public ResponseEntity<?> endMatch(
//            @RequestHeader(value = "Authorization", required = false) String authHeader,
//            @RequestBody Map<String, Object> body) {
//
//        return ResponseEntity.ok(gameMatchService.endMatch(authHeader, body));
//
//    }
//
//    @PostMapping("/match/move")
//    public ResponseEntity<?> recordMove(@RequestHeader(value = "Authorization", required = false)
//                                        String authHeader, @RequestBody Map<String, Object> body) {
//
//        return ResponseEntity.ok(gameMatchService.recordMove(authHeader, body));
//
//    }
//
//    @GetMapping("/history")
//    public ResponseEntity<?> getHistory(@RequestHeader(value = "Authorization", required = false)
//                                        String authHeader, @RequestParam(defaultValue = "20") int limit) {
//
//        return ResponseEntity.ok(Map.of("history", gameMatchService.getMatchHistory(authHeader, limit)));
//
//    }
//
//    @GetMapping("/leaderboard")
//    public ResponseEntity<?> getLeaderboard(@RequestParam(defaultValue = "10") int limit) {
//        List<SOSGameDTO.LeaderboardEntry> leaderboard = gameMatchService.getLeaderboard(limit);
//        for (int i = 0; i < leaderboard.size(); i++) {
//            leaderboard.get(i).setRank(i + 1);
//        }
//        return ResponseEntity.ok(Map.of("leaderboard", leaderboard));
//    }
//
//
//}
