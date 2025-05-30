import java.util.ArrayList;

/**
 * Lớp Individual đại diện cho một giải pháp của bài toán tìm đường đi
 * Sử dụng kết hợp PSO và VNS để tối ưu hóa
 */
public class Individual {
    ArrayList<Merchandise> require = Params.REQUIRE; // Sử dụng danh sách từ file input
    ArrayList<Robot> robots;
    private PSO pso;
    private static final Position DEFAULT_COUNTER_POSITION = new Position(0, 0, 0); // Vị trí mặc định [0,0,0]

    /**
     * Khởi tạo một cá thể với các robot
     */
    public Individual() {
        robots = new ArrayList<>();

        // Tạo danh sách robot theo tham số, đảm bảo ít nhất 1 robot
        int numRobots = Math.max(1, Params.ROBOTS);
        for (int i = 0; i < numRobots; i++) {
            Robot robot = new Robot(String.valueOf(i + 1));
            robot.setStartPosition(DEFAULT_COUNTER_POSITION.copy()); // Thiết lập vị trí xuất phát
            robots.add(robot);
        }

        // Khởi tạo PSO với các tham số từ lớp Params
        pso = new PSO(Params.PSO_SWARM_SIZE,
                Params.PSO_MAX_ITERATIONS,
                Params.PSO_INERTIA_WEIGHT,
                Params.PSO_COGNITIVE_COEFFICIENT,
                Params.PSO_SOCIAL_COEFFICIENT);
    }

    /**
     * Giải bài toán tìm đường đi tối ưu bằng PSO-VNS
     * @param positionCurrent Vị trí hiện tại (counter)
     * @param warehousing Kho hàng
     * @return Tổng chi phí quãng đường
     */
    public float solvePsoVns(Position positionCurrent, ArrayList<Merchandise> warehousing) {
        System.out.println("========= THUẬT TOÁN PSO-VNS =========");
        System.out.println("Yêu cầu lấy " + require.size() + " món hàng");
        for (Merchandise item : require) {
            System.out.println("- " + item.getName() + ": " + item.getQuantity() + " đơn vị");
        }
        System.out.println("Số robot: " + robots.size() + " (sức chứa mỗi robot: " + Params.CAPACITY + ")");
        System.out.println("Vị trí xuất phát: " + positionCurrent);
        System.out.println("======================================");

        // Đặt vị trí xuất phát cho tất cả robot
        for (Robot robot : robots) {
            robot.setStartPosition(positionCurrent.copy());
        }

        // Thực hiện giải thuật PSO-VNS
        System.out.println("\nĐang thực hiện tối ưu hóa...");
        Solution bestSolution = pso.solve(warehousing, require, robots);

        // Áp dụng lời giải cho robot
        for (int i = 0; i < robots.size() && i < bestSolution.getRobotRoutes().size(); i++) {
            robots.get(i).shoppingCart.clear();
            robots.get(i).shoppingCart.addAll(bestSolution.getRobotRoutes().get(i));
        }

        // In thông tin kết quả
        System.out.println("\n========= KẾT QUẢ TỐI ƯU =========");
        System.out.println("Tổng quãng đường: " + bestSolution.getFitness());

        // In chi tiết đường đi cho mỗi robot
        for (int i = 0; i < robots.size(); i++) {
            Robot robot = robots.get(i);
            System.out.println("\nRobot " + robot.nameRobot + ":");
            System.out.println("- Bắt đầu từ Counter " + positionCurrent);

            float robotDistance = 0;
            Position currentPos = positionCurrent;

            for (Merchandise item : robot.shoppingCart) {
                // Tìm vị trí trong kho
                Merchandise warehouseItem = findInWarehouse(item, warehousing);
                if (warehouseItem != null) {
                    // Tính khoảng cách từ vị trí hiện tại đến mặt hàng
                    float distance = calculateDistance(currentPos, warehouseItem.getPosition());
                    robotDistance += distance;

                    System.out.println("- Đi đến " + warehouseItem.getName() + " tại " +
                            warehouseItem.getPosition() + " (+" + distance + " đơn vị)");

                    // Cập nhật vị trí hiện tại
                    currentPos = warehouseItem.getPosition();
                }
            }

            // Quay về counter
            float returnDistance = calculateDistance(currentPos, positionCurrent);
            robotDistance += returnDistance;
            System.out.println("- Quay về Counter " + positionCurrent + " (+" + returnDistance + " đơn vị)");
            System.out.println("=> Tổng quãng đường của Robot " + robot.nameRobot + ": " + robotDistance);
        }

        return (float) bestSolution.getFitness();
    }

