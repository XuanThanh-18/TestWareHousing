import java.util.ArrayList;

/**
 * File Demo đơn giản để chạy và hiển thị kết quả so sánh PSO-VNS vs Greedy
 * Không sử dụng SolutionVisualizer, hiển thị trực tiếp
 */
public class Demo {
    public static void main(String[] args) {
        System.out.println("=== DEMO WAREHOUSE ROBOT OPTIMIZATION ===");

        try {
            // 1. Đọc dữ liệu và cấu hình
            System.out.println("\n1. Đọc dữ liệu cấu hình...");
            Params.ReadParams();

            // Điều chỉnh tham số cho demo nhanh
            Params.PSO_SWARM_SIZE = 15;
            Params.PSO_MAX_ITERATIONS = 50;
            Params.VNS_MAX_ITERATIONS = 15;

            System.out.println("✓ Đã đọc cấu hình:");
            System.out.println("  - Kho: " + Params.SHELVES + " kệ x " + Params.TIERS + " tầng x " + Params.SLOTS + " ô");
            System.out.println("  - Robot: " + Params.ROBOTS + " (sức chứa: " + Params.CAPACITY + ")");
            System.out.println("  - Mặt hàng: " + Params.WAREHOUSE.size() + " trong kho, " + Params.REQUIRE.size() + " cần lấy");

            // 2. Thiết lập hệ thống
            System.out.println("\n2. Thiết lập hệ thống...");
            WarehouseMap warehouseMap;
            if (Params.WAREHOUSE_MAP != null) {
                warehouseMap = new WarehouseMap(Params.WAREHOUSE_MAP);
            } else {
                warehouseMap = WarehouseMap.createMapFromWarehouse(Params.SHELVES, Params.SLOTS);
            }

            DistanceCalculator.initialize(warehouseMap);
            ArrayList<Merchandise> warehousing = WareHousing.setWareHousing();
            Position counterPosition = new Position(0, 0, 0);
            DistanceCalculator.precomputeAllDistances(warehousing, counterPosition);

            System.out.println("✓ Hệ thống đã sẵn sàng");

            // Hiển thị bản đồ kho hàng
            System.out.println("\n=== BẢN ĐỒ KHO HÀNG ===");
            warehouseMap.printMap();

            // 3. Chạy Greedy
            System.out.println("\n" + "=".repeat(50));
            System.out.println("3. CHẠY THUẬT TOÁN GREEDY");
            System.out.println("=".repeat(50));

            Individual greedyIndividual = new Individual();
            long startGreedy = System.currentTimeMillis();
            float greedyResult = greedyIndividual.greedy(counterPosition, warehousing);
            long greedyTime = System.currentTimeMillis() - startGreedy;

            // Hiển thị solution Greedy
            displaySolution(greedyIndividual.robots, warehousing, counterPosition, warehouseMap, "GREEDY");

            // 4. Chạy PSO-VNS
            System.out.println("\n" + "=".repeat(50));
            System.out.println("4. CHẠY THUẬT TOÁN PSO-VNS");
            System.out.println("=".repeat(50));

            Individual psoIndividual = new Individual();
            long startPSO = System.currentTimeMillis();
            float psoResult = psoIndividual.solvePsoVns(counterPosition, warehousing);
            long psoTime = System.currentTimeMillis() - startPSO;

            // Hiển thị solution PSO-VNS
            displaySolution(psoIndividual.robots, warehousing, counterPosition, warehouseMap, "PSO-VNS");

            // 5. So sánh kết quả
            System.out.println("\n" + "=".repeat(50));
            System.out.println("5. SO SÁNH KẾT QUẢ");
            System.out.println("=".repeat(50));

            compareResults(greedyIndividual.robots, psoIndividual.robots, warehousing,
                    counterPosition, greedyResult, psoResult, greedyTime, psoTime);

            // 6. Tổng kết
            System.out.println("\n" + "=".repeat(50));
            System.out.println("6. TỔNG KẾT");
            System.out.println("=".repeat(50));

            float improvement = greedyResult - psoResult;
            float improvementPercent = (improvement / greedyResult) * 100;

            System.out.printf("Greedy:  %.2f units trong %d ms\n", greedyResult, greedyTime);
            System.out.printf("PSO-VNS: %.2f units trong %d ms\n", psoResult, psoTime);

            if (improvement > 0) {
                System.out.printf("✓ PSO-VNS tốt hơn %.2f units (%.1f%%)\n", improvement, improvementPercent);
            } else {
                System.out.printf("✗ Greedy tốt hơn %.2f units (%.1f%%)\n", Math.abs(improvement), Math.abs(improvementPercent));
            }

            System.out.printf("Tỷ lệ thời gian: %.1fx\n", (float)psoTime / greedyTime);
            System.out.println("\n✓ Demo hoàn thành!");

        } catch (Exception e) {
            System.err.println("Lỗi trong demo: " + e.getMessage());
            e.printStackTrace();

            // Chạy test đơn giản nếu có lỗi
            runBasicTest();
        }
    }

