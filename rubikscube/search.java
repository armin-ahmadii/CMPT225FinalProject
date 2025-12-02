package rubikscube;

import java.util.ArrayList;
import java.util.List;

public class search {

    private static final int MAX_DEPTH = 100; 
    private static final int FOUND = -1; 

    // pdb tables - heuristic
    private static byte[][] edgeDistTable = new byte[24][24];
    private static byte[][] cornerDistTable = new byte[24][24];

    static {
        initializeEdgeDistTable();
        initializeCornerDistTable();
    }
    
    // buffers
    private static StringBuilder currentSolutionPath;

    public static String solve(cube initialcube) {
        StringBuilder totalSolution = new StringBuilder();

        cube current = initialcube.clone();

        // cross
        System.out.println("Solving Cross...");
        String sol1 = idaStar(current, search::isCrossSolved, search::crossHeuristic);
        totalSolution.append(sol1);
        applyMoves(current, sol1);
        System.out.println("Cross Solved: " + sol1);

        // corners
        String sol2 = idaStar(current, search::isCornersSolved, search::cornersHeuristic);
        totalSolution.append(sol2);
        applyMoves(current, sol2);
        System.out.println("Corners Solved: " + sol2);

        // middle edge (F2L)
        String sol3 = idaStar(current, search::isF2LSolved, search::f2lHeuristic);
        totalSolution.append(sol3);
        applyMoves(current, sol3);
        System.out.println("F2L Solved: " + sol3);

        // edge orientation (Top cross)
        String sol4 = idaStar(current, search::isTopCrossSolved, search::topCrossHeuristic);
        totalSolution.append(sol4);
        applyMoves(current, sol4);
        System.out.println("EO Solved: " + sol4);

        // OLL 
        String sol5 = idaStar(current, search::isTopFaceSolved, search::topFaceHeuristic);
        totalSolution.append(sol5);
        applyMoves(current, sol5);
        System.out.println("OLL Solved: " + sol5);

        // corner permutation
        String sol6 = idaStar(current, search::isCornerPermuted, search::cpHeuristic);
        totalSolution.append(sol6);
        applyMoves(current, sol6);
        System.out.println("CP Solved: " + sol6);

        // edge permutation
        String sol7 = idaStar(current, cube::isSolved, search::epHeuristic);
        totalSolution.append(sol7);
        applyMoves(current, sol7);
        System.out.println("EP Solved: " + sol7);

        return totalSolution.toString();
    }

    private static void applyMoves(cube cube, String moves) {
        for (char c : moves.toCharArray()) {
            int move = getMoveIndex(c);
            cube.performMove(move);
        }
    }
    
    // i did this to iterate backwards 
    private static void applyInverseMoves(cube cube, String moves) {
        for (int i = moves.length() - 1; i >= 0; i--) {
            char c = moves.charAt(i);
            int move = getMoveIndex(c);
            cube.performInverseMove(move);
        }
    }

    private static int getMoveIndex(char c) {
        switch (c) {
            case 'U': return 0;
            case 'L': return 1;
            case 'F': return 2;
            case 'R': return 3;
            case 'B': return 4;
            case 'D': return 5;
            default: return -1;
        }
    }

    // we start ida logic here
    private static String idaStar(cube start, GoalPredicate goal, HeuristicFunction h) {
        int threshold = h.calculate(start);
        while (true) {
            currentSolutionPath = new StringBuilder();
            int nextThreshold = search(start, 0, threshold, goal, h, -1, 0);
            
            if (nextThreshold == FOUND) {
                return currentSolutionPath.toString();
            }
            if (nextThreshold == Integer.MAX_VALUE) {
                return "FAIL"; 
            }
            threshold = nextThreshold;
            if (threshold > MAX_DEPTH)
                return "FAIL_DEPTH";
        }
    }

    // the algorithm macros, just look at jperms thing and use those
    static class Macro {
        String name;
        String moves;
        int cost; 

