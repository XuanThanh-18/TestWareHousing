import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Lớp WarehouseMap đại diện cho bản đồ cửa hàng/kho hàng
 * Sử dụng ma trận 2 chiều để biểu diễn không gian
 * 0: Ô đi được
 * 1: Ô kệ hàng (không đi được)
 */
public class WarehouseMap {
    private int[][] map;
    private int rows;
    private int cols;

    /**
     * Khởi tạo bản đồ từ ma trận
     * @param map Ma trận biểu diễn bản đồ
     */
    public WarehouseMap(int[][] map) {
        this.map = map;
        this.rows = map.length;
        this.cols = (rows > 0) ? map[0].length : 0;
    }

    /**
     * Lấy ô tại vị trí (row, col)
     * @param row Hàng
     * @param col Cột
     * @return Giá trị ô (0: đi được, 1: không đi được)
     */
    public int getCell(int row, int col) {
        if (isValidPosition(row, col)) {
            return map[row][col];
        }
        return 1; // Mặc định là không đi được nếu vị trí không hợp lệ
    }

    /**
     * Kiểm tra vị trí có hợp lệ không
     * @param row Hàng
     * @param col Cột
     * @return true nếu vị trí hợp lệ, false nếu không
     */
    public boolean isValidPosition(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    /**
     * Kiểm tra xem vị trí có đi được không
     * @param row Hàng
     * @param col Cột
     * @return true nếu đi được, false nếu không
     */
    public boolean isWalkable(int row, int col) {
        return isValidPosition(row, col) && map[row][col] == 0;
    }

    /**
     * Chuyển đổi từ vị trí (shelf, tier, slot) thành tọa độ 2D (row, col)
     * @param position Vị trí trong kho
     * @return Mảng 2 phần tử [row, col]
     */
    public int[] positionToCoordinates(Position position) {
        // Mỗi kệ chiếm 2 dòng (một cho lối đi, một cho kệ)
        int row = position.getShelf() * 2 + 1; // kệ nằm ở dòng lẻ
        int col = position.getSlot();

        return new int[] {row, col};
    }

    /**
     * Chuyển đổi từ tọa độ 2D (row, col) thành vị trí (shelf, tier, slot)
     * @param row Hàng
     * @param col Cột
     * @return Vị trí trong kho
     */
    public Position coordinatesToPosition(int row, int col) {
        int shelf = row / 2;
        int tier = 0; // Mặc định tier là 0 vì không thể xác định từ ma trận 2D
        int slot = col;

        return new Position(shelf, tier, slot);
    }

    /**
     * Tính đường đi ngắn nhất từ vị trí nguồn đến đích sử dụng thuật toán BFS
     * @param startRow Hàng bắt đầu
     * @param startCol Cột bắt đầu
     * @param endRow Hàng kết thúc
     * @param endCol Cột kết thúc
     * @return Danh sách các tọa độ [row, col] biểu diễn đường đi
     */
    public ArrayList<int[]> findShortestPath(int startRow, int startCol, int endRow, int endCol) {
        // Kiểm tra vị trí hợp lệ
        if (!isValidPosition(startRow, startCol) || !isValidPosition(endRow, endCol)) {
            return new ArrayList<>();
        }

        // Kiểm tra vị trí đi được
        if (!isWalkable(startRow, startCol) || !isWalkable(endRow, endCol)) {
            return new ArrayList<>();
        }

        // Mảng đánh dấu ô đã thăm
        boolean[][] visited = new boolean[rows][cols];
        // Mảng lưu trữ ô cha (để truy vết đường đi)
        int[][][] parent = new int[rows][cols][2];

        // Khởi tạo hàng đợi cho BFS
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[] {startRow, startCol});
        visited[startRow][startCol] = true;

        // Mảng các hướng di chuyển (lên, phải, xuống, trái)
        int[] dr = {-1, 0, 1, 0};
        int[] dc = {0, 1, 0, -1};

        // BFS
        boolean found = false;
        while (!queue.isEmpty() && !found) {
            int[] current = queue.poll();
            int row = current[0];
            int col = current[1];

            // Kiểm tra nếu đã đến đích
            if (row == endRow && col == endCol) {
                found = true;
                break;
            }

            // Thử tất cả các hướng di chuyển
            for (int i = 0; i < 4; i++) {
                int newRow = row + dr[i];
                int newCol = col + dc[i];

                // Kiểm tra vị trí mới có hợp lệ, đi được và chưa thăm
                if (isValidPosition(newRow, newCol) && isWalkable(newRow, newCol) && !visited[newRow][newCol]) {
                    queue.add(new int[] {newRow, newCol});
                    visited[newRow][newCol] = true;
                    parent[newRow][newCol] = new int[] {row, col};
                }
            }
        }

        // Nếu không tìm thấy đường đi
        if (!found) {
            return new ArrayList<>();
        }

        // Truy vết đường đi từ đích về nguồn
        ArrayList<int[]> path = new ArrayList<>();
        int[] current = {endRow, endCol};
        path.add(0, current.clone());

        while (current[0] != startRow || current[1] != startCol) {
            current = parent[current[0]][current[1]];
            path.add(0, current.clone());
        }

        return path;
    }

