package com.telegram.bot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Common score entity for all mini games.
 * Table: GAME_SCORE
 */
@Data
@Entity
@Builder
@Table(name = "GAME_SCORE")
@NoArgsConstructor
@AllArgsConstructor
public class GameScore {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_score_seq")
    @SequenceGenerator(name = "game_score_seq", sequenceName = "GAME_SCORE_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "GAME_CODE", nullable = false, length = 30)
    private String gameCode;

    @Column(name = "CHAT_ID", nullable = false)
    private Long chatId;

    @Column(name = "PLAYER_NAME", nullable = false)
    private String playerName;

    @Column(name = "SCORE", nullable = false)
    private Integer score;

    @Column(name = "OPPONENT_SCORE")
    private Integer opponentScore;

    @Column(name = "BOARD_SIZE")
    private Integer boardSize;

    @Column(name = "IS_WIN")
    private Boolean isWin;

    @Column(name = "GAME_PLAYED_AT")
    private LocalDateTime gamePlayedAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.gameCode == null || this.gameCode.isBlank()) {
            this.gameCode = "SOS";
        }
        this.gameCode = this.gameCode.toUpperCase();
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.gamePlayedAt == null) {
            this.gamePlayedAt = LocalDateTime.now();
        }
    }
}
