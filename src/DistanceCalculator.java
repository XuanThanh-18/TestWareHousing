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
    // Biến theo dõi việc đã tính toàn bộ khoảng cách hay chưa
    private static boolean hasPrecomputedAllDistances = false;

    /**
     * Khởi tạo với bản đồ kho hàng
     * @param map Bản đồ kho hàng
     */
    public static void initialize(WarehouseMap map) {
        warehouseMap = map;
        // Xóa cache cũ khi khởi tạo với bản đồ mới
        distanceCache.clear();
        hasPrecomputedAllDistances = false;
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

        // Tính khoảng cách từ counter đến mỗi mặt hàng
        for (Merchandise item : warehousing) {
            String key = getCacheKey(counterPosition, item.getPosition());
            float distance = computeActualDistance(counterPosition, item.getPosition());
            distanceCache.put(key, distance);
        }

        // Tính khoảng cách giữa các cặp mặt hàng
        for (int i = 0; i < warehousing.size(); i++) {
            for (int j = i + 1; j < warehousing.size(); j++) {
                Position pos1 = warehousing.get(i).getPosition();
                Position pos2 = warehousing.get(j).getPosition();

                String key = getCacheKey(pos1, pos2);
                float distance = computeActualDistance(pos1, pos2);
                distanceCache.put(key, distance);
            }
        }

        hasPrecomputedAllDistances = true;
        System.out.println("Đã tính trước " + distanceCache.size() + " khoảng cách.");
    }

    /**
     * Tạo khóa duy nhất cho cache dựa trên hai vị trí
     * @param pos1 Vị trí thứ nhất
     * @param pos2 Vị trí thứ hai
     * @return Chuỗi khóa duy nhất
     */
    private static String getCacheKey(Position pos1, Position pos2) {
        // Đảm bảo khóa là duy nhất cho cặp vị trí, không phụ thuộc thứ tự
        int hash1 = pos1.hashCode();
        int hash2 = pos2.hashCode();

        // Sắp xếp để đảm bảo tính đối xứng (A-B và B-A có cùng khóa)
        if (hash1 <= hash2) {
            return hash1 + "-" + hash2;
        } else {
            return hash2 + "-" + hash1;
        }
    }

    /**
     * Tính khoảng cách từ vị trí 1 đến vị trí 2
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

        // Kiểm tra cache
        String cacheKey = getCacheKey(pos1, pos2);
        if (distanceCache.containsKey(cacheKey)) {
            return distanceCache.get(cacheKey);
        }

        // Tính khoảng cách và lưu vào cache
        float distance;
        if (warehouseMap != null) {
            distance = computeActualDistance(pos1, pos2);
        } else {
            distance = calculateManhattanDistance(pos1, pos2);
        }

        distanceCache.put(cacheKey, distance);
        return distance;
    }

    /**
     * Phương thức nội bộ để tính khoảng cách thực tế
     * Tách riêng để tránh gọi đệ quy vô hạn khi tính precompute
     */
    private static float computeActualDistance(Position pos1, Position pos2) {
        return warehouseMap.calculateActualDistance(pos1, pos2);
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

        int[] coords1 = warehouseMap.positionToCoordinates(pos1);
        int[] coords2 = warehouseMap.positionToCoordinates(pos2);

        return warehouseMap.findShortestPath(coords1[0], coords1[1], coords2[0], coords2[1]);
    }

    /**
     * Xóa cache
     */
    public static void clearCache() {
        distanceCache.clear();
        hasPrecomputedAllDistances = false;
    }

    /**
     * Lấy kích thước cache
     * @return Số lượng khoảng cách đã lưu trong cache
     */
    public static int getCacheSize() {
        return distanceCache.size();
    }
}