    /**
     * Tính khoảng cách thực tế giữa hai vị trí trên bản đồ (theo đường đi thực)
     * @param pos1 Vị trí bắt đầu
     * @param pos2 Vị trí kết thúc
     * @return Khoảng cách thực tế (số bước đi)
     */
    public float calculateActualDistance(Position pos1, Position pos2) {
        // Chuyển đổi từ Position sang tọa độ 2D
        int[] coords1 = positionToCoordinates(pos1);
        int[] coords2 = positionToCoordinates(pos2);

        // Tìm đường đi ngắn nhất
        ArrayList<int[]> path = findShortestPath(coords1[0], coords1[1], coords2[0], coords2[1]);

        // Nếu không tìm thấy đường đi
        if (path.isEmpty()) {
            // Trả về khoảng cách Manhattan như cũ
            int xDiff = Math.abs(pos1.x - pos2.x);
            int yDiff = Math.abs(pos1.y - pos2.y);
            float tierDistance = 0.5f * (pos1.getTier() + pos2.getTier() - 2);
            return xDiff + yDiff + tierDistance;
        }

        // Tính khoảng cách dựa trên số bước đi
        float distance = path.size() - 1; // Số bước đi = số ô - 1

        // Thêm khoảng cách theo tầng
        float tierDistance = 0;
        if (pos1.getShelf() == pos2.getShelf() && pos1.getSlot() == pos2.getSlot()) {
            tierDistance = 0.5f * Math.abs(pos1.getTier() - pos2.getTier());
        } else {
            tierDistance = 0.5f * (pos1.getTier() + pos2.getTier() - 2);
        }

        return distance + tierDistance;
    }

    /**
     * Tạo bản đồ từ thông tin kho hàng
     * @param shelves Số kệ hàng
     * @param slots Số ô trên mỗi tầng
     * @return Bản đồ kho hàng
     */
    public static WarehouseMap createMapFromWarehouse(int shelves, int slots) {
        // Mỗi kệ chiếm 1 dòng và có 1 dòng lối đi
        int rows = shelves * 2 + 1;
        int cols = slots;

        int[][] map = new int[rows][cols];

        // Thiết lập các kệ hàng (dòng lẻ)
        for (int i = 1; i < rows; i += 2) {
            for (int j = 0; j < cols; j++) {
                map[i][j] = 1; // Kệ hàng
            }
        }

        return new WarehouseMap(map);
    }

    /**
     * In bản đồ ra console
     */
    public void printMap() {
        System.out.println("Bản đồ kho hàng (" + rows + "x" + cols + "):");
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (map[i][j] == 0) {
                    System.out.print("  "); // Ô đi được
                } else {
                    System.out.print("■ "); // Kệ hàng
                }
            }
            System.out.println();
        }
    }

    /**
     * In đường đi trên bản đồ
     * @param path Đường đi biểu diễn bằng mảng tọa độ
     */
    public void printPathOnMap(ArrayList<int[]> path) {
        char[][] displayMap = new char[rows][cols];

        // Khởi tạo bản đồ hiển thị
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (map[i][j] == 0) {
                    displayMap[i][j] = ' '; // Ô đi được
                } else {
                    displayMap[i][j] = '■'; // Kệ hàng
                }
            }
        }

        // Đánh dấu đường đi
        for (int i = 0; i < path.size(); i++) {
            int[] pos = path.get(i);
            if (i == 0) {
                displayMap[pos[0]][pos[1]] = 'S'; // Điểm bắt đầu
            } else if (i == path.size() - 1) {
                displayMap[pos[0]][pos[1]] = 'E'; // Điểm kết thúc
            } else {
                displayMap[pos[0]][pos[1]] = '•'; // Đường đi
            }
        }

        // In bản đồ với đường đi
        System.out.println("Đường đi trên bản đồ:");
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                System.out.print(displayMap[i][j] + " ");
            }
            System.out.println();
        }
    }
}