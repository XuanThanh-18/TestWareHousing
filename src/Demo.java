import java.util.ArrayList;

/**
 * Demo đặc biệt để kiểm tra và đảm bảo tất cả robot được sử dụng
 * So sánh hiệu quả giữa việc sử dụng toàn bộ vs một phần robot
 */
public class Demo {
    public static void main(String[] args) {
        System.out.println("🤖 === DEMO SỬ DỤNG TẤT CẢ ROBOT ===");

        try {
            // 1. Đọc dữ liệu và cấu hình
            System.out.println("\n1️⃣ Đọc dữ liệu cấu hình...");
            Params.ReadParams();

            // Điều chỉnh tham số để demo nhanh
            Params.PSO_SWARM_SIZE = 20;
            Params.PSO_MAX_ITERATIONS = 30;
            Params.VNS_MAX_ITERATIONS = 10;

            System.out.println("✅ Cấu hình hiện tại:");
            System.out.println("  - Kho: " + Params.SHELVES + " kệ x " + Params.TIERS + " tầng x " + Params.SLOTS + " ô");
            System.out.println("  - Robot: " + Params.ROBOTS + " (sức chứa: " + Params.CAPACITY + " mỗi robot)");
            System.out.println("  - Mặt hàng: " + Params.WAREHOUSE.size() + " trong kho, " + Params.REQUIRE.size() + " cần lấy");

            // 2. Thiết lập hệ thống
            System.out.println("\n2️⃣ Thiết lập hệ thống...");
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

            // 3. Kiểm tra tính khả thi
            System.out.println("\n3️⃣ Kiểm tra tính khả thi...");
            checkFeasibility();

            // 4. Test với số robot khác nhau
            System.out.println("\n4️⃣ Test với số robot khác nhau...");
            testWithDifferentRobotCounts(warehousing, counterPosition);

            // 5. So sánh Greedy vs PSO-VNS với tất cả robot
            System.out.println("\n5️⃣ So sánh thuật toán với TẤT CẢ robot...");
            compareAlgorithmsAllRobots(warehousing, counterPosition);

            // 6. Phân tích hiệu quả sử dụng robot
            System.out.println("\n6️⃣ Phân tích hiệu quả sử dụng robot...");
            analyzeRobotEfficiency(warehousing, counterPosition);

        } catch (Exception e) {
            System.err.println("❌ Lỗi trong demo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Kiểm tra tính khả thi của bài toán
     */
    private static void checkFeasibility() {
        int totalRequired = 0;
        for (Merchandise item : Params.REQUIRE) {
            totalRequired += item.getQuantity();
        }

        int totalCapacity = Params.ROBOTS * Params.CAPACITY;
        double utilizationRate = (totalRequired * 100.0) / totalCapacity;

        System.out.println("📊 Phân tích tính khả thi:");
        System.out.println("  - Tổng hàng cần lấy: " + totalRequired + " đơn vị");
        System.out.println("  - Tổng sức chứa robot: " + totalCapacity + " đơn vị");
        System.out.printf("  - Tỷ lệ sử dụng: %.1f%%\n", utilizationRate);

        if (utilizationRate > 100) {
            System.out.println("  ⚠️ CẢNH BÁO: Vượt quá sức chứa!");
        } else if (utilizationRate < 30) {
            System.out.println("  📝 Gợi ý: Có thể giảm số robot hoặc tăng khối lượng công việc");
        } else {
            System.out.println("  ✅ Tỷ lệ sử dụng hợp lý");
        }

        // Phân tích khả năng phân bổ đều
        double avgLoadPerRobot = (double) totalRequired / Params.ROBOTS;
        System.out.printf("  - Tải trọng trung bình/robot: %.1f đơn vị\n", avgLoadPerRobot);
        System.out.printf("  - Tỷ lệ so với sức chứa: %.1f%%\n", (avgLoadPerRobot * 100.0) / Params.CAPACITY);
    }

    /**
     * Test với số robot khác nhau
     */
    private static void testWithDifferentRobotCounts(ArrayList<Merchandise> warehousing, Position counterPosition) {
        int originalRobots = Params.ROBOTS;
        int[] robotCounts = {1, 2, 3, Math.max(4, originalRobots)};

        System.out.println("🧪 Test hiệu quả với số robot khác nhau:");
        System.out.println("─".repeat(80));
        System.out.printf("%-8s %-12s %-12s %-15s %-15s %-10s\n",
                "Robots", "Greedy", "PSO-VNS", "Robots Active", "Efficiency", "Time(s)");
        System.out.println("─".repeat(80));

        for (int robotCount : robotCounts) {
            if (robotCount > originalRobots) continue;

            Params.ROBOTS = robotCount;

            // Test Greedy
            long startTime = System.currentTimeMillis();
            Individual greedyInd = new Individual();
            float greedyResult = greedyInd.greedy(counterPosition, warehousing);
            int greedyActive = countActiveRobots(greedyInd.robots);
            long greedyTime = System.currentTimeMillis() - startTime;

            // Test PSO-VNS
            startTime = System.currentTimeMillis();
            Individual psoInd = new Individual();
            float psoResult = psoInd.solvePsoVns(counterPosition, warehousing);
            int psoActive = countActiveRobots(psoInd.robots);
            long psoTime = System.currentTimeMillis() - startTime;

            // Tính efficiency
            double greedyEff = greedyActive * 100.0 / robotCount;
            double psoEff = psoActive * 100.0 / robotCount;

            System.out.printf("%-8d %-12.1f %-12.1f %-15s %-15s %-10.1f\n",
                    robotCount, greedyResult, psoResult,
                    greedyActive + "/" + robotCount + "(" + String.format("%.0f", greedyEff) + "%)",
                    psoActive + "/" + robotCount + "(" + String.format("%.0f", psoEff) + "%)",
                    (psoTime / 1000.0));
        }

        System.out.println("─".repeat(80));
        Params.ROBOTS = originalRobots; // Khôi phục giá trị gốc
    }

    /**
     * So sánh thuật toán với tất cả robot
     */
    private static void compareAlgorithmsAllRobots(ArrayList<Merchandise> warehousing, Position counterPosition) {
        System.out.println("🏁 So sánh chi tiết với TẤT CẢ " + Params.ROBOTS + " robot:");

        // Test Greedy
        System.out.println("\n🔸 GREEDY ALGORITHM:");
        Individual greedyIndividual = new Individual();
        long startGreedy = System.currentTimeMillis();
        float greedyResult = greedyIndividual.greedy(counterPosition, warehousing);
        long greedyTime = System.currentTimeMillis() - startGreedy;

        analyzeRobotUsage(greedyIndividual.robots, "GREEDY");

        // Test PSO-VNS
        System.out.println("\n🔸 PSO-VNS ALGORITHM:");
        Individual psoIndividual = new Individual();
        long startPSO = System.currentTimeMillis();
        float psoResult = psoIndividual.solvePsoVns(counterPosition, warehousing);
        long psoTime = System.currentTimeMillis() - startPSO;

        analyzeRobotUsage(psoIndividual.robots, "PSO-VNS");

        // So sánh tổng thể
        System.out.println("\n📊 SO SÁNH TỔNG THỂ:");
        printComparisonTable(greedyIndividual.robots, psoIndividual.robots,
                greedyResult, psoResult, greedyTime, psoTime);
    }

    /**
     * Phân tích cách sử dụng robot
     */
    private static void analyzeRobotUsage(ArrayList<Robot> robots, String algorithmName) {
        int activeRobots = countActiveRobots(robots);
        int totalRobots = robots.size();

        System.out.println("  📈 Phân tích " + algorithmName + ":");
        System.out.printf("    - Robot hoạt động: %d/%d (%.1f%%)\n",
                activeRobots, totalRobots, (activeRobots * 100.0 / totalRobots));

        // Phân tích phân bổ tải
        ArrayList<Integer> loads = new ArrayList<>();
        int totalLoad = 0;
        int maxLoad = 0;
        int minLoad = Integer.MAX_VALUE;

        for (Robot robot : robots) {
            int load = robot.getCurrentLoad();
            loads.add(load);
            totalLoad += load;
            maxLoad = Math.max(maxLoad, load);
            if (load > 0) minLoad = Math.min(minLoad, load);
        }

        if (minLoad == Integer.MAX_VALUE) minLoad = 0;

        double avgLoad = totalLoad / (double) Math.max(1, activeRobots);
        double loadBalance = activeRobots > 0 ? (1.0 - (double)(maxLoad - minLoad) / Math.max(1, maxLoad)) * 100 : 0;

        System.out.printf("    - Tải trọng TB: %.1f, Min: %d, Max: %d\n", avgLoad, minLoad, maxLoad);
        System.out.printf("    - Độ cân bằng tải: %.1f%%\n", loadBalance);

        // Chi tiết từng robot
        System.out.println("    - Chi tiết robot:");
        for (Robot robot : robots) {
            int load = robot.getCurrentLoad();
            int itemCount = robot.shoppingCart.size();
            String status = itemCount > 0 ? "🟢" : "🔴";

            System.out.printf("      %s Robot %s: %d items, %d/%d units (%.1f%%)\n",
                    status, robot.nameRobot, itemCount, load, robot.capacity,
                    (load * 100.0 / robot.capacity));
        }
    }

    /**
     * In bảng so sánh
     */
    private static void printComparisonTable(ArrayList<Robot> greedyRobots, ArrayList<Robot> psoRobots,
                                             float greedyResult, float psoResult, long greedyTime, long psoTime) {

        int greedyActive = countActiveRobots(greedyRobots);
        int psoActive = countActiveRobots(psoRobots);
        int totalRobots = greedyRobots.size();

        System.out.println("┌─────────────────────┬──────────────┬──────────────┬─────────────┐");
        System.out.println("│ METRIC              │    GREEDY    │   PSO-VNS    │  CHÊNH LỆCH │");
        System.out.println("├─────────────────────┼──────────────┼──────────────┼─────────────┤");
        System.out.printf("│ Tổng quãng đường    │ %12.1f │ %12.1f │ %11.1f │\n",
                greedyResult, psoResult, greedyResult - psoResult);
        System.out.printf("│ Robot hoạt động     │ %12s │ %12s │ %11s │\n",
                greedyActive + "/" + totalRobots, psoActive + "/" + totalRobots,
                (psoActive - greedyActive > 0 ? "+" : "") + (psoActive - greedyActive));
        System.out.printf("│ Tỷ lệ sử dụng robot │ %11.1f%% │ %11.1f%% │ %10.1f%% │\n",
                (greedyActive * 100.0 / totalRobots), (psoActive * 100.0 / totalRobots),
                ((psoActive - greedyActive) * 100.0 / totalRobots));

        // Tính tải trọng trung bình
        double greedyAvgLoad = calculateAverageLoad(greedyRobots);
        double psoAvgLoad = calculateAverageLoad(psoRobots);

        System.out.printf("│ TB tải/robot hoạt động │ %10.1f │ %12.1f │ %11.1f │\n",
                greedyAvgLoad, psoAvgLoad, psoAvgLoad - greedyAvgLoad);
        System.out.printf("│ Thời gian (giây)    │ %12.1f │ %12.1f │ %10.1fx │\n",
                greedyTime / 1000.0, psoTime / 1000.0, (double)psoTime / greedyTime);

        // Tính hiệu quả tổng thể
        double greedyEfficiency = calculateEfficiency(greedyRobots, greedyResult);
        double psoEfficiency = calculateEfficiency(psoRobots, psoResult);

        System.out.printf("│ Hiệu quả tổng thể   │ %12.1f │ %12.1f │ %11.1f │\n",
                greedyEfficiency, psoEfficiency, psoEfficiency - greedyEfficiency);
        System.out.println("└─────────────────────┴──────────────┴──────────────┴─────────────┘");

        // Kết luận
        System.out.println("\n🎯 KẾT LUẬN:");

        if (psoActive > greedyActive) {
            System.out.println("✅ PSO-VNS sử dụng nhiều robot hơn (" + psoActive + " vs " + greedyActive + ")");
        } else if (psoActive == greedyActive && psoActive == totalRobots) {
            System.out.println("🏆 CẢ HAI thuật toán đều sử dụng TẤT CẢ robot!");
        } else {
            System.out.println("⚠️ Cần cải thiện để sử dụng tất cả " + totalRobots + " robot");
        }

        float improvement = greedyResult - psoResult;
        if (improvement > 0) {
            System.out.printf("✅ PSO-VNS tốt hơn %.1f đơn vị (%.1f%% cải thiện)\n",
                    improvement, (improvement * 100.0 / greedyResult));
        } else {
            System.out.printf("❌ Greedy tốt hơn %.1f đơn vị\n", Math.abs(improvement));
        }

        // Khuyến nghị
        double utilizationRate = (psoActive * 100.0) / totalRobots;
        if (utilizationRate < 100) {
            System.out.println("💡 KHUYẾN NGHỊ:");
            System.out.println("  - Điều chỉnh tham số PSO để khuyến khích sử dụng tất cả robot");
            System.out.println("  - Tăng penalty cho robot không hoạt động");
            System.out.println("  - Cân nhắc chia nhỏ mặt hàng lớn để phân bổ đều hơn");
        }
    }

    /**
     * Phân tích hiệu quả sử dụng robot
     */
    private static void analyzeRobotEfficiency(ArrayList<Merchandise> warehousing, Position counterPosition) {
        System.out.println("🔍 Phân tích sâu về hiệu quả sử dụng robot:");

        // Test với các chiến lược khác nhau
        String[] strategies = {"Mặc định", "Ưu tiên tất cả robot", "Tối ưu cân bằng"};

        for (String strategy : strategies) {
            System.out.println("\n📋 Chiến lược: " + strategy);

            // Điều chỉnh tham số theo chiến lược
            adjustParametersForStrategy(strategy);

            Individual individual = new Individual();
            long startTime = System.currentTimeMillis();
            float result = individual.solvePsoVns(counterPosition, warehousing);
            long time = System.currentTimeMillis() - startTime;

            int activeRobots = countActiveRobots(individual.robots);
            double efficiency = calculateEfficiency(individual.robots, result);

            System.out.printf("  ⚡ Kết quả: %.1f distance, %d/%d robots (%.1f%%), hiệu quả: %.1f, thời gian: %.1fs\n",
                    result, activeRobots, individual.robots.size(),
                    (activeRobots * 100.0 / individual.robots.size()), efficiency, time / 1000.0);
        }

        // Khôi phục tham số mặc định
        resetDefaultParameters();
    }

    /**
     * Điều chỉnh tham số theo chiến lược
     */
    private static void adjustParametersForStrategy(String strategy) {
        switch (strategy) {
            case "Ưu tiên tất cả robot":
                // Tăng penalty cho robot không hoạt động (được implement trong PSO)
                Params.PSO_SWARM_SIZE = 25;
                Params.PSO_MAX_ITERATIONS = 40;
                break;
            case "Tối ưu cân bằng":
                // Focus vào cân bằng tải
                Params.PSO_SWARM_SIZE = 30;
                Params.VNS_MAX_ITERATIONS = 20;
                break;
            default:
                // Giữ nguyên tham số mặc định
                break;
        }
    }

    /**
     * Khôi phục tham số mặc định
     */
    private static void resetDefaultParameters() {
        Params.PSO_SWARM_SIZE = 20;
        Params.PSO_MAX_ITERATIONS = 30;
        Params.VNS_MAX_ITERATIONS = 10;
    }

    /**
     * Đếm số robot hoạt động
     */
    private static int countActiveRobots(ArrayList<Robot> robots) {
        int count = 0;
        for (Robot robot : robots) {
            if (!robot.shoppingCart.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Tính tải trọng trung bình của robot hoạt động
     */
    private static double calculateAverageLoad(ArrayList<Robot> robots) {
        int totalLoad = 0;
        int activeRobots = 0;

        for (Robot robot : robots) {
            if (!robot.shoppingCart.isEmpty()) {
                totalLoad += robot.getCurrentLoad();
                activeRobots++;
            }
        }

        return activeRobots > 0 ? (double) totalLoad / activeRobots : 0;
    }

    /**
     * Tính hiệu quả tổng thể (số robot hoạt động / tổng quãng đường)
     */
    private static double calculateEfficiency(ArrayList<Robot> robots, float totalDistance) {
        int activeRobots = countActiveRobots(robots);
        if (totalDistance <= 0) return 0;

        // Hiệu quả = (số robot hoạt động * 1000) / tổng quãng đường
        return (activeRobots * 1000.0) / totalDistance;
    }

    /**
     * In bản đồ với đường đi của tất cả robot
     */
    private static void printRobotMap(ArrayList<Robot> robots, ArrayList<Merchandise> warehousing,
                                      Position counterPosition, WarehouseMap warehouseMap, String title) {
        System.out.println("\n🗺️ " + title);

        try {
            int rows = Params.SHELVES * 3 + 1;
            int cols = Params.SLOTS;
            char[][] map = new char[rows][cols];

            // Khởi tạo bản đồ cơ bản
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

            // Ký hiệu cho các robot
            char[] robotSymbols = {'1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

            // Vẽ đường đi cho từng robot
            for (int robotIndex = 0; robotIndex < Math.min(robots.size(), robotSymbols.length); robotIndex++) {
                Robot robot = robots.get(robotIndex);
                if (robot.shoppingCart.isEmpty()) continue;

                char symbol = robotSymbols[robotIndex];
                DistanceCalculator.setCurrentRobotPosition(counterPosition);

                // Đánh dấu các điểm lấy hàng
                for (Merchandise item : robot.shoppingCart) {
                    Merchandise warehouseItem = findItemInWarehouse(item, warehousing);
                    if (warehouseItem != null) {
                        int[] itemCoords = warehouseMap.positionToCoordinates(warehouseItem.getPosition());
                        if (itemCoords[0] < rows && itemCoords[1] < cols) {
                            map[itemCoords[0]][itemCoords[1]] = symbol;
                        }
                    }
                }
            }

            // In chú thích
            System.out.println("Ký hiệu: C=Counter, ·=lối đi, ■=kệ hàng");
            System.out.print("Robot: ");
            for (int i = 0; i < Math.min(robots.size(), robotSymbols.length); i++) {
                if (!robots.get(i).shoppingCart.isEmpty()) {
                    System.out.print(robotSymbols[i] + "=Robot" + robots.get(i).nameRobot + " ");
                }
            }
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
            System.out.println("❌ Lỗi hiển thị bản đồ: " + e.getMessage());
        }
    }

    /**
     * Tìm mặt hàng trong kho
     */
    private static Merchandise findItemInWarehouse(Merchandise item, ArrayList<Merchandise> warehousing) {
        for (Merchandise warehouseItem : warehousing) {
            if (warehouseItem.getName().equals(item.getName())) {
                return warehouseItem;
            }
        }
        return null;
    }
}