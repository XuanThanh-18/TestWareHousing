import java.util.ArrayList;
import java.util.List;

public class Nhap {
    public static void main(String[] args) {
        Params.ReadParams();
        WarehouseMap warehouseMap;
        if (Params.WAREHOUSE_MAP != null) {
            warehouseMap = new WarehouseMap(Params.WAREHOUSE_MAP);
        } else {
            warehouseMap = WarehouseMap.createMapFromWarehouse(Params.SHELVES, Params.SLOTS);
        }
        if (Params.REQUIRE == null || Params.REQUIRE.isEmpty()) {
            System.out.println("CẢNH BÁO: Không có mặt hàng nào cần lấy. Hãy kiểm tra file input.");
            return;
        }

        // In bản đồ kho hàng
        System.out.println("\n=== BẢN ĐỒ KHO HÀNG ===");
        warehouseMap.printMap();

        // Khởi tạo DistanceCalculator
        DistanceCalculator.initialize(warehouseMap);

        // Định nghĩa vị trí bắt đầu và kết thúc
        Position start = new Position(1, 1, 1); // Kệ 1, tầng 1, ô 1
        Position end = new Position(2, 1, 3);   // Kệ 2, tầng 1, ô 3

        System.out.println("\n=== THÔNG TIN VỊ TRÍ ===");
        System.out.println("Vị trí bắt đầu: " + start);
        System.out.println("Vị trí kết thúc: " + end);

        // Chuyển đổi vị trí thành tọa độ trên bản đồ
        int[] startCoords = warehouseMap.positionToCoordinates(start);
        int[] endCoords = warehouseMap.positionToCoordinates(end);

        System.out.println("\nTọa độ trên bản đồ:");
        System.out.println("Bắt đầu: [" + startCoords[0] + ", " + startCoords[1] + "]");
        System.out.println("Kết thúc: [" + endCoords[0] + ", " + endCoords[1] + "]");

        // Vẽ đường đi
        ArrayList<int[]> path = warehouseMap.findShortestPath(startCoords[0], startCoords[1],
                endCoords[0], endCoords[1]);
        if (path == null || path.isEmpty()) {
            System.out.println("Không có đường đi nào để hiển thị.");
            return;
        }

        System.out.println("\n=== ĐƯỜNG ĐI ===");
        System.out.println("Số bước đi: " + (path.size() - 1));
        warehouseMap.printPathOnMap(path);

        // Tính và in khoảng cách thực tế
        float actualDistance = warehouseMap.calculateActualDistance(start, end);
        System.out.println("\nKhoảng cách thực tế: " + actualDistance);

        // In khoảng cách Manhattan để so sánh
        float manhattanDistance = DistanceCalculator.calculateManhattanDistance(start, end);
        System.out.println("Khoảng cách Manhattan: " + manhattanDistance);

        // Test thêm một số cặp vị trí khác nhau
        System.out.println("\n=== THÊM VÍ DỤ VỀ KHOẢNG CÁCH ===");
        testDistance(warehouseMap, new Position(1, 1, 1), new Position(1, 2, 1)); // Cùng kệ, khác tầng
        testDistance(warehouseMap, new Position(1, 1, 1), new Position(3, 1, 1)); // Khác kệ, cùng ô và tầng
        testDistance(warehouseMap, new Position(1, 1, 1), new Position(3, 2, 4)); // Khác kệ, tầng và ô
    }

    // Phương thức kiểm tra khoảng cách giữa hai vị trí
    private static void testDistance(WarehouseMap warehouseMap, Position pos1, Position pos2) {
        System.out.println("\nTính khoảng cách từ " + pos1 + " đến " + pos2 + ":");

        // Tính khoảng cách thực tế
        float actualDistance = warehouseMap.calculateActualDistance(pos1, pos2);

        // Tính khoảng cách Manhattan
        float manhattanDistance = DistanceCalculator.calculateManhattanDistance(pos1, pos2);

        System.out.println("- Khoảng cách thực tế: " + actualDistance);
        System.out.println("- Khoảng cách Manhattan: " + manhattanDistance);

        // Tìm đường đi
        int[] coords1 = warehouseMap.positionToCoordinates(pos1);
        int[] coords2 = warehouseMap.positionToCoordinates(pos2);
        ArrayList<int[]> path = warehouseMap.findShortestPath(coords1[0], coords1[1], coords2[0], coords2[1]);

        System.out.println("- Số bước đi: " + (path.size() > 0 ? path.size() - 1 : "Không tìm thấy đường đi"));

        // In đường đi
        if (path != null && !path.isEmpty()) {
            System.out.println("\nĐường đi từ " + pos1 + " đến " + pos2 + ":");
            warehouseMap.printPathOnMap(path);
        }
    }
}