/**
 * Lớp DistanceCalculator tính toán khoảng cách giữa các vị trí trong kho hàng
 * Hỗ trợ tính toán theo đường đi thực tế trên bản đồ
 */
public class DistanceCalculator {
    private static WarehouseMap warehouseMap;

    /**
     * Khởi tạo với bản đồ kho hàng
     * @param map Bản đồ kho hàng
     */
    public static void initialize(WarehouseMap map) {
        warehouseMap = map;
    }

    /**
     * Tính khoảng cách từ vị trí 1 đến vị trí 2
     * @param pos1 Vị trí bắt đầu
     * @param pos2 Vị trí kết thúc
     * @return Khoảng cách
     */
    public static float calculateDistance(Position pos1, Position pos2) {
        // Nếu có bản đồ, sử dụng đường đi thực tế
        if (warehouseMap != null) {
            return warehouseMap.calculateActualDistance(pos1, pos2);
        }

        // Nếu không có bản đồ, sử dụng phương thức tính khoảng cách cũ
        return calculateManhattanDistance(pos1, pos2);
    }

    /**
     * Tính khoảng cách Manhattan giữa hai vị trí
     * @param pos1 Vị trí bắt đầu
     * @param pos2 Vị trí kết thúc
     * @return Khoảng cách Manhattan
     */
    public static float calculateManhattanDistance(Position pos1, Position pos2) {
        // Khoảng cách Manhattan với khác biệt về tầng
        int xDiff = Math.abs(pos1.x - pos2.x);
        int yDiff = Math.abs(pos1.y - pos2.y);

        float tierDistance = 0;
        if (pos1.getShelf() == pos2.getShelf() && pos1.getSlot() == pos2.getSlot()) {
            tierDistance = 0.5f * Math.abs(pos1.getTier() - pos2.getTier());
        } else {
            tierDistance = 0.5f * (pos1.getTier() + pos2.getTier() - 2);
        }

        return xDiff + yDiff + tierDistance;
    }

    /**
     * Tìm đường đi ngắn nhất từ vị trí 1 đến vị trí 2
     * @param pos1 Vị trí bắt đầu
     * @param pos2 Vị trí kết thúc
     * @return Đường đi (mảng các tọa độ)
     */
    public static java.util.ArrayList<int[]> findPath(Position pos1, Position pos2) {
        if (warehouseMap == null) {
            return new java.util.ArrayList<>();
        }

        int[] coords1 = warehouseMap.positionToCoordinates(pos1);
        int[] coords2 = warehouseMap.positionToCoordinates(pos2);

        return warehouseMap.findShortestPath(coords1[0], coords1[1], coords2[0], coords2[1]);
    }
}