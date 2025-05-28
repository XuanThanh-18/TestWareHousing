import java.util.ArrayList;

/**
 * Test đơn giản cho cấu trúc kho mới
 * Mỗi ô hàng chỉ có 1 điểm tiếp cận duy nhất
 */
public class Test {
    public static void main(String[] args) {
        System.out.println("=== TEST CẤU TRÚC KHO MỚI ===");

        // Đọc tham số từ file
        Params.ReadParams();

        // Tạo bản đồ kho hàng
        WarehouseMap warehouseMap;
        if (Params.WAREHOUSE_MAP != null) {
            warehouseMap = new WarehouseMap(Params.WAREHOUSE_MAP);
        } else {
            warehouseMap = WarehouseMap.createMapFromWarehouse(Params.SHELVES, Params.SLOTS);
        }

        // In bản đồ kho hàng
        System.out.println("\n=== BẢN ĐỒ KHO HÀNG ===");
        warehouseMap.printMap();

        // Khởi tạo DistanceCalculator
        DistanceCalculator.initialize(warehouseMap);

        // Test các vị trí mặt hàng và điểm tiếp cận
        System.out.println("\n=== TEST ĐIỂM TIẾP CẬN ===");
        testAccessPoints(warehouseMap);

        // Test đường đi
        System.out.println("\n=== TEST ĐƯỜNG ĐI ===");
        testPaths(warehouseMap);

        // Test với mặt hàng thực tế
        System.out.println("\n=== TEST VỚI MẶT HÀNG THỰC TẾ ===");
        testWithRealMerchandise(warehouseMap);
    }

    private static void testAccessPoints(WarehouseMap map) {
        // Test các vị trí kệ hàng
        Position[] testPositions = {
                new Position(1, 1, 1), // Kệ 1, tầng 1, ô 1
                new Position(1, 1, 2), // Kệ 1, tầng 1, ô 2
                new Position(1, 2, 1), // Kệ 1, tầng 2, ô 1
                new Position(2, 1, 3), // Kệ 2, tầng 1, ô 3
                new Position(2, 2, 4)  // Kệ 2, tầng 2, ô 4
        };

        for (Position pos : testPositions) {
            System.out.println("\nVị trí mặt hàng: " + pos);

            int[] coords = map.positionToCoordinates(pos);
            System.out.println("Tọa độ trên bản đồ: [" + coords[0] + ", " + coords[1] + "]");
            System.out.println("Có thể đi được: " + map.isWalkable(coords[0], coords[1]));

            int[] accessPoint = map.findUniqueAccessPoint(coords[0], coords[1]);
            Position accessPos = map.coordinatesToPosition(accessPoint[0], accessPoint[1]);
            System.out.println("Điểm tiếp cận: " + accessPos + " tại tọa độ [" + accessPoint[0] + ", " + accessPoint[1] + "]");
            System.out.println("Điểm tiếp cận có thể đi được: " + map.isWalkable(accessPoint[0], accessPoint[1]));
        }
    }

    private static void testPaths(WarehouseMap map) {
        Position counter = new Position(0, 0, 0);
        Position item1 = new Position(1, 1, 2); // Kệ 1, tầng 1, ô 2
        Position item2 = new Position(2, 2, 3); // Kệ 2, tầng 2, ô 3

        System.out.println("Counter: " + counter);
        System.out.println("Mặt hàng 1: " + item1);
        System.out.println("Mặt hàng 2: " + item2);

        // Test đường đi từ counter đến mặt hàng 1
        testSinglePath(map, counter, item1, "Counter đến Mặt hàng 1");

        // Test đường đi từ mặt hàng 1 đến mặt hàng 2
        testSinglePath(map, item1, item2, "Mặt hàng 1 đến Mặt hàng 2");

        // Test đường đi từ mặt hàng 2 về counter
        testSinglePath(map, item2, counter, "Mặt hàng 2 về Counter");
    }

    private static void testSinglePath(WarehouseMap map, Position start, Position end, String description) {
        System.out.println("\n--- " + description + " ---");

        int[] startCoords = map.positionToCoordinates(start);
        int[] endCoords = map.positionToCoordinates(end);

        int[] startAccess = map.findUniqueAccessPoint(startCoords[0], startCoords[1]);
        int[] endAccess = map.findUniqueAccessPoint(endCoords[0], endCoords[1]);

        System.out.println("Từ: " + start + " (điểm tiếp cận: [" + startAccess[0] + ", " + startAccess[1] + "])");
        System.out.println("Đến: " + end + " (điểm tiếp cận: [" + endAccess[0] + ", " + endAccess[1] + "])");

        ArrayList<int[]> path = map.findShortestPath(startAccess[0], startAccess[1], endAccess[0], endAccess[1]);

        if (path != null && !path.isEmpty()) {
            System.out.println("Số bước đi: " + (path.size() - 1));
            float actualDistance = map.calculateActualDistance(start, end);
            System.out.println("Khoảng cách thực tế: " + actualDistance);

            // Hiển thị đường đi ngắn
            if (path.size() <= 10) {
                System.out.println("Đường đi chi tiết:");
                for (int i = 0; i < path.size(); i++) {
                    int[] coord = path.get(i);
                    Position pathPos = map.coordinatesToPosition(coord[0], coord[1]);
                    System.out.println("  Bước " + (i+1) + ": [" + coord[0] + ", " + coord[1] + "] -> " + pathPos);
                }
            } else {
                System.out.println("Đường đi quá dài (" + path.size() + " bước), chỉ hiển thị đầu và cuối:");
                for (int i = 0; i < 3; i++) {
                    int[] coord = path.get(i);
                    Position pathPos = map.coordinatesToPosition(coord[0], coord[1]);
                    System.out.println("  Bước " + (i+1) + ": [" + coord[0] + ", " + coord[1] + "] -> " + pathPos);
                }
                System.out.println("  ...");
                for (int i = path.size() - 3; i < path.size(); i++) {
                    int[] coord = path.get(i);
                    Position pathPos = map.coordinatesToPosition(coord[0], coord[1]);
                    System.out.println("  Bước " + (i+1) + ": [" + coord[0] + ", " + coord[1] + "] -> " + pathPos);
                }
            }

            // Hiển thị đường đi trên bản đồ nếu ngắn
            if (path.size() <= 15) {
                System.out.println("Đường đi trên bản đồ:");
                map.printPathOnMap(path);
            }
        } else {
            System.out.println("KHÔNG TÌM THẤY ĐƯỜNG ĐI!");
        }
    }

