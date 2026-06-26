package com.telegram.bot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class BingoGame {

    private int[][] board;
    private boolean[][] marked;
    private List<Integer> calledNumbers;
    private List<Integer> allNumbers;
    private boolean gameOver;

    public BingoGame() {
        board = new int[5][5];
        marked = new boolean[5][5];
        calledNumbers = new ArrayList<>();
        allNumbers = new ArrayList<>();
        gameOver = false;

        // Numbers 1-75 for calling
        for (int i = 1; i <= 75; i++) {
            allNumbers.add(i);
        }
        Collections.shuffle(allNumbers);

        generateBoard();

        // Free space in center
        marked[2][2] = true;
        board[2][2] = 0; // 0 = FREE
    }

    private void generateBoard() {
        // B(1-15), I(16-30), N(31-45), G(46-60), O(61-75)
        int[][] ranges = {{1, 15}, {16, 30}, {31, 45}, {46, 60}, {61, 75}};

        for (int col = 0; col < 5; col++) {
            List<Integer> columnNumbers = new ArrayList<>();
            for (int n = ranges[col][0]; n <= ranges[col][1]; n++) {
                columnNumbers.add(n);
            }
            Collections.shuffle(columnNumbers);
            for (int row = 0; row < 5; row++) {
                board[row][col] = columnNumbers.get(row);
            }
        }
    }

    public int callNextNumber() {
        if (calledNumbers.size() >= allNumbers.size()) return -1;
        int number = allNumbers.get(calledNumbers.size());
        calledNumbers.add(number);
        return number;
    }

    public boolean markNumber(int row, int col) {
        if (row < 0 || row > 4 || col < 0 || col > 4) return false;
        if (marked[row][col]) return false;

        int number = board[row][col];
        if (calledNumbers.contains(number)) {
            marked[row][col] = true;
            return true;
        }
        return false; // Number not called yet
    }

    public boolean checkWin() {
        // Check rows
        for (int r = 0; r < 5; r++) {
            boolean win = true;
            for (int c = 0; c < 5; c++) {
                if (!marked[r][c]) { win = false; break; }
            }
            if (win) return true;
        }

        // Check columns
        for (int c = 0; c < 5; c++) {
            boolean win = true;
            for (int r = 0; r < 5; r++) {
                if (!marked[r][c]) { win = false; break; }
            }
            if (win) return true;
        }

        // Check diagonal (top-left to bottom-right)
        boolean win = true;
        for (int i = 0; i < 5; i++) {
            if (!marked[i][i]) { win = false; break; }
        }
        if (win) return true;

        // Check diagonal (top-right to bottom-left)
        win = true;
        for (int i = 0; i < 5; i++) {
            if (!marked[i][4 - i]) { win = false; break; }
        }
        return win;
    }

    public int[][] getBoard() { return board; }
    public boolean[][] getMarked() { return marked; }
    public List<Integer> getCalledNumbers() { return calledNumbers; }
    public boolean isGameOver() { return gameOver; }
    public void setGameOver(boolean gameOver) { this.gameOver = gameOver; }

    public String getLastCalledNumber() {
        if (calledNumbers.isEmpty()) return "None";
        int num = calledNumbers.get(calledNumbers.size() - 1);
        return getLetterForNumber(num) + "-" + num;
    }

    public String getLetterForNumber(int num) {
        if (num <= 15) return "B";
        if (num <= 30) return "I";
        if (num <= 45) return "N";
        if (num <= 60) return "G";
        return "O";
    }



}