        Macro(String name, String moves, int cost) {
            this.name = name;
            this.moves = moves;
            this.cost = cost;
        }
    }

    private static List<Macro> macros = new ArrayList<>();

    // jperm thingy (human algos)
    static {
        macros.add(new Macro("I-Shape", "FRURRRUUUFFF", 2));
        macros.add(new Macro("Sune", "RURRRURUURRR", 2)); 
        macros.add(new Macro("PLL (Ub)", "RRURURRRUUURRRUUURRRURRR", 2)); 
        macros.add(new Macro("PLL (A)", "RRRFRRRBBRFFFRRRBBRR", 2));
    }

    private static int search(cube cube, int g, int threshold, GoalPredicate goal, HeuristicFunction h, int lastMove, int consecutiveMoves) {
        int estimatedCost = h.calculate(cube) * 3; // weight of the heuristic is changed here, 3 is the sweet spot w my trial and error but u can try the other values too
        int f = g + estimatedCost;

        if (f > threshold) {
            return f;
        }

        if (goal.test(cube)) {
            return FOUND;
        }

        int min = Integer.MAX_VALUE;

        for (int i = 0; i < 6; i++) {
            
            // optimization : no 4 moves (BBBB or UUUU)
            if (i == lastMove && consecutiveMoves >= 3) {
                continue;
            }

            // optimization : commutative thingy i talked about
            if (lastMove != -1 && isCommutativePruned(lastMove, i)) {
                continue;
            }

            
            cube.performMove(i);  
            int nextConsecutive = (i == lastMove) ? consecutiveMoves + 1 : 1;
            int res = search(cube, g + 1, threshold, goal, h, i, nextConsecutive);
            
            if (res == FOUND) {
                currentSolutionPath.insert(0, cube.getMoveName(i));
                cube.performInverseMove(i);
                return FOUND;
            }
            
            if (res < min) {
                min = res;
            }

            cube.performInverseMove(i);
        }

        if (isF2LSolved(cube)) {
            for (Macro m : macros) {
                applyMoves(cube, m.moves);
                int res = search(cube, g + m.cost, threshold, goal, h, -1, 0);
                
                if (res == FOUND) {
                    currentSolutionPath.insert(0, m.moves);
                    applyInverseMoves(cube, m.moves);
                    return FOUND;
                }
                
                if (res < min) {
                    min = res;
                }
                
                //undo macrdo
                applyInverseMoves(cube, m.moves);
            }
        }

        return min;
    }

    // logic for the commutative thingy
    private static boolean isCommutativePruned(int last, int current) {
        if (last == 0 && current == 5) return false; // U then D ok
        if (last == 5 && current == 0) return true;  // D then U prune

        if (last == 1 && current == 3) return false; // L then R ok
        if (last == 3 && current == 1) return true;  // R then L prune

        if (last == 2 && current == 4) return false; // F then B ok
        if (last == 4 && current == 2) return true;  // B then F prune

        return false;
    }

    @FunctionalInterface
    interface GoalPredicate {
        boolean test(cube c);
    }

    @FunctionalInterface
    interface HeuristicFunction {
        int calculate(cube c);
    }

    public static boolean isCrossSolved(cube c) {
        char dColor = c.state[cube.DOWN][4];
        char fColor = c.state[cube.FRONT][4];
        char bColor = c.state[cube.BACK][4];
        char lColor = c.state[cube.LEFT][4];
        char rColor = c.state[cube.RIGHT][4];

        if (c.state[cube.DOWN][1] != dColor || c.state[cube.FRONT][7] != fColor) return false;
        if (c.state[cube.DOWN][3] != dColor || c.state[cube.LEFT][7] != lColor) return false;
        if (c.state[cube.DOWN][5] != dColor || c.state[cube.RIGHT][7] != rColor) return false;
        if (c.state[cube.DOWN][7] != dColor || c.state[cube.BACK][7] != bColor) return false;
        return true;
    }

