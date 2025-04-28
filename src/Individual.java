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
     * @param warehousing Kho hàng
     * @return Tổng chi phí quãng đường
     */
    public float solvePsoVns(ArrayList<Merchandise> warehousing) {
        // Sử dụng vị trí mặc định
        return solvePsoVns(DEFAULT_COUNTER_POSITION, warehousing);
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
    public float greedy(Position positionCurrent, ArrayList<Merchandise> warehousing) {
        System.out.println("\n========= THUẬT TOÁN GREEDY =========");
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

        // Xóa sạch giỏ hàng hiện tại
        for (Robot robot : robots) {
            robot.shoppingCart.clear();
        }

        // Kiểm tra nếu không có robot
        if (robots.size() == 0) {
            System.out.println("CẢNH BÁO: Không có robot nào để phân bổ mặt hàng!");
            return 0;
        }

        // Phân bổ đơn giản các mặt hàng cho robot
        for (int i = 0; i < require.size(); i++) {
            int robotIndex = i % robots.size(); // An toàn vì đã kiểm tra robots.size() > 0
            Robot robot = robots.get(robotIndex);

            // Kiểm tra sức chứa
            if (robot.canPickUp(require.get(i))) {
                robot.shoppingCart.add(require.get(i));
            } else {
                // Nếu robot hiện tại đã đầy, tìm robot khác có thể chứa
                boolean allocated = false;
                for (int j = 0; j < robots.size(); j++) {
                    if (j != robotIndex && robots.get(j).canPickUp(require.get(i))) {
                        robots.get(j).shoppingCart.add(require.get(i));
                        allocated = true;
                        break;
                    }
                }

                if (!allocated) {
                    System.out.println("Cảnh báo: Không thể phân bổ mặt hàng " + require.get(i).getName() +
                            " cho bất kỳ robot nào do hạn chế về sức chứa");
                }
            }
        }

        // Tính tổng quãng đường
        float totalDistance = 0;

        // In chi tiết đường đi cho mỗi robot
        System.out.println("\n========= KẾT QUẢ GREEDY =========");
        for (Robot robot : robots) {
            float robotDistance = 0;
            Position currentPos = positionCurrent;

            System.out.println("\nRobot " + robot.nameRobot + ":");
            System.out.println("- Bắt đầu từ Counter " + positionCurrent);

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

            totalDistance += robotDistance;
        }

        System.out.println("\nTổng quãng đường: " + totalDistance);
        return totalDistance;
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