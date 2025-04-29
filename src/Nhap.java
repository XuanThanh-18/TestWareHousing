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

        // Test 1: Đường đi từ lối đi đến lối đi
        Position walkableStart = new Position(1, 1, 0); // Kệ 1, tầng 1, lối đi
        Position walkableEnd = new Position(3, 1, 0);   // Kệ 3, tầng 1, lối đi
        System.out.println("\n=== TEST 1: ĐƯỜNG ĐI TỪ LỐI ĐI ĐẾN LỐI ĐI ===");
        testPathAndDistance(warehouseMap, walkableStart, walkableEnd);

        // Test 2: Đường đi từ lối đi đến kệ hàng
        Position walkableStart2 = new Position(1, 1, 0); // Kệ 1, tầng 1, lối đi
        Position shelfEnd = new Position(2, 1, 2);      // Kệ 2, tầng 1, ô 2 (trên kệ)
        System.out.println("\n=== TEST 2: ĐƯỜNG ĐI TỪ LỐI ĐI ĐẾN KỆ HÀNG ===");
        testPathAndDistance(warehouseMap, walkableStart2, shelfEnd);

        // Test 3: Đường đi từ kệ hàng đến kệ hàng
        Position shelfStart = new Position(1, 2, 2); // Kệ 1, tầng 2, ô 2 (trên kệ)
        Position shelfEnd2 = new Position(3, 2, 3);  // Kệ 3, tầng 2, ô 3 (trên kệ)
        System.out.println("\n=== TEST 3: ĐƯỜNG ĐI TỪ KỆ HÀNG ĐẾN KỆ HÀNG ===");
        testPathAndDistance(warehouseMap, shelfStart, shelfEnd2);

        // Test 4: Đường đi cùng kệ, khác tầng
        Position sameShelfDiffTier1 = new Position(2, 1, 3); // Kệ 2, tầng 1, ô 3
        Position sameShelfDiffTier2 = new Position(2, 2, 3); // Kệ 2, tầng 2, ô 3
        System.out.println("\n=== TEST 4: ĐƯỜNG ĐI CÙNG KỆ, KHÁC TẦNG ===");
        testPathAndDistance(warehouseMap, sameShelfDiffTier1, sameShelfDiffTier2);

        // Test với các mặt hàng thực tế từ kho hàng
        if (!Params.WAREHOUSE.isEmpty()) {
            System.out.println("\n=== TEST VỚI MẶT HÀNG THỰC TẾ ===");
            // Lấy hai mặt hàng từ kho để test
            Merchandise item1 = Params.WAREHOUSE.get(0);
            Merchandise item2 = Params.WAREHOUSE.size() > 1 ? Params.WAREHOUSE.get(1) : Params.WAREHOUSE.get(0);

            System.out.println("Mặt hàng 1: " + item1.getName() + " tại " + item1.getPosition());
            System.out.println("Mặt hàng 2: " + item2.getName() + " tại " + item2.getPosition());

            // Test đường đi và khoảng cách giữa hai mặt hàng
            testPathAndDistance(warehouseMap, item1.getPosition(), item2.getPosition());

            // Test khoảng cách từ counter đến mặt hàng
            Position counter = new Position(0, 0, 0);
            System.out.println("\n=== TEST ĐƯỜNG ĐI TỪ COUNTER ĐẾN MẶT HÀNG ===");
            testPathAndDistance(warehouseMap, counter, item1.getPosition());
        }
    }

    // Phương thức test đường đi và khoảng cách
    private static void testPathAndDistance(WarehouseMap warehouseMap, Position pos1, Position pos2) {
        System.out.println("Vị trí bắt đầu: " + pos1);
        System.out.println("Vị trí kết thúc: " + pos2);

        int[] coords1 = warehouseMap.positionToCoordinates(pos1);
        int[] coords2 = warehouseMap.positionToCoordinates(pos2);

        System.out.println("Tọa độ bắt đầu trên bản đồ: [" + coords1[0] + ", " + coords1[1] + "]");
        System.out.println("Tọa độ kết thúc trên bản đồ: [" + coords2[0] + ", " + coords2[1] + "]");

        // Kiểm tra xem các vị trí có đi được không
        System.out.println("Vị trí bắt đầu đi được: " + warehouseMap.isWalkable(coords1[0], coords1[1]));
        System.out.println("Vị trí kết thúc đi được: " + warehouseMap.isWalkable(coords2[0], coords2[1]));

        // Tìm điểm tiếp cận - truyền vị trí đích để tối ưu
        int[] access1 = warehouseMap.findNearestAccessPoint(coords1[0], coords1[1], coords2[0], coords2[1]);
        int[] access2 = warehouseMap.findNearestAccessPoint(coords2[0], coords2[1], coords1[0], coords1[1]);

        System.out.println("Điểm tiếp cận bắt đầu: [" + access1[0] + ", " + access1[1] + "]");
        System.out.println("Điểm tiếp cận kết thúc: [" + access2[0] + ", " + access2[1] + "]");

        // Tìm đường đi
        ArrayList<int[]> path = warehouseMap.findShortestPath(access1[0], access1[1], access2[0], access2[1]);

        if (path == null || path.isEmpty()) {
            System.out.println("Không tìm thấy đường đi!");
            return;
        }

        System.out.println("Số bước đi: " + (path.size() - 1));

        // In đường đi
        System.out.println("Đường đi:");
        warehouseMap.printPathOnMap(path);

        // Tính và in khoảng cách
        float actualDistance = warehouseMap.calculateActualDistance(pos1, pos2);
        float manhattanDistance = DistanceCalculator.calculateManhattanDistance(pos1, pos2);

        System.out.println("Khoảng cách thực tế: " + actualDistance);
        System.out.println("Khoảng cách Manhattan: " + manhattanDistance);
    }
}