    public static boolean isCornersSolved(cube c) {
        if (!isCrossSolved(c)) return false;
        char d = c.state[cube.DOWN][4];
        char f = c.state[cube.FRONT][4];
        char b = c.state[cube.BACK][4];
        char l = c.state[cube.LEFT][4];
        char r = c.state[cube.RIGHT][4];

        if (c.state[cube.DOWN][0] != d || c.state[cube.FRONT][6] != f || c.state[cube.LEFT][8] != l) return false;
        if (c.state[cube.DOWN][2] != d || c.state[cube.FRONT][8] != f || c.state[cube.RIGHT][6] != r) return false;
        if (c.state[cube.DOWN][6] != d || c.state[cube.BACK][8] != b || c.state[cube.LEFT][6] != l) return false;
        if (c.state[cube.DOWN][8] != d || c.state[cube.BACK][6] != b || c.state[cube.RIGHT][8] != r) return false;
        return true;
    }

    public static boolean isF2LSolved(cube c) {
        if (!isCornersSolved(c)) return false;
        char f = c.state[cube.FRONT][4];
        char b = c.state[cube.BACK][4];
        char l = c.state[cube.LEFT][4];
        char r = c.state[cube.RIGHT][4];

        if (c.state[cube.FRONT][3] != f || c.state[cube.LEFT][5] != l) return false;
        if (c.state[cube.FRONT][5] != f || c.state[cube.RIGHT][3] != r) return false;
        if (c.state[cube.BACK][5] != b || c.state[cube.LEFT][3] != l) return false;
        if (c.state[cube.BACK][3] != b || c.state[cube.RIGHT][5] != r) return false;
        return true;
    }

    public static boolean isTopCrossSolved(cube c) {
        if (!isF2LSolved(c)) return false;
        char u = c.state[cube.UP][4];
        return c.state[cube.UP][1] == u && c.state[cube.UP][3] == u &&
                c.state[cube.UP][5] == u && c.state[cube.UP][7] == u;
    }

    public static boolean isTopFaceSolved(cube c) {
        if (!isTopCrossSolved(c)) return false;
        char u = c.state[cube.UP][4];
        return c.state[cube.UP][0] == u && c.state[cube.UP][2] == u &&
                c.state[cube.UP][6] == u && c.state[cube.UP][8] == u;
    }
    
    public static boolean isCornerPermuted(cube c) {
        if (!isTopFaceSolved(c)) return false;
        char f = c.state[cube.FRONT][4];
        char b = c.state[cube.BACK][4];
        char l = c.state[cube.LEFT][4];
        char r = c.state[cube.RIGHT][4];

        return c.state[cube.FRONT][0] == f && c.state[cube.LEFT][2] == l &&
                c.state[cube.FRONT][2] == f && c.state[cube.RIGHT][0] == r &&
                c.state[cube.BACK][2] == b && c.state[cube.LEFT][0] == l &&
                c.state[cube.BACK][0] == b && c.state[cube.RIGHT][2] == r;
    }

    public static int epHeuristic(cube c) {
        if (!isCornerPermuted(c)) return 2;
        int count = 0;
        char f = c.state[cube.FRONT][4];
        char b = c.state[cube.BACK][4];
        char l = c.state[cube.LEFT][4];
        char r = c.state[cube.RIGHT][4];

        if (c.state[cube.FRONT][1] != f) count++;
        if (c.state[cube.LEFT][1] != l) count++;
        if (c.state[cube.BACK][1] != b) count++;
        if (c.state[cube.RIGHT][1] != r) count++;
        return count;
    }

    // heuristic parts
    private static void initializeEdgeDistTable() {
        for (int src = 0; src < 24; src++) {
            for (int dst = 0; dst < 24; dst++) {
                edgeDistTable[src][dst] = -1;
            }
        }
        for (int src = 0; src < 24; src++) {
            int[] q = new int[100];
            int head = 0, tail = 0;
            q[tail++] = src;
            edgeDistTable[src][src] = 0;
            int[] dist = new int[24];
            java.util.Arrays.fill(dist, -1);
            dist[src] = 0;
            while (head < tail) {
                int u = q[head++];
                if (dist[u] > 10) continue;
                for (int m = 0; m < 6; m++) {
                    int v = applyMoveToEdge(u, m);
                    if (dist[v] == -1) {
                        dist[v] = dist[u] + 1;
                        edgeDistTable[src][v] = (byte) dist[v];
                        q[tail++] = v;
                    }
                }
            }
        }
    }

