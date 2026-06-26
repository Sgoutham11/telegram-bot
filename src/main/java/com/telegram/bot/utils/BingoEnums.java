package com.telegram.bot.utils;


public class BingoEnums {

    public enum GameStatus {
        WAITING,    // In lobby, waiting for players
        PLAYING,    // Game in progress
        FINISHED    // Game ended
    }

    public enum PlayerStatus {
        WAITING,    // In lobby
        PLAYING,    // Active in game
        WON,        // Completed all points
        LOST        // Last remaining player
    }

    public enum BoardSize {
        FIVE(5, "BINGO"),
        SIX(6, "BINGOS"),
        SEVEN(7, "BINGOES"),
        EIGHT(8, "BINGOESS");

        private final int size;
        private final String header;

        BoardSize(int size, String header) {
            this.size = size;
            this.header = header;
        }

        public int getSize() {
            return size;
        }

        public String getHeader() {
            return header;
        }

        public int getTotalNumbers() {
            return size * size;
        }

        public int getPointsToWin() {
            return size;
        }

        public static BoardSize fromSize(int size) {
            for (BoardSize bs : values()) {
                if (bs.size == size) return bs;
            }
            throw new IllegalArgumentException("Invalid board size: " + size);
        }
    }
}
