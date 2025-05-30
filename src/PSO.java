import java.util.ArrayList;
import java.util.Random;

/**
 * Lớp PSO đã được tối ưu để đảm bảo sử dụng toàn bộ robot có sẵn
 * Bổ sung các chiến lược phân bổ thông minh và cân bằng tải
 */
public class PSO {
    private final ArrayList<Particle> swarm;
    private Particle globalBest;
    private final int swarmSize;
    private final int maxIterations;
    private final double w; // trọng số quán tính
    private final double c1; // hệ số nhận thức
    private final double c2; // hệ số xã hội
    private  Random random;
    private VNS vns;

    /**
     * Khởi tạo PSO với các tham số
     */
    public PSO(int swarmSize, int maxIterations, double w, double c1, double c2) {
        this.swarmSize = swarmSize;
        this.maxIterations = maxIterations;
        this.w = w;
        this.c1 = c1;
        this.c2 = c2;
        this.random = new Random();
        this.swarm = new ArrayList<>();
        this.vns = new VNS();
    }

    /**
     * Giải bài toán tìm đường đi tối ưu - ĐẢM BẢO TẤT CẢ ROBOT HOẠT ĐỘNG
     */
    public Solution solve(ArrayList<Merchandise> warehousing, ArrayList<Merchandise> require, ArrayList<Robot> robots) {
        System.out.println("🚀 Bắt đầu PSO với mục tiêu sử dụng TẤT CẢ " + robots.size() + " robot");

        // Khởi tạo đàn với chiến lược đảm bảo tất cả robot hoạt động
        initializeSwarmWithAllRobots(warehousing, require, robots);

        System.out.println("📊 PSO khởi tạo với " + swarmSize + " hạt và " + maxIterations + " vòng lặp");

        // Vòng lặp chính của PSO
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            for (Particle particle : swarm) {
                updateVelocityAndPosition(particle, warehousing);

                // Đặt lại vị trí hiện tại của robot trước khi đánh giá
                for (Robot robot : particle.getSolution().getRobots()) {
                    robot.setCurrentPosition(robot.getStartPosition().copy());
                }
                DistanceCalculator.setCurrentRobotPosition(particle.getSolution().getRobots().get(0).getStartPosition());

                // Đánh giá độ thích nghi với penalty cho robot không hoạt động
                double fitness = evaluateFitnessWithRobotPenalty(particle.getSolution(), warehousing);
                particle.getSolution().setFitness(fitness);

                // Cập nhật vị trí tốt nhất của hạt
                boolean improved = false;
                if (fitness < particle.getBestFitness()) {
                    particle.setBestSolution(new Solution(particle.getSolution()));
                    particle.setBestFitness(fitness);
                    improved = true;

                    // Cập nhật vị trí tốt nhất toàn cục nếu cần
                    if (fitness < globalBest.getBestFitness()) {
                        globalBest.setBestSolution(new Solution(particle.getSolution()));
                        globalBest.setBestFitness(fitness);

                        // In thông tin về số robot hoạt động
                        int activeRobots = countActiveRobots(particle.getSolution());
                        System.out.println("  🎯 Giải pháp mới tốt nhất: " + fitness +
                                " (Robot hoạt động: " + activeRobots + "/" + robots.size() + ")");
                    }
                }
            }

            // Áp dụng VNS để cải thiện giải pháp tốt nhất toàn cục
            if (iteration % 5 == 0) {
                double oldFitness = globalBest.getBestFitness();
                Solution improvedSolution = vns.improve(globalBest.getBestSolution(), warehousing);

                if (improvedSolution.getFitness() < oldFitness) {
                    globalBest.setBestSolution(improvedSolution);
                    globalBest.setBestFitness(improvedSolution.getFitness());

                    int activeRobots = countActiveRobots(improvedSolution);
                    System.out.println("  🔧 VNS cải thiện: " + improvedSolution.getFitness() +
                            " (Robot hoạt động: " + activeRobots + "/" + robots.size() + ")");
                }
            }

            // Đảm bảo đa dạng trong đàn - khuyến khích sử dụng tất cả robot
            if (iteration % 10 == 0) {
                diversifySwarmForAllRobots();
                int activeRobots = countActiveRobots(globalBest.getBestSolution());
                System.out.println("  📈 Vòng lặp " + iteration + ": Tốt nhất = " + globalBest.getBestFitness() +
                        " (Robot: " + activeRobots + "/" + robots.size() + ")");
            }
        }

