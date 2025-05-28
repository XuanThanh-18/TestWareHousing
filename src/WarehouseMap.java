import java.util.ArrayList;
import java.util.PriorityQueue;

/**
 * Lớp WarehouseMap đại diện cho bản đồ kho hàng
 * Cấu trúc: các khối kệ hình chữ nhật với lối đi xung quanh
 * Mỗi ô hàng chỉ có 1 điểm tiếp cận duy nhất
 * 0: Ô đi được (lối đi)
 * 1: Ô kệ hàng (không đi được trực tiếp)
 */
public class WarehouseMap {
    private final int[][] map;
    private final int rows;
    private final int cols;

    public WarehouseMap(int[][] map) {
        this.map = map;
        this.rows = map.length;
        this.cols = (rows > 0) ? map[0].length : 0;
    }

    public int getCell(int row, int col) {
        if (isValidPosition(row, col)) {
            return map[row][col];
        }
        return 1;
    }

    /**
     * Tìm điểm tiếp cận DUY NHẤT cho một ô hàng
     * Với cấu trúc kho mới, mỗi ô hàng chỉ có 1 điểm tiếp cận
     */
    public int[] findUniqueAccessPoint(int row, int col) {
        // Nếu vị trí đã đi được, trả về chính nó
        if (isWalkable(row, col)) {
            return new int[] {row, col};
        }

        // Xác định điểm tiếp cận duy nhất dựa trên vị trí trong khối kệ
        // Cấu trúc kho:
        // 0 0 0 0 0 0  <- lối đi
        // 0 1 1 1 1 0  <- kệ hàng (hàng 1 của khối)
        // 0 1 1 1 1 0  <- kệ hàng (hàng 2 của khối)
        // 0 0 0 0 0 0  <- lối đi

        // Xác định vị trí trong khối kệ
        int blockRow = row % 3; // 0=lối đi, 1=hàng trên kệ, 2=hàng dưới kệ

        if (blockRow == 1) {
            // Hàng trên của khối kệ -> điểm tiếp cận ở phía trên
            return new int[] {row - 1, col};
        } else if (blockRow == 2) {
            // Hàng dưới của khối kệ -> điểm tiếp cận ở phía dưới
            return new int[] {row + 1, col};
        }

        // Trường hợp đặc biệt: nếu không xác định được, tìm điểm gần nhất
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            if (isValidPosition(newRow, newCol) && isWalkable(newRow, newCol)) {
                return new int[] {newRow, newCol};
            }
        }

