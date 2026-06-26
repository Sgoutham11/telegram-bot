package com.telegram.bot.entity;

import com.telegram.bot.utils.BingoEnums;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Data
@Entity
@Builder
@Table(name = "BINGO_GAME_SESSION")
@NoArgsConstructor
@AllArgsConstructor
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_session_seq")
    @SequenceGenerator(name = "game_session_seq", sequenceName = "BINGO_GAME_SESSION_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "HOST_ID", unique = true, nullable = false, length = 4)
    private String hostId; // 4-digit unique code

    @Column(name = "HOST_CHAT_ID", nullable = false)
    private Long hostChatId;

    @Column(name = "BOARD_SIZE")
    private Integer boardSize; // 5, 6, 7, or 8

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    private BingoEnums.GameStatus status;

    @Column(name = "CURRENT_TURN_INDEX")
    private Integer currentTurnIndex; // Index in player list

    @Column(name = "TURN_START_TIME")
    private LocalDateTime turnStartTime;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "gameSession", fetch = FetchType.EAGER)
    @OrderBy("turnOrder ASC")
    private List<Player> players = new ArrayList<>();

    @OneToMany(mappedBy = "gameSession",fetch = FetchType.EAGER)
    @OrderBy("moveOrder ASC")
    private List<GameMove> moves = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (status == null) status = BingoEnums.GameStatus.WAITING;
        currentTurnIndex = 0;
    }

    public Player getCurrentTurnPlayer() {
        List<Player> activePlayers = players.stream()
                .filter(p -> p.getStatus() == BingoEnums.PlayerStatus.PLAYING)
                .toList();
        if (activePlayers.isEmpty()) return null;
        return activePlayers.get(currentTurnIndex % activePlayers.size());
    }

    public void advanceTurn() {
        List<Player> activePlayers = players.stream()
                .filter(p -> p.getStatus() == BingoEnums.PlayerStatus.PLAYING)
                .toList();
        if (!activePlayers.isEmpty()) {
            currentTurnIndex = (currentTurnIndex + 1) % activePlayers.size();
        }
        turnStartTime = LocalDateTime.now();
    }

    public Set<Integer> getAllSelectedNumbers() {
        return moves.stream()
                .map(GameMove::getSelectedNumber)
                .collect(Collectors.toSet());
    }


}