    /**
     * Hiển thị solution của thuật toán
     */
    private static void displaySolution(ArrayList<Robot> robots, ArrayList<Merchandise> warehousing,
                                        Position counterPosition, WarehouseMap warehouseMap, String algorithmName) {
        System.out.println("\n=== SOLUTION " + algorithmName + " ===");

        // Thống kê tổng quan
        int activeRobots = 0;
        int totalItems = 0;
        int totalQuantity = 0;
        float totalDistance = 0;

        for (Robot robot : robots) {
            if (!robot.shoppingCart.isEmpty()) {
                activeRobots++;
                totalItems += robot.shoppingCart.size();
                for (Merchandise item : robot.shoppingCart) {
                    totalQuantity += item.getQuantity();
                }
                totalDistance += calculateRobotDistance(robot, warehousing, counterPosition);
            }
        }

        System.out.println("Robot hoạt động: " + activeRobots + "/" + robots.size());
        System.out.println("Tổng mặt hàng: " + totalItems + " items");
        System.out.println("Tổng số lượng: " + totalQuantity + " units");
        System.out.println("Tổng quãng đường: " + String.format("%.2f", totalDistance) + " units");

        // Chi tiết từng robot
        System.out.println("\nChi tiết robot:");
        for (int i = 0; i < robots.size(); i++) {
            Robot robot = robots.get(i);
            char symbol = (i == 0) ? '*' : (i == 1) ? '#' : '@';

            if (robot.shoppingCart.isEmpty()) {
                System.out.println("Robot " + robot.nameRobot + " (" + symbol + "): Không hoạt động");
            } else {
                float robotDistance = calculateRobotDistance(robot, warehousing, counterPosition);
                System.out.printf("Robot %s (%c): %d items, %d/%d units, %.2f distance\n",
                        robot.nameRobot, symbol, robot.shoppingCart.size(),
                        robot.getCurrentLoad(), robot.capacity, robotDistance);

                // Route chi tiết
                System.out.print("  Route: Counter");
                Position currentPos = counterPosition;
                DistanceCalculator.setCurrentRobotPosition(currentPos);

                for (Merchandise item : robot.shoppingCart) {
                    Merchandise warehouseItem = findItemInWarehouse(item, warehousing);
                    if (warehouseItem != null) {
                        float stepDistance = DistanceCalculator.calculateDistance(currentPos, warehouseItem.getPosition());
                        System.out.printf(" → %s[%d](+%.1f)", item.getName(), item.getQuantity(), stepDistance);
                        currentPos = DistanceCalculator.getCurrentRobotPosition();
                    }
                }

                float returnDistance = DistanceCalculator.calculateDistance(currentPos, counterPosition);
                System.out.printf(" → Counter(+%.1f)\n", returnDistance);
            }
        }

        // Hiển thị bản đồ với đường đi
        displayMapWithPaths(robots, warehousing, counterPosition, warehouseMap, algorithmName);
    }

    /**
     * Hiển thị bản đồ với đường đi robot
     */
    private static void displayMapWithPaths(ArrayList<Robot> robots, ArrayList<Merchandise> warehousing,
                                            Position counterPosition, WarehouseMap warehouseMap, String algorithmName) {
        System.out.println("\n=== BẢN ĐỒ " + algorithmName + " ===");

        try {
            int rows = Params.SHELVES * 3 + 1;
            int cols = Params.SLOTS;
            char[][] map = new char[rows][cols];

            // Khởi tạo bản đồ
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    if (warehouseMap.isWalkable(i, j)) {
                        map[i][j] = '·';
                    } else {
                        map[i][j] = '■';
                    }
                }
            }

            // Đánh dấu Counter
            int[] counterCoords = warehouseMap.positionToCoordinates(counterPosition);
            if (counterCoords[0] < rows && counterCoords[1] < cols) {
                map[counterCoords[0]][counterCoords[1]] = 'C';
            }