    /**
     * Phương thức greedy đơn giản để so sánh
     * @param warehousing Kho hàng
     * @return Tổng chi phí quãng đường
     */
    public float greedy(ArrayList<Merchandise> warehousing) {
        // Sử dụng vị trí mặc định
        return greedy(DEFAULT_COUNTER_POSITION, warehousing);
    }

    /**
     * Phương thức greedy đơn giản để so sánh
     * @param positionCurrent Vị trí hiện tại (counter)
     * @param warehousing Kho hàng
     * @return Tổng chi phí quãng đường
     */
    /**
     * Phương thức greedy được cải thiện để sử dụng TẤT CẢ robot có sẵn
     * Thêm vào class Individual.java
     */
    public float greedy(Position positionCurrent, ArrayList<Merchandise> warehousing) {
        System.out.println("\n========= THUẬT TOÁN GREEDY (CẢI THIỆN) =========");
        System.out.println("Yêu cầu lấy " + require.size() + " món hàng");
        for (Merchandise item : require) {
            System.out.println("- " + item.getName() + ": " + item.getQuantity() + " đơn vị");
        }
        System.out.println("Số robot: " + robots.size() + " (sức chứa mỗi robot: " + Params.CAPACITY + ")");
        System.out.println("Vị trí xuất phát: " + positionCurrent);
        System.out.println("======================================");

        // Đặt vị trí xuất phát cho tất cả robot
        for (Robot robot : robots) {
            robot.setStartPosition(positionCurrent.copy());
            robot.shoppingCart.clear();
        }

        // Kiểm tra nếu không có robot
        if (robots.size() == 0) {
            System.out.println("CẢNH BÁO: Không có robot nào để phân bổ mặt hàng!");
            return 0;
        }

        // ===== CHIẾN LƯỢC GREEDY CẢI THIỆN =====
        // 1. Tạo danh sách tất cả items cần lấy với vị trí từ kho
        ArrayList<MerchandiseWithDistance> itemsWithDistance = new ArrayList<>();

        for (Merchandise reqItem : require) {
            Merchandise warehouseItem = findInWarehouse(reqItem, warehousing);
            if (warehouseItem != null) {
                // Tính khoảng cách từ counter đến item này
                DistanceCalculator.setCurrentRobotPosition(positionCurrent);
                float distance = calculateDistance(positionCurrent, warehouseItem.getPosition());

                itemsWithDistance.add(new MerchandiseWithDistance(
                        reqItem, warehouseItem.getPosition(), distance));
            }
        }

        // 2. Sắp xếp items theo tiêu chí greedy (gần nhất trước)
        itemsWithDistance.sort((a, b) -> Float.compare(a.distance, b.distance));

        System.out.println("\n🎯 CHIẾN LƯỢC GREEDY:");
        System.out.println("- Ưu tiên mặt hàng gần nhất");
        System.out.println("- Phân bổ đều cho tất cả robot");
        System.out.println("- Cân bằng tải trọng");

        // 3. Phân bổ thông minh cho robot
        distributeItemsGreedy(itemsWithDistance);

        // 4. Tối ưu hóa thứ tự đường đi cho từng robot
        optimizeRobotRoutes(warehousing, positionCurrent);

        // 5. Tính tổng quãng đường
        float totalDistance = calculateTotalDistance(warehousing, positionCurrent);

        // 6. In kết quả chi tiết
        printGreedyResults(positionCurrent, warehousing, totalDistance);

        return totalDistance;
    }