    private static int applyMoveToEdge(int edge, int move) {
        int id = edge % 12;
        int flip = edge / 12;
        int nextId = id;
        int nextFlip = flip;
        switch (move) {
            case 0:
                if (id == 0) nextId = 1; else if (id == 1) nextId = 2;
                else if (id == 2) nextId = 3; else if (id == 3) nextId = 0;
                break;
            case 1:
                if (id == 1) nextId = 4; else if (id == 4) nextId = 9;
                else if (id == 9) nextId = 6; else if (id == 6) nextId = 1;
                break;
            case 2:
                if (id == 0) { nextId = 5; nextFlip ^= 1; }
                else if (id == 5) { nextId = 8; nextFlip ^= 1; }
                else if (id == 8) { nextId = 4; nextFlip ^= 1; }
                else if (id == 4) { nextId = 0; nextFlip ^= 1; }
                break;
            case 3:
                if (id == 3) nextId = 7; else if (id == 7) nextId = 11;
                else if (id == 11) nextId = 5; else if (id == 5) nextId = 3;
                break;
            case 4:
                if (id == 2) { nextId = 6; nextFlip ^= 1; }
                else if (id == 6) { nextId = 10; nextFlip ^= 1; }
                else if (id == 10) { nextId = 7; nextFlip ^= 1; }
                else if (id == 7) { nextId = 2; nextFlip ^= 1; }
                break;
            case 5:
                if (id == 8) nextId = 11; else if (id == 11) nextId = 10;
                else if (id == 10) nextId = 9; else if (id == 9) nextId = 8;
                break;
        }
        return nextId + nextFlip * 12;
    }

    private static void initializeCornerDistTable() {
        for (int src = 0; src < 24; src++) {
            for (int dst = 0; dst < 24; dst++) {
                cornerDistTable[src][dst] = -1;
            }
        }
        for (int src = 0; src < 24; src++) {
            int[] q = new int[100];
            int head = 0, tail = 0;
            q[tail++] = src;
            cornerDistTable[src][src] = 0;
            int[] dist = new int[24];
            java.util.Arrays.fill(dist, -1);
            dist[src] = 0;
            while (head < tail) {
                int u = q[head++];
                if (dist[u] > 10) continue;
                for (int m = 0; m < 6; m++) {
                    int v = applyMoveToCorner(u, m);
                    if (dist[v] == -1) {
                        dist[v] = dist[u] + 1;
                        cornerDistTable[src][v] = (byte) dist[v];
                        q[tail++] = v;
                    }
                }
            }
        }
    }