            // Vẽ đường đi cho 2 robot đầu
            for (int robotIndex = 0; robotIndex < Math.min(robots.size(), 2); robotIndex++) {
                Robot robot = robots.get(robotIndex);
                char symbol = (robotIndex == 0) ? '*' : '#';

                if (robot.shoppingCart.isEmpty()) continue;

                Position currentPos = counterPosition;
                DistanceCalculator.setCurrentRobotPosition(currentPos);

                // Vẽ đường đi đến từng mặt hàng
                for (Merchandise item : robot.shoppingCart) {
                    Merchandise warehouseItem = findItemInWarehouse(item, warehousing);
                    if (warehouseItem != null) {
                        ArrayList<int[]> path = DistanceCalculator.findPath(currentPos, warehouseItem.getPosition());

                        if (path != null) {
                            // Vẽ đường đi
                            for (int i = 1; i < path.size() - 1; i++) {
                                int[] pos = path.get(i);
                                if (pos[0] < rows && pos[1] < cols && map[pos[0]][pos[1]] == '·') {
                                    map[pos[0]][pos[1]] = symbol;
                                }
                            }

                            // Đánh dấu điểm lấy hàng
                            int[] itemCoords = warehouseMap.positionToCoordinates(warehouseItem.getPosition());
                            if (itemCoords[0] < rows && itemCoords[1] < cols) {
                                map[itemCoords[0]][itemCoords[1]] = Character.toUpperCase(symbol);
                            }
                        }

                        currentPos = DistanceCalculator.getCurrentRobotPosition();
                    }
                }

                // Vẽ đường về
                ArrayList<int[]> returnPath = DistanceCalculator.findPath(currentPos, counterPosition);
                if (returnPath != null) {
                    for (int i = 1; i < returnPath.size() - 1; i++) {
                        int[] pos = returnPath.get(i);
                        if (pos[0] < rows && pos[1] < cols && map[pos[0]][pos[1]] == '·') {
                            map[pos[0]][pos[1]] = symbol;
                        }
                    }
                }
            }

            // In chú thích
            System.out.println("Ký hiệu: C=Counter, ·=lối đi, ■=kệ hàng");
            System.out.println("Robot 1: * = đường đi, * = điểm lấy hàng");
            System.out.println("Robot 2: # = đường đi, # = điểm lấy hàng");
            System.out.println();

            // In bản đồ
            System.out.print("   ");
            for (int j = 0; j < cols; j++) {
                System.out.printf("%2d", j);
            }
            System.out.println();