    /**
     * Phân bổ items cho robot theo chiến lược greedy cải thiện
     */
    private void distributeItemsGreedy(ArrayList<MerchandiseWithDistance> itemsWithDistance) {
        System.out.println("\n📊 PHÂN BỔ MẶT HÀNG:");

        for (MerchandiseWithDistance itemWithDist : itemsWithDistance) {
            Merchandise item = itemWithDist.merchandise;

            // Tìm robot tốt nhất để gán item này
            Robot bestRobot = findBestRobotForItem(item, itemWithDist.position);

            if (bestRobot != null) {
                bestRobot.shoppingCart.add(item);
                System.out.println("✓ " + item.getName() + " (" + item.getQuantity() +
                        ") → Robot " + bestRobot.nameRobot +
                        " (tải: " + bestRobot.getCurrentLoad() + "/" + bestRobot.capacity + ")");
            } else {
                // Nếu không tìm được robot phù hợp, gán cho robot có tải trọng ít nhất
                Robot leastLoadedRobot = findLeastLoadedRobot();
                leastLoadedRobot.shoppingCart.add(item);
                System.out.println("⚠️ " + item.getName() + " (" + item.getQuantity() +
                        ") → Robot " + leastLoadedRobot.nameRobot +
                        " (vượt sức chứa tạm thời)");
            }
        }
    }

    /**
     * Tìm robot tốt nhất để gán một item
     * Tiêu chí: robot có thể chứa + gần với item + cân bằng tải
     */
    private Robot findBestRobotForItem(Merchandise item, Position itemPosition) {
        Robot bestRobot = null;
        double bestScore = Double.MAX_VALUE;

        for (Robot robot : robots) {
            // Kiểm tra xem robot có thể chứa item không
            if (!robot.canPickUp(item)) {
                continue;
            }

            // Tính điểm cho robot này
            double score = calculateRobotScore(robot, item, itemPosition);

            if (score < bestScore) {
                bestScore = score;
                bestRobot = robot;
            }
        }

        return bestRobot;
    }

    /**
     * Tính điểm cho robot (càng thấp càng tốt)
     * Kết hợp: khoảng cách + cân bằng tải + số items
     */
    private double calculateRobotScore(Robot robot, Merchandise item, Position itemPosition) {
        // 1. Khoảng cách từ vị trí hiện tại của robot đến item
        Position robotCurrentPos = robot.shoppingCart.isEmpty() ?
                robot.getStartPosition() :
                getLastItemPosition(robot);

        DistanceCalculator.setCurrentRobotPosition(robotCurrentPos);
        float distance = calculateDistance(robotCurrentPos, itemPosition);

        // 2. Tải trọng hiện tại (để cân bằng)
        int currentLoad = robot.getCurrentLoad();

        // 3. Số items hiện tại (để phân bổ đều)
        int itemCount = robot.shoppingCart.size();

        // Công thức tính điểm (có thể điều chỉnh trọng số)
        double distanceWeight = 1.0;      // Ưu tiên khoảng cách
        double loadWeight = 0.3;          // Cân bằng tải
        double itemCountWeight = 0.2;     // Phân bổ đều số items

        return distance * distanceWeight +
                currentLoad * loadWeight +
                itemCount * itemCountWeight;
    }

    /**
     * Lấy vị trí của item cuối cùng trong giỏ hàng robot
     */
    private Position getLastItemPosition(Robot robot) {
        if (robot.shoppingCart.isEmpty()) {
            return robot.getStartPosition();
        }

        Merchandise lastItem = robot.shoppingCart.get(robot.shoppingCart.size() - 1);
        // Tìm vị trí trong kho
        for (Merchandise warehouseItem : Params.WAREHOUSE) {
            if (warehouseItem.getName().equals(lastItem.getName())) {
                return warehouseItem.getPosition();
            }
        }

        return robot.getStartPosition();
    }

    /**
     * Tìm robot có tải trọng thấp nhất
     */
    private Robot findLeastLoadedRobot() {
        Robot leastLoaded = robots.get(0);
        int minLoad = leastLoaded.getCurrentLoad();

        for (Robot robot : robots) {
            int load = robot.getCurrentLoad();
            if (load < minLoad) {
                minLoad = load;
                leastLoaded = robot;
            }
        }

        return leastLoaded;
    }

