import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Chương trình chính để chạy và so sánh các thuật toán:
 * - Greedy Algorithm
 * - PSO Algorithm
 * - PSO-VNS Hybrid Algorithm
 *
 * Kết quả được lưu vào file output với báo cáo chi tiết
 */
public class Main {
    private static final String OUTPUT_FILE = "results_output.txt";
    private static PrintWriter writer;
    private static ArrayList<Merchandise> warehousing;
    private static Position counterPosition;

    public static void main(String[] args) {
        try {
            // Khởi tạo file output
            initializeOutputFile();

            // 1. Đọc và khởi tạo dữ liệu
            logSection("KHỞI TẠO HỆ THỐNG");
            initializeSystem();

            // 2. Hiển thị thông tin cấu hình
            logSection("THÔNG TIN CẤU HÌNH");
            displayConfiguration();

            // 3. Kiểm tra tính khả thi
            logSection("KIỂM TRA TÍNH KHẢ THI");
            checkFeasibility();

            // 4. Chạy các thuật toán
            logSection("THỰC HIỆN CÁC THUẬT TOÁN");

            // Greedy Algorithm
            AlgorithmResult greedyResult = runGreedy();

            // PSO Algorithm
            AlgorithmResult psoResult = runPSO();

            // PSO-VNS Hybrid Algorithm
            AlgorithmResult psoVnsResult = runPSOVNS();

            // 5. So sánh và báo cáo
            logSection("BẢNG SO SÁNH KẾT QUẢ");
            generateComparisonReport(greedyResult, psoResult, psoVnsResult);

            // 6. Báo cáo chi tiết từng thuật toán
            logSection("BÁO CÁO CHI TIẾT");
            generateDetailedReports(greedyResult, psoResult, psoVnsResult);

            // 7. Phân tích và khuyến nghị
            logSection("PHÂN TÍCH VÀ KHUYẾN NGHỊ");
            generateAnalysisAndRecommendations(greedyResult, psoResult, psoVnsResult);

            // 8. Kết luận
            logSection("KẾT LUẬN");
            generateConclusion(greedyResult, psoResult, psoVnsResult);

            log("✅ Chương trình hoàn thành thành công!");
            log("📄 Kết quả đã được lưu vào file: " + OUTPUT_FILE);

        } catch (Exception e) {
            logError("❌ Lỗi trong quá trình thực hiện: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeOutputFile();
        }
    }

    /**
     * Khởi tạo file output
     */
    private static void initializeOutputFile() throws IOException {
        writer = new PrintWriter(new FileWriter(OUTPUT_FILE));
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        log("=".repeat(80));
        log("              BÁO CÁO KẾT QUẢ THUẬT TOÁN TỐI ƯU HÓA KHO HÀNG");
        log("=".repeat(80));
        log("Thời gian tạo báo cáo: " + dateFormat.format(new Date()));
        log("Các thuật toán: Greedy, PSO, PSO-VNS Hybrid");
        log("=".repeat(80));
        log("");
    }

    /**
     * Khởi tạo hệ thống
     */
    private static void initializeSystem() {
        try {
            log("Đang đọc tham số từ file cấu hình...");
            Params.ReadParams();

            log("Đã đọc thành công các tham số:");
            log("   - Kho: " + Params.SHELVES + " kệ x " + Params.TIERS + " tầng x " + Params.SLOTS + " ô");
            log("   - Robot: " + Params.ROBOTS + " (sức chứa: " + Params.CAPACITY + " mỗi robot)");
            log("   - Mặt hàng: " + Params.WAREHOUSE.size() + " trong kho, " + Params.REQUIRE.size() + " cần lấy");

            log("\nKhởi tạo bản đồ kho hàng...");
            WarehouseMap warehouseMap;
            if (Params.WAREHOUSE_MAP != null) {
                warehouseMap = new WarehouseMap(Params.WAREHOUSE_MAP);
                log("Sử dụng bản đồ từ file cấu hình");
            } else {
                warehouseMap = WarehouseMap.createMapFromWarehouse(Params.SHELVES, Params.SLOTS);
                log("Tạo bản đồ mặc định");
            }

//            log("\nKhởi tạo DistanceCalculator...");
            DistanceCalculator.initialize(warehouseMap);

//            log("\nThiết lập kho hàng...");
            warehousing = WareHousing.setWareHousing();

//            log("\nThiết lập vị trí xuất phát...");
            counterPosition = new Position(0, 0, 0);

//            log("\nTính toán trước khoảng cách...");
            DistanceCalculator.precomputeAllDistances(warehousing, counterPosition);

            log("Hệ thống đã được khởi tạo hoàn toàn!");

        } catch (Exception e) {
            logError("Lỗi khởi tạo hệ thống: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Hiển thị thông tin cấu hình
     */
    private static void displayConfiguration() {
        log("📋 THÔNG TIN CHI TIẾT:");
        log("─".repeat(50));

        // Thông tin kho hàng
        log("🏢 KHO HÀNG:");
        log("   Cấu trúc: " + Params.SHELVES + " khối kệ x " + Params.TIERS + " tầng x " + Params.SLOTS + " ô");
        log("   Tổng ô: " + (Params.SHELVES * Params.TIERS * Params.SLOTS));
        if (Params.WAREHOUSE_MAP != null) {
            log("   Bản đồ: " + Params.WAREHOUSE_MAP.length + " x " + Params.WAREHOUSE_MAP[0].length);
        }

        // Thông tin robot
        log("\n🤖 ROBOT:");
        log("   Số lượng: " + Params.ROBOTS);
        log("   Sức chứa mỗi robot: " + Params.CAPACITY + " đơn vị");
        log("   Tổng sức chứa: " + (Params.ROBOTS * Params.CAPACITY) + " đơn vị");
        log("   Vị trí xuất phát: " + counterPosition);

        // Thống kê mặt hàng
        int totalWarehouseQty = warehousing.stream().mapToInt(Merchandise::getQuantity).sum();
        int totalRequiredQty = Params.REQUIRE.stream().mapToInt(Merchandise::getQuantity).sum();

        log("\n📦 MẶT HÀNG:");
        log("   Trong kho: " + warehousing.size() + " loại (" + totalWarehouseQty + " đơn vị)");
        log("   Cần lấy: " + Params.REQUIRE.size() + " loại (" + totalRequiredQty + " đơn vị)");
        log("   Tỷ lệ sử dụng kho: " + String.format("%.1f%%", totalRequiredQty * 100.0 / totalWarehouseQty));
        log("   Tỷ lệ sử dụng robot: " + String.format("%.1f%%", totalRequiredQty * 100.0 / (Params.ROBOTS * Params.CAPACITY)));

        // Chi tiết mặt hàng cần lấy
        log("\n📝 DANH SÁCH MẶT HÀNG CẦN LẤY:");
        for (int i = 0; i < Params.REQUIRE.size(); i++) {
            Merchandise item = Params.REQUIRE.get(i);
            log(String.format("   %2d. %-20s: %3d đơn vị", i+1, item.getName(), item.getQuantity()));
        }

        // Tham số thuật toán
        log("\n THAM SỐ THUẬT TOÁN:");
        log("   PSO - Kích thước đàn: " + Params.PSO_SWARM_SIZE);
        log("   PSO - Số vòng lặp: " + Params.PSO_MAX_ITERATIONS);
        log("   PSO - Trọng số quán tính: " + Params.PSO_INERTIA_WEIGHT);
        log("   PSO - Hệ số nhận thức: " + Params.PSO_COGNITIVE_COEFFICIENT);
        log("   PSO - Hệ số xã hội: " + Params.PSO_SOCIAL_COEFFICIENT);
        log("   VNS - Số vòng lặp: " + Params.VNS_MAX_ITERATIONS);
        log("   VNS - Số lân cận: " + Params.VNS_MAX_NEIGHBORHOODS);
    }

    /**
     * Kiểm tra tính khả thi
     */
    private static void checkFeasibility() {
        int totalRequired = Params.REQUIRE.stream().mapToInt(Merchandise::getQuantity).sum();
        int totalCapacity = Params.ROBOTS * Params.CAPACITY;

        log("🔍 KIỂM TRA TÍNH KHẢ THI:");
        log("─".repeat(40));

        boolean feasible = true;

        // Kiểm tra sức chứa tổng thể
        log("📊 Kiểm tra sức chứa:");
        log("   Tổng yêu cầu: " + totalRequired + " đơn vị");
        log("   Tổng sức chứa robot: " + totalCapacity + " đơn vị");
        if (totalRequired <= totalCapacity) {
            log("   ✅ Đủ sức chứa");
        } else {
            log("   ❌ Thiếu " + (totalRequired - totalCapacity) + " đơn vị sức chứa");
            feasible = false;
        }

        // Kiểm tra từng mặt hàng
        log("\n📝 Kiểm tra tồn kho:");
        for (Merchandise reqItem : Params.REQUIRE) {
            int available = warehousing.stream()
                    .filter(w -> w.getName().equals(reqItem.getName()))
                    .mapToInt(Merchandise::getQuantity)
                    .sum();

            if (available >= reqItem.getQuantity()) {
                log("   ✅ " + reqItem.getName() + ": " + reqItem.getQuantity() + "/" + available);
            } else {
                log("   ❌ " + reqItem.getName() + ": " + reqItem.getQuantity() + "/" + available +
                        " (thiếu " + (reqItem.getQuantity() - available) + ")");
                feasible = false;
            }
        }

        // Đánh giá khả năng phân bổ đều
        double avgLoadPerRobot = (double) totalRequired / Params.ROBOTS;
        log("\n⚖️ Phân tích cân bằng tải:");
        log("   Tải trọng TB/robot: " + String.format("%.1f", avgLoadPerRobot) + " đơn vị");
        log("   Tỷ lệ sử dụng TB: " + String.format("%.1f%%", avgLoadPerRobot * 100.0 / Params.CAPACITY));

        if (avgLoadPerRobot < Params.CAPACITY * 0.3) {
            log("   📝 Ghi chú: Tải trọng thấp, có thể giảm số robot");
        } else if (avgLoadPerRobot > Params.CAPACITY * 0.9) {
            log("   ⚠️ Cảnh báo: Tải trọng cao, khó cân bằng");
        }

        log("\n🎯 KẾT QUẢ TỔNG THỂ: " + (feasible ? "✅ BÀI TOÁN KHẢ THI" : "❌ BÀI TOÁN CÓ VẤN ĐỀ"));
    }

    /**
     * Chạy thuật toán Greedy
     */
    private static AlgorithmResult runGreedy() {
        log("🟢 CHẠY THUẬT TOÁN GREEDY");
        log("─".repeat(40));

        try {
            Individual individual = new Individual();

            long startTime = System.currentTimeMillis();
            float totalDistance = individual.greedy(counterPosition, warehousing);
            long endTime = System.currentTimeMillis();

            long executionTime = endTime - startTime;
            int activeRobots = countActiveRobots(individual.robots);

            log("✅ Greedy hoàn thành trong " + executionTime + "ms");
            log("📊 Kết quả: " + totalDistance + " đơn vị quãng đường");
            log("🤖 Robot hoạt động: " + activeRobots + "/" + individual.robots.size());

            return new AlgorithmResult("Greedy", totalDistance, executionTime, activeRobots,
                    individual.robots.size(), individual.robots);

        } catch (Exception e) {
            logError("❌ Lỗi Greedy: " + e.getMessage());
            return new AlgorithmResult("Greedy", Float.MAX_VALUE, 0, 0, 0, new ArrayList<>());
        }
    }

    /**
     * Chạy thuật toán PSO
     */
    private static AlgorithmResult runPSO() {
        log("\n🔵 CHẠY THUẬT TOÁN PSO");
        log("─".repeat(40));

        try {
            // Tạo PSO với tham số
            PSO pso = new PSO(Params.PSO_SWARM_SIZE, Params.PSO_MAX_ITERATIONS,
                    Params.PSO_INERTIA_WEIGHT, Params.PSO_COGNITIVE_COEFFICIENT,
                    Params.PSO_SOCIAL_COEFFICIENT);

            // Tạo robot cho PSO
            ArrayList<Robot> robots = new ArrayList<>();
            for (int i = 0; i < Params.ROBOTS; i++) {
                Robot robot = new Robot(String.valueOf(i + 1), counterPosition.copy());
                robot.capacity = Params.CAPACITY;
                robots.add(robot);
            }

            log("🚀 Bắt đầu PSO với " + Params.PSO_SWARM_SIZE + " hạt, " +
                    Params.PSO_MAX_ITERATIONS + " vòng lặp");

            long startTime = System.currentTimeMillis();
            Solution solution = pso.solve(warehousing, Params.REQUIRE, robots);
            long endTime = System.currentTimeMillis();

            long executionTime = endTime - startTime;
            int activeRobots = countActiveRobotsSolution(solution);

            log("✅ PSO hoàn thành trong " + executionTime + "ms");
            log("📊 Kết quả: " + solution.getFitness() + " đơn vị quãng đường");
            log("🤖 Robot hoạt động: " + activeRobots + "/" + solution.getRobots().size());

            return new AlgorithmResult("PSO", (float)solution.getFitness(), executionTime,
                    activeRobots, solution.getRobots().size(), solution.getRobots());

        } catch (Exception e) {
            logError("❌ Lỗi PSO: " + e.getMessage());
            return new AlgorithmResult("PSO", Float.MAX_VALUE, 0, 0, 0, new ArrayList<>());
        }
    }

    /**
     * Chạy thuật toán PSO-VNS
     */
    private static AlgorithmResult runPSOVNS() {
        log("\n🟣 CHẠY THUẬT TOÁN PSO-VNS HYBRID");
        log("─".repeat(40));

        try {
            Individual individual = new Individual();

            log("🚀 Bắt đầu PSO-VNS Hybrid");
            log("   PSO: " + Params.PSO_SWARM_SIZE + " hạt, " + Params.PSO_MAX_ITERATIONS + " vòng lặp");
            log("   VNS: " + Params.VNS_MAX_ITERATIONS + " vòng lặp, " + Params.VNS_MAX_NEIGHBORHOODS + " lân cận");

            long startTime = System.currentTimeMillis();
            float totalDistance = individual.solvePsoVns(counterPosition, warehousing);
            long endTime = System.currentTimeMillis();

            long executionTime = endTime - startTime;
            int activeRobots = countActiveRobots(individual.robots);

            log("✅ PSO-VNS hoàn thành trong " + executionTime + "ms");
            log("📊 Kết quả: " + totalDistance + " đơn vị quãng đường");
            log("🤖 Robot hoạt động: " + activeRobots + "/" + individual.robots.size());

            return new AlgorithmResult("PSO-VNS", totalDistance, executionTime, activeRobots,
                    individual.robots.size(), individual.robots);

        } catch (Exception e) {
            logError("❌ Lỗi PSO-VNS: " + e.getMessage());
            return new AlgorithmResult("PSO-VNS", Float.MAX_VALUE, 0, 0, 0, new ArrayList<>());
        }
    }

    /**
     * Tạo bảng so sánh kết quả
     */
    private static void generateComparisonReport(AlgorithmResult greedy, AlgorithmResult pso, AlgorithmResult psoVns) {
        log("📊 BẢNG SO SÁNH TỔNG QUAN");
        log("=".repeat(80));

        // Header
        log(String.format("%-15s %-15s %-15s %-15s %-15s %-10s",
                "THUẬT TOÁN", "QUÃNG ĐƯỜNG", "THỜI GIAN(ms)", "ROBOT HOẠT ĐỘNG", "TỶ LỆ SỬ DỤNG", "HIỆU QUẢ"));
        log("─".repeat(80));

        // Dữ liệu
        printResultRow(greedy);
        printResultRow(pso);
        printResultRow(psoVns);

        log("─".repeat(80));

        // Tìm kết quả tốt nhất
        AlgorithmResult bestDistance = findBestByDistance(greedy, pso, psoVns);
        AlgorithmResult bestTime = findBestByTime(greedy, pso, psoVns);
        AlgorithmResult bestRobotUsage = findBestByRobotUsage(greedy, pso, psoVns);

        log("🏆 THUẬT TOÁN TỐT NHẤT:");
        log("   Quãng đường ngắn nhất: " + bestDistance.algorithmName +
                " (" + String.format("%.1f", bestDistance.totalDistance) + ")");
        log("   Thời gian nhanh nhất: " + bestTime.algorithmName +
                " (" + bestTime.executionTime + "ms)");
        log("   Sử dụng robot tốt nhất: " + bestRobotUsage.algorithmName +
                " (" + bestRobotUsage.activeRobots + "/" + bestRobotUsage.totalRobots + ")");

        // Tính phần trăm cải thiện
        if (!bestDistance.algorithmName.equals("Greedy")) {
            float improvement = greedy.totalDistance - bestDistance.totalDistance;
            float percentImprovement = (improvement / greedy.totalDistance) * 100;
            log("   Cải thiện so với Greedy: " + String.format("%.1f", improvement) +
                    " đơn vị (" + String.format("%.1f%%", percentImprovement) + ")");
        }
    }

    /**
     * Tạo báo cáo chi tiết từng thuật toán
     */
    private static void generateDetailedReports(AlgorithmResult greedy, AlgorithmResult pso, AlgorithmResult psoVns) {
        generateAlgorithmDetailReport(greedy);
        generateAlgorithmDetailReport(pso);
        generateAlgorithmDetailReport(psoVns);
    }

    /**
     * Tạo báo cáo chi tiết cho một thuật toán
     */
    private static void generateAlgorithmDetailReport(AlgorithmResult result) {
        log("\n📋 BÁO CÁO CHI TIẾT - " + result.algorithmName.toUpperCase());
        log("=".repeat(60));

        log("📊 Kết quả tổng quan:");
        log("   Tổng quãng đường: " + String.format("%.1f", result.totalDistance) + " đơn vị");
        log("   Thời gian thực hiện: " + result.executionTime + " ms (" +
                String.format("%.2f", result.executionTime / 1000.0) + " giây)");
        log("   Robot hoạt động: " + result.activeRobots + "/" + result.totalRobots +
                " (" + String.format("%.1f%%", result.activeRobots * 100.0 / result.totalRobots) + ")");

        // Phân tích hiệu suất
        double efficiency = result.activeRobots * 1000.0 / Math.max(result.totalDistance, 1);
        log("   Chỉ số hiệu quả: " + String.format("%.2f", efficiency));

        // Chi tiết từng robot
        log("\n🤖 Chi tiết robot:");
        log("─".repeat(50));
        log(String.format("%-8s %-12s %-15s %-15s", "ROBOT", "SỐ MẶT HÀNG", "TẢI TRỌNG", "QUÃNG ĐƯỜNG"));
        log("─".repeat(50));

        float totalRobotDistance = 0;
        for (int i = 0; i < result.robots.size(); i++) {
            Robot robot = result.robots.get(i);
            int itemCount = robot.shoppingCart.size();
            int load = robot.getCurrentLoad();

            // Tính quãng đường cho robot này
            float robotDistance = calculateRobotDistance(robot, warehousing);
            totalRobotDistance += robotDistance;

            String status = itemCount > 0 ? "🟢" : "🔴";
            log(String.format("%-8s %-12d %-15s %-15s",
                    status + " " + robot.nameRobot,
                    itemCount,
                    load + "/" + robot.capacity,
                    String.format("%.1f", robotDistance)));

            // Chi tiết các mặt hàng nếu có
            if (itemCount > 0 && itemCount <= 10) { // Chỉ hiển thị nếu ít hơn 10 items
                log("        Hàng hóa: " +
                        robot.shoppingCart.stream()
                                .map(m -> m.getName() + "(" + m.getQuantity() + ")")
                                .reduce((a, b) -> a + ", " + b)
                                .orElse(""));
            }
        }

        log("─".repeat(50));
        log("TỔNG CỘNG: " + String.format("%.1f", totalRobotDistance) + " đơn vị");

        // Phân tích phân bổ tải
        analyzeLoadDistribution(result);
    }

    /**
     * Phân tích phân bổ tải
     */
    private static void analyzeLoadDistribution(AlgorithmResult result) {
        log("\n⚖️ Phân tích phân bổ tải:");

        ArrayList<Integer> loads = new ArrayList<>();
        for (Robot robot : result.robots) {
            if (!robot.shoppingCart.isEmpty()) {
                loads.add(robot.getCurrentLoad());
            }
        }

        if (loads.isEmpty()) {
            log("   Không có robot nào hoạt động");
            return;
        }

        int minLoad = loads.stream().min(Integer::compareTo).orElse(0);
        int maxLoad = loads.stream().max(Integer::compareTo).orElse(0);
        double avgLoad = loads.stream().mapToInt(Integer::intValue).average().orElse(0);

        log("   Tải trọng TB: " + String.format("%.1f", avgLoad));
        log("   Tải trọng Min/Max: " + minLoad + " / " + maxLoad);
        log("   Độ chênh lệch: " + (maxLoad - minLoad));

        // Tính độ lệch chuẩn
        double variance = loads.stream()
                .mapToDouble(load -> Math.pow(load - avgLoad, 2))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);

        log("   Độ lệch chuẩn: " + String.format("%.2f", stdDev));

        // Đánh giá cân bằng
        if (stdDev < avgLoad * 0.1) {
            log("   ✅ Phân bổ rất cân bằng");
        } else if (stdDev < avgLoad * 0.2) {
            log("   ✅ Phân bổ cân bằng");
        } else if (stdDev < avgLoad * 0.3) {
            log("   ⚠️ Phân bổ tạm chấp nhận");
        } else {
            log("   ❌ Phân bổ không cân bằng");
        }
    }

    /**
     * Tạo phân tích và khuyến nghị
     */
    private static void generateAnalysisAndRecommendations(AlgorithmResult greedy, AlgorithmResult pso, AlgorithmResult psoVns) {
        log("💡 PHÂN TÍCH VÀ KHUYẾN NGHỊ");
        log("=".repeat(60));

        // So sánh hiệu suất
        log("📈 So sánh hiệu suất:");

        float bestDistance = Math.min(Math.min(greedy.totalDistance, pso.totalDistance), psoVns.totalDistance);
        float worstDistance = Math.max(Math.max(greedy.totalDistance, pso.totalDistance), psoVns.totalDistance);

        if (worstDistance > 0) {
            float improvementRange = ((worstDistance - bestDistance) / worstDistance) * 100;
            log("   Khoảng cải thiện: " + String.format("%.1f%%", improvementRange));
        }

        // Khuyến nghị tối ưu tham số
        log("\n⚙️ Khuyến nghị tối ưu tham số:");

        if (pso.activeRobots < pso.totalRobots) {
            log("   🔧 PSO: Tăng penalty cho robot không hoạt động");
            log("   🔧 PSO: Tăng số vòng lặp hoặc kích thước đàn");
        }

        if (psoVns.executionTime > greedy.executionTime * 10) {
            log("   ⏱️ PSO-VNS: Cân nhắc giảm tham số VNS để tăng tốc độ");
        }

        // Phân tích theo quy mô bài toán
        int totalItems = Params.REQUIRE.stream().mapToInt(Merchandise::getQuantity).sum();
        log("\n📊 Phân tích theo quy mô:");

        if (totalItems < 50) {
            log("   📝 Bài toán nhỏ: Greedy có thể đủ hiệu quả");
        } else if (totalItems < 200) {
            log("   📝 Bài toán vừa: PSO mang lại hiệu quả tốt");
        } else {
            log("   📝 Bài toán lớn: PSO-VNS cần thiết để tối ưu");
        }

        // Khuyến nghị cấu hình robot
        log("\n🤖 Khuyến nghị cấu hình robot:");
        double avgUtilization = (greedy.activeRobots + pso.activeRobots + psoVns.activeRobots) / 3.0 / Params.ROBOTS;

        if (avgUtilization < 0.7) {
            log("   📉 Cân nhắc giảm số robot xuống " + (int)(Params.ROBOTS * 0.8));
        } else if (avgUtilization > 0.95) {
            log("   📈 Có thể tăng số robot để giảm tải");
        } else {
            log("   ✅ Số robot hiện tại phù hợp");
        }
    }

    /**
     * Tạo kết luận
     */
    private static void generateConclusion(AlgorithmResult greedy, AlgorithmResult pso, AlgorithmResult psoVns) {
        log("🎯 KẾT LUẬN TỔNG THỂ");
        log("=".repeat(60));

        // Tìm thuật toán tổng thể tốt nhất
        AlgorithmResult best = findBestOverall(greedy, pso, psoVns);

        log("🏆 Thuật toán được khuyến nghị: " + best.algorithmName);
        log("📊 Lý do:");

        if (best == greedy) {
            log("   - Tốc độ nhanh nhất (" + best.executionTime + "ms)");
            log("   - Kết quả chấp nhận được");
            log("   - Phù hợp cho ứng dụng thời gian thực");
        } else if (best == pso) {
            log("   - Quãng đường tối ưu (" + String.format("%.1f", best.totalDistance) + ")");
            log("   - Thời gian hợp lý (" + best.executionTime + "ms)");
            log("   - Cân bằng tốt giữa chất lượng và tốc độ");
        } else {
            log("   - Sử dụng tối đa robot (" + best.activeRobots + "/" + best.totalRobots + ")");
            log("   - Kết quả tối ưu từ kết hợp PSO-VNS");
            log("   - Phù hợp cho bài toán phức tạp");
        }

        // Tổng kết số liệu
        log("\n📈 Tổng kết số liệu:");
        log("   Khoảng quãng đường: " +
                String.format("%.1f", Math.min(Math.min(greedy.totalDistance, pso.totalDistance), psoVns.totalDistance)) +
                " - " +
                String.format("%.1f", Math.max(Math.max(greedy.totalDistance, pso.totalDistance), psoVns.totalDistance)));

        log("   Khoảng thời gian: " +
                Math.min(Math.min(greedy.executionTime, pso.executionTime), psoVns.executionTime) +
                " - " +
                Math.max(Math.max(greedy.executionTime, pso.executionTime), psoVns.executionTime) + " ms");

        int maxActiveRobots = Math.max(Math.max(greedy.activeRobots, pso.activeRobots), psoVns.activeRobots);
        log("   Robot hoạt động tối đa: " + maxActiveRobots + "/" + Params.ROBOTS);

        // Khuyến nghị cho tương lai
        log("\n🔮 Khuyến nghị cho tương lai:");
        log("   - Thử nghiệm với các tham số PSO khác nhau");
        log("   - Cân nhắc hybrid algorithms khác (GA-VNS, SA-VNS)");
        log("   - Tối ưu hóa cấu trúc dữ liệu để tăng tốc độ");
        log("   - Phát triển giao diện trực quan để theo dõi robot");

        // Đánh giá độ tin cậy
        boolean consistent = Math.abs(best.totalDistance - getSecondBest(greedy, pso, psoVns).totalDistance) / best.totalDistance < 0.1;
        log("   - Độ tin cậy: " + (consistent ? "Cao (kết quả ổn định)" : "Trung bình (có biến động)"));
    }

    // === UTILITY METHODS ===

    /**
     * Đếm số robot hoạt động
     */
    private static int countActiveRobots(ArrayList<Robot> robots) {
        return (int) robots.stream().filter(r -> !r.shoppingCart.isEmpty()).count();
    }

    private static int countActiveRobotsSolution(Solution solution) {
        return (int) solution.getRobotRoutes().stream().filter(route -> !route.isEmpty()).count();
    }

    /**
     * Tính quãng đường của một robot
     */
    private static float calculateRobotDistance(Robot robot, ArrayList<Merchandise> warehousing) {
        if (robot.shoppingCart.isEmpty()) return 0;

        float distance = 0;
        Position current = robot.getStartPosition();
        DistanceCalculator.setCurrentRobotPosition(current);

        for (Merchandise item : robot.shoppingCart) {
            Merchandise warehouseItem = findItemInWarehouse(item, warehousing);
            if (warehouseItem != null) {
                distance += DistanceCalculator.calculateDistance(current, warehouseItem.getPosition());
                current = DistanceCalculator.getCurrentRobotPosition();
            }
        }

        // Quay về điểm xuất phát
        distance += DistanceCalculator.calculateDistance(current, robot.getStartPosition());
        return distance;
    }

    private static Merchandise findItemInWarehouse(Merchandise item, ArrayList<Merchandise> warehousing) {
        return warehousing.stream()
                .filter(w -> w.getName().equals(item.getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * In một dòng kết quả
     */
    private static void printResultRow(AlgorithmResult result) {
        double efficiency = result.activeRobots * 1000.0 / Math.max(result.totalDistance, 1);
        double utilizationRate = result.activeRobots * 100.0 / result.totalRobots;

        log(String.format("%-15s %-15s %-15d %-15s %-15s %-10s",
                result.algorithmName,
                String.format("%.1f", result.totalDistance),
                result.executionTime,
                result.activeRobots + "/" + result.totalRobots,
                String.format("%.1f%%", utilizationRate),
                String.format("%.2f", efficiency)));
    }

    /**
     * Tìm thuật toán tốt nhất theo tiêu chí
     */
    private static AlgorithmResult findBestByDistance(AlgorithmResult... results) {
        AlgorithmResult best = results[0];
        for (AlgorithmResult result : results) {
            if (result.totalDistance < best.totalDistance) {
                best = result;
            }
        }
        return best;
    }

    private static AlgorithmResult findBestByTime(AlgorithmResult... results) {
        AlgorithmResult best = results[0];
        for (AlgorithmResult result : results) {
            if (result.executionTime < best.executionTime) {
                best = result;
            }
        }
        return best;
    }

    private static AlgorithmResult findBestByRobotUsage(AlgorithmResult... results) {
        AlgorithmResult best = results[0];
        for (AlgorithmResult result : results) {
            double rate1 = result.activeRobots * 1.0 / result.totalRobots;
            double rate2 = best.activeRobots * 1.0 / best.totalRobots;
            if (rate1 > rate2) {
                best = result;
            }
        }
        return best;
    }

    private static AlgorithmResult findBestOverall(AlgorithmResult greedy, AlgorithmResult pso, AlgorithmResult psoVns) {
        // Tính điểm tổng hợp (normalize và weight)
        float maxDistance = Math.max(Math.max(greedy.totalDistance, pso.totalDistance), psoVns.totalDistance);
        long maxTime = Math.max(Math.max(greedy.executionTime, pso.executionTime), psoVns.executionTime);

        double greedyScore = calculateOverallScore(greedy, maxDistance, maxTime);
        double psoScore = calculateOverallScore(pso, maxDistance, maxTime);
        double psoVnsScore = calculateOverallScore(psoVns, maxDistance, maxTime);

        if (greedyScore >= psoScore && greedyScore >= psoVnsScore) return greedy;
        if (psoScore >= psoVnsScore) return pso;
        return psoVns;
    }

    private static double calculateOverallScore(AlgorithmResult result, float maxDistance, long maxTime) {
        // Normalize (0-1) và weight: distance 50%, time 20%, robot usage 30%
        double distanceScore = 1.0 - (result.totalDistance / maxDistance);
        double timeScore = 1.0 - (result.executionTime / (double)maxTime);
        double robotScore = result.activeRobots / (double)result.totalRobots;

        return distanceScore * 0.5 + timeScore * 0.2 + robotScore * 0.3;
    }

    private static AlgorithmResult getSecondBest(AlgorithmResult greedy, AlgorithmResult pso, AlgorithmResult psoVns) {
        AlgorithmResult best = findBestByDistance(greedy, pso, psoVns);
        if (best == greedy) return findBestByDistance(pso, psoVns);
        if (best == pso) return findBestByDistance(greedy, psoVns);
        return findBestByDistance(greedy, pso);
    }

    /**
     * Logging methods
     */
    private static void logSection(String title) {
        log("\n\n" + "=".repeat(80));
        log("  " + title);
        log("=".repeat(80));
    }

    private static void log(String message) {
        System.out.println(message);
        if (writer != null) {
            writer.println(message);
            writer.flush();
        }
    }

    private static void logError(String message) {
        System.err.println(message);
        if (writer != null) {
            writer.println("ERROR: " + message);
            writer.flush();
        }
    }

    private static void closeOutputFile() {
        if (writer != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            writer.println("\n" + "=".repeat(80));
            writer.println("Báo cáo hoàn thành lúc: " + dateFormat.format(new Date()));
            writer.println("=".repeat(80));
            writer.close();
        }
    }

    /**
     * Class để lưu kết quả thuật toán
     */
    static class AlgorithmResult {
        String algorithmName;
        float totalDistance;
        long executionTime;
        int activeRobots;
        int totalRobots;
        ArrayList<Robot> robots;

        public AlgorithmResult(String algorithmName, float totalDistance, long executionTime,
                               int activeRobots, int totalRobots, ArrayList<Robot> robots) {
            this.algorithmName = algorithmName;
            this.totalDistance = totalDistance;
            this.executionTime = executionTime;
            this.activeRobots = activeRobots;
            this.totalRobots = totalRobots;
            this.robots = new ArrayList<>(robots);
        }
    }
}