    private static int applyMoveToCorner(int corner, int move) {
        int id = corner % 8;
        int orient = corner / 8;
        int nextId = id;
        int nextOrient = orient;
        switch (move) {
            case 0:
                if (id == 1) nextId = 0; else if (id == 0) nextId = 2;
                else if (id == 2) nextId = 3; else if (id == 3) nextId = 1;
                break;
            case 1:
                if (id == 0) { nextId = 4; nextOrient = (orient + 1) % 3; }
                else if (id == 4) { nextId = 6; nextOrient = (orient + 2) % 3; }
                else if (id == 6) { nextId = 2; nextOrient = (orient + 1) % 3; }
                else if (id == 2) { nextId = 0; nextOrient = (orient + 2) % 3; }
                break;
            case 2:
                if (id == 0) { nextId = 1; nextOrient = (orient + 2) % 3; }
                else if (id == 1) { nextId = 5; nextOrient = (orient + 1) % 3; }
                else if (id == 5) { nextId = 4; nextOrient = (orient + 2) % 3; }
                else if (id == 4) { nextId = 0; nextOrient = (orient + 1) % 3; }
                break;
            case 3:
                if (id == 1) { nextId = 3; nextOrient = (orient + 1) % 3; }
                else if (id == 3) { nextId = 7; nextOrient = (orient + 2) % 3; }
                else if (id == 7) { nextId = 5; nextOrient = (orient + 1) % 3; }
                else if (id == 5) { nextId = 1; nextOrient = (orient + 2) % 3; }
                break;
            case 4:
                if (id == 2) { nextId = 6; nextOrient = (orient + 1) % 3; }
                else if (id == 6) { nextId = 7; nextOrient = (orient + 2) % 3; }
                else if (id == 7) { nextId = 3; nextOrient = (orient + 1) % 3; }
                else if (id == 3) { nextId = 2; nextOrient = (orient + 2) % 3; }
                break;
            case 5:
                if (id == 4) nextId = 5; else if (id == 5) nextId = 7;
                else if (id == 7) nextId = 6; else if (id == 6) nextId = 4;
                break;
        }
        return nextId + nextOrient * 8;
    }

    // state
    
    private static int getCornerState(cube c, char c1, char c2, char c3) {
        if (matchCorner(c, cube.UP, 6, cube.LEFT, 2, cube.FRONT, 0, c1, c2, c3)) return 0;
        if (matchCorner(c, cube.UP, 6, cube.LEFT, 2, cube.FRONT, 0, c2, c3, c1)) return 8;
        if (matchCorner(c, cube.UP, 6, cube.LEFT, 2, cube.FRONT, 0, c3, c1, c2)) return 16;
        if (matchCorner(c, cube.UP, 8, cube.FRONT, 2, cube.RIGHT, 0, c1, c2, c3)) return 1;
        if (matchCorner(c, cube.UP, 8, cube.FRONT, 2, cube.RIGHT, 0, c2, c3, c1)) return 9;
        if (matchCorner(c, cube.UP, 8, cube.FRONT, 2, cube.RIGHT, 0, c3, c1, c2)) return 17;
        if (matchCorner(c, cube.UP, 0, cube.BACK, 2, cube.LEFT, 0, c1, c2, c3)) return 2;
        if (matchCorner(c, cube.UP, 0, cube.BACK, 2, cube.LEFT, 0, c2, c3, c1)) return 10;
        if (matchCorner(c, cube.UP, 0, cube.BACK, 2, cube.LEFT, 0, c3, c1, c2)) return 18;
        if (matchCorner(c, cube.UP, 2, cube.RIGHT, 2, cube.BACK, 0, c1, c2, c3)) return 3;
        if (matchCorner(c, cube.UP, 2, cube.RIGHT, 2, cube.BACK, 0, c2, c3, c1)) return 11;
        if (matchCorner(c, cube.UP, 2, cube.RIGHT, 2, cube.BACK, 0, c3, c1, c2)) return 19;
        if (matchCorner(c, cube.DOWN, 0, cube.FRONT, 6, cube.LEFT, 8, c1, c2, c3)) return 4;
        if (matchCorner(c, cube.DOWN, 0, cube.FRONT, 6, cube.LEFT, 8, c2, c3, c1)) return 12;
        if (matchCorner(c, cube.DOWN, 0, cube.FRONT, 6, cube.LEFT, 8, c3, c1, c2)) return 20;
        if (matchCorner(c, cube.DOWN, 2, cube.RIGHT, 6, cube.FRONT, 8, c1, c2, c3)) return 5;
        if (matchCorner(c, cube.DOWN, 2, cube.RIGHT, 6, cube.FRONT, 8, c2, c3, c1)) return 13;
        if (matchCorner(c, cube.DOWN, 2, cube.RIGHT, 6, cube.FRONT, 8, c3, c1, c2)) return 21;
        if (matchCorner(c, cube.DOWN, 6, cube.LEFT, 6, cube.BACK, 8, c1, c2, c3)) return 6;
        if (matchCorner(c, cube.DOWN, 6, cube.LEFT, 6, cube.BACK, 8, c2, c3, c1)) return 14;
        if (matchCorner(c, cube.DOWN, 6, cube.LEFT, 6, cube.BACK, 8, c3, c1, c2)) return 22;
        if (matchCorner(c, cube.DOWN, 8, cube.BACK, 6, cube.RIGHT, 8, c1, c2, c3)) return 7;
        if (matchCorner(c, cube.DOWN, 8, cube.BACK, 6, cube.RIGHT, 8, c2, c3, c1)) return 15;
        if (matchCorner(c, cube.DOWN, 8, cube.BACK, 6, cube.RIGHT, 8, c3, c1, c2)) return 23;
        return -1;
    }