    /**
     * Tối ưu hóa thứ tự đường đi cho từng robot (Nearest Neighbor)
     */
    private void optimizeRobotRoutes(ArrayList<Merchandise> warehousing, Position startPosition) {
        System.out.println("\n🔧 TỐI ƯU HÓA ĐƯỜNG ĐI:");

        for (Robot robot : robots) {
            if (robot.shoppingCart.isEmpty()) {
                continue;
            }

            System.out.println("Robot " + robot.nameRobot + " (" + robot.shoppingCart.size() + " items):");

            // Áp dụng Nearest Neighbor Algorithm
            ArrayList<Merchandise> optimizedRoute = nearestNeighborTSP(robot.shoppingCart,
                    startPosition, warehousing);
            robot.shoppingCart.clear();
            robot.shoppingCart.addAll(optimizedRoute);

            // In thứ tự đã tối ưu
            for (int i = 0; i < optimizedRoute.size(); i++) {
                System.out.println("  " + (i+1) + ". " + optimizedRoute.get(i).getName());
            }
        }
    }

    /**
     * Thuật toán Nearest Neighbor cho TSP
     */
    private ArrayList<Merchandise> nearestNeighborTSP(ArrayList<Merchandise> items,
                                                      Position startPosition,
                                                      ArrayList<Merchandise> warehousing) {
        if (items.size() <= 1) {
            return new ArrayList<>(items);
        }

        ArrayList<Merchandise> remaining = new ArrayList<>(items);
        ArrayList<Merchandise> route = new ArrayList<>();
        Position currentPos = startPosition;

        DistanceCalculator.setCurrentRobotPosition(currentPos);

        while (!remaining.isEmpty()) {
            Merchandise nearest = null;
            float minDistance = Float.MAX_VALUE;

            // Tìm item gần nhất với vị trí hiện tại
            for (Merchandise item : remaining) {
                Merchandise warehouseItem = findInWarehouse(item, warehousing);
                if (warehouseItem != null) {
                    float distance = calculateDistance(currentPos, warehouseItem.getPosition());
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearest = item;
                    }
                }
            }

            if (nearest != null) {
                route.add(nearest);
                remaining.remove(nearest);

                // Cập nhật vị trí hiện tại
                Merchandise warehouseItem = findInWarehouse(nearest, warehousing);
                if (warehouseItem != null) {
                    DistanceCalculator.calculateDistance(currentPos, warehouseItem.getPosition());
                    currentPos = DistanceCalculator.getCurrentRobotPosition();
                }
            } else {
                break;
            }
        }

