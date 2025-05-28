import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Lớp DistanceCalculator tính toán khoảng cách giữa các vị trí trong kho hàng
 * Được đơn giản hóa cho cấu trúc kho mới: mỗi ô hàng có 1 điểm tiếp cận duy nhất
 */
public class DistanceCalculator {
    private static WarehouseMap warehouseMap;
    private static Map<String, Float> distanceCache = new HashMap<>();
    private static Map<String, ArrayList<int[]>> pathCache = new HashMap<>();
    private static boolean hasPrecomputedAllDistances = false;
    private static Position currentRobotPosition = null;

    /**
     * Khởi tạo với bản đồ kho hàng
     */
    public static void initialize(WarehouseMap map) {
        warehouseMap = map;
        distanceCache.clear();
        pathCache.clear();
        hasPrecomputedAllDistances = false;
        currentRobotPosition = null;
        System.out.println("DistanceCalculator đã được khởi tạo với cấu trúc kho mới");
    }

    /**
     * Đặt vị trí hiện tại của robot
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
     */
    public static Position getCurrentRobotPosition() {
        return currentRobotPosition;
    }

    /**
     * Tính trước tất cả khoảng cách
     * Đơn giản hóa vì mỗi ô hàng chỉ có 1 điểm tiếp cận
     */
    public static void precomputeAllDistances(ArrayList<Merchandise> warehousing, Position counterPosition) {
        if (hasPrecomputedAllDistances) return;

        System.out.println("Đang tính toán trước tất cả khoảng cách (cấu trúc đơn giản hóa)...");
        setCurrentRobotPosition(counterPosition);

        int totalCalculations = warehousing.size() * (warehousing.size() + 1); // n*(n+1) cho counter + inter-item
        int completed = 0;

        // Tính khoảng cách từ counter đến mỗi mặt hàng
        for (Merchandise item : warehousing) {
            Position itemPos = item.getPosition();
            setCurrentRobotPosition(counterPosition);

            String key = getCacheKey(counterPosition, itemPos);
            float distance = computeDistance(counterPosition, itemPos);
            distanceCache.put(key, distance);

            ArrayList<int[]> path = findPathBetween(counterPosition, itemPos);
            if (path != null && !path.isEmpty()) {
                pathCache.put(key, path);
            }

            completed++;
            if (completed % 10 == 0) {
                System.out.println("Tiến độ: " + completed + "/" + totalCalculations + " khoảng cách");
            }
        }

        // Tính khoảng cách giữa các cặp mặt hàng
        for (int i = 0; i < warehousing.size(); i++) {
            Merchandise item1 = warehousing.get(i);
            Position pos1 = item1.getPosition();

            for (int j = 0; j < warehousing.size(); j++) {
                if (i == j) continue;

                Merchandise item2 = warehousing.get(j);
                Position pos2 = item2.getPosition();

                setCurrentRobotPosition(pos1);

                String key = getCacheKey(pos1, pos2);
                float distance = computeDistance(pos1, pos2);
                distanceCache.put(key, distance);

                ArrayList<int[]> path = findPathBetween(pos1, pos2);
                if (path != null && !path.isEmpty()) {
                    pathCache.put(key, path);
                }

                completed++;
            }
        }

        // Tính khoảng cách từ mỗi mặt hàng về counter
        for (Merchandise item : warehousing) {
            Position itemPos = item.getPosition();
            setCurrentRobotPosition(itemPos);

            String key = getCacheKey(itemPos, counterPosition);
            float distance = computeDistance(itemPos, counterPosition);
            distanceCache.put(key, distance);

            ArrayList<int[]> path = findPathBetween(itemPos, counterPosition);
            if (path != null && !path.isEmpty()) {
                pathCache.put(key, path);
            }

            completed++;
        }

        setCurrentRobotPosition(counterPosition);
        hasPrecomputedAllDistances = true;
        System.out.println("Hoàn thành tính trước " + distanceCache.size() + " khoảng cách và " + pathCache.size() + " đường đi.");
    }

    /**
     * Tạo khóa duy nhất cho cache
     */
    private static String getCacheKey(Position pos1, Position pos2) {
        int hash1 = pos1.hashCode();
        int hash2 = pos2.hashCode();
        return hash1 + "->" + hash2;
    }

    /**
     * Tính khoảng cách thực tế giữa hai vị trí
     * Đơn giản hóa vì mỗi ô hàng chỉ có 1 điểm tiếp cận duy nhất
     */
    private static float computeDistance(Position pos1, Position pos2) {
        if (warehouseMap == null) {
            return calculateManhattanDistance(pos1, pos2);
        }

        // Chuyển đổi từ Position sang tọa độ 2D
        int[] coords1 = warehouseMap.positionToCoordinates(pos1);
        int[] coords2 = warehouseMap.positionToCoordinates(pos2);

        // Tìm điểm tiếp cận duy nhất cho mỗi vị trí
        int[] accessPoint1 = warehouseMap.findUniqueAccessPoint(coords1[0], coords1[1]);
        int[] accessPoint2 = warehouseMap.findUniqueAccessPoint(coords2[0], coords2[1]);

        // Tính đường đi giữa hai điểm tiếp cận
        ArrayList<int[]> path = warehouseMap.findShortestPath(
                accessPoint1[0], accessPoint1[1],
                accessPoint2[0], accessPoint2[1]
        );

        if (path == null || path.isEmpty()) {
            return calculateManhattanDistance(pos1, pos2);
        }

        // Khoảng cách = số bước đi + phí truy cập kệ hàng
        float distance = path.size() - 1;

        // Thêm 0.5 cho mỗi vị trí kệ hàng (không đi được trực tiếp)
        if (!warehouseMap.isWalkable(coords1[0], coords1[1])) {
            distance += 0.5f;
        }
        if (!warehouseMap.isWalkable(coords2[0], coords2[1])) {
            distance += 0.5f;
        }

        return distance;
    }

