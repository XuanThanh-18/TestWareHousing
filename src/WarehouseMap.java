import java.util.ArrayList;
import java.util.LinkedList;
import java.util.PriorityQueue;
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
     * Tìm điểm tiếp cận tối ưu cho một vị trí, có xét đến vị trí hiện tại của robot
     * @param row Hàng của vị trí cần tìm điểm tiếp cận
     * @param col Cột của vị trí cần tìm điểm tiếp cận
     * @param currentRow Hàng của vị trí hiện tại của robot (hoặc -1 nếu không có)
     * @param currentCol Cột của vị trí hiện tại của robot (hoặc -1 nếu không có)
     * @param targetRow Hàng của vị trí đích (hoặc -1 nếu không có)
     * @param targetCol Cột của vị trí đích (hoặc -1 nếu không có)
     * @return Mảng int[2] chứa tọa độ [row, col] của điểm tiếp cận
     */
    public int[] findOptimalAccessPoint(int row, int col, int currentRow, int currentCol, int targetRow, int targetCol) {
        // Nếu vị trí đã đi được, trả về chính nó
        if (isWalkable(row, col)) {
            return new int[] {row, col};
        }

        // Danh sách các điểm tiếp cận có thể
        ArrayList<int[]> accessPoints = new ArrayList<>();

        // Kiểm tra các điểm xung quanh
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; // trên, dưới, trái, phải
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            if (isValidPosition(newRow, newCol) && isWalkable(newRow, newCol)) {
                accessPoints.add(new int[] {newRow, newCol});
            }
        }

        if (accessPoints.isEmpty()) {
            // Nếu không tìm thấy điểm tiếp cận, trả về vị trí ban đầu
            return new int[] {row, col};
        }

        if (accessPoints.size() == 1) {
            // Nếu chỉ có một điểm tiếp cận, trả về điểm đó
            return accessPoints.get(0);
        }

        // Nếu có nhiều điểm tiếp cận, chọn điểm tối ưu
        int bestIndex = 0;
        float bestScore = Float.MAX_VALUE;

        for (int i = 0; i < accessPoints.size(); i++) {
            int[] point = accessPoints.get(i);

            // Tính khoảng cách từ vị trí hiện tại đến điểm tiếp cận
            float distFromCurrent = 0;
            if (currentRow >= 0 && currentCol >= 0) {
                distFromCurrent = Math.abs(point[0] - currentRow) + Math.abs(point[1] - currentCol);
            }

            // Tính khoảng cách từ điểm tiếp cận đến đích (nếu có)
            float distToTarget = 0;
            if (targetRow >= 0 && targetCol >= 0) {
                distToTarget = Math.abs(point[0] - targetRow) + Math.abs(point[1] - targetCol);
            }

            // Điểm số tổng hợp (ưu tiên điểm tiếp cận gần với vị trí hiện tại)
            float score = distFromCurrent * 0.7f + distToTarget * 0.3f;

            if (score < bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }

        return accessPoints.get(bestIndex);
    }

    /**
     * Tìm điểm tiếp cận gần nhất cho một vị trí trên kệ
     * @param row Hàng của vị trí cần tìm điểm tiếp cận
     * @param col Cột của vị trí cần tìm điểm tiếp cận
     * @return Mảng int[2] chứa tọa độ [row, col] của điểm tiếp cận
     */
    public int[] findNearestAccessPoint(int row, int col) {
        // Nếu vị trí đã đi được, trả về chính nó
        if (isWalkable(row, col)) {
            return new int[] {row, col};
        }

        // Bán kính tìm kiếm
        int maxRadius = Math.max(rows, cols);

        // Các hướng di chuyển: trên, dưới, trái, phải
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        // Kiểm tra hàng chẵn ở trên và dưới hàng hiện tại
        // Hàng lẻ là kệ, hàng chẵn là lối đi
        if (row % 2 == 1) {  // Nếu đang ở hàng lẻ (kệ)
            // Ưu tiên tìm lối đi ở hàng trên và dưới
            if (row - 1 >= 0 && isWalkable(row - 1, col)) {
                return new int[] {row - 1, col};  // Lối đi phía trên
            }
            if (row + 1 < rows && isWalkable(row + 1, col)) {
                return new int[] {row + 1, col};  // Lối đi phía dưới
            }
        }

        // Nếu không tìm thấy điểm tiếp cận trực tiếp, thực hiện tìm kiếm bán kính
        for (int radius = 1; radius <= maxRadius; radius++) {
            for (int[] dir : directions) {
                int newRow = row + dir[0] * radius;
                int newCol = col + dir[1] * radius;

                if (isValidPosition(newRow, newCol) && isWalkable(newRow, newCol)) {
                    return new int[] {newRow, newCol};
                }
            }
        }

        // Nếu không tìm thấy điểm tiếp cận, trả về vị trí gần nhất có thể đi được
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (isWalkable(i, j)) {
                    return new int[] {i, j};
                }
            }
        }

        // Trường hợp cực kỳ hiếm: không có ô nào đi được trên bản đồ
        return new int[] {0, 0};
    }
    /**
     * Phương thức tương thích ngược với phiên bản cũ có tham số đích
     * @param row Hàng của vị trí trên kệ
     * @param col Cột của vị trí trên kệ
     * @param targetRow Hàng của vị trí đích
     * @param targetCol Cột của vị trí đích
     * @return Mảng int[2] chứa tọa độ [row, col] của điểm tiếp cận
     */
    public int[] findNearestAccessPoint(int row, int col, int targetRow, int targetCol) {
        return findOptimalAccessPoint(row, col, -1, -1, targetRow, targetCol);
    }

    /**
     * Kiểm tra xem vị trí có hợp lệ không
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
        // Kiểm tra vị trí hợp lệ trước
        if (!isValidPosition(row, col)) {
            return false; // Vị trí không hợp lệ thì không đi được
        }

        // Vị trí hợp lệ, kiểm tra giá trị trong bản đồ
        // Giá trị 0 là ô đi được, 1 là ô không đi được (kệ hàng)
        return map[row][col] == 0;
    }

    /**
     * Kiểm tra xem một vị trí trong kho có đi được không
     * @param position Vị trí cần kiểm tra
     * @return true nếu đi được, false nếu không
     */
    public boolean isPositionWalkable(Position position) {
        int[] coords = positionToCoordinates(position);
        return isWalkable(coords[0], coords[1]);
    }

    /**
     * Chuyển đổi từ vị trí (shelf, tier, slot) thành tọa độ 2D (row, col)
     * @param position Vị trí trong kho
     * @return Mảng 2 phần tử [row, col]
     */
    public int[] positionToCoordinates(Position position) {
        int shelf = position.getShelf();
        int tier = position.getTier();
        int slot = position.getSlot();

        // Điều chỉnh giá trị shelf và slot để không có giá trị âm
        shelf = Math.max(0, shelf);
        slot = Math.max(0, slot);

        // Mỗi kệ chiếm 2 dòng (một cho lối đi, một cho kệ)
        // Kệ 0 đại diện cho lối đi chính (dọc)
        // Kệ từ 1 trở lên đại diện cho các kệ hàng

        int row;
        if (shelf == 0) {
            // Kệ 0 là lối đi dọc (cột 0)
            row = 0;
        } else {
            // Với các kệ từ 1 trở lên
            if (tier == 0) {
                // Tier 0 là lối đi ngang
                row = shelf * 2;
            } else {
                // Tier > 0 là vị trí trên kệ
                row = shelf * 2 - 1;
            }
        }

        // Đảm bảo tọa độ nằm trong khoảng hợp lệ
        if (row < 0) row = 0;
//        if (col < 0) col = 0;
        if (row >= rows) row = rows - 1;
        if (slot >= cols) slot = cols - 1;

        return new int[] {row, slot};  // Sử dụng slot trực tiếp làm cột
    }

    /**
     * Chuyển đổi từ tọa độ 2D (row, col) thành vị trí (shelf, tier, slot)
     * @param row Hàng
     * @param col Cột
     * @return Vị trí trong kho
     */
    public Position coordinatesToPosition(int row, int col) {
        int shelf;
        int tier;

        if (row == 0) {
            // Hàng 0 là lối đi dọc (kệ 0)
            shelf = 0;
            tier = 0;
        } else if (row % 2 == 0) {
            // Hàng chẵn khác 0 là lối đi ngang
            shelf = row / 2;
            tier = 0;
        } else {
            // Hàng lẻ là kệ
            shelf = (row + 1) / 2;
            tier = 1;  // Mặc định là tier 1 cho vị trí trên kệ
        }

        // Slot chính là cột
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

        // Mảng mở - chứa các node đang xem xét
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>();
        // Mảng đóng - chứa các node đã xem xét
        boolean[][] closedSet = new boolean[rows][cols];
        // Mảng lưu trữ ô cha (để truy vết đường đi)
        int[][][] parent = new int[rows][cols][2];
        // Mảng lưu giá trị g (chi phí từ điểm bắt đầu đến node hiện tại)
        float[][] gScore = new float[rows][cols];
        // Khởi tạo tất cả gScore với giá trị vô cùng
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                gScore[i][j] = Float.MAX_VALUE;
            }
        }

        // Điểm bắt đầu có gScore = 0
        gScore[startRow][startCol] = 0;

        // Thêm node bắt đầu vào openSet
        openSet.add(new AStarNode(startRow, startCol, 0, heuristic(startRow, startCol, endRow, endCol)));


        // Mảng các hướng di chuyển (lên, phải, xuống, trái)
        int[] dr = {-1, 0, 1, 0};
        int[] dc = {0, 1, 0, -1};
        boolean found = false;
        while (!openSet.isEmpty()) {
            // Lấy node có fScore thấp nhất từ openSet
            AStarNode current = openSet.poll();
            int row = current.row;
            int col = current.col;

            // Nếu đã đến đích
            if (row == endRow && col == endCol) {
                found = true;
                break;
            }

            // Đánh dấu đã xem xét
            closedSet[row][col] = true;

            // Xem xét tất cả các hướng di chuyển
            for (int i = 0; i < 4; i++) {
                int newRow = row + dr[i];
                int newCol = col + dc[i];

                // Kiểm tra vị trí mới có hợp lệ, đi được và chưa ở trong closedSet
                if (isValidPosition(newRow, newCol) && isWalkable(newRow, newCol) && !closedSet[newRow][newCol]) {
                    // Chi phí đến node mới
                    float tentativeGScore = gScore[row][col] + 1.0f;  // Khoảng cách giữa hai ô liền kề là 1.0

                    // Nếu tìm thấy đường tốt hơn đến node này
                    if (tentativeGScore < gScore[newRow][newCol]) {
                        // Cập nhật parent
                        parent[newRow][newCol] = new int[] {row, col};
                        // Cập nhật gScore
                        gScore[newRow][newCol] = tentativeGScore;
                        // Tính fScore = gScore + heuristic
                        float fScore = tentativeGScore + heuristic(newRow, newCol, endRow, endCol);

                        // Thêm vào openSet hoặc cập nhật nếu đã tồn tại
                        boolean inOpenSet = false;
                        for (AStarNode node : openSet) {
                            if (node.row == newRow && node.col == newCol) {
                                inOpenSet = true;
                                // Nếu tìm thấy đường tốt hơn, cập nhật fScore
                                if (fScore < node.fScore) {
                                    openSet.remove(node);
                                    openSet.add(new AStarNode(newRow, newCol, tentativeGScore, fScore));
                                }
                                break;
                            }
                        }

                        if (!inOpenSet) {
                            openSet.add(new AStarNode(newRow, newCol, tentativeGScore, fScore));
                        }
                    }
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
    // Hàm heuristic (khoảng cách Manhattan)
    private float heuristic(int row1, int col1, int row2, int col2) {
        return Math.abs(row1 - row2) + Math.abs(col1 - col2);
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

        // Tìm điểm tiếp cận cho vị trí bắt đầu nếu không đi được
        boolean startPointIsShelf = false;
        if (!isWalkable(coords1[0], coords1[1])) {
            startPointIsShelf = true;
            int[] accessCoords = findNearestAccessPoint(coords1[0], coords1[1]);
            coords1 = accessCoords;
        }

        // Tìm điểm tiếp cận cho vị trí kết thúc nếu không đi được
        boolean endPointIsShelf = false;
        if (!isWalkable(coords2[0], coords2[1])) {
            endPointIsShelf = true;
            int[] accessCoords = findNearestAccessPoint(coords2[0], coords2[1]);
            coords2 = accessCoords;
        }

        // Tìm đường đi ngắn nhất giữa hai điểm tiếp cận
        ArrayList<int[]> path = findShortestPath(coords1[0], coords1[1], coords2[0], coords2[1]);

        // Nếu không tìm thấy đường đi
        if (path.isEmpty()) {
            // Trả về khoảng cách Manhattan
            int xDiff = Math.abs(pos1.x - pos2.x);
            int yDiff = Math.abs(pos1.y - pos2.y);
            float tierDistance = 0.5f * Math.max(0, (pos1.getTier() + pos2.getTier() - 2));
            return xDiff + yDiff + tierDistance;
        }

        // Tính khoảng cách dựa trên số bước đi
        float distance = path.size() - 1; // Số bước đi = số ô - 1

        // Thêm khoảng cách từ điểm trên kệ đến điểm tiếp cận (thường là 0.5 đơn vị)
        if (startPointIsShelf) {
            distance += 0.5f;
        }

        if (endPointIsShelf) {
            distance += 0.5f;
        }

        return distance;
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
    private class AStarNode implements Comparable<AStarNode> {
        int row, col;
        float gScore;  // Chi phí từ điểm bắt đầu đến node này
        float fScore;  // gScore + heuristic

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