        return route;
    }

    /**
     * Tính tổng quãng đường của tất cả robot
     */
    private float calculateTotalDistance(ArrayList<Merchandise> warehousing, Position startPosition) {
        float totalDistance = 0;

        for (Robot robot : robots) {
            if (robot.shoppingCart.isEmpty()) {
                continue;
            }

            float robotDistance = 0;
            Position currentPos = startPosition;
            DistanceCalculator.setCurrentRobotPosition(currentPos);

            // Đi qua tất cả items trong giỏ hàng
            for (Merchandise item : robot.shoppingCart) {
                Merchandise warehouseItem = findInWarehouse(item, warehousing);
                if (warehouseItem != null) {
                    float distance = calculateDistance(currentPos, warehouseItem.getPosition());
                    robotDistance += distance;
                    currentPos = DistanceCalculator.getCurrentRobotPosition();
                }
            }

            // Quay về điểm xuất phát
            float returnDistance = calculateDistance(currentPos, startPosition);
            robotDistance += returnDistance;

            totalDistance += robotDistance;
        }

        return totalDistance;
    }

    /**
     * In kết quả chi tiết
     */
    private void printGreedyResults(Position startPosition, ArrayList<Merchandise> warehousing,
                                    float totalDistance) {
        System.out.println("\n========= KẾT QUẢ GREEDY CẢI THIỆN =========");

        int activeRobots = 0;
        int totalItems = 0;
        int totalLoad = 0;

        for (Robot robot : robots) {
            if (!robot.shoppingCart.isEmpty()) {
                activeRobots++;
            }
            totalItems += robot.shoppingCart.size();
            totalLoad += robot.getCurrentLoad();
        }

        System.out.println("📊 Tổng quan:");
        System.out.println("- Tổng quãng đường: " + totalDistance);
        System.out.println("- Robot hoạt động: " + activeRobots + "/" + robots.size());
        System.out.println("- Tổng items: " + totalItems);
        System.out.println("- Tổng tải trọng: " + totalLoad + "/" + (robots.size() * Params.CAPACITY));

        // Chi tiết từng robot
        System.out.println("\n🤖 Chi tiết robot:");
        for (Robot robot : robots) {
            float robotDistance = 0;
            Position currentPos = startPosition;
            DistanceCalculator.setCurrentRobotPosition(currentPos);

            System.out.println("\nRobot " + robot.nameRobot + ":");
            System.out.println("- Tải trọng: " + robot.getCurrentLoad() + "/" + robot.capacity);
            System.out.println("- Số items: " + robot.shoppingCart.size());

            if (!robot.shoppingCart.isEmpty()) {
                System.out.println("- Lộ trình:");
                System.out.println("  0. Bắt đầu từ Counter " + startPosition);

                for (int i = 0; i < robot.shoppingCart.size(); i++) {
                    Merchandise item = robot.shoppingCart.get(i);
                    Merchandise warehouseItem = findInWarehouse(item, warehousing);
                    if (warehouseItem != null) {
                        float distance = calculateDistance(currentPos, warehouseItem.getPosition());
                        robotDistance += distance;
                        currentPos = DistanceCalculator.getCurrentRobotPosition();

                        System.out.println("  " + (i+1) + ". " + item.getName() +
                                " tại " + warehouseItem.getPosition() +
                                " (+" + distance + " đơn vị)");
                    }
                }

                float returnDistance = calculateDistance(currentPos, startPosition);
                robotDistance += returnDistance;
                System.out.println("  " + (robot.shoppingCart.size() + 1) +
                        ". Quay về Counter (+" + returnDistance + " đơn vị)");
                System.out.println("=> Tổng quãng đường Robot " + robot.nameRobot + ": " + robotDistance);
            } else {
                System.out.println("- Không có nhiệm vụ");
            }
        }

        // Phân tích hiệu quả
        System.out.println("\n📈 Phân tích:");
        if (activeRobots == robots.size()) {
            System.out.println("✅ Sử dụng tất cả robot");
        } else {
            System.out.println("⚠️ Chưa sử dụng hết robot (" + (robots.size() - activeRobots) + " robot rảnh)");
        }

        double avgLoad = activeRobots > 0 ? (double)totalLoad / activeRobots : 0;
        System.out.println("📊 Tải trọng TB/robot hoạt động: " + String.format("%.1f", avgLoad));

        double utilization = (double)totalLoad / (robots.size() * Params.CAPACITY) * 100;
        System.out.println("📊 Tỷ lệ sử dụng sức chứa: " + String.format("%.1f%%", utilization));
    }

    /**
     * Inner class để lưu merchandise với khoảng cách
     */
    private static class MerchandiseWithDistance {
        Merchandise merchandise;
        Position position;
        float distance;

        public MerchandiseWithDistance(Merchandise merchandise, Position position, float distance) {
            this.merchandise = merchandise;
            this.position = position;
            this.distance = distance;
        }
    }
    /**
     * Tìm mặt hàng trong kho
     * @param item Mặt hàng cần tìm
     * @param warehousing Kho hàng
     * @return Mặt hàng trong kho kèm vị trí, hoặc null nếu không tìm thấy
     */
    private Merchandise findInWarehouse(Merchandise item, ArrayList<Merchandise> warehousing) {
        for (Merchandise w : warehousing) {
            if (w.getName().equals(item.getName())) {
                return w;
            }
        }
        return null;
    }

    /**
     * Tính khoảng cách giữa hai vị trí
     * @param pos1 Vị trí thứ nhất
     * @param pos2 Vị trí thứ hai
     * @return Khoảng cách
     */
    private float calculateDistance(Position pos1, Position pos2) {
        try {
            // Sử dụng DistanceCalculator để tính khoảng cách
            return DistanceCalculator.calculateDistance(pos1, pos2);
        } catch (Exception e) {
            System.out.println("Lỗi khi tính khoảng cách: " + e.getMessage());
            return 0; // Trả về khoảng cách mặc định an toàn
        }
    }
}