package com.telegram.bot.entity;

import com.telegram.bot.utils.BingoEnums;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;


@Data
@Entity
@Builder
@Table(name = "BINGO_PLAYER")
@NoArgsConstructor
@AllArgsConstructor
public class Player {


    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "player_seq")
    @SequenceGenerator(name = "player_seq", sequenceName = "BINGO_PLAYER_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "CHAT_ID", nullable = false)
    private Long chatId;

    @Column(name = "PLAYER_NAME", nullable = false)
    private String playerName;

    @Column(name = "IS_BOT")
    private Boolean isBot = false;

    @Column(name = "IS_HOST")
    private Boolean isHost = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private BingoEnums.PlayerStatus status;

    @Column(name = "SCORE")
    private Integer score = 0;

    @Column(name = "TURN_ORDER")
    private Integer turnOrder;

    @Column(name = "FINISH_RANK")
    private Integer finishRank; // 1st, 2nd, etc.

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "GAME_SESSION_ID")
    private GameSession gameSession;

    @OneToOne(mappedBy = "player",fetch = FetchType.EAGER)
    private PlayerBoard playerBoard;

    @PrePersist
    public void prePersist() {
        if (status == null) status = BingoEnums.PlayerStatus.WAITING;
        if (score == null) score = 0;
        if (isBot == null) isBot = false;
        if (isHost == null) isHost = false;
    }
}
