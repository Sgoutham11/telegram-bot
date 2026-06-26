package com.telegram.bot.service;

import com.telegram.bot.entity.PlayerBoard;
import com.telegram.bot.utils.BingoEnums;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class BingoBoardService {
    public int[][] generateBoard(int size) {
        int totalNumbers = size * size;
        List<Integer> numbers = new ArrayList<>();
        for (int i = 1; i <= totalNumbers; i++) {
            numbers.add(i);
        }
        Collections.shuffle(numbers);

        int[][] board = new int[size][size];
        int index = 0;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                board[row][col] = numbers.get(index++);
            }
        }
        return board;
    }

    public boolean[][] createEmptyMarked(int size) {
        return new boolean[size][size];
    }

    public void markNumberOnBoard(PlayerBoard playerBoard, int number) {
        int[][] board = playerBoard.getBoardArray();
        boolean[][] marked = playerBoard.getMarkedArray();
        int size = board.length;

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (board[r][c] == number) {
                    marked[r][c] = true;
                    playerBoard.setMarkedArray(marked);
                    return;
                }
            }
        }
    }

    /**
     * Check for NEW completed patterns and return how many new points earned
     */
    public int checkNewPatterns(PlayerBoard playerBoard) {
        boolean[][] marked = playerBoard.getMarkedArray();
        int size = marked.length;
        Set<String> alreadyCompleted = playerBoard.getCompletedPatternsSet();
        int newPoints = 0;

        // Check rows
        for (int r = 0; r < size; r++) {
            String pattern = "ROW_" + r;
            if (alreadyCompleted.contains(pattern)) continue;
            boolean complete = true;
            for (int c = 0; c < size; c++) {
                if (!marked[r][c]) { complete = false; break; }
            }
            if (complete) {
                playerBoard.addCompletedPattern(pattern);
                newPoints++;
            }
        }

        // Check columns
        for (int c = 0; c < size; c++) {
            String pattern = "COL_" + c;
            if (alreadyCompleted.contains(pattern)) continue;
            boolean complete = true;
            for (int r = 0; r < size; r++) {
                if (!marked[r][c]) { complete = false; break; }
            }
            if (complete) {
                playerBoard.addCompletedPattern(pattern);
                newPoints++;
            }
        }

        // Check diagonal (top-left to bottom-right)
        String diagPattern1 = "DIAG_1";
        if (!alreadyCompleted.contains(diagPattern1)) {
            boolean complete = true;
            for (int i = 0; i < size; i++) {
                if (!marked[i][i]) { complete = false; break; }
            }
            if (complete) {
                playerBoard.addCompletedPattern(diagPattern1);
                newPoints++;
            }
        }

        // Check diagonal (top-right to bottom-left)
        String diagPattern2 = "DIAG_2";
        if (!alreadyCompleted.contains(diagPattern2)) {
            boolean complete = true;
            for (int i = 0; i < size; i++) {
                if (!marked[i][size - 1 - i]) { complete = false; break; }
            }
            if (complete) {
                playerBoard.addCompletedPattern(diagPattern2);
                newPoints++;
            }
        }

        return newPoints;
    }

    public String buildBoardDisplay(PlayerBoard playerBoard, BingoEnums.BoardSize boardSize, int score) {
        int[][] board = playerBoard.getBoardArray();
        boolean[][] marked = playerBoard.getMarkedArray();
        int size = board.length;
        String header = boardSize.getHeader();

        StringBuilder sb = new StringBuilder();

        // Header with progress
        sb.append("  ");
        for (int i = 0; i < header.length(); i++) {
            if (i < score) {
                sb.append("🟢").append(header.charAt(i)).append(" ");
            } else {
                sb.append("⚪").append(header.charAt(i)).append(" ");
            }
        }
        sb.append("\n\n");

        // Board
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                String num = String.format("%2d", board[r][c]);
                if (marked[r][c]) {
                    sb.append("[✅").append(num).append("]");
                } else {
                    sb.append("[ ").append(num).append(" ]");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }

}



