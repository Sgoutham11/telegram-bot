package com.telegram.bot.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SOS_MATCH_PLAYER")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SOSMatchPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sos_match_player_seq")
    @SequenceGenerator(name = "sos_match_player_seq", sequenceName = "SOS_MATCH_PLAYER_SEQ", allocationSize = 1)
    @Column(name = "MATCH_PLAYER_ID")
    private Long matchPlayerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MATCH_ID")
    private SOSMatch match;

    @Column(name = "TELEGRAM_ID")
    private Long telegramId;

    @Column(name = "PLAYER_NAME", length = 100)
    private String playerName;

    @Column(name = "SCORE")
    private Integer score;
}
