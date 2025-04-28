import java.util.ArrayList;

/**
 * Lớp Test thực hiện thuật toán tìm đường đi ngắn nhất cho robot
 * sử dụng PSO kết hợp VNS trên bản đồ kho hàng
 */
public class Test {
    public static void main(String[] args) {
        // Đọc tham số từ file
        Params.ReadParams();

        // Đảm bảo tham số ROBOTS luôn lớn hơn 0
        if (Params.ROBOTS <= 0) {
            System.out.println("Số robot <= 0, đặt lại thành 2");
            Params.ROBOTS = 2;
        }

        // In thông tin cấu hình
        System.out.println("=== THÔNG TIN CẤU HÌNH ===");
        Params.printWarehouseMap();
        Params.printWarehouse();
        Params.printRequiredItems();

        // Khởi tạo kho hàng
        ArrayList<Merchandise> warehousing = WareHousing.setWareHousing();

        // Định nghĩa vị trí counter [0,0]
        Position counterPosition = new Position(0, 0, 0);
        System.out.println("\nVị trí Counter (xuất phát): " + counterPosition);

        // Kiểm tra nếu không có mặt hàng nào cần lấy
        if (Params.REQUIRE == null || Params.REQUIRE.isEmpty()) {
            System.out.println("CẢNH BÁO: Không có mặt hàng nào cần lấy. Hãy kiểm tra file input.");
            return;
        }

        // Tạo bản đồ kho hàng từ dữ liệu đọc được
        WarehouseMap warehouseMap;
        if (Params.WAREHOUSE_MAP != null) {
            warehouseMap = new WarehouseMap(Params.WAREHOUSE_MAP);
        } else {
            warehouseMap = WarehouseMap.createMapFromWarehouse(Params.SHELVES, Params.SLOTS);
        }

        // In bản đồ kho hàng
        System.out.println("\n=== BẢN ĐỒ KHO HÀNG ===");
        warehouseMap.printMap();

        // Khởi tạo trình tính khoảng cách với bản đồ
        System.out.println("\nĐang khởi tạo bộ tính khoảng cách...");
        DistanceCalculator.initialize(warehouseMap);

        // Tính trước tất cả khoảng cách (một lần ở đầu chương trình)
        System.out.println("Đang tính trước các khoảng cách để tối ưu hiệu suất...");
        long precomputeStartTime = System.currentTimeMillis();
        DistanceCalculator.precomputeAllDistances(warehousing, counterPosition);
        long precomputeEndTime = System.currentTimeMillis();
        System.out.println("Đã hoàn thành tính trước khoảng cách trong " +
                (precomputeEndTime - precomputeStartTime) + " ms");

        // Tạo một cá thể
        Individual individual = new Individual();

        // Thực hiện thuật toán PSO-VNS
        System.out.println("\n==================================================");
        System.out.println("THUẬT TOÁN PSO-VNS TÌM ĐƯỜNG ĐI NGẮN NHẤT CHO ROBOT");
        System.out.println("==================================================");

        // Thực thi thuật toán PSO-VNS với vị trí counter
        float psoVnsDistance = 0;
        long startTime = System.currentTimeMillis();
        try {
            psoVnsDistance = individual.solvePsoVns(counterPosition, warehousing);
        } catch (Exception e) {
            System.out.println("Lỗi khi thực hiện PSO-VNS: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        System.out.println("\n==================================================");
        System.out.println("TỔNG CHI PHÍ QUÃNG ĐƯỜNG PSO-VNS: " + psoVnsDistance);
        System.out.println("THỜI GIAN THỰC THI: " + executionTime + " ms");
        System.out.println("SỐ LƯỢNG KHOẢNG CÁCH ĐÃ TÍNH: " + DistanceCalculator.getCacheSize());
        System.out.println("==================================================");

        // In thông tin chi tiết đường đi cho mỗi robot
        System.out.println("\nCHI TIẾT ĐƯỜNG ĐI CỦA CÁC ROBOT:");
        for (Robot robot : individual.robots) {
            System.out.println("\n--- ROBOT " + robot.nameRobot + " ---");
            System.out.println("Vị trí xuất phát: " + robot.getStartPosition());

            if (robot.shoppingCart.isEmpty()) {
                System.out.println("Không có mặt hàng nào được phân công");
                continue;
            }

            // Hiển thị đường đi chi tiết
            System.out.println("1. Bắt đầu từ COUNTER tại " + robot.getStartPosition());
            Position currentPos = robot.getStartPosition();
            float totalDistance = 0;

            // Lưu trữ đường đi chi tiết để hiển thị trên bản đồ
            ArrayList<Position> robotPath = new ArrayList<>();
            robotPath.add(robot.getStartPosition());

            for (int i = 0; i < robot.shoppingCart.size(); i++) {
                Merchandise item = robot.shoppingCart.get(i);

                // Tìm vị trí trong kho
                Merchandise warehouseItem = null;
                for (Merchandise w : warehousing) {
                    if (w.getName().equals(item.getName())) {
                        warehouseItem = w;
                        break;
                    }
                }

                if (warehouseItem != null) {
                    try {
                        // Tính đường đi thực tế
                        ArrayList<int[]> pathCoords = DistanceCalculator.findPath(currentPos, warehouseItem.getPosition());

                        // Tính khoảng cách
                        float distance = DistanceCalculator.calculateDistance(currentPos, warehouseItem.getPosition());
                        totalDistance += distance;

                        // Hiển thị thông tin mặt hàng
                        System.out.println((i + 2) + ". Đi đến " + warehouseItem.getName() +
                                " (số lượng: " + warehouseItem.getQuantity() + ") tại vị trí " +
                                warehouseItem.getPosition() + " (+" + distance + " đơn vị)");

                        // In ra đường đi chi tiết (nếu có)
                        if (pathCoords != null && !pathCoords.isEmpty() && pathCoords.size() < 20) { // Giới hạn hiển thị nếu đường đi quá dài
                            System.out.println("   Đường đi chi tiết:");
                            for (int j = 0; j < pathCoords.size(); j++) {
                                int[] coord = pathCoords.get(j);
                                Position pathPos = warehouseMap.coordinatesToPosition(coord[0], coord[1]);
                                System.out.println("   - Bước " + (j+1) + ": " + pathPos);
                            }
                        } else if (pathCoords != null && pathCoords.size() >= 20) {
                            System.out.println("   Đường đi chi tiết (hiển thị một phần):");
                            for (int j = 0; j < 5; j++) {
                                int[] coord = pathCoords.get(j);
                                Position pathPos = warehouseMap.coordinatesToPosition(coord[0], coord[1]);
                                System.out.println("   - Bước " + (j+1) + ": " + pathPos);
                            }
                            System.out.println("   - ...");
                            for (int j = pathCoords.size() - 5; j < pathCoords.size(); j++) {
                                int[] coord = pathCoords.get(j);
                                Position pathPos = warehouseMap.coordinatesToPosition(coord[0], coord[1]);
                                System.out.println("   - Bước " + (j+1) + ": " + pathPos);
                            }
                        }

                        // Cập nhật vị trí hiện tại
                        currentPos = warehouseItem.getPosition();
                        robotPath.add(currentPos);
                    } catch (Exception e) {
                        System.out.println("Lỗi khi tính toán đường đi: " + e.getMessage());
                    }
                }
            }

            try {
                // Đường đi về counter
                ArrayList<int[]> returnPathCoords = DistanceCalculator.findPath(currentPos, robot.getStartPosition());
                float returnDistance = DistanceCalculator.calculateDistance(currentPos, robot.getStartPosition());
                totalDistance += returnDistance;

                System.out.println((robot.shoppingCart.size() + 2) + ". Quay về COUNTER tại " +
                        robot.getStartPosition() + " (+" + returnDistance + " đơn vị)");

                // In ra đường đi chi tiết khi quay về
                if (returnPathCoords != null && !returnPathCoords.isEmpty() && returnPathCoords.size() < 20) {
                    System.out.println("   Đường đi chi tiết khi quay về:");
                    for (int j = 0; j < returnPathCoords.size(); j++) {
                        int[] coord = returnPathCoords.get(j);
                        Position pathPos = warehouseMap.coordinatesToPosition(coord[0], coord[1]);
                        System.out.println("   - Bước " + (j+1) + ": " + pathPos);
                    }
                } else if (returnPathCoords != null && returnPathCoords.size() >= 20) {
                    System.out.println("   Đường đi chi tiết khi quay về (hiển thị một phần):");
                    for (int j = 0; j < 5; j++) {
                        int[] coord = returnPathCoords.get(j);
                        Position pathPos = warehouseMap.coordinatesToPosition(coord[0], coord[1]);
                        System.out.println("   - Bước " + (j+1) + ": " + pathPos);
                    }
                    System.out.println("   - ...");
                    for (int j = returnPathCoords.size() - 5; j < returnPathCoords.size(); j++) {
                        int[] coord = returnPathCoords.get(j);
                        Position pathPos = warehouseMap.coordinatesToPosition(coord[0], coord[1]);
                        System.out.println("   - Bước " + (j+1) + ": " + pathPos);
                    }
                }

                robotPath.add(robot.getStartPosition()); // Thêm điểm cuối là vị trí xuất phát
                System.out.println("\nTổng quãng đường của ROBOT " + robot.nameRobot + ": " + totalDistance);

                // Hiển thị đường đi trên bản đồ
                System.out.println("\nĐường đi của ROBOT " + robot.nameRobot + " trên bản đồ:");

                // Chuyển đổi danh sách Position thành tọa độ trên bản đồ
                ArrayList<int[]> fullPath = new ArrayList<>();
                for (Position pos : robotPath) {
                    int[] coords = warehouseMap.positionToCoordinates(pos);
                    fullPath.add(coords);
                }

                // In đường đi trên bản đồ
                warehouseMap.printPathOnMap(fullPath);
            } catch (Exception e) {
                System.out.println("Lỗi khi tính đường đi về: " + e.getMessage());
            }
        }

        // Thực hiện thuật toán Greedy để so sánh
        System.out.println("\n==================================================");
        System.out.println("SO SÁNH VỚI THUẬT TOÁN GREEDY");
        System.out.println("==================================================");

        float greedyDistance = 0;
        long greedyStartTime = System.currentTimeMillis();
        try {
            greedyDistance = individual.greedy(counterPosition, warehousing);
        } catch (Exception e) {
            System.out.println("Lỗi khi thực hiện Greedy: " + e.getMessage());
            e.printStackTrace();
        }
        long greedyEndTime = System.currentTimeMillis();
        long greedyExecutionTime = greedyEndTime - greedyStartTime;

        System.out.println("\n==================================================");
        System.out.println("KẾT QUẢ SO SÁNH:");
        System.out.println("- Quãng đường PSO-VNS: " + psoVnsDistance + " (thực thi trong " + executionTime + " ms)");
        System.out.println("- Quãng đường Greedy: " + greedyDistance + " (thực thi trong " + greedyExecutionTime + " ms)");

        // Tính phần trăm cải thiện, tránh chia cho 0
        float improvement = 0;
        try {
            if (greedyDistance > 0) {
                improvement = ((greedyDistance - psoVnsDistance) / greedyDistance) * 100;
            } else if (psoVnsDistance < greedyDistance) {
                improvement = 100; // Nếu greedyDistance là 0 và psoVnsDistance < 0, giả sử cải thiện 100%
            } else if (psoVnsDistance > greedyDistance) {
                improvement = -100; // Nếu greedyDistance là 0 và psoVnsDistance > 0, giả sử kém hơn 100%
            }
        } catch (Exception e) {
            System.out.println("Lỗi khi tính phần trăm cải thiện: " + e.getMessage());
        }

        System.out.println("- Cải thiện: " + String.format("%.2f", improvement) + "%");
        System.out.println("==================================================");

        // In thông tin về cache khoảng cách
        System.out.println("\nSố khoảng cách đã tính và lưu trong cache: " + DistanceCalculator.getCacheSize());
    }
}