    private static boolean matchCorner(cube c, int f1, int i1, int f2, int i2, int f3, int i3, char c1, char c2, char c3) {
        return c.state[f1][i1] == c1 && c.state[f2][i2] == c2 && c.state[f3][i3] == c3;
    }

    private static int getEdgeState(cube c, char c1, char c2) {
        if (matchEdge(c, cube.UP, 7, cube.FRONT, 1, c1, c2)) return 0;
        if (matchEdge(c, cube.UP, 7, cube.FRONT, 1, c2, c1)) return 12;
        if (matchEdge(c, cube.UP, 3, cube.LEFT, 1, c1, c2)) return 1;
        if (matchEdge(c, cube.UP, 3, cube.LEFT, 1, c2, c1)) return 13;
        if (matchEdge(c, cube.UP, 1, cube.BACK, 1, c1, c2)) return 2;
        if (matchEdge(c, cube.UP, 1, cube.BACK, 1, c2, c1)) return 14;
        if (matchEdge(c, cube.UP, 5, cube.RIGHT, 1, c1, c2)) return 3;
        if (matchEdge(c, cube.UP, 5, cube.RIGHT, 1, c2, c1)) return 15;
        if (matchEdge(c, cube.FRONT, 3, cube.LEFT, 5, c1, c2)) return 4;
        if (matchEdge(c, cube.FRONT, 3, cube.LEFT, 5, c2, c1)) return 16;
        if (matchEdge(c, cube.FRONT, 5, cube.RIGHT, 3, c1, c2)) return 5;
        if (matchEdge(c, cube.FRONT, 5, cube.RIGHT, 3, c2, c1)) return 17;
        if (matchEdge(c, cube.BACK, 5, cube.LEFT, 3, c1, c2)) return 6;
        if (matchEdge(c, cube.BACK, 5, cube.LEFT, 3, c2, c1)) return 18;
        if (matchEdge(c, cube.BACK, 3, cube.RIGHT, 5, c1, c2)) return 7;
        if (matchEdge(c, cube.BACK, 3, cube.RIGHT, 5, c2, c1)) return 19;
        if (matchEdge(c, cube.DOWN, 1, cube.FRONT, 7, c1, c2)) return 8;
        if (matchEdge(c, cube.DOWN, 1, cube.FRONT, 7, c2, c1)) return 20;
        if (matchEdge(c, cube.DOWN, 3, cube.LEFT, 7, c1, c2)) return 9;
        if (matchEdge(c, cube.DOWN, 3, cube.LEFT, 7, c2, c1)) return 21;
        if (matchEdge(c, cube.DOWN, 7, cube.BACK, 7, c1, c2)) return 10;
        if (matchEdge(c, cube.DOWN, 7, cube.BACK, 7, c2, c1)) return 22;
        if (matchEdge(c, cube.DOWN, 5, cube.RIGHT, 7, c1, c2)) return 11;
        if (matchEdge(c, cube.DOWN, 5, cube.RIGHT, 7, c2, c1)) return 23;
        return -1;
    }

    private static boolean matchEdge(cube c, int f1, int i1, int f2, int i2, char c1, char c2) {
        return c.state[f1][i1] == c1 && c.state[f2][i2] == c2;
    }

