import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Lớp Solution đại diện cho một lời giải của bài toán tìm đường đi cho robot
 * Lưu trữ các tuyến đường và tính toán chi phí quãng đường
 */
public class Solution {
    private ArrayList<Robot> robots;
    private ArrayList<ArrayList<Merchandise>> robotRoutes;
    private double fitness;
    private Random random;

    /**
     * Khởi tạo một giải pháp từ danh sách robot
     * @param robots Danh sách robot
     */
    public Solution(ArrayList<Robot> robots) {
        this.robots = new ArrayList<>(robots);
        this.robotRoutes = new ArrayList<>();
        for (int i = 0; i < robots.size(); i++) {
            this.robotRoutes.add(new ArrayList<>());
        }
        this.random = new Random();
        this.fitness = Double.MAX_VALUE;
    }

    /**
     * Constructor sao chép từ Solution khác
     * @param other Solution cần sao chép
     */
    public Solution(Solution other) {
        this.robots = new ArrayList<>(other.robots);
        this.robotRoutes = new ArrayList<>();
        for (ArrayList<Merchandise> route : other.robotRoutes) {
            ArrayList<Merchandise> newRoute = new ArrayList<>();
            for (Merchandise merchandise : route) {
                newRoute.add(merchandise);
            }
            this.robotRoutes.add(newRoute);
        }
        this.fitness = other.fitness;
        this.random = new Random();
    }

    /**
     * Khởi tạo một giải pháp ngẫu nhiên
     * @param requiredItems Danh sách các mặt hàng cần lấy
     */
    public void initializeRandomSolution(ArrayList<Merchandise> requiredItems) {
        // Tạo bản sao của danh sách mặt hàng cần lấy để làm việc
        ArrayList<Merchandise> items = new ArrayList<>();
        for (Merchandise item : requiredItems) {
            items.add(item);
        }
        Collections.shuffle(items, random);

        // Phân phối các mặt hàng ngẫu nhiên cho các robot, tôn trọng ràng buộc về sức chứa
        for (Merchandise item : items) {
            // Thử tìm một robot có thể lấy mặt hàng này
            boolean allocated = false;
            ArrayList<Integer> robotIndices = new ArrayList<>();
            for (int i = 0; i < robots.size(); i++) {
                robotIndices.add(i);
            }
            Collections.shuffle(robotIndices, random);

            for (int i : robotIndices) {
                Robot robot = robots.get(i);
                ArrayList<Merchandise> route = robotRoutes.get(i);

                // Tính toán tải trọng hiện tại
                int currentLoad = 0;
                for (Merchandise m : route) {
                    currentLoad += m.getQuantity();
                }

                // Kiểm tra xem robot này có thể lấy mặt hàng
                if (currentLoad + item.getQuantity() <= robot.capacity) {
                    route.add(item);
                    allocated = true;
                    break;
                }
            }

            // Nếu không robot nào có thể lấy, gán cho robot có tải trọng ít nhất và điều chỉnh sau
            if (!allocated) {
                int leastLoadedRobot = findLeastLoadedRobot();
                robotRoutes.get(leastLoadedRobot).add(item);
            }
        }

        // Đảm bảo ràng buộc về sức chứa được tôn trọng
        for (int i = 0; i < robots.size(); i++) {
            enforceCapacityConstraint(i);
        }
    }

    /**
     * Tìm robot có tải trọng thấp nhất
     * @return Chỉ số của robot có tải trọng thấp nhất
     */
    private int findLeastLoadedRobot() {
        int minLoad = Integer.MAX_VALUE;
        int robotIndex = 0;

        for (int i = 0; i < robots.size(); i++) {
            int load = 0;
            for (Merchandise item : robotRoutes.get(i)) {
                load += item.getQuantity();
            }

            if (load < minLoad) {
                minLoad = load;
                robotIndex = i;
            }
        }

        return robotIndex;
    }

    /**
     * Đảm bảo ràng buộc sức chứa cho một robot
     * @param robotIndex Chỉ số robot cần kiểm tra
     */
    private void enforceCapacityConstraint(int robotIndex) {
        Robot robot = robots.get(robotIndex);
        ArrayList<Merchandise> route = robotRoutes.get(robotIndex);

        int totalQuantity = 0;
        for (Merchandise item : route) {
            totalQuantity += item.getQuantity();
        }

        // Loại bỏ các mặt hàng từ cuối cho đến khi tôn trọng sức chứa
        while (totalQuantity > robot.capacity && !route.isEmpty()) {
            Merchandise removedItem = route.remove(route.size() - 1);
            totalQuantity -= removedItem.getQuantity();

            // Thử phân bổ lại cho robot khác
            reallocateItem(removedItem);
        }
    }

    /**
     * Thử phân bổ lại một mặt hàng cho robot khác
     * @param item Mặt hàng cần phân bổ lại
     */
    private void reallocateItem(Merchandise item) {
        for (int i = 0; i < robots.size(); i++) {
            Robot robot = robots.get(i);
            ArrayList<Merchandise> route = robotRoutes.get(i);

            int currentLoad = 0;
            for (Merchandise m : route) {
                currentLoad += m.getQuantity();
            }

            if (currentLoad + item.getQuantity() <= robot.capacity) {
                route.add(item);
                return;
            }
        }

        // Nếu không thể phân bổ lại, xử lý tình huống này
        // Điều này có thể có nghĩa là một số mặt hàng không được thu thập
        System.out.println("Cảnh báo: Không thể phân bổ mặt hàng " + item.getName() +
                " cho bất kỳ robot nào do hạn chế về sức chứa");
    }

    /**
     * Lấy danh sách robot
     * @return Danh sách robot
     */
    public ArrayList<Robot> getRobots() {
        return robots;
    }

    /**
     * Lấy danh sách các tuyến đường của robot
     * @return Danh sách các tuyến đường của robot
     */
    public ArrayList<ArrayList<Merchandise>> getRobotRoutes() {
        return robotRoutes;
    }

    /**
     * Lấy giá trị fitness (chi phí quãng đường)
     * @return Giá trị fitness
     */
    public double getFitness() {
        return fitness;
    }

    /**
     * Đặt giá trị fitness
     * @param fitness Giá trị fitness mới
     */
    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    /**
     * Lấy tất cả các mặt hàng cần lấy trong giải pháp
     * @return Danh sách tất cả các mặt hàng
     */
    public ArrayList<Merchandise> getAllRequiredItems() {
        ArrayList<Merchandise> allItems = new ArrayList<>();
        for (ArrayList<Merchandise> route : robotRoutes) {
            allItems.addAll(route);
        }
        return allItems;
    }

    /**
     * Tối ưu thứ tự các mặt hàng trong một tuyến đường sử dụng thuật toán gần nhất
     * @param robotIndex Chỉ số robot cần tối ưu
     * @param warehousing Kho hàng
     */
    public void optimizeRouteOrder(int robotIndex, ArrayList<Merchandise> warehousing) {
        ArrayList<Merchandise> route = robotRoutes.get(robotIndex);
        if (route.size() <= 1) return;

        ArrayList<Merchandise> optimizedRoute = new ArrayList<>();
        ArrayList<Merchandise> remaining = new ArrayList<>(route);

        // Bắt đầu từ vị trí xuất phát của robot
        Position currentPos = robots.get(robotIndex).getStartPosition();

        while (!remaining.isEmpty()) {
            // Tìm mặt hàng gần nhất từ vị trí hiện tại
            Merchandise closest = null;
            float minDistance = Float.MAX_VALUE;

            for (Merchandise item : remaining) {
                Merchandise warehouseItem = findInWarehouse(item, warehousing);
                if (warehouseItem != null) {
                    float distance = DistanceCalculator.calculateDistance(currentPos, warehouseItem.getPosition());
                    if (distance < minDistance) {
                        minDistance = distance;
                        closest = item;
                    }
                }
            }

            if (closest != null) {
                optimizedRoute.add(closest);
                remaining.remove(closest);

                // Cập nhật vị trí hiện tại
                Merchandise warehouseItem = findInWarehouse(closest, warehousing);
                if (warehouseItem != null) {
                    currentPos = warehouseItem.getPosition();
                }
            }
        }

        // Cập nhật tuyến đường
        robotRoutes.set(robotIndex, optimizedRoute);
    }

    /**
     * Tìm mặt hàng trong kho
     * @param item Mặt hàng cần tìm
     * @param warehousing Kho hàng
     * @return Mặt hàng trong kho với vị trí, hoặc null nếu không tìm thấy
     */
    private Merchandise findInWarehouse(Merchandise item, ArrayList<Merchandise> warehousing) {
        for (Merchandise w : warehousing) {
            if (w.getName().equals(item.getName())) {
                return w;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Giải pháp có chi phí quãng đường: ").append(fitness).append("\n");

        for (int i = 0; i < robots.size(); i++) {
            Robot robot = robots.get(i);
            ArrayList<Merchandise> route = robotRoutes.get(i);

            sb.append("Robot ").append(robot.nameRobot).append(": ");
            for (Merchandise item : route) {
                sb.append(item.getName()).append("(").append(item.getQuantity()).append(") ");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}