        // Tối ưu hóa cuối cùng với focus vào việc sử dụng tất cả robot
        optimizeForAllRobots(globalBest.getBestSolution(), warehousing);

        int finalActiveRobots = countActiveRobots(globalBest.getBestSolution());
        System.out.println("🏁 PSO hoàn thành. Robot hoạt động: " + finalActiveRobots + "/" + robots.size() +
                ", Quãng đường: " + globalBest.getBestFitness());

        return globalBest.getBestSolution();
    }

    /**
     * Khởi tạo đàn với chiến lược đảm bảo tất cả robot hoạt động
     */
    private void initializeSwarmWithAllRobots(ArrayList<Merchandise> warehousing, ArrayList<Merchandise> require, ArrayList<Robot> robots) {
        ArrayList<Robot> initializedRobots = new ArrayList<>();
        for (Robot robot : robots) {
            Robot newRobot = new Robot(robot.nameRobot, robot.getStartPosition());
            newRobot.capacity = robot.capacity;
            initializedRobots.add(newRobot);
        }

        System.out.println("🎲 Tạo " + swarmSize + " hạt với các chiến lược khác nhau...");

        for (int i = 0; i < swarmSize; i++) {
            Particle particle = new Particle();

            // Tạo bản sao robot cho mỗi hạt
            ArrayList<Robot> particleRobots = new ArrayList<>();
            for (Robot robot : initializedRobots) {
                Robot robotCopy = new Robot(robot.nameRobot, robot.getStartPosition());
                robotCopy.capacity = robot.capacity;
                particleRobots.add(robotCopy);
            }

            // Khởi tạo với chiến lược khác nhau để đảm bảo đa dạng
            Solution solution = new Solution(particleRobots);

            if (i < swarmSize / 3) {
                // 1/3 đầu: Chiến lược round-robin
                solution.initializeRandomSolution(require);
            } else if (i < 2 * swarmSize / 3) {
                // 1/3 giữa: Chiến lược cân bằng tải
                initializeBalancedSolution(solution, require);
            } else {
                // 1/3 cuối: Chiến lược ngẫu nhiên có điều chỉnh
                initializeAdjustedRandomSolution(solution, require);
            }

            // Đảm bảo tất cả robot hoạt động
            ensureAllRobotsActiveInSolution(solution);

            // Đặt vị trí robot và tính fitness
            setRobotPositions(solution);
            optimizeRouteOrders(solution, warehousing);

            double fitness = evaluateFitnessWithRobotPenalty(solution, warehousing);
            solution.setFitness(fitness);

            particle.setSolution(solution);
            particle.setBestSolution(new Solution(solution));
            particle.setBestFitness(fitness);

            swarm.add(particle);

            // Khởi tạo hoặc cập nhật best toàn cục
            if (i == 0 || fitness < globalBest.getBestFitness()) {
                if (globalBest == null) globalBest = new Particle();
                globalBest.setBestSolution(new Solution(solution));
                globalBest.setBestFitness(fitness);
            }
        }

        int bestActiveRobots = countActiveRobots(globalBest.getBestSolution());
        System.out.println("✅ Khởi tạo hoàn thành. Giải pháp tốt nhất có " + bestActiveRobots +
                "/" + robots.size() + " robot hoạt động");
    }

    /**
     * Khởi tạo giải pháp cân bằng tải
     */
    private void initializeBalancedSolution(Solution solution, ArrayList<Merchandise> require) {
        ArrayList<Robot> robots = solution.getRobots();
        ArrayList<ArrayList<Merchandise>> routes = solution.getRobotRoutes();

        // Sắp xếp hàng theo thứ tự giảm dần của quantity
        ArrayList<Merchandise> sortedItems = new ArrayList<>(require);
        sortedItems.sort((a, b) -> Integer.compare(b.getQuantity(), a.getQuantity()));

        // Phân bổ theo chiến lược "longest processing time first"
        for (Merchandise item : sortedItems) {
            int leastLoadedRobot = findLeastLoadedRobot(solution);
            routes.get(leastLoadedRobot).add(item);
        }
    }

    /**
     * Khởi tạo giải pháp ngẫu nhiên có điều chỉnh
     */
    private void initializeAdjustedRandomSolution(Solution solution, ArrayList<Merchandise> require) {
        ArrayList<Merchandise> items = new ArrayList<>(require);
        java.util.Collections.shuffle(items, random);

        // Phân bổ ngẫu nhiên ban đầu
        int robotIndex = 0;
        for (Merchandise item : items) {
            solution.getRobotRoutes().get(robotIndex).add(item);
            robotIndex = (robotIndex + 1) % solution.getRobots().size();
        }

        // Điều chỉnh để cân bằng
        balanceLoadAcrossRobots(solution);
    }

    /**
     * Đảm bảo tất cả robot hoạt động trong solution
     */
    private void ensureAllRobotsActiveInSolution(Solution solution) {
        ArrayList<ArrayList<Merchandise>> routes = solution.getRobotRoutes();
        ArrayList<Robot> robots = solution.getRobots();

        // Tìm robot không hoạt động
        for (int i = 0; i < robots.size(); i++) {
            if (routes.get(i).isEmpty()) {
                // Tìm robot có nhiều hàng nhất để chia sẻ
                int maxItemsRobot = -1;
                int maxItems = 0;

                for (int j = 0; j < robots.size(); j++) {
                    if (j != i && routes.get(j).size() > maxItems) {
                        maxItems = routes.get(j).size();
                        maxItemsRobot = j;
                    }
                }

                // Chuyển một item từ robot có nhiều hàng nhất
                if (maxItemsRobot != -1 && maxItems > 1) {
                    ArrayList<Merchandise> sourceRoute = routes.get(maxItemsRobot);
                    ArrayList<Merchandise> targetRoute = routes.get(i);

                    // Chuyển item cuối cùng
                    Merchandise item = sourceRoute.removeLast();
                    targetRoute.add(item);
                }
            }
        }
    }

    /**
     * Cân bằng tải giữa các robot
     */
    private void balanceLoadAcrossRobots(Solution solution) {
        boolean improved = true;
        int iterations = 0;

        while (improved && iterations < 20) {
            improved = false;
            iterations++;

            // Tìm robot quá tải và robot nhàn rỗi
            for (int i = 0; i < solution.getRobots().size(); i++) {
                for (int j = 0; j < solution.getRobots().size(); j++) {
                    if (i != j && tryTransferItem(solution, i, j)) {
                        improved = true;
                    }
                }
            }
        }
    }

    /**
     * Thử chuyển item từ robot i sang robot j
     */
    private boolean tryTransferItem(Solution solution, int fromRobot, int toRobot) {
        ArrayList<Merchandise> fromRoute = solution.getRobotRoutes().get(fromRobot);
        ArrayList<Merchandise> toRoute = solution.getRobotRoutes().get(toRobot);
        Robot toRobotObj = solution.getRobots().get(toRobot);

        if (fromRoute.isEmpty()) return false;

        int fromLoad = calculateRobotLoad(solution, fromRobot);
        int toLoad = calculateRobotLoad(solution, toRobot);

        // Chỉ chuyển nếu có thể cân bằng tải và không vi phạm capacity
        if (fromLoad > toLoad + 5) { // Threshold để tránh dao động
            for (int i = fromRoute.size() - 1; i >= 0; i--) {
                Merchandise item = fromRoute.get(i);
                if (toLoad + item.getQuantity() <= toRobotObj.capacity) {
                    fromRoute.remove(i);
                    toRoute.add(item);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Tính tải trọng của robot
     */
    private int calculateRobotLoad(Solution solution, int robotIndex) {
        int load = 0;
        for (Merchandise item : solution.getRobotRoutes().get(robotIndex)) {
            load += item.getQuantity();
        }
        return load;
    }

    /**
     * Tìm robot có tải trọng thấp nhất
     */
    private int findLeastLoadedRobot(Solution solution) {
        int minLoad = Integer.MAX_VALUE;
        int robotIndex = 0;

        for (int i = 0; i < solution.getRobots().size(); i++) {
            int load = calculateRobotLoad(solution, i);
            if (load < minLoad) {
                minLoad = load;
                robotIndex = i;
            }
        }

        return robotIndex;
    }

    /**
     * Đếm số robot hoạt động
     */
    private int countActiveRobots(Solution solution) {
        int count = 0;
        for (ArrayList<Merchandise> route : solution.getRobotRoutes()) {
            if (!route.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Đánh giá fitness với penalty cho robot không hoạt động
     */
    private double evaluateFitnessWithRobotPenalty(Solution solution, ArrayList<Merchandise> warehousing) {
        double baseFitness = evaluateFitness(solution, warehousing);

        // Tính penalty cho robot không hoạt động
        int totalRobots = solution.getRobots().size();
        int activeRobots = countActiveRobots(solution);
        int inactiveRobots = totalRobots - activeRobots;

        // Penalty tỷ lệ với số robot không hoạt động
        double inactiveRobotPenalty = inactiveRobots * 1000.0; // Penalty lớn để khuyến khích sử dụng tất cả robot

        // Penalty cho sự không cân bằng tải
        double imbalancePenalty = calculateLoadImbalancePenalty(solution);

        return baseFitness + inactiveRobotPenalty + imbalancePenalty;
    }

    /**
     * Tính penalty cho sự không cân bằng tải
     */
    private double calculateLoadImbalancePenalty(Solution solution) {
        if (solution.getRobots().isEmpty()) return 0;

        ArrayList<Integer> loads = new ArrayList<>();
        for (int i = 0; i < solution.getRobots().size(); i++) {
            loads.add(calculateRobotLoad(solution, i));
        }

        // Tính độ lệch chuẩn của tải trọng
        double mean = loads.stream().mapToInt(Integer::intValue).average().orElse(0);
        double variance = loads.stream().mapToDouble(load -> Math.pow(load - mean, 2)).average().orElse(0);
        double stdDev = Math.sqrt(variance);

        return stdDev * 10.0; // Penalty cho độ lệch chuẩn cao
    }

    /**
     * Đa dạng hóa đàn để khuyến khích sử dụng tất cả robot
     */
    private void diversifySwarmForAllRobots() {
        // Tìm các hạt có ít robot hoạt động
        for (int i = 0; i < swarm.size() / 4; i++) { // Đa dạng hóa 25% đàn
            Particle particle = swarm.get(i);
            int activeRobots = countActiveRobots(particle.getSolution());

            if (activeRobots < particle.getSolution().getRobots().size()) {
                // Tái phân bổ để kích hoạt thêm robot
                redistributeToActivateAllRobots(particle.getSolution());

                // Tính lại fitness
                double newFitness = evaluateFitnessWithRobotPenalty(particle.getSolution(), null);
                particle.getSolution().setFitness(newFitness);
            }
        }
    }

    /**
     * Phân bổ lại để kích hoạt tất cả robot
     */
    private void redistributeToActivateAllRobots(Solution solution) {
        ArrayList<ArrayList<Merchandise>> routes = solution.getRobotRoutes();

        // Thu thập tất cả items
        ArrayList<Merchandise> allItems = new ArrayList<>();
        for (ArrayList<Merchandise> route : routes) {
            allItems.addAll(route);
            route.clear();
        }

        // Phân bổ lại theo round-robin
        if (!allItems.isEmpty()) {
            int robotIndex = 0;
            for (Merchandise item : allItems) {
                routes.get(robotIndex).add(item);
                robotIndex = (robotIndex + 1) % routes.size();
            }
        }
    }

    /**
     * Tối ưu hóa cuối cùng cho tất cả robot
     */
    private void optimizeForAllRobots(Solution solution, ArrayList<Merchandise> warehousing) {
        System.out.println("🎯 Tối ưu hóa cuối cùng cho tất cả robot...");

        // Đảm bảo tất cả robot hoạt động
        ensureAllRobotsActiveInSolution(solution);

        // Cân bằng tải
        balanceLoadAcrossRobots(solution);

        // Tối ưu thứ tự đường đi
        optimizeRouteOrders(solution, warehousing);

        // Tính lại fitness
        double newFitness = evaluateFitness(solution, warehousing);
        solution.setFitness(newFitness);

        int activeRobots = countActiveRobots(solution);
        System.out.println("  ✅ Robot hoạt động cuối cùng: " + activeRobots + "/" + solution.getRobots().size());
    }

    /**
     * Đặt vị trí cho tất cả robot
     */
    private void setRobotPositions(Solution solution) {
        for (Robot robot : solution.getRobots()) {
            robot.setCurrentPosition(robot.getStartPosition().copy());
        }
        if (!solution.getRobots().isEmpty()) {
            DistanceCalculator.setCurrentRobotPosition(solution.getRobots().getFirst().getStartPosition());
        }
    }

    /**
     * Tối ưu hóa thứ tự các mặt hàng trong tất cả các tuyến đường robot
     */
    private void optimizeRouteOrders(Solution solution, ArrayList<Merchandise> warehousing) {
        for (int i = 0; i < solution.getRobotRoutes().size(); i++) {
            if (!solution.getRobotRoutes().get(i).isEmpty()) {
                Robot robot = solution.getRobots().get(i);
                robot.setCurrentPosition(robot.getStartPosition().copy());
                DistanceCalculator.setCurrentRobotPosition(robot.getStartPosition());
                solution.optimizeRouteOrder(i, warehousing);
            }
        }

        double newFitness = evaluateFitness(solution, warehousing);
        solution.setFitness(newFitness);
    }

    /**
     * Cập nhật vận tốc và vị trí cho một hạt - Tối ưu cho tất cả robot
     */
    private void updateVelocityAndPosition(Particle particle, ArrayList<Merchandise> warehousing) {
        Solution currentSolution = particle.getSolution();
        Solution personalBest = particle.getBestSolution();
        Solution globalBestSolution = globalBest.getBestSolution();

        // Tạo danh sách tạm thời các tuyến đường mới
        ArrayList<ArrayList<Merchandise>> newRoutes = new ArrayList<>();
        for (int i = 0; i < currentSolution.getRobotRoutes().size(); i++) {
            ArrayList<Merchandise> currentRoute = currentSolution.getRobotRoutes().get(i);
            ArrayList<Merchandise> newRoute = new ArrayList<>(currentRoute);
            newRoutes.add(newRoute);
        }

        // Áp dụng các phép toán PSO với focus vào việc sử dụng tất cả robot
        for (int i = 0; i < newRoutes.size(); i++) {
            ArrayList<Merchandise> currentRoute = newRoutes.get(i);

            // Với xác suất dựa trên w, giữ một số mặt hàng ở vị trí hiện tại
            for (int j = 0; j < currentRoute.size(); j++) {
                if (random.nextDouble() > w) {
                    // Kết hợp thông tin từ personal best
                    if (random.nextDouble() < c1 && i < personalBest.getRobotRoutes().size()) {
                        ArrayList<Merchandise> personalBestRoute = personalBest.getRobotRoutes().get(i);
                        if (!personalBestRoute.isEmpty()) {
                            int randomIndex = random.nextInt(personalBestRoute.size());
                            Merchandise itemFromPersonalBest = personalBestRoute.get(randomIndex);

                            if (!containsMerchandise(currentRoute, itemFromPersonalBest)) {
                                int insertPos = random.nextInt(currentRoute.size() + 1);
                                currentRoute.add(insertPos, itemFromPersonalBest);
                            }
                        }
                    }

                    // Kết hợp thông tin từ global best
                    if (random.nextDouble() < c2 && i < globalBestSolution.getRobotRoutes().size()) {
                        ArrayList<Merchandise> globalBestRoute = globalBestSolution.getRobotRoutes().get(i);
                        if (!globalBestRoute.isEmpty()) {
                            int randomIndex = random.nextInt(globalBestRoute.size());
                            Merchandise itemFromGlobalBest = globalBestRoute.get(randomIndex);

                            if (!containsMerchandise(currentRoute, itemFromGlobalBest)) {
                                int insertPos = random.nextInt(currentRoute.size() + 1);
                                currentRoute.add(insertPos, itemFromGlobalBest);
                            }
                        }
                    }
                }
            }

            // Đảm bảo ràng buộc về sức chứa
            enforceCapacityConstraints(currentRoute, currentSolution.getRobots().get(i).capacity);
        }

        // Cập nhật tuyến đường của giải pháp hiện tại
        for (int i = 0; i < newRoutes.size(); i++) {
            currentSolution.getRobotRoutes().set(i, newRoutes.get(i));
        }

        // Đảm bảo tất cả các mặt hàng yêu cầu đều được phân bổ
        ensureAllItemsAllocated(currentSolution, globalBestSolution.getAllRequiredItems());

        // Đảm bảo tất cả robot hoạt động
        ensureAllRobotsActiveInSolution(currentSolution);

        // Tối ưu hóa thỉnh thoảng
        if (random.nextDouble() < 0.3) {
            optimizeRouteOrders(currentSolution, warehousing);
        }
    }

    // Các phương thức utility - giữ nguyên từ code gốc
    private boolean containsMerchandise(ArrayList<Merchandise> list, Merchandise item) {
        for (Merchandise m : list) {
            if (m.getName().equals(item.getName()) && m.getQuantity() == item.getQuantity()) {
                return true;
            }
        }
        return false;
    }

    private void enforceCapacityConstraints(ArrayList<Merchandise> route, int capacity) {
        int totalQuantity = 0;
        for (Merchandise item : route) {
            totalQuantity += item.getQuantity();
        }

        while (totalQuantity > capacity && !route.isEmpty()) {
            Merchandise removedItem = route.removeLast();
            totalQuantity -= removedItem.getQuantity();
        }
    }

    private void ensureAllItemsAllocated(Solution solution, ArrayList<Merchandise> allRequiredItems) {
        ArrayList<Merchandise> allocatedItems = new ArrayList<>();
        for (ArrayList<Merchandise> route : solution.getRobotRoutes()) {
            allocatedItems.addAll(route);
        }

        ArrayList<Merchandise> missingItems = new ArrayList<>();
        for (Merchandise required : allRequiredItems) {
            boolean found = false;
            for (Merchandise allocated : allocatedItems) {
                if (allocated.getName().equals(required.getName()) &&
                        allocated.getQuantity() == required.getQuantity()) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                missingItems.add(required);
            }
        }

        // Phân bổ các mặt hàng còn thiếu
        for (Merchandise missing : missingItems) {
            boolean allocated = false;
            for (int i = 0; i < solution.getRobots().size(); i++) {
                Robot robot = solution.getRobots().get(i);
                ArrayList<Merchandise> route = solution.getRobotRoutes().get(i);

                int currentLoad = calculateRobotLoad(solution, i);
                if (currentLoad + missing.getQuantity() <= robot.capacity) {
                    route.add(missing);
                    allocated = true;
                    break;
                }
            }

            if (!allocated) {
                int leastLoadedRobot = findLeastLoadedRobot(solution);
                solution.getRobotRoutes().get(leastLoadedRobot).add(missing);
            }
        }
    }

    /**
     * Đánh giá độ thích nghi (chi phí quãng đường) của một giải pháp
     */
    private double evaluateFitness(Solution solution, ArrayList<Merchandise> warehousing) {
        double totalDistance = 0;

        for (int i = 0; i < solution.getRobotRoutes().size(); i++) {
            ArrayList<Merchandise> route = solution.getRobotRoutes().get(i);
            if (route.isEmpty()) continue;

            Robot robot = solution.getRobots().get(i);
            Position startPosition = robot.getStartPosition();

            robot.setCurrentPosition(startPosition.copy());
            DistanceCalculator.setCurrentRobotPosition(startPosition);

            for (Merchandise merchandise : route) {
                Merchandise warehouseItem = findMerchandiseInWarehouse(merchandise, warehousing);
                if (warehouseItem != null) {
                    float distance = DistanceCalculator.calculateDistance(
                            robot.getCurrentPosition(),
                            warehouseItem.getPosition()
                    );
                    totalDistance += distance;
                    robot.setCurrentPosition(DistanceCalculator.getCurrentRobotPosition());
                }
            }

            // Quay lại vị trí xuất phát
            float returnDistance = DistanceCalculator.calculateDistance(
                    robot.getCurrentPosition(),
                    startPosition
            );
            totalDistance += returnDistance;
        }

        return totalDistance;
    }

    /**
     * Tìm mặt hàng trong kho dựa trên tên
     */
    private Merchandise findMerchandiseInWarehouse(Merchandise merchandise, ArrayList<Merchandise> warehousing) {
        for (Merchandise item : warehousing) {
            if (item.getName().equals(merchandise.getName())) {
                return item;
            }
        }
        return null;
    }
}