package com.telegram.bot.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "SOS_MATCH_MOVE")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SOSMatchMove {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sos_match_move_seq")
    @SequenceGenerator(name = "sos_match_move_seq", sequenceName = "SOS_MATCH_MOVE_SEQ", allocationSize = 1)
    @Column(name = "MOVE_ID")
    private Long moveId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MATCH_ID")
    private SOSMatch match;

    @Column(name = "TELEGRAM_ID")
    private Long telegramId;

    @Column(name = "ROW_INDEX")
    private Integer rowIndex;

    @Column(name = "COLUMN_INDEX")
    private Integer columnIndex;

    @Column(name = "LETTER", length = 1)
    private String letter;

    @Column(name = "MOVE_TIME")
    private LocalDateTime moveTime;
}
