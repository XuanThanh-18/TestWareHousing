import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Lớp DistanceCalculator tính toán khoảng cách giữa các vị trí trong kho hàng
 * Hỗ trợ tính toán theo đường đi thực tế trên bản đồ
 */
public class DistanceCalculator {
    private static WarehouseMap warehouseMap;
    // Cache để lưu khoảng cách đã tính
    private static Map<String, Float> distanceCache = new HashMap<>();
    // Cache để lưu đường đi đã tính
    private static Map<String, ArrayList<int[]>> pathCache = new HashMap<>();
    // Biến theo dõi việc đã tính toàn bộ khoảng cách hay chưa
    private static boolean hasPrecomputedAllDistances = false;
    // Lưu trữ vị trí hiện tại của robot khi đi qua các điểm
    private static Position currentRobotPosition = null;

    /**
     * Khởi tạo với bản đồ kho hàng
     * @param map Bản đồ kho hàng
     */
    public static void initialize(WarehouseMap map) {
        warehouseMap = map;
        // Xóa cache cũ khi khởi tạo với bản đồ mới
        distanceCache.clear();
        pathCache.clear();
        hasPrecomputedAllDistances = false;
        currentRobotPosition = null;
    }

    /**
     * Đặt vị trí hiện tại của robot
     * @param position Vị trí hiện tại
     */
    public static void setCurrentRobotPosition(Position position) {
        if (position != null) {
            currentRobotPosition = position.copy();
        } else {
            currentRobotPosition = null;
        }
    }

    /**
     * Lấy vị trí hiện tại của robot
     * @return Vị trí hiện tại
     */
    public static Position getCurrentRobotPosition() {
        return currentRobotPosition;
    }

    /**
     * Tính trước tất cả khoảng cách giữa vị trí counter và các mặt hàng
     * cũng như giữa các mặt hàng với nhau
     * @param warehousing Danh sách mặt hàng trong kho
     * @param counterPosition Vị trí counter
     */
    public static void precomputeAllDistances(ArrayList<Merchandise> warehousing, Position counterPosition) {
        if (hasPrecomputedAllDistances) return;

        System.out.println("Đang tính toán trước tất cả khoảng cách...");

        // Đặt vị trí ban đầu là counter
        setCurrentRobotPosition(counterPosition);

        // Tính khoảng cách từ counter đến mỗi mặt hàng
        for (Merchandise item : warehousing) {
            Position itemPos = item.getPosition();

            // Đặt vị trí hiện tại là counter
            setCurrentRobotPosition(counterPosition);

            // Tính khoảng cách từ counter đến mặt hàng
            String key = getCacheKey(counterPosition, itemPos);
            float distance = computeDistance(counterPosition, itemPos);
            distanceCache.put(key, distance);

            // Tính đường đi từ counter đến mặt hàng
            ArrayList<int[]> path = findPathBetween(counterPosition, itemPos);
            if (path != null && !path.isEmpty()) {
                pathCache.put(key, path);
            }
        }

        // Tính khoảng cách giữa các cặp mặt hàng
        for (int i = 0; i < warehousing.size(); i++) {
            Merchandise item1 = warehousing.get(i);
            Position pos1 = item1.getPosition();

            for (int j = 0; j < warehousing.size(); j++) {
                if (i == j) continue; // Không tính khoảng cách của mặt hàng với chính nó

                Merchandise item2 = warehousing.get(j);
                Position pos2 = item2.getPosition();

                // Đặt vị trí hiện tại là vị trí của mặt hàng thứ nhất
                setCurrentRobotPosition(pos1);

                // Tính khoảng cách từ mặt hàng 1 đến mặt hàng 2
                String key = getCacheKey(pos1, pos2);
                float distance = computeDistance(pos1, pos2);
                distanceCache.put(key, distance);

                // Tính đường đi từ mặt hàng 1 đến mặt hàng 2
                ArrayList<int[]> path = findPathBetween(pos1, pos2);
                if (path != null && !path.isEmpty()) {
                    pathCache.put(key, path);
                }
            }
        }

        // Tính các đường đi từ mỗi mặt hàng về counter
        for (Merchandise item : warehousing) {
            Position itemPos = item.getPosition();

            // Đặt vị trí hiện tại là vị trí của mặt hàng
            setCurrentRobotPosition(itemPos);

            // Tính khoảng cách từ mặt hàng về counter
            String key = getCacheKey(itemPos, counterPosition);
            float distance = computeDistance(itemPos, counterPosition);
            distanceCache.put(key, distance);

            // Tính đường đi từ mặt hàng về counter
            ArrayList<int[]> path = findPathBetween(itemPos, counterPosition);
            if (path != null && !path.isEmpty()) {
                pathCache.put(key, path);
            }
        }

        // Reset vị trí hiện tại về counter
        setCurrentRobotPosition(counterPosition);

        hasPrecomputedAllDistances = true;
        System.out.println("Đã tính trước " + distanceCache.size() + " khoảng cách và " + pathCache.size() + " đường đi.");
    }

