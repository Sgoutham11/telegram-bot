package com.telegram.bot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Entity
@Builder
@Table(name = "BINGO_GAME_MOVE")
@NoArgsConstructor
@AllArgsConstructor
public class GameMove {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_move_seq")
    @SequenceGenerator(name = "game_move_seq", sequenceName = "BINGO_GAME_MOVE_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "GAME_SESSION_ID")
    private GameSession gameSession;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "PLAYER_ID")
    private Player player;

    @Column(name = "SELECTED_NUMBER")
    private Integer selectedNumber;

    @Column(name = "MOVE_ORDER")
    private Integer moveOrder;

    @Column(name = "AUTO_SELECTED")
    private Boolean autoSelected = false; // true if timer expired

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
