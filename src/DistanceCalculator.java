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
        currentRobotPosition = position != null ? position.copy() : null;
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
            float distance = computeActualDistance(counterPosition, itemPos);
            distanceCache.put(key, distance);

            // Tính đường đi từ counter đến mặt hàng
            ArrayList<int[]> path = computePath(counterPosition, itemPos);
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

                // Tính toán đường đi từ pos1 đến pos2
                setCurrentRobotPosition(pos1);
                String key = getCacheKey(pos1, pos2);

                float distance = computeActualDistance(pos1, pos2);
                distanceCache.put(key, distance);

                // Lưu trữ đường đi
                ArrayList<int[]> path = computePath(pos1, pos2);
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
            float distance = computeActualDistance(itemPos, counterPosition);
            distanceCache.put(key, distance);

            // Tính đường đi từ mặt hàng về counter
            ArrayList<int[]> path = computePath(itemPos, counterPosition);
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

        // Quan trọng: Sử dụng vị trí thực tế hiện tại của robot (nếu có) làm điểm xuất phát
        Position actualStartPosition = (currentRobotPosition != null) ? currentRobotPosition : pos1;

        // Kiểm tra cache với vị trí thực tế
        String cacheKey = getCacheKey(actualStartPosition, pos2);
        if (distanceCache.containsKey(cacheKey)) {
            // Khi lấy từ cache, vẫn cập nhật vị trí hiện tại
            currentRobotPosition = pos2.copy();
            return distanceCache.get(cacheKey);
        }

        // Tính khoảng cách từ vị trí thực tế đến đích
        float distance;
        if (warehouseMap != null) {
            distance = computeActualDistance(actualStartPosition, pos2);
        } else {
            distance = calculateManhattanDistance(actualStartPosition, pos2);
        }

        distanceCache.put(cacheKey, distance);

        // Cập nhật vị trí hiện tại thành đích
        currentRobotPosition = pos2.copy();

        return distance;
    }

    // Phương thức mới để cập nhật vị trí hiện tại dựa trên đích
    private static void updateCurrentPositionBasedOnTarget(Position target) {
        if (warehouseMap != null) {
            int[] targetCoords = warehouseMap.positionToCoordinates(target);
            // Nếu vị trí đích không đi được (trên kệ), thì vị trí hiện tại sẽ là điểm tiếp cận
            if (!warehouseMap.isWalkable(targetCoords[0], targetCoords[1])) {
                int[] accessCoords = warehouseMap.findNearestAccessPoint(targetCoords[0], targetCoords[1]);
                currentRobotPosition = warehouseMap.coordinatesToPosition(accessCoords[0], accessCoords[1]);
            } else {
                // Nếu vị trí đích đi được, vị trí hiện tại sẽ là đích
                currentRobotPosition = target.copy();
            }
        } else {
            // Nếu không có bản đồ, giả định robot có thể đến đích
            currentRobotPosition = target.copy();
        }
    }

    /**
     * Phương thức nội bộ để tính khoảng cách thực tế
     * @param pos1 Vị trí bắt đầu
     * @param pos2 Vị trí kết thúc
     * @return Khoảng cách thực tế
     */
    private static float computeActualDistance(Position pos1, Position pos2) {
        if (warehouseMap != null) {
            return warehouseMap.calculateActualDistance(pos1, pos2, currentRobotPosition);
        } else {
            return calculateManhattanDistance(pos1, pos2);
        }
    }

    /**
     * Phương thức nội bộ để tính đường đi thực tế
     * @param pos1 Vị trí bắt đầu
     * @param pos2 Vị trí kết thúc
     * @return Đường đi thực tế
     */
    private static ArrayList<int[]> computePath(Position pos1, Position pos2) {
        if (warehouseMap != null) {
            int[] coords1 = warehouseMap.positionToCoordinates(pos1);
            int[] coords2 = warehouseMap.positionToCoordinates(pos2);

            // Sử dụng vị trí hiện tại để tính đường đi liên tục
            int[] currentCoords = currentRobotPosition != null ?
                    warehouseMap.positionToCoordinates(currentRobotPosition) :
                    new int[]{-1, -1};

            // Tìm điểm tiếp cận tối ưu cho vị trí bắt đầu, có xét đến vị trí hiện tại
            int[] accessPoint1 = warehouseMap.findOptimalAccessPoint(
                    coords1[0], coords1[1],
                    currentCoords[0], currentCoords[1],
                    coords2[0], coords2[1]
            );

            // Tìm điểm tiếp cận tối ưu cho vị trí kết thúc, có xét đến điểm tiếp cận của vị trí bắt đầu
            int[] accessPoint2 = warehouseMap.findOptimalAccessPoint(
                    coords2[0], coords2[1],
                    accessPoint1[0], accessPoint1[1],
                    -1, -1
            );

            // Tìm đường đi ngắn nhất giữa hai điểm tiếp cận
            return warehouseMap.findShortestPath(
                    accessPoint1[0], accessPoint1[1],
                    accessPoint2[0], accessPoint2[1]
            );
        }

        return new ArrayList<>();
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

        // Kiểm tra cache
        String cacheKey = getCacheKey(pos1, pos2);
        if (pathCache.containsKey(cacheKey)) {
            // Khi lấy từ cache, vẫn cập nhật vị trí hiện tại
            currentRobotPosition = pos2.copy();
            return new ArrayList<>(pathCache.get(cacheKey)); // Trả về bản sao để tránh thay đổi cache
        }

        // Quan trọng: Sử dụng vị trí hiện tại của robot (nếu có) làm điểm xuất phát thực tế
        Position actualStartPosition = (currentRobotPosition != null) ? currentRobotPosition : pos1;

        // Lấy tọa độ
        int[] coords1 = warehouseMap.positionToCoordinates(actualStartPosition);
        int[] coords2 = warehouseMap.positionToCoordinates(pos2);

        // Tìm điểm tiếp cận cho vị trí đích
        int[] accessPoint2 = warehouseMap.findNearestAccessPoint(coords2[0], coords2[1]);

        // Tìm đường đi từ vị trí thực tế hiện tại đến điểm tiếp cận của đích
        ArrayList<int[]> path = warehouseMap.findShortestPath(coords1[0], coords1[1], accessPoint2[0], accessPoint2[1]);

        // Lưu vào cache nếu có kết quả
        if (path != null && !path.isEmpty()) {
            pathCache.put(cacheKey, new ArrayList<>(path)); // Lưu bản sao để tránh thay đổi cache
        }

        // Cập nhật vị trí hiện tại của robot - điểm cuối của đường đi thực tế
        // Quan trọng: Nếu pos2 là vị trí trên kệ, thì currentRobotPosition sẽ là điểm tiếp cận
        if (path != null && !path.isEmpty()) {
            int[] lastPoint = path.get(path.size() - 1);
            currentRobotPosition = warehouseMap.coordinatesToPosition(lastPoint[0], lastPoint[1]);
        } else {
            // Nếu không tìm được đường đi, giữ nguyên vị trí
            currentRobotPosition = actualStartPosition;
        }

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