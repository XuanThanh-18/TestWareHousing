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
    private final int[][] map;
    private final int rows;
    private final int cols;

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
     * Tìm điểm tiếp cận gần nhất cho một vị trí trên kệ, có thể tối ưu dựa trên đích đến
     * @param row Hàng của vị trí trên kệ
     * @param col Cột của vị trí trên kệ
     * @param targetRow Hàng của vị trí đích (tùy chọn, -1 nếu không có)
     * @param targetCol Cột của vị trí đích (tùy chọn, -1 nếu không có)
     * @return Mảng int[2] chứa tọa độ [row, col] của điểm tiếp cận
     */
    public int[] findNearestAccessPoint(int row, int col, int targetRow, int targetCol) {
        // Nếu vị trí đã đi được, trả về chính nó
        if (isWalkable(row, col)) {
            System.out.println("Debug: Vị trí [" + row + ", " + col + "] đã đi được, trả về nó");
            return new int[] {row, col};
        }

        System.out.println("Debug: Tìm điểm tiếp cận cho [" + row + ", " + col + "] với đích [" + targetRow + ", " + targetCol + "]");

        // Danh sách các điểm tiếp cận có thể
        ArrayList<int[]> accessPoints = new ArrayList<>();

        // Kiểm tra hàng trên
        if (row > 0 && isWalkable(row - 1, col)) {
            int[] point = new int[] {row - 1, col};
            accessPoints.add(point);
            System.out.println("Debug: Tìm thấy điểm tiếp cận trên: [" + point[0] + ", " + point[1] + "]");
        }

        // Kiểm tra hàng dưới
        if (row < rows - 1 && isWalkable(row + 1, col)) {
            int[] point = new int[] {row + 1, col};
            accessPoints.add(point);
            System.out.println("Debug: Tìm thấy điểm tiếp cận dưới: [" + point[0] + ", " + point[1] + "]");
        }

        // Nếu không có đích đến hoặc chỉ có một điểm tiếp cận
        if (targetRow < 0 || targetCol < 0 || accessPoints.size() <= 1) {
            int[] result = accessPoints.isEmpty() ? new int[] {row, col} : accessPoints.get(0);
            System.out.println("Debug: Không có đích hoặc chỉ có một điểm tiếp cận, trả về: [" + result[0] + ", " + result[1] + "]");
            return result;
        }

        // Nếu có đích đến và nhiều điểm tiếp cận, chọn điểm tối ưu nhất
        int bestIndex = 0;
        int minDistance = Integer.MAX_VALUE;

        System.out.println("Debug: Đánh giá các điểm tiếp cận để tìm điểm tối ưu...");
        for (int i = 0; i < accessPoints.size(); i++) {
            int[] point = accessPoints.get(i);
            int distance = Math.abs(point[0] - targetRow) + Math.abs(point[1] - targetCol);
            System.out.println("Debug: Điểm [" + point[0] + ", " + point[1] + "] có khoảng cách đến đích: " + distance);

            if (distance < minDistance) {
                minDistance = distance;
                bestIndex = i;
                System.out.println("Debug: Đây là điểm tốt nhất hiện tại");
            }
        }

        int[] result = accessPoints.get(bestIndex);
        System.out.println("Debug: Điểm tiếp cận tối ưu nhất: [" + result[0] + ", " + result[1] + "]");
        return result;
    }

    // Overload phương thức để tương thích với code cũ
    public int[] findNearestAccessPoint(int row, int col) {
        return findNearestAccessPoint(row, col, -1, -1);
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
        return getCell(row, col) == 0;
    }

    /**
     * Chuyển đổi từ vị trí (shelf, tier, slot) thành tọa độ 2D (row, col)
     * @param position Vị trí trong kho
     * @return Mảng 2 phần tử [row, col]
     */
    public int[] positionToCoordinates(Position position) {
        // Mỗi kệ chiếm 2 dòng (một cho lối đi, một cho kệ)
        int row = (position.getShelf()-1) * 2 + 1; // kệ nằm ở dòng lẻ
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
        int shelf = (row / 2) + 1;
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

        // Tối ưu hóa: Nếu là cùng vị trí
        if (startRow == endRow && startCol == endCol) {
            ArrayList<int[]> path = new ArrayList<>();
            path.add(new int[] {startRow, startCol});
            return path;
        }

        // Tối ưu hóa: Kiểm tra xem liệu đường đi Manhattan có khả thi
        // Nếu không có chướng ngại vật trên đường đi trực tiếp, không cần chạy BFS
        boolean canUseDirectPath = true;
        // Kiểm tra đường ngang
        if (startRow == endRow) {
            int minCol = Math.min(startCol, endCol);
            int maxCol = Math.max(startCol, endCol);
            for (int col = minCol + 1; col < maxCol; col++) {
                if (!isWalkable(startRow, col)) {
                    canUseDirectPath = false;
                    break;
                }
            }

            if (canUseDirectPath) {
                ArrayList<int[]> path = new ArrayList<>();
                path.add(new int[] {startRow, startCol});

                // Thêm các điểm trung gian
                if (startCol < endCol) {
                    for (int col = startCol + 1; col <= endCol; col++) {
                        path.add(new int[] {startRow, col});
                    }
                } else {
                    for (int col = startCol - 1; col >= endCol; col--) {
                        path.add(new int[] {startRow, col});
                    }
                }

                return path;
            }
        }

        // Kiểm tra đường dọc
        if (startCol == endCol) {
            int minRow = Math.min(startRow, endRow);
            int maxRow = Math.max(startRow, endRow);
            for (int row = minRow + 1; row < maxRow; row++) {
                if (!isWalkable(row, startCol)) {
                    canUseDirectPath = false;
                    break;
                }
            }

            if (canUseDirectPath) {
                ArrayList<int[]> path = new ArrayList<>();
                path.add(new int[] {startRow, startCol});

                // Thêm các điểm trung gian
                if (startRow < endRow) {
                    for (int row = startRow + 1; row <= endRow; row++) {
                        path.add(new int[] {row, startCol});
                    }
                } else {
                    for (int row = startRow - 1; row >= endRow; row--) {
                        path.add(new int[] {row, startCol});
                    }
                }

                return path;
            }
        }

        // Mảng đánh dấu ô đã thăm
        boolean[][] visited = new boolean[rows][cols];
        // Mảng lưu trữ ô cha (để truy vết đường đi)
        int[][][] parent = new int[rows][cols][2];

        // Khởi tạo hàng đợi cho BFS - Sử dụng ArrayDeque thay vì LinkedList để cải thiện hiệu suất
        java.util.Deque<int[]> queue = new java.util.ArrayDeque<>();
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

            // Tối ưu hóa: Ưu tiên hướng về phía đích
            int[] priorities = new int[4];
            for (int i = 0; i < 4; i++) {
                priorities[i] = i;
            }

            // Sắp xếp hướng theo độ ưu tiên (hướng nào gần đích hơn sẽ được xem xét trước)
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3 - i; j++) {
                    int dir1 = priorities[j];
                    int dir2 = priorities[j + 1];

                    int dist1 = Math.abs((row + dr[dir1]) - endRow) + Math.abs((col + dc[dir1]) - endCol);
                    int dist2 = Math.abs((row + dr[dir2]) - endRow) + Math.abs((col + dc[dir2]) - endCol);

                    if (dist1 > dist2) {
                        // Hoán đổi
                        priorities[j] = dir2;
                        priorities[j + 1] = dir1;
                    }
                }
            }

            // Thử tất cả các hướng di chuyển theo thứ tự ưu tiên
            for (int i = 0; i < 4; i++) {
                int dir = priorities[i];
                int newRow = row + dr[dir];
                int newCol = col + dc[dir];

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

        // Tìm điểm tiếp cận tối ưu, có xét đến vị trí đích
        int[] accessPoint1 = findNearestAccessPoint(coords1[0], coords1[1], coords2[0], coords2[1]);
        int[] accessPoint2 = findNearestAccessPoint(coords2[0], coords2[1], coords1[0], coords1[1]);

        // Tìm đường đi ngắn nhất giữa hai điểm tiếp cận
        ArrayList<int[]> path = findShortestPath(accessPoint1[0], accessPoint1[1],
                accessPoint2[0], accessPoint2[1]);

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

        // Thêm khoảng cách từ điểm trên kệ đến điểm tiếp cận (thường là 0.5 đơn vị)
        // Chỉ cộng thêm nếu điểm tiếp cận khác với điểm ban đầu
        if (coords1[0] != accessPoint1[0] || coords1[1] != accessPoint1[1]) {
            distance += 0.5f; // Khoảng cách để lấy/đặt hàng từ kệ
        }

        if (coords2[0] != accessPoint2[0] || coords2[1] != accessPoint2[1]) {
            distance += 0.5f; // Khoảng cách để lấy/đặt hàng từ kệ
        }

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