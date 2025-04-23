import java.util.ArrayList;

/**
 * Lớp Individual đại diện cho một giải pháp của bài toán tìm đường đi
 * Sử dụng kết hợp PSO và VNS để tối ưu hóa
 */
public class Individual {
    ArrayList<Merchandise> require = Params.REQUIRE;
    ArrayList<Robot> robots;
    private PSO pso;

    /**
     * Khởi tạo một cá thể với các robot
     */
    public Individual() {
        robots = new ArrayList<>();

        // Tạo danh sách robot theo tham số
        for (int i = 0; i < Params.ROBOTS; i++) {
            robots.add(new Robot(String.valueOf(i + 1)));
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
        System.out.println("Số robot: " + robots.size() + " (sức chứa mỗi robot: " + Params.CAPACITY + ")");
        System.out.println("======================================");

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
     * @param positionCurrent Vị trí hiện tại (counter)
     * @param warehousing Kho hàng
     * @return Tổng chi phí quãng đường
     */
    public float greedy(Position positionCurrent, ArrayList<Merchandise> warehousing) {
        System.out.println("\n========= THUẬT TOÁN GREEDY =========");
        System.out.println("Yêu cầu lấy " + require.size() + " món hàng");
        System.out.println("Số robot: " + robots.size() + " (sức chứa mỗi robot: " + Params.CAPACITY + ")");
        System.out.println("======================================");

        // Xóa sạch giỏ hàng hiện tại
        for (Robot robot : robots) {
            robot.shoppingCart.clear();
        }

        // Phân bổ đơn giản các mặt hàng cho robot
        for (int i = 0; i < require.size(); i++) {
            int robotIndex = i % robots.size();
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
}