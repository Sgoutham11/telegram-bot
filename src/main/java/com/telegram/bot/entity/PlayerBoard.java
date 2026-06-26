package com.telegram.bot.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


@Data
@Entity
@Builder
@Table(name = "BINGO_PLAYER_BOARD")
@NoArgsConstructor
@AllArgsConstructor
public class PlayerBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "player_board_seq")
    @SequenceGenerator(name = "player_board_seq", sequenceName = "BINGO_PLAYER_BOARD_SEQ", allocationSize = 1)
    private Long id;


    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "PLAYER_ID")
    private Player player;

    @Column(name = "BOARD_DATA", length = 1000)
    private String boardData; // JSON string: "[[1,2,3],[4,5,6],...]"

    @Column(name = "MARKED_DATA", length = 1000)
    private String markedData; // JSON string: "[[true,false],[false,true],...]"

    // Completed patterns tracking (to avoid counting same pattern twice)
    @Column(name = "COMPLETED_PATTERNS", length = 500)
    private String completedPatterns; // e.g., "ROW_0,COL_2,DIAG_1"

    public int[][] getBoardArray() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(boardData, int[][].class);
        } catch (Exception e) {
            return new int[0][0];
        }
    }

    public boolean[][] getMarkedArray() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(markedData, boolean[][].class);
        } catch (Exception e) {
            return new boolean[0][0];
        }
    }

    public void setBoardArray(int[][] board) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.boardData = mapper.writeValueAsString(board);
        } catch (Exception e) {
            this.boardData = "[]";
        }
    }

    public void setMarkedArray(boolean[][] marked) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.markedData = mapper.writeValueAsString(marked);
        } catch (Exception e) {
            this.markedData = "[]";
        }
    }

    public Set<String> getCompletedPatternsSet() {
        if (completedPatterns == null || completedPatterns.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(completedPatterns.split(",")));
    }

    public void addCompletedPattern(String pattern) {
        Set<String> patterns = getCompletedPatternsSet();
        patterns.add(pattern);
        this.completedPatterns = String.join(",", patterns);
    }


}
