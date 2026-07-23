package com.telegram.bot.controller;

import com.telegram.bot.dto.DebugLogRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("logs")
@RequiredArgsConstructor
@Log4j2
public class LoggerService {

    @PostMapping("print")
    public ResponseEntity<?> saveGameScore(@RequestBody DebugLogRequest request) {
        try {
            log.info("[FRONTEND] [{}] {}", request.getLevel(), request.getMessage());

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error while logging ",e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to save score: " + e.getMessage()));
        }
    }


}