    public static int crossHeuristic(cube c) {
        int sum = 0;
        char d = c.state[cube.DOWN][4];
        char f = c.state[cube.FRONT][4];
        char b = c.state[cube.BACK][4];
        char l = c.state[cube.LEFT][4];
        char r = c.state[cube.RIGHT][4];

        int p1 = getEdgeState(c, d, f); if (p1 != -1) sum += edgeDistTable[p1][8];
        int p2 = getEdgeState(c, d, l); if (p2 != -1) sum += edgeDistTable[p2][9];
        int p3 = getEdgeState(c, d, b); if (p3 != -1) sum += edgeDistTable[p3][10];
        int p4 = getEdgeState(c, d, r); if (p4 != -1) sum += edgeDistTable[p4][11];
        return sum;
    }

    public static int cornersHeuristic(cube c) {
        int h = crossHeuristic(c);
        char d = c.state[cube.DOWN][4];
        char f = c.state[cube.FRONT][4];
        char b = c.state[cube.BACK][4];
        char l = c.state[cube.LEFT][4];
        char r = c.state[cube.RIGHT][4];

        int p1 = getCornerState(c, d, f, l); if (p1 != -1) h += cornerDistTable[p1][4];
        int p2 = getCornerState(c, d, r, f); if (p2 != -1) h += cornerDistTable[p2][5];
        int p3 = getCornerState(c, d, l, b); if (p3 != -1) h += cornerDistTable[p3][6];
        int p4 = getCornerState(c, d, b, r); if (p4 != -1) h += cornerDistTable[p4][7];
        return h;
    }

    public static int f2lHeuristic(cube c) {
        int h = cornersHeuristic(c);
        char f = c.state[cube.FRONT][4];
        char b = c.state[cube.BACK][4];
        char l = c.state[cube.LEFT][4];
        char r = c.state[cube.RIGHT][4];

        int p1 = getEdgeState(c, f, l); if (p1 != -1) h += edgeDistTable[p1][4];
        int p2 = getEdgeState(c, f, r); if (p2 != -1) h += edgeDistTable[p2][5];
        int p3 = getEdgeState(c, b, l); if (p3 != -1) h += edgeDistTable[p3][6];
        int p4 = getEdgeState(c, b, r); if (p4 != -1) h += edgeDistTable[p4][7];
        return h;
    }

    public static int topCrossHeuristic(cube c) {
        if (!isF2LSolved(c)) return 2;
        int count = 0;
        char u = c.state[cube.UP][4];
        if (c.state[cube.UP][1] != u) count++;
        if (c.state[cube.UP][3] != u) count++;
        if (c.state[cube.UP][5] != u) count++;
        if (c.state[cube.UP][7] != u) count++;
        return count;
    }

    public static int topFaceHeuristic(cube c) {
        if (!isTopCrossSolved(c)) return 2;
        int count = 0;
        char u = c.state[cube.UP][4];
        if (c.state[cube.UP][0] != u) count++;
        if (c.state[cube.UP][2] != u) count++;
        if (c.state[cube.UP][6] != u) count++;
        if (c.state[cube.UP][8] != u) count++;
        return count;
    }

    public static int cpHeuristic(cube c) {
        if (!isTopFaceSolved(c)) return 2;
        int count = 0;
        char f = c.state[cube.FRONT][4];
        char b = c.state[cube.BACK][4];
        char l = c.state[cube.LEFT][4];
        char r = c.state[cube.RIGHT][4];

        if (c.state[cube.FRONT][0] != f || c.state[cube.LEFT][2] != l) count++;
        if (c.state[cube.FRONT][2] != f || c.state[cube.RIGHT][0] != r) count++;
        if (c.state[cube.BACK][2] != b || c.state[cube.LEFT][0] != l) count++;
        if (c.state[cube.BACK][0] != b || c.state[cube.RIGHT][2] != r) count++;
        return count;
    }
}