        return new int[] {row, col};
    }

    /**
     * Các phương thức tương thích ngược
     */
    public int[] findNearestAccessPoint(int row, int col) {
        return findUniqueAccessPoint(row, col);
    }

    public int[] findNearestAccessPoint(int row, int col, int targetRow, int targetCol) {
        return findUniqueAccessPoint(row, col);
    }

    public int[] findOptimalAccessPoint(int row, int col, int currentRow, int currentCol, int targetRow, int targetCol) {
        return findUniqueAccessPoint(row, col);
    }

    public boolean isValidPosition(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    public boolean isWalkable(int row, int col) {
        if (!isValidPosition(row, col)) {
            return false;
        }
        return map[row][col] == 0;
    }

    public boolean isPositionWalkable(Position position) {
        int[] coords = positionToCoordinates(position);
        return isWalkable(coords[0], coords[1]);
    }

    /**
     * Chuyển đổi từ vị trí (shelf, tier, slot) thành tọa độ 2D
     * Cấu trúc đơn giản hơn:
     * - Shelf 0: Counter (vị trí [0,0])
     * - Shelf 1: Khối kệ 1 (hàng 1-2)
     * - Shelf 2: Khối kệ 2 (hàng 4-5)
     * - Tier 1: Hàng trên của khối kệ
     * - Tier 2: Hàng dưới của khối kệ
     * - Slot: Cột (1-4 tương ứng cột 1-4)
     */
    public int[] positionToCoordinates(Position position) {
        int shelf = position.getShelf();
        int tier = position.getTier();
        int slot = position.getSlot();

        // Counter
        if (shelf == 0) {
            return new int[] {0, 0};
        }

        // Tính hàng dựa trên shelf và tier
        int baseRow = (shelf - 1) * 3 + 1; // Khối kệ bắt đầu từ hàng 1, 4, 7, ...
        int row = baseRow + (tier - 1);     // tier 1 -> hàng baseRow, tier 2 -> hàng baseRow+1

        // Cột trực tiếp từ slot
        int col = slot;

        // Đảm bảo trong phạm vi hợp lệ
        row = Math.max(0, Math.min(row, rows - 1));
        col = Math.max(0, Math.min(col, cols - 1));

        return new int[] {row, col};
    }

    /**
     * Chuyển đổi từ tọa độ 2D thành vị trí
     */
    public Position coordinatesToPosition(int row, int col) {
        // Counter
        if (row == 0 && col == 0) {
            return new Position(0, 0, 0);
        }

        // Lối đi ngang
        if (row % 3 == 0 && row > 0) {
            return new Position(0, 0, col); // Vị trí lối đi
        }

        // Kệ hàng
        int shelf = (row - 1) / 3 + 1;
        int tier = (row - 1) % 3 + 1;
        int slot = col;

        return new Position(shelf, tier, slot);
    }

    /**
     * Tìm đường đi ngắn nhất sử dụng A*
     */
    public ArrayList<int[]> findShortestPath(int startRow, int startCol, int endRow, int endCol) {
        if (!isValidPosition(startRow, startCol) || !isValidPosition(endRow, endCol)) {
            return new ArrayList<>();
        }

        if (!isWalkable(startRow, startCol) || !isWalkable(endRow, endCol)) {
            return new ArrayList<>();
        }

        if (startRow == endRow && startCol == endCol) {
            ArrayList<int[]> path = new ArrayList<>();
            path.add(new int[] {startRow, startCol});
            return path;
        }

        // Tối ưu hóa: kiểm tra đường đi trực tiếp
        if (canMoveDirectly(startRow, startCol, endRow, endCol)) {
            return createDirectPath(startRow, startCol, endRow, endCol);
        }

        return findPathAStar(startRow, startCol, endRow, endCol);
    }

    private boolean canMoveDirectly(int startRow, int startCol, int endRow, int endCol) {
        if (startRow == endRow) {
            // Đường ngang
            int minCol = Math.min(startCol, endCol);
            int maxCol = Math.max(startCol, endCol);
            for (int col = minCol; col <= maxCol; col++) {
                if (!isWalkable(startRow, col)) {
                    return false;
                }
            }
            return true;
        }

        if (startCol == endCol) {
            // Đường dọc
            int minRow = Math.min(startRow, endRow);
            int maxRow = Math.max(startRow, endRow);
            for (int row = minRow; row <= maxRow; row++) {
                if (!isWalkable(row, startCol)) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    private ArrayList<int[]> createDirectPath(int startRow, int startCol, int endRow, int endCol) {
        ArrayList<int[]> path = new ArrayList<>();

        if (startRow == endRow) {
            int step = (startCol < endCol) ? 1 : -1;
            for (int col = startCol; col != endCol + step; col += step) {
                path.add(new int[] {startRow, col});
            }
        } else {
            int step = (startRow < endRow) ? 1 : -1;
            for (int row = startRow; row != endRow + step; row += step) {
                path.add(new int[] {row, startCol});
            }
        }

        return path;
    }

    private ArrayList<int[]> findPathAStar(int startRow, int startCol, int endRow, int endCol) {
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>();
        boolean[][] closedSet = new boolean[rows][cols];
        int[][][] parent = new int[rows][cols][2];
        float[][] gScore = new float[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                gScore[i][j] = Float.MAX_VALUE;
            }
        }

        gScore[startRow][startCol] = 0;
        openSet.add(new AStarNode(startRow, startCol, 0, heuristic(startRow, startCol, endRow, endCol)));

        int[] dr = {-1, 0, 1, 0};
        int[] dc = {0, 1, 0, -1};
        boolean found = false;

        while (!openSet.isEmpty()) {
            AStarNode current = openSet.poll();
            int row = current.row;
            int col = current.col;

            if (row == endRow && col == endCol) {
                found = true;
                break;
            }

            closedSet[row][col] = true;

            for (int i = 0; i < 4; i++) {
                int newRow = row + dr[i];
                int newCol = col + dc[i];

                if (isValidPosition(newRow, newCol) && isWalkable(newRow, newCol) && !closedSet[newRow][newCol]) {
                    float tentativeGScore = gScore[row][col] + 1.0f;

                    if (tentativeGScore < gScore[newRow][newCol]) {
                        parent[newRow][newCol] = new int[] {row, col};
                        gScore[newRow][newCol] = tentativeGScore;
                        float fScore = tentativeGScore + heuristic(newRow, newCol, endRow, endCol);

                        openSet.removeIf(node -> node.row == newRow && node.col == newCol);
                        openSet.add(new AStarNode(newRow, newCol, tentativeGScore, fScore));
                    }
                }
            }
        }

        if (!found) {
            return new ArrayList<>();
        }

        // Truy vết đường đi
        ArrayList<int[]> path = new ArrayList<>();
        int[] current = {endRow, endCol};
        path.add(0, current.clone());

        while (current[0] != startRow || current[1] != startCol) {
            current = parent[current[0]][current[1]];
            path.add(0, current.clone());
        }

        return path;
    }

    private float heuristic(int row1, int col1, int row2, int col2) {
        return Math.abs(row1 - row2) + Math.abs(col1 - col2);
    }

    public float calculateActualDistance(Position pos1, Position pos2) {
        int[] coords1 = positionToCoordinates(pos1);
        int[] coords2 = positionToCoordinates(pos2);

        boolean startPointIsShelf = false;
        if (!isWalkable(coords1[0], coords1[1])) {
            startPointIsShelf = true;
            coords1 = findUniqueAccessPoint(coords1[0], coords1[1]);
        }

        boolean endPointIsShelf = false;
        if (!isWalkable(coords2[0], coords2[1])) {
            endPointIsShelf = true;
            coords2 = findUniqueAccessPoint(coords2[0], coords2[1]);
        }

        ArrayList<int[]> path = findShortestPath(coords1[0], coords1[1], coords2[0], coords2[1]);

        if (path.isEmpty()) {
            return Math.abs(pos1.x - pos2.x) + Math.abs(pos1.y - pos2.y);
        }

        float distance = path.size() - 1;

        if (startPointIsShelf) distance += 0.5f;
        if (endPointIsShelf) distance += 0.5f;

        return distance;
    }

    /**
     * Tạo bản đồ từ thông tin kho hàng theo cấu trúc mới
     */
    public static WarehouseMap createMapFromWarehouse(int shelves, int slots) {
        int rows = shelves * 3 + 1;
        int cols = slots;

        int[][] map = new int[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (i % 3 == 0) {
                    // Lối đi ngang
                    map[i][j] = 0;
                } else {
                    // Hàng kệ
                    if (j == 0 || j == cols - 1) {
                        map[i][j] = 0; // Lối đi hai bên
                    } else {
                        map[i][j] = 1; // Kệ hàng
                    }
                }
            }
        }

        return new WarehouseMap(map);
    }

    public void printMap() {
        System.out.println("Bản đồ kho hàng (" + rows + "x" + cols + "):");
        System.out.print("   ");
        for (int j = 0; j < cols; j++) {
            System.out.printf("%2d", j);
        }
        System.out.println();

        for (int i = 0; i < rows; i++) {
            System.out.printf("%2d ", i);
            for (int j = 0; j < cols; j++) {
                if (map[i][j] == 0) {
                    System.out.print(" ·");
                } else {
                    System.out.print(" ■");
                }
            }
            System.out.println();
        }
    }

    public void printPathOnMap(ArrayList<int[]> path) {
        char[][] displayMap = new char[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (map[i][j] == 0) {
                    displayMap[i][j] = '·';
                } else {
                    displayMap[i][j] = '■';
                }
            }
        }

        for (int i = 0; i < path.size(); i++) {
            int[] pos = path.get(i);
            if (i == 0) {
                displayMap[pos[0]][pos[1]] = 'S';
            } else if (i == path.size() - 1) {
                displayMap[pos[0]][pos[1]] = 'E';
            } else {
                displayMap[pos[0]][pos[1]] = '•';
            }
        }

        System.out.println("Đường đi trên bản đồ:");
        System.out.print("   ");
        for (int j = 0; j < cols; j++) {
            System.out.printf("%2d", j);
        }
        System.out.println();

        for (int i = 0; i < rows; i++) {
            System.out.printf("%2d ", i);
            for (int j = 0; j < cols; j++) {
                System.out.print(" " + displayMap[i][j]);
            }
            System.out.println();
        }
    }

    private class AStarNode implements Comparable<AStarNode> {
        int row, col;
        float gScore;
        float fScore;

        public AStarNode(int row, int col, float gScore, float fScore) {
            this.row = row;
            this.col = col;
            this.gScore = gScore;
            this.fScore = fScore;
        }

        @Override
        public int compareTo(AStarNode other) {
            return Float.compare(this.fScore, other.fScore);
        }
    }
}