    private static void testWithRealMerchandise(WarehouseMap map) {
        if (Params.WAREHOUSE == null || Params.WAREHOUSE.isEmpty()) {
            System.out.println("Không có mặt hàng nào để test");
            return;
        }

        // Khởi tạo DistanceCalculator với precompute
        Position counter = new Position(0, 0, 0);
        DistanceCalculator.precomputeAllDistances(Params.WAREHOUSE, counter);

        System.out.println("Có " + Params.WAREHOUSE.size() + " mặt hàng trong kho:");

        // Hiển thị thông tin chi tiết của một vài mặt hàng
        for (int i = 0; i < Math.min(5, Params.WAREHOUSE.size()); i++) {
            Merchandise item = Params.WAREHOUSE.get(i);
            System.out.println("\n--- Mặt hàng " + (i+1) + " ---");
            item.printDetailedInfo(map);

            // Test khoảng cách từ counter
            DistanceCalculator.setCurrentRobotPosition(counter);
            float distance = DistanceCalculator.calculateDistance(counter, item.getPosition());
            System.out.println("Khoảng cách từ Counter: " + distance);
        }

        // Test khoảng cách giữa các mặt hàng
        if (Params.WAREHOUSE.size() >= 2) {
            System.out.println("\n--- TEST KHOẢNG CÁCH GIỮA CÁC MẶT HÀNG ---");
            Merchandise item1 = Params.WAREHOUSE.get(0);
            Merchandise item2 = Params.WAREHOUSE.get(1);

            DistanceCalculator.setCurrentRobotPosition(item1.getPosition());
            float distance = DistanceCalculator.calculateDistance(item1.getPosition(), item2.getPosition());

            System.out.println("Từ: " + item1.getName() + " " + item1.getPosition());
            System.out.println("Đến: " + item2.getName() + " " + item2.getPosition());
            System.out.println("Khoảng cách: " + distance);
        }

        // Hiển thị thông tin cache
        DistanceCalculator.printCacheInfo();

        // Test một tuyến đường đơn giản
        if (Params.REQUIRE != null && !Params.REQUIRE.isEmpty()) {
            System.out.println("\n--- TEST TUYẾN ĐƯỜNG ĐƠN GIẢN ---");
            testSimpleRoute(map, counter);
        }
    }

    private static void testSimpleRoute(WarehouseMap map, Position counter) {
        System.out.println("Test tuyến đường cho " + Math.min(3, Params.REQUIRE.size()) + " mặt hàng đầu tiên:");

        DistanceCalculator.setCurrentRobotPosition(counter);
        Position currentPos = counter;
        float totalDistance = 0;

        System.out.println("1. Bắt đầu từ Counter: " + counter);

        for (int i = 0; i < Math.min(3, Params.REQUIRE.size()); i++) {
            Merchandise reqItem = Params.REQUIRE.get(i);

            // Tìm mặt hàng trong kho
            Merchandise warehouseItem = null;
            for (Merchandise item : Params.WAREHOUSE) {
                if (item.getName().equals(reqItem.getName())) {
                    warehouseItem = item;
                    break;
                }
            }

            if (warehouseItem != null) {
                float distance = DistanceCalculator.calculateDistance(currentPos, warehouseItem.getPosition());
                totalDistance += distance;
                currentPos = DistanceCalculator.getCurrentRobotPosition();

                System.out.println((i+2) + ". Đến " + warehouseItem.getName() + " tại " +
                        warehouseItem.getPosition() + " (+" + distance + " đơn vị)");
            }
        }

        // Quay về counter
        float returnDistance = DistanceCalculator.calculateDistance(currentPos, counter);
        totalDistance += returnDistance;

        System.out.println((Math.min(3, Params.REQUIRE.size()) + 2) + ". Quay về Counter " +
                counter + " (+" + returnDistance + " đơn vị)");
        System.out.println("TỔNG QUÃNG ĐƯỜNG: " + totalDistance);
    }
}