    /**
     * Tìm đường đi giữa hai vị trí - Đơn giản hóa
     */
    private static ArrayList<int[]> findPathBetween(Position pos1, Position pos2) {
        if (warehouseMap == null) {
            return new ArrayList<>();
        }

        int[] coords1 = warehouseMap.positionToCoordinates(pos1);
        int[] coords2 = warehouseMap.positionToCoordinates(pos2);

        // Tìm điểm tiếp cận duy nhất cho mỗi vị trí
        int[] accessPoint1 = warehouseMap.findUniqueAccessPoint(coords1[0], coords1[1]);
        int[] accessPoint2 = warehouseMap.findUniqueAccessPoint(coords2[0], coords2[1]);

        return warehouseMap.findShortestPath(accessPoint1[0], accessPoint1[1], accessPoint2[0], accessPoint2[1]);
    }

    /**
     * Tính khoảng cách từ vị trí 1 đến vị trí 2, cập nhật vị trí hiện tại của robot
     */
    public static float calculateDistance(Position pos1, Position pos2) {
        if (pos1 == null || pos2 == null) {
            System.out.println("CẢNH BÁO: Vị trí null được truyền vào calculateDistance()");
            return 0.0f;
        }

        Position actualStartPosition = (currentRobotPosition != null) ? currentRobotPosition : pos1;

        // Kiểm tra cache
        String cacheKey = getCacheKey(actualStartPosition, pos2);
        if (distanceCache.containsKey(cacheKey)) {
            updateCurrentPosition(pos2);
            return distanceCache.get(cacheKey);
        }

        // Tính khoảng cách mới
        float distance = computeDistance(actualStartPosition, pos2);
        distanceCache.put(cacheKey, distance);

        updateCurrentPosition(pos2);
        return distance;
    }

    /**
     * Cập nhật vị trí hiện tại của robot sau khi di chuyển đến vị trí đích
     * Đơn giản hóa vì mỗi ô hàng chỉ có 1 điểm tiếp cận
     */
    private static void updateCurrentPosition(Position targetPos) {
        if (warehouseMap != null) {
            int[] coords = warehouseMap.positionToCoordinates(targetPos);

            if (!warehouseMap.isWalkable(coords[0], coords[1])) {
                // Tìm điểm tiếp cận duy nhất
                int[] accessPoint = warehouseMap.findUniqueAccessPoint(coords[0], coords[1]);
                currentRobotPosition = warehouseMap.coordinatesToPosition(accessPoint[0], accessPoint[1]);
            } else {
                currentRobotPosition = targetPos.copy();
            }
        } else {
            currentRobotPosition = targetPos.copy();
        }
    }

    /**
     * Tính khoảng cách Manhattan giữa hai vị trí
     */
    public static float calculateManhattanDistance(Position pos1, Position pos2) {
        if (pos1 == null || pos2 == null) {
            System.out.println("CẢNH BÁO: Vị trí null được truyền vào calculateManhattanDistance()");
            return 0.0f;
        }

        int xDiff = Math.abs(pos1.x - pos2.x);
        int yDiff = Math.abs(pos1.y - pos2.y);

        // Với cấu trúc mới, tier chỉ ảnh hưởng ít đến khoảng cách
        float tierDistance = 0;
        if (pos1.getShelf() == pos2.getShelf() && pos1.getSlot() == pos2.getSlot()) {
            tierDistance = 0.1f * Math.abs(pos1.getTier() - pos2.getTier()); // Rất nhỏ vì cùng cột
        }

        return xDiff + yDiff + tierDistance;
    }

    /**
     * Tìm đường đi ngắn nhất từ vị trí 1 đến vị trí 2
     */
    public static ArrayList<int[]> findPath(Position pos1, Position pos2) {
        if (pos1 == null || pos2 == null || warehouseMap == null) {
            System.out.println("CẢNH BÁO: Tham số null trong findPath()");
            return new ArrayList<>();
        }

        Position actualStartPosition = (currentRobotPosition != null) ? currentRobotPosition : pos1;

        // Kiểm tra cache
        String cacheKey = getCacheKey(actualStartPosition, pos2);
        if (pathCache.containsKey(cacheKey)) {
            updateCurrentPosition(pos2);
            return new ArrayList<>(pathCache.get(cacheKey));
        }

        // Tìm đường đi mới
        ArrayList<int[]> path = findPathBetween(actualStartPosition, pos2);

        if (path != null && !path.isEmpty()) {
            pathCache.put(cacheKey, new ArrayList<>(path));
        }

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
        System.out.println("Cache đã được xóa");
    }

    /**
     * Lấy kích thước cache
     */
    public static int getCacheSize() {
        return distanceCache.size();
    }

    /**
     * In thông tin debug
     */
    public static void printCacheInfo() {
        System.out.println("=== THÔNG TIN CACHE ===");
        System.out.println("Số khoảng cách đã cache: " + distanceCache.size());
        System.out.println("Số đường đi đã cache: " + pathCache.size());
        System.out.println("Đã tính trước toàn bộ: " + hasPrecomputedAllDistances);
        System.out.println("Vị trí robot hiện tại: " + currentRobotPosition);
    }
}