    /**
     * Tạo khóa duy nhất cho cache dựa trên hai vị trí
     * @param pos1 Vị trí thứ nhất
     * @param pos2 Vị trí thứ hai
     * @return Chuỗi khóa duy nhất
     */
    private static String getCacheKey(Position pos1, Position pos2) {
        // Đảm bảo khóa là duy nhất cho cặp vị trí có thứ tự
        int hash1 = pos1.hashCode();
        int hash2 = pos2.hashCode();
        return hash1 + "->" + hash2;
    }

    /**
     * Tính khoảng cách thực tế giữa hai vị trí
     * @param pos1 Vị trí bắt đầu
     * @param pos2 Vị trí kết thúc
     * @return Khoảng cách
     */
    private static float computeDistance(Position pos1, Position pos2) {
        if (warehouseMap == null) {
            return calculateManhattanDistance(pos1, pos2);
        }

        // Chuyển đổi từ Position sang tọa độ 2D
        int[] coords1 = warehouseMap.positionToCoordinates(pos1);
        int[] coords2 = warehouseMap.positionToCoordinates(pos2);

        // Kiểm tra xem các vị trí có đi được không
        boolean pos1IsWalkable = warehouseMap.isWalkable(coords1[0], coords1[1]);
        boolean pos2IsWalkable = warehouseMap.isWalkable(coords2[0], coords2[1]);

        // Nếu vị trí đầu không đi được, tìm điểm tiếp cận
        int[] accessPoint1 = coords1;
        if (!pos1IsWalkable) {
            accessPoint1 = warehouseMap.findNearestAccessPoint(coords1[0], coords1[1]);
        }

        // Nếu vị trí cuối không đi được, tìm điểm tiếp cận
        int[] accessPoint2 = coords2;
        if (!pos2IsWalkable) {
            accessPoint2 = warehouseMap.findNearestAccessPoint(coords2[0], coords2[1]);
        }

        // Tính toán đường đi giữa hai điểm tiếp cận
        ArrayList<int[]> path = warehouseMap.findShortestPath(
                accessPoint1[0], accessPoint1[1],
                accessPoint2[0], accessPoint2[1]
        );

        // Nếu không tìm được đường đi
        if (path == null || path.isEmpty()) {
            return calculateManhattanDistance(pos1, pos2);
        }

        // Tính khoảng cách dựa trên số bước đi
        float distance = path.size() - 1; // Số bước đi = số ô - 1

        // Nếu vị trí đầu không đi được, thêm khoảng cách từ vị trí đầu đến điểm tiếp cận (0.5)
        if (!pos1IsWalkable) {
            distance += 0.5f;
        }

        // Nếu vị trí cuối không đi được, thêm khoảng cách từ điểm tiếp cận đến vị trí cuối (0.5)
        if (!pos2IsWalkable) {
            distance += 0.5f;
        }

        // Log thông tin để giúp gỡ lỗi
        System.out.println("DEBUG: Khoảng cách từ " + pos1 + " đến " + pos2 + " = " + distance);
        if (!pos1IsWalkable) {
            System.out.println("DEBUG: Vị trí bắt đầu " + pos1 + " không đi được, điểm tiếp cận: [" +
                    accessPoint1[0] + ", " + accessPoint1[1] + "]");
        }
        if (!pos2IsWalkable) {
            System.out.println("DEBUG: Vị trí kết thúc " + pos2 + " không đi được, điểm tiếp cận: [" +
                    accessPoint2[0] + ", " + accessPoint2[1] + "]");
        }

        return distance;
    }

    /**
     * Tìm đường đi giữa hai vị trí
     * @param pos1 Vị trí bắt đầu
     * @param pos2 Vị trí kết thúc
     * @return Đường đi
     */
    private static ArrayList<int[]> findPathBetween(Position pos1, Position pos2) {
        if (warehouseMap == null) {
            return new ArrayList<>();
        }

        // Chuyển đổi từ Position sang tọa độ 2D
        int[] coords1 = warehouseMap.positionToCoordinates(pos1);
        int[] coords2 = warehouseMap.positionToCoordinates(pos2);

        // Nếu điểm đầu không đi được, tìm điểm tiếp cận
        if (!warehouseMap.isWalkable(coords1[0], coords1[1])) {
            int[] accessPoint = warehouseMap.findNearestAccessPoint(coords1[0], coords1[1]);
            coords1 = accessPoint;
        }

        // Nếu điểm cuối không đi được, tìm điểm tiếp cận
        if (!warehouseMap.isWalkable(coords2[0], coords2[1])) {
            int[] accessPoint = warehouseMap.findNearestAccessPoint(coords2[0], coords2[1]);
            coords2 = accessPoint;
        }

        // Tìm đường đi giữa hai điểm
        return warehouseMap.findShortestPath(coords1[0], coords1[1], coords2[0], coords2[1]);
    }

    /**
     * Tính khoảng cách từ vị trí 1 đến vị trí 2, đồng thời
     * cập nhật vị trí hiện tại của robot
     * @param pos1 Vị trí bắt đầu
     * @param pos2 Vị trí kết thúc
     * @return Khoảng cách
     */
    public static float calculateDistance(Position pos1, Position pos2) {
        // Kiểm tra vị trí null để tránh NullPointerException
        if (pos1 == null || pos2 == null) {
            System.out.println("CẢNH BÁO: Vị trí null được truyền vào DistanceCalculator.calculateDistance()");
            return 0.0f;
        }

        // Sử dụng vị trí hiện tại của robot làm điểm xuất phát nếu có
        Position actualStartPosition = (currentRobotPosition != null) ? currentRobotPosition : pos1;

        // Kiểm tra cache
        String cacheKey = getCacheKey(actualStartPosition, pos2);
        if (distanceCache.containsKey(cacheKey)) {
            // Vì đã di chuyển đến điểm đích, cập nhật vị trí hiện tại
            updateCurrentPosition(pos2);
            return distanceCache.get(cacheKey);
        }

        // Tính khoảng cách
        float distance = computeDistance(actualStartPosition, pos2);

        // Lưu vào cache
        distanceCache.put(cacheKey, distance);

        // Cập nhật vị trí hiện tại
        updateCurrentPosition(pos2);

        return distance;
    }

    /**
     * Cập nhật vị trí hiện tại của robot sau khi di chuyển đến vị trí đích
     * @param targetPos Vị trí đích
     */
    private static void updateCurrentPosition(Position targetPos) {
        if (warehouseMap != null) {
            int[] coords = warehouseMap.positionToCoordinates(targetPos);

            // Nếu vị trí đích không đi được, tìm điểm tiếp cận
            if (!warehouseMap.isWalkable(coords[0], coords[1])) {
                int[] accessPoint = warehouseMap.findNearestAccessPoint(coords[0], coords[1]);
                currentRobotPosition = warehouseMap.coordinatesToPosition(accessPoint[0], accessPoint[1]);
            } else {
                // Nếu vị trí đích đi được, cập nhật vị trí hiện tại là vị trí đích
                currentRobotPosition = targetPos.copy();
            }
        } else {
            // Nếu không có bản đồ, cập nhật vị trí hiện tại là vị trí đích
            currentRobotPosition = targetPos.copy();
        }
    }

    /**
     * Tính khoảng cách Manhattan giữa hai vị trí
     * @param pos1 Vị trí bắt đầu
     * @param pos2 Vị trí kết thúc
     * @return Khoảng cách Manhattan
     */
    public static float calculateManhattanDistance(Position pos1, Position pos2) {
        // Kiểm tra vị trí null để tránh NullPointerException
        if (pos1 == null || pos2 == null) {
            System.out.println("CẢNH BÁO: Vị trí null được truyền vào DistanceCalculator.calculateManhattanDistance()");
            return 0.0f;
        }

        // Khoảng cách Manhattan với khác biệt về tầng
        int xDiff = Math.abs(pos1.x - pos2.x);
        int yDiff = Math.abs(pos1.y - pos2.y);

        float tierDistance = 0;
        if (pos1.getShelf() == pos2.getShelf() && pos1.getSlot() == pos2.getSlot()) {
            tierDistance = 0.5f * Math.abs(pos1.getTier() - pos2.getTier());
        } else {
            tierDistance = 0.5f * Math.max(0, (pos1.getTier() + pos2.getTier() - 2));
        }

        return xDiff + yDiff + tierDistance;
    }

    /**
     * Tìm đường đi ngắn nhất từ vị trí 1 đến vị trí 2
     * @param pos1 Vị trí bắt đầu
     * @param pos2 Vị trí kết thúc
     * @return Đường đi (mảng các tọa độ)
     */
    public static ArrayList<int[]> findPath(Position pos1, Position pos2) {
        // Kiểm tra vị trí null để tránh NullPointerException
        if (pos1 == null || pos2 == null || warehouseMap == null) {
            System.out.println("CẢNH BÁO: Vị trí null hoặc bản đồ null được truyền vào DistanceCalculator.findPath()");
            return new ArrayList<>();
        }

        // Sử dụng vị trí hiện tại của robot làm điểm xuất phát nếu có
        Position actualStartPosition = (currentRobotPosition != null) ? currentRobotPosition : pos1;

        // Kiểm tra cache
        String cacheKey = getCacheKey(actualStartPosition, pos2);
        if (pathCache.containsKey(cacheKey)) {
            // Vì đã di chuyển đến điểm đích, cập nhật vị trí hiện tại
            updateCurrentPosition(pos2);
            return new ArrayList<>(pathCache.get(cacheKey));
        }

        // Tìm đường đi
        ArrayList<int[]> path = findPathBetween(actualStartPosition, pos2);

        // Lưu vào cache
        if (path != null && !path.isEmpty()) {
            pathCache.put(cacheKey, new ArrayList<>(path));
        }

        // Cập nhật vị trí hiện tại
        updateCurrentPosition(pos2);

        return path;
    }

    /**
     * Xóa cache
     */
    public static void clearCache() {
        distanceCache.clear();
        pathCache.clear();
        hasPrecomputedAllDistances = false;
        currentRobotPosition = null;
    }

    /**
     * Lấy kích thước cache
     * @return Số lượng khoảng cách đã lưu trong cache
     */
    public static int getCacheSize() {
        return distanceCache.size();
    }
}