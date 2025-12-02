/*
CMPT 225 Final Project
cube.java 
Written by Armin Ahmadi and Nathan Omana
*/
package rubikscube;

import java.util.Arrays;

public class cube implements Cloneable {

    public char[][] state;

    public static final int UP = 0;
    public static final int LEFT = 1;
    public static final int FRONT = 2;
    public static final int RIGHT = 3;
    public static final int BACK = 4;
    public static final int DOWN = 5;

    public cube() {
        state = new char[6][9];
    }

    public cube(char[][] state) {
        this.state = new char[6][9];
        for (int i = 0; i < 6; i++) {
            System.arraycopy(state[i], 0, this.state[i], 0, 9);
        }
    }

    @Override
    public cube clone() {
        return new cube(this.state);
    }

    // inverse move logic
    public void performInverseMove(int move) {
        // a clockwise rotation 3 times -> 1 counter-clockwise rotation
        performMove(move);
        performMove(move);
        performMove(move);
    }
    private void rotateFace(int face) {
        char[] temp = new char[9];
        System.arraycopy(state[face], 0, temp, 0, 9);
        state[face][0] = temp[6];
        state[face][1] = temp[3];
        state[face][2] = temp[0];
        state[face][3] = temp[7];
        state[face][4] = temp[4];
        state[face][5] = temp[1];
        state[face][6] = temp[8];
        state[face][7] = temp[5];
        state[face][8] = temp[2];
    }

    public void rotateU() {
        rotateFace(UP);
        char[] temp = new char[3];
        temp[0] = state[FRONT][0]; temp[1] = state[FRONT][1]; temp[2] = state[FRONT][2];
        state[FRONT][0] = state[RIGHT][0]; state[FRONT][1] = state[RIGHT][1]; state[FRONT][2] = state[RIGHT][2];
        state[RIGHT][0] = state[BACK][0]; state[RIGHT][1] = state[BACK][1]; state[RIGHT][2] = state[BACK][2];
        state[BACK][0] = state[LEFT][0]; state[BACK][1] = state[LEFT][1]; state[BACK][2] = state[LEFT][2];
        state[LEFT][0] = temp[0]; state[LEFT][1] = temp[1]; state[LEFT][2] = temp[2];
    }

    public void rotateD() {
        rotateFace(DOWN);
        char[] temp = new char[3];
        temp[0] = state[FRONT][6]; temp[1] = state[FRONT][7]; temp[2] = state[FRONT][8];
        state[FRONT][6] = state[LEFT][6]; state[FRONT][7] = state[LEFT][7]; state[FRONT][8] = state[LEFT][8];
        state[LEFT][6] = state[BACK][6]; state[LEFT][7] = state[BACK][7]; state[LEFT][8] = state[BACK][8];
        state[BACK][6] = state[RIGHT][6]; state[BACK][7] = state[RIGHT][7]; state[BACK][8] = state[RIGHT][8];
        state[RIGHT][6] = temp[0]; state[RIGHT][7] = temp[1]; state[RIGHT][8] = temp[2];
    }

    public void rotateF() {
        rotateFace(FRONT);
        char[] temp = new char[3];
        temp[0] = state[UP][6]; temp[1] = state[UP][7]; temp[2] = state[UP][8];
        state[UP][6] = state[LEFT][8]; state[UP][7] = state[LEFT][5]; state[UP][8] = state[LEFT][2];
        state[LEFT][2] = state[DOWN][0]; state[LEFT][5] = state[DOWN][1]; state[LEFT][8] = state[DOWN][2];
        state[DOWN][0] = state[RIGHT][6]; state[DOWN][1] = state[RIGHT][3]; state[DOWN][2] = state[RIGHT][0];
        state[RIGHT][0] = temp[0]; state[RIGHT][3] = temp[1]; state[RIGHT][6] = temp[2];
    }

    public void rotateB() {
        rotateFace(BACK);
        char[] temp = new char[3];
        temp[0] = state[UP][0]; temp[1] = state[UP][1]; temp[2] = state[UP][2];
        state[UP][0] = state[RIGHT][2]; state[UP][1] = state[RIGHT][5]; state[UP][2] = state[RIGHT][8];
        state[RIGHT][2] = state[DOWN][8]; state[RIGHT][5] = state[DOWN][7]; state[RIGHT][8] = state[DOWN][6];
        state[DOWN][6] = state[LEFT][0]; state[DOWN][7] = state[LEFT][3]; state[DOWN][8] = state[LEFT][6];
        state[LEFT][0] = temp[2]; state[LEFT][3] = temp[1]; state[LEFT][6] = temp[0];
    }

    public void rotateL() {
        rotateFace(LEFT);
        char[] temp = new char[3];
        temp[0] = state[UP][0]; temp[1] = state[UP][3]; temp[2] = state[UP][6];
        state[UP][0] = state[BACK][8]; state[UP][3] = state[BACK][5]; state[UP][6] = state[BACK][2];
        state[BACK][2] = state[DOWN][6]; state[BACK][5] = state[DOWN][3]; state[BACK][8] = state[DOWN][0];
        state[DOWN][0] = state[FRONT][0]; state[DOWN][3] = state[FRONT][3]; state[DOWN][6] = state[FRONT][6];
        state[FRONT][0] = temp[0]; state[FRONT][3] = temp[1]; state[FRONT][6] = temp[2];
    }

    public void rotateR() {
        rotateFace(RIGHT);
        char[] temp = new char[3];
        temp[0] = state[UP][2]; temp[1] = state[UP][5]; temp[2] = state[UP][8];
        state[UP][2] = state[FRONT][2]; state[UP][5] = state[FRONT][5]; state[UP][8] = state[FRONT][8];
        state[FRONT][2] = state[DOWN][2]; state[FRONT][5] = state[DOWN][5]; state[FRONT][8] = state[DOWN][8];
        state[DOWN][2] = state[BACK][6]; state[DOWN][5] = state[BACK][3]; state[DOWN][8] = state[BACK][0];
        state[BACK][0] = temp[2]; state[BACK][3] = temp[1]; state[BACK][6] = temp[0];
    }

    public void performMove(int move) {
        switch (move) {
            case 0: rotateU(); break;
            case 1: rotateL(); break;
            case 2: rotateF(); break;
            case 3: rotateR(); break;
            case 4: rotateB(); break;
            case 5: rotateD(); break;
        }
    }

    public String getMoveName(int move) {
        switch (move) {
            case 0: return "U";
            case 1: return "L";
            case 2: return "F";
            case 3: return "R";
            case 4: return "B";
            case 5: return "D";
            default: return "";
        }
    }

    public boolean isSolved() {
        for (int i = 0; i < 6; i++) {
            char center = state[i][4];
            for (int j = 0; j < 9; j++) {
                if (state[i][j] != center) return false;
            }
        }
        return true;
    }
}