            for (int i = 0; i < rows; i++) {
                System.out.printf("%2d ", i);
                for (int j = 0; j < cols; j++) {
                    System.out.print(" " + map[i][j]);
                }
                System.out.println();
            }

        } catch (Exception e) {
            System.out.println("Lỗi hiển thị bản đồ: " + e.getMessage());
        }
    }

    /**
     * So sánh kết quả giữa các thuật toán
     */
    private static void compareResults(ArrayList<Robot> greedyRobots, ArrayList<Robot> psoRobots,
                                       ArrayList<Merchandise> warehousing, Position counterPosition,
                                       float greedyResult, float psoResult, long greedyTime, long psoTime) {

        // Tính toán metrics
        int greedyActive = countActiveRobots(greedyRobots);
        int psoActive = countActiveRobots(psoRobots);

        float greedyAvg = greedyActive > 0 ? greedyResult / greedyActive : 0;
        float psoAvg = psoActive > 0 ? psoResult / psoActive : 0;

        // Bảng so sánh
        System.out.println("┌────────────────────┬────────────┬────────────┬──────────┐");
        System.out.println("│ METRIC             │   GREEDY   │  PSO-VNS   │ CHÊNH LỆCH│");
        System.out.println("├────────────────────┼────────────┼────────────┼──────────┤");
        System.out.printf("│ Tổng quãng đường   │ %10.2f │ %10.2f │ %8.2f │\n",
                greedyResult, psoResult, greedyResult - psoResult);
        System.out.printf("│ TB/robot hoạt động │ %10.2f │ %10.2f │ %8.2f │\n",
                greedyAvg, psoAvg, greedyAvg - psoAvg);
        System.out.printf("│ Robot hoạt động    │ %10d │ %10d │ %8d │\n",
                greedyActive, psoActive, greedyActive - psoActive);
        System.out.printf("│ Thời gian (ms)     │ %10d │ %10d │ %8.1fx │\n",
                greedyTime, psoTime, (float)psoTime/greedyTime);
        System.out.println("└────────────────────┴────────────┴────────────┴──────────┘");

        // Kết luận
        System.out.println("\n=== KẾT LUẬN ===");
        float improvement = greedyResult - psoResult;
        float improvementPercent = (improvement / greedyResult) * 100;

        if (improvement > 0) {
            System.out.printf("✓ PSO-VNS TỐT HƠN: Giảm %.2f units (%.1f%%)\n", improvement, improvementPercent);
            if (improvementPercent >= 5) {
                System.out.println("  → Cải thiện đáng kể, nên sử dụng PSO-VNS");
            } else {
                System.out.println("  → Cải thiện nhẹ, có thể cân nhắc");
            }
        } else {
            System.out.printf("✗ GREEDY TỐT HƠN: Ít hơn %.2f units (%.1f%%)\n",
                    Math.abs(improvement), Math.abs(improvementPercent));
            System.out.println("  → Nên sử dụng Greedy cho bài toán này");
        }

        float timeRatio = (float) psoTime / greedyTime;
        System.out.printf("Thời gian: PSO-VNS chậm hơn %.1f lần\n", timeRatio);

        // So sánh từng robot
        System.out.println("\n=== SO SÁNH TỪNG ROBOT ===");
        for (int i = 0; i < Math.max(greedyRobots.size(), psoRobots.size()); i++) {
            String robotName = "Robot " + (i + 1);
            char symbol = (i == 0) ? '*' : (i == 1) ? '#' : '@';

            String greedyInfo = "Không hoạt động";
            String psoInfo = "Không hoạt động";

            if (i < greedyRobots.size() && !greedyRobots.get(i).shoppingCart.isEmpty()) {
                Robot robot = greedyRobots.get(i);
                float distance = calculateRobotDistance(robot, warehousing, counterPosition);
                greedyInfo = String.format("%d items, %.1f units", robot.shoppingCart.size(), distance);
            }

            if (i < psoRobots.size() && !psoRobots.get(i).shoppingCart.isEmpty()) {
                Robot robot = psoRobots.get(i);
                float distance = calculateRobotDistance(robot, warehousing, counterPosition);
                psoInfo = String.format("%d items, %.1f units", robot.shoppingCart.size(), distance);
            }

            System.out.printf("%s (%c): Greedy[%s] vs PSO-VNS[%s]\n",
                    robotName, symbol, greedyInfo, psoInfo);
        }
    }

    // Utility methods

    private static float calculateRobotDistance(Robot robot, ArrayList<Merchandise> warehousing, Position counterPosition) {
        if (robot.shoppingCart.isEmpty()) return 0;

        float total = 0;
        Position currentPos = counterPosition;
        DistanceCalculator.setCurrentRobotPosition(currentPos);

        for (Merchandise item : robot.shoppingCart) {
            Merchandise warehouseItem = findItemInWarehouse(item, warehousing);
            if (warehouseItem != null) {
                total += DistanceCalculator.calculateDistance(currentPos, warehouseItem.getPosition());
                currentPos = DistanceCalculator.getCurrentRobotPosition();
            }
        }

        total += DistanceCalculator.calculateDistance(currentPos, counterPosition);
        return total;
    }

    private static Merchandise findItemInWarehouse(Merchandise item, ArrayList<Merchandise> warehousing) {
        for (Merchandise warehouseItem : warehousing) {
            if (warehouseItem.getName().equals(item.getName())) {
                return warehouseItem;
            }
        }
        return null;
    }

    private static int countActiveRobots(ArrayList<Robot> robots) {
        int count = 0;
        for (Robot robot : robots) {
            if (!robot.shoppingCart.isEmpty()) count++;
        }
        return count;
    }

    /**
     * Test đơn giản khi có lỗi
     */
    private static void runBasicTest() {
        System.out.println("\n=== CHẠY TEST CƠ BẢN ===");

        try {
            // Tạo dữ liệu test tối thiểu
            System.out.println("Tạo dữ liệu test...");

            Params.SHELVES = 3;
            Params.TIERS = 2;
            Params.SLOTS = 4;
            Params.ROBOTS = 2;
            Params.CAPACITY = 20;

            // Tạo kho hàng test
            Params.WAREHOUSE = new ArrayList<>();
            Params.WAREHOUSE.add(new Merchandise("Item1", 5, new Position(1, 1, 1)));
            Params.WAREHOUSE.add(new Merchandise("Item2", 3, new Position(1, 2, 2)));
            Params.WAREHOUSE.add(new Merchandise("Item3", 4, new Position(2, 1, 3)));

            // Tạo yêu cầu test
            Params.REQUIRE = new ArrayList<>();
            Params.REQUIRE.add(new Merchandise("Item1", 3));
            Params.REQUIRE.add(new Merchandise("Item2", 2));

            System.out.println("Thiết lập hệ thống test...");
            WarehouseMap map = WarehouseMap.createMapFromWarehouse(Params.SHELVES, Params.SLOTS);
            DistanceCalculator.initialize(map);
            ArrayList<Merchandise> warehousing = WareHousing.setWareHousing();
            Position counter = new Position(0, 0, 0);

            System.out.println("Chạy Greedy test...");
            Individual individual = new Individual();
            float result = individual.greedy(counter, warehousing);

            System.out.println("✓ Test cơ bản thành công! Kết quả: " + result);

            // Hiển thị thông tin robot
            for (Robot robot : individual.robots) {
                if (!robot.shoppingCart.isEmpty()) {
                    System.out.println("Robot " + robot.nameRobot + ": " +
                            robot.shoppingCart.size() + " items, " +
                            robot.getCurrentLoad() + " units");
                }
            }

        } catch (Exception e) {
            System.err.println("Lỗi test cơ bản: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

