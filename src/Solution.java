import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Lớp Solution đã được tối ưu để sử dụng toàn bộ robot có sẵn
 * Đảm bảo phân bổ đều hàng hóa cho tất cả robot
 */
public class Solution {
    private ArrayList<Robot> robots;
    private ArrayList<ArrayList<Merchandise>> robotRoutes;
    private double fitness;
    private final Random random;

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
            ArrayList<Merchandise> newRoute = new ArrayList<>(route);
            this.robotRoutes.add(newRoute);
        }
        this.fitness = other.fitness;
        this.random = new Random();
    }

    /**
     * Khởi tạo một giải pháp ngẫu nhiên - ĐẢM BẢO SỬ DỤNG TẤT CẢ ROBOT
     * @param requiredItems Danh sách các mặt hàng cần lấy
     */
    public void initializeRandomSolution(ArrayList<Merchandise> requiredItems) {
        // Tạo bản sao của danh sách mặt hàng cần lấy để làm việc
        ArrayList<Merchandise> items = new ArrayList<>(requiredItems);
        Collections.shuffle(items, random);

        System.out.println("🔄 Khởi tạo giải pháp cho " + robots.size() + " robot với " + items.size() + " mặt hàng");

        // CHIẾN LƯỢC 1: Phân bổ Round-Robin để đảm bảo tất cả robot được sử dụng
        if (items.size() >= robots.size()) {
            distributeRoundRobin(items);
        } else {
            // CHIẾN LƯỢC 2: Nếu ít hàng hơn robot, ưu tiên phân bổ đều
            distributeEvenWhenFewItems(items);
        }

        // Tối ưu hóa phân bổ để tôn trọng ràng buộc sức chứa
        optimizeCapacityDistribution();

        // Đảm bảo mọi robot đều có ít nhất một mặt hàng nếu có thể
        ensureAllRobotsActive();

        // In thống kê phân bổ
        printAllocationStats();
    }

    /**
     * Phân bổ theo kiểu round-robin để đảm bảo tất cả robot được sử dụng
     */
    private void distributeRoundRobin(ArrayList<Merchandise> items) {
        System.out.println("📊 Sử dụng chiến lược Round-Robin");

        int robotIndex = 0;
        for (Merchandise item : items) {
            boolean allocated = false;
            int startRobot = robotIndex;

            // Thử phân bổ cho robot hiện tại, nếu không được thì thử robot tiếp theo
            do {
                Robot robot = robots.get(robotIndex);
                ArrayList<Merchandise> route = robotRoutes.get(robotIndex);

                if (canAddToRobot(robot, route, item)) {
                    route.add(item);
                    allocated = true;
                    System.out.println("  ✓ Robot " + robot.nameRobot + " nhận " + item.getName() + " (" + item.getQuantity() + ")");
                }

                robotIndex = (robotIndex + 1) % robots.size();
            } while (!allocated && robotIndex != startRobot);

            // Nếu không thể phân bổ với ràng buộc sức chứa, gán cho robot có tải trọng ít nhất
            if (!allocated) {
                int leastLoadedRobot = findLeastLoadedRobot();
                robotRoutes.get(leastLoadedRobot).add(item);
                System.out.println("  ⚠ Robot " + robots.get(leastLoadedRobot).nameRobot +
                        " nhận " + item.getName() + " (vượt sức chứa tạm thời)");
            }
        }
    }

    /**
     * Phân bổ đều khi có ít mặt hàng hơn robot
     */
    private void distributeEvenWhenFewItems(ArrayList<Merchandise> items) {
        System.out.println("📊 Sử dụng chiến lược phân bổ đều (ít hàng)");

        // Chia nhỏ các mặt hàng lớn nếu có thể
        ArrayList<Merchandise> expandedItems = expandLargeItems(items);

        // Phân bổ theo round-robin với danh sách mở rộng
        int robotIndex = 0;
        for (Merchandise item : expandedItems) {
            robotRoutes.get(robotIndex).add(item);
            System.out.println("  ✓ Robot " + robots.get(robotIndex).nameRobot +
                    " nhận " + item.getName() + " (" + item.getQuantity() + ")");
            robotIndex = (robotIndex + 1) % robots.size();
        }
    }

    /**
     * Mở rộng các mặt hàng lớn thành nhiều phần nhỏ để phân bổ cho nhiều robot
     */
    private ArrayList<Merchandise> expandLargeItems(ArrayList<Merchandise> items) {
        ArrayList<Merchandise> expanded = new ArrayList<>();

        for (Merchandise item : items) {
            if (item.getQuantity() > robots.size() * 2) {
                // Chia mặt hàng lớn thành nhiều phần
                int partsPerRobot = Math.max(1, item.getQuantity() / robots.size());
                int remainingQuantity = item.getQuantity();

                while (remainingQuantity > 0) {
                    int partQuantity = Math.min(partsPerRobot, remainingQuantity);
                    Merchandise part = new Merchandise(item.getName(), partQuantity);
                    expanded.add(part);
                    remainingQuantity -= partQuantity;
                }

                System.out.println("  🔄 Chia " + item.getName() + " (" + item.getQuantity() +
                        ") thành " + (item.getQuantity() / partsPerRobot + 1) + " phần");
            } else {
                expanded.add(item);
            }
        }

        return expanded;
    }

    /**
     * Kiểm tra xem có thể thêm mặt hàng vào robot không
     */
    private boolean canAddToRobot(Robot robot, ArrayList<Merchandise> route, Merchandise item) {
        int currentLoad = 0;
        for (Merchandise m : route) {
            currentLoad += m.getQuantity();
        }
        return currentLoad + item.getQuantity() <= robot.capacity;
    }

    /**
     * Tối ưu hóa phân bổ để tôn trọng ràng buộc sức chứa
     */
    private void optimizeCapacityDistribution() {
        System.out.println("🔧 Tối ưu hóa phân bổ sức chứa...");

        boolean improved = true;
        int iterations = 0;
        final int MAX_ITERATIONS = 50;

        while (improved && iterations < MAX_ITERATIONS) {
            improved = false;
            iterations++;

            // Kiểm tra và sửa các robot vượt sức chứa
            for (int i = 0; i < robots.size(); i++) {
                if (isRobotOverloaded(i)) {
                    if (redistributeFromOverloadedRobot(i)) {
                        improved = true;
                    }
                }
            }

            // Cân bằng tải giữa các robot
            if (balanceLoad()) {
                improved = true;
            }
        }

        System.out.println("  ✓ Hoàn thành sau " + iterations + " vòng lặp");
    }

    /**
     * Kiểm tra robot có bị quá tải không
     */
    private boolean isRobotOverloaded(int robotIndex) {
        Robot robot = robots.get(robotIndex);
        ArrayList<Merchandise> route = robotRoutes.get(robotIndex);

        int totalLoad = 0;
        for (Merchandise item : route) {
            totalLoad += item.getQuantity();
        }

        return totalLoad > robot.capacity;
    }

    /**
     * Phân phối lại từ robot bị quá tải
     */
    private boolean redistributeFromOverloadedRobot(int overloadedRobotIndex) {
        ArrayList<Merchandise> overloadedRoute = robotRoutes.get(overloadedRobotIndex);
        if (overloadedRoute.isEmpty()) return false;

        // Tìm robot có thể nhận thêm hàng
        for (int i = 0; i < robots.size(); i++) {
            if (i == overloadedRobotIndex) continue;

            Robot targetRobot = robots.get(i);
            ArrayList<Merchandise> targetRoute = robotRoutes.get(i);

            // Thử chuyển từng mặt hàng
            for (int j = overloadedRoute.size() - 1; j >= 0; j--) {
                Merchandise item = overloadedRoute.get(j);

                if (canAddToRobot(targetRobot, targetRoute, item)) {
                    overloadedRoute.remove(j);
                    targetRoute.add(item);

                    System.out.println("  🔄 Chuyển " + item.getName() + " từ Robot " +
                            robots.get(overloadedRobotIndex).nameRobot + " đến Robot " +
                            targetRobot.nameRobot);

                    // Kiểm tra xem robot nguồn còn quá tải không
                    if (!isRobotOverloaded(overloadedRobotIndex)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Cân bằng tải giữa các robot
     */
    private boolean balanceLoad() {
        // Tìm robot có tải trọng cao nhất và thấp nhất
        int maxLoadRobot = -1, minLoadRobot = -1;
        int maxLoad = -1, minLoad = Integer.MAX_VALUE;

        for (int i = 0; i < robots.size(); i++) {
            int load = calculateRobotLoad(i);
            if (load > maxLoad) {
                maxLoad = load;
                maxLoadRobot = i;
            }
            if (load < minLoad) {
                minLoad = load;
                minLoadRobot = i;
            }
        }

        // Nếu chênh lệch quá lớn, cân bằng
        if (maxLoad - minLoad > robots.get(minLoadRobot).capacity / 3) {
            return transferSmallestItem(maxLoadRobot, minLoadRobot);
        }

        return false;
    }

    /**
     * Chuyển mặt hàng nhỏ nhất từ robot này sang robot khác
     */
    private boolean transferSmallestItem(int fromRobot, int toRobot) {
        ArrayList<Merchandise> fromRoute = robotRoutes.get(fromRobot);
        ArrayList<Merchandise> toRoute = robotRoutes.get(toRobot);
        Robot targetRobot = robots.get(toRobot);

        // Tìm mặt hàng nhỏ nhất có thể chuyển
        Merchandise smallestItem = null;
        int smallestIndex = -1;
        int smallestQuantity = Integer.MAX_VALUE;

        for (int i = 0; i < fromRoute.size(); i++) {
            Merchandise item = fromRoute.get(i);
            if (item.getQuantity() < smallestQuantity &&
                    canAddToRobot(targetRobot, toRoute, item)) {
                smallestItem = item;
                smallestIndex = i;
                smallestQuantity = item.getQuantity();
            }
        }

        if (smallestItem != null) {
            fromRoute.remove(smallestIndex);
            toRoute.add(smallestItem);

            System.out.println("  ⚖ Cân bằng: Chuyển " + smallestItem.getName() +
                    " từ Robot " + robots.get(fromRobot).nameRobot +
                    " đến Robot " + robots.get(toRobot).nameRobot);
            return true;
        }

        return false;
    }

    /**
     * Tính tải trọng của robot
     */
    private int calculateRobotLoad(int robotIndex) {
        int load = 0;
        for (Merchandise item : robotRoutes.get(robotIndex)) {
            load += item.getQuantity();
        }
        return load;
    }

    /**
     * Đảm bảo mọi robot đều hoạt động nếu có thể
     */
    private void ensureAllRobotsActive() {
        System.out.println("🎯 Đảm bảo tất cả robot hoạt động...");

        // Tìm robot không hoạt động
        ArrayList<Integer> inactiveRobots = new ArrayList<>();
        for (int i = 0; i < robots.size(); i++) {
            if (robotRoutes.get(i).isEmpty()) {
                inactiveRobots.add(i);
            }
        }

        if (inactiveRobots.isEmpty()) {
            System.out.println("  ✓ Tất cả robot đã hoạt động");
            return;
        }

        System.out.println("  ⚠ Có " + inactiveRobots.size() + " robot chưa hoạt động");

        // Thử phân chia hàng từ robot hoạt động cho robot không hoạt động
        for (int inactiveRobot : inactiveRobots) {
            boolean activated = false;

            // Tìm robot có nhiều hàng nhất để chia sẻ
            int maxItemsRobot = -1;
            int maxItems = 0;

            for (int i = 0; i < robots.size(); i++) {
                if (robotRoutes.get(i).size() > maxItems) {
                    maxItems = robotRoutes.get(i).size();
                    maxItemsRobot = i;
                }
            }

            // Chuyển một mặt hàng từ robot có nhiều hàng nhất
            if (maxItemsRobot != -1 && maxItems > 1) {
                ArrayList<Merchandise> sourceRoute = robotRoutes.get(maxItemsRobot);
                ArrayList<Merchandise> targetRoute = robotRoutes.get(inactiveRobot);
                Robot targetRobot = robots.get(inactiveRobot);

                // Tìm mặt hàng phù hợp để chuyển
                for (int j = sourceRoute.size() - 1; j >= 0; j--) {
                    Merchandise item = sourceRoute.get(j);
                    if (item.getQuantity() <= targetRobot.capacity) {
                        sourceRoute.remove(j);
                        targetRoute.add(item);
                        activated = true;

                        System.out.println("  ✓ Kích hoạt Robot " + targetRobot.nameRobot +
                                " với " + item.getName());
                        break;
                    }
                }
            }

            if (!activated) {
                System.out.println("  ⚠ Không thể kích hoạt Robot " +
                        robots.get(inactiveRobot).nameRobot);
            }
        }
    }

    /**
     * In thống kê phân bổ
     */
    private void printAllocationStats() {
        System.out.println("\n📊 THỐNG KÊ PHÂN BỔ:");
        System.out.println("─".repeat(60));

        int totalItems = 0;
        int totalQuantity = 0;
        int activeRobots = 0;

        for (int i = 0; i < robots.size(); i++) {
            Robot robot = robots.get(i);
            ArrayList<Merchandise> route = robotRoutes.get(i);

            int robotLoad = calculateRobotLoad(i);
            int itemCount = route.size();

            totalItems += itemCount;
            totalQuantity += robotLoad;

            if (itemCount > 0) {
                activeRobots++;
            }

            String status = itemCount > 0 ? "🟢" : "🔴";
            String capacityStatus = robotLoad <= robot.capacity ? "✓" : "⚠";

            System.out.printf("%s Robot %-2s: %2d items, %3d/%3d units %s\n",
                    status, robot.nameRobot, itemCount, robotLoad,
                    robot.capacity, capacityStatus);
        }

        System.out.println("─".repeat(60));
        System.out.printf("TỔNG: %d robot hoạt động/%d, %d items, %d units\n",
                activeRobots, robots.size(), totalItems, totalQuantity);
        System.out.printf("TỶ LỆ SỬ DỤNG ROBOT: %.1f%%\n",
                (activeRobots * 100.0 / robots.size()));
    }

    /**
     * Tìm robot có tải trọng thấp nhất
     * @return Chỉ số của robot có tải trọng thấp nhất
     */
    private int findLeastLoadedRobot() {
        int minLoad = Integer.MAX_VALUE;
        int robotIndex = 0;

        for (int i = 0; i < robots.size(); i++) {
            int load = calculateRobotLoad(i);
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

        int totalQuantity = calculateRobotLoad(robotIndex);

        // Loại bỏ các mặt hàng từ cuối cho đến khi tôn trọng sức chứa
        while (totalQuantity > robot.capacity && !route.isEmpty()) {
            Merchandise removedItem = route.removeLast();
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

            if (canAddToRobot(robot, route, item)) {
                route.add(item);
                System.out.println("  ♻ Phân bổ lại " + item.getName() +
                        " cho Robot " + robot.nameRobot);
                return;
            }
        }

        // Nếu không thể phân bổ lại, báo cáo
        System.out.println("⚠ Cảnh báo: Không thể phân bổ mặt hàng " + item.getName() +
                " cho bất kỳ robot nào do hạn chế về sức chứa");
    }

    // Các phương thức getter/setter và utility methods giữ nguyên
    public ArrayList<Robot> getRobots() {
        return robots;
    }

    public ArrayList<ArrayList<Merchandise>> getRobotRoutes() {
        return robotRoutes;
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

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
        Position startPosition = robots.get(robotIndex).getStartPosition();
        DistanceCalculator.setCurrentRobotPosition(startPosition);

        Position currentPos = startPosition;

        while (!remaining.isEmpty()) {
            Merchandise closest = null;
            float minDistance = Float.MAX_VALUE;

            for (Merchandise item : remaining) {
                Merchandise warehouseItem = findInWarehouse(item, warehousing);
                if (warehouseItem != null) {
                    float distance = DistanceCalculator.calculateDistance(currentPos, warehouseItem.getPosition());
                    Position tempPosition = DistanceCalculator.getCurrentRobotPosition();
                    DistanceCalculator.setCurrentRobotPosition(currentPos);

                    if (distance < minDistance) {
                        minDistance = distance;
                        closest = item;
                        currentPos = tempPosition;
                    }
                }
            }

            if (closest != null) {
                optimizedRoute.add(closest);
                remaining.remove(closest);
                DistanceCalculator.setCurrentRobotPosition(currentPos);
            } else {
                break;
            }
        }

        robotRoutes.set(robotIndex, optimizedRoute);
        DistanceCalculator.setCurrentRobotPosition(startPosition);
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