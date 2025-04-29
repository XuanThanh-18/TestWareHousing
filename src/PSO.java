import java.util.ArrayList;
import java.util.Random;

/**
 * Lớp PSO thực hiện thuật toán Particle Swarm Optimization
 * để tìm đường đi tối ưu cho robot trong kho hàng
 */
public class PSO {
    private ArrayList<Particle> swarm;
    private Particle globalBest;
    private int swarmSize;
    private int maxIterations;
    private double w; // trọng số quán tính
    private double c1; // hệ số nhận thức
    private double c2; // hệ số xã hội
    private Random random;
    private VNS vns;

    /**
     * Khởi tạo PSO với các tham số
     * @param swarmSize Kích thước đàn
     * @param maxIterations Số vòng lặp tối đa
     * @param w Trọng số quán tính
     * @param c1 Hệ số nhận thức
     * @param c2 Hệ số xã hội
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
     * Giải bài toán tìm đường đi tối ưu
     * @param warehousing Kho hàng
     * @param require Danh sách mặt hàng cần lấy
     * @param robots Danh sách robot
     * @return Giải pháp tốt nhất
     */
    public Solution solve(ArrayList<Merchandise> warehousing, ArrayList<Merchandise> require, ArrayList<Robot> robots) {
        // Khởi tạo đàn
        initializeSwarm(warehousing, require, robots);

        System.out.println("Bắt đầu thuật toán PSO với " + swarmSize + " hạt và " + maxIterations + " vòng lặp");

        // Vòng lặp chính của PSO
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            // Cập nhật vị trí và vận tốc cho mỗi hạt
            for (Particle particle : swarm) {
                updateVelocityAndPosition(particle, warehousing);

                // Đặt lại vị trí hiện tại của robot trước khi đánh giá
                for (Robot robot : particle.getSolution().getRobots()) {
                    robot.setCurrentPosition(robot.getStartPosition().copy());
                }
                DistanceCalculator.setCurrentRobotPosition(particle.getSolution().getRobots().get(0).getStartPosition());

                // Đánh giá độ thích nghi
                double fitness = evaluateFitness(particle.getSolution(), warehousing);
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
                    }
                }

                // In thông tin nếu có cải thiện đáng kể
                if (improved && iteration % 5 == 0) {
                    System.out.println("  Hạt cải thiện: " + fitness + " (giảm " +
                            String.format("%.2f", (particle.getBestFitness() - fitness)) + " đơn vị)");
                }
            }

            // Áp dụng VNS để cải thiện giải pháp tốt nhất toàn cục sau mỗi N vòng lặp
            if (iteration % 5 == 0) {
                double oldFitness = globalBest.getBestFitness();
                Solution improvedSolution = vns.improve(globalBest.getBestSolution(), warehousing);

                if (improvedSolution.getFitness() < oldFitness) {
                    globalBest.setBestSolution(improvedSolution);
                    globalBest.setBestFitness(improvedSolution.getFitness());
                    System.out.println("  VNS cải thiện giải pháp: " + improvedSolution.getFitness() +
                            " (giảm " + String.format("%.2f", (oldFitness - improvedSolution.getFitness())) + " đơn vị)");
                }
            }

            // In tiến độ
            if (iteration % 10 == 0) {
                System.out.println("Vòng lặp " + iteration + ": Quãng đường tốt nhất = " + globalBest.getBestFitness());
            }
        }

        System.out.println("PSO đã hoàn thành. Quãng đường tốt nhất: " + globalBest.getBestFitness());

        // Tối ưu hóa cuối cùng cho giải pháp tốt nhất
        optimizeRouteOrders(globalBest.getBestSolution(), warehousing);

        return globalBest.getBestSolution();
    }

    /**
     * Khởi tạo đàn với các hạt ngẫu nhiên
     * @param warehousing Kho hàng
     * @param require Danh sách mặt hàng cần lấy
     * @param robots Danh sách robot
     */
    private void initializeSwarm(ArrayList<Merchandise> warehousing, ArrayList<Merchandise> require, ArrayList<Robot> robots) {
        // Tạo danh sách các robot với vị trí hiện tại được đặt
        ArrayList<Robot> initializedRobots = new ArrayList<>();
        for (Robot robot : robots) {
            Robot newRobot = new Robot(robot.nameRobot, robot.getStartPosition());
            newRobot.capacity = robot.capacity;
            initializedRobots.add(newRobot);
        }

        // Tạo các hạt ban đầu
        for (int i = 0; i < swarmSize; i++) {
            Particle particle = new Particle();

            // Tạo bản sao mới của danh sách robot cho mỗi giải pháp
            ArrayList<Robot> particleRobots = new ArrayList<>();
            for (Robot robot : initializedRobots) {
                Robot robotCopy = new Robot(robot.nameRobot, robot.getStartPosition());
                robotCopy.capacity = robot.capacity;
                particleRobots.add(robotCopy);
            }

            // Khởi tạo với một giải pháp ngẫu nhiên
            Solution solution = new Solution(particleRobots);
            solution.initializeRandomSolution(require);

            // Đặt vị trí hiện tại của tất cả robot về vị trí xuất phát
            for (Robot robot : solution.getRobots()) {
                robot.setCurrentPosition(robot.getStartPosition().copy());
            }

            // Đặt vị trí hiện tại cho DistanceCalculator
            if (!solution.getRobots().isEmpty()) {
                DistanceCalculator.setCurrentRobotPosition(solution.getRobots().get(0).getStartPosition());
            }

            // Tối ưu hóa thứ tự trong mỗi tuyến đường
            optimizeRouteOrders(solution, warehousing);

            // Đánh giá độ thích nghi ban đầu
            double fitness = evaluateFitness(solution, warehousing);
            solution.setFitness(fitness);

            particle.setSolution(solution);
            particle.setBestSolution(new Solution(solution));
            particle.setBestFitness(fitness);

            swarm.add(particle);

            // Khởi tạo vị trí tốt nhất toàn cục với hạt đầu tiên
            if (i == 0) {
                globalBest = new Particle();
                globalBest.setBestSolution(new Solution(solution));
                globalBest.setBestFitness(fitness);
            } else if (fitness < globalBest.getBestFitness()) {
                globalBest.setBestSolution(new Solution(solution));
                globalBest.setBestFitness(fitness);
            }
        }
    }

    /**
     * Tối ưu hóa thứ tự các mặt hàng trong tất cả các tuyến đường robot
     * @param solution Giải pháp cần tối ưu hóa
     * @param warehousing Kho hàng
     */
    private void optimizeRouteOrders(Solution solution, ArrayList<Merchandise> warehousing) {
        for (int i = 0; i < solution.getRobotRoutes().size(); i++) {
            if (!solution.getRobotRoutes().get(i).isEmpty()) {
                // Đặt vị trí hiện tại của robot về vị trí xuất phát
                Robot robot = solution.getRobots().get(i);
                robot.setCurrentPosition(robot.getStartPosition().copy());
                DistanceCalculator.setCurrentRobotPosition(robot.getStartPosition());

                // Tối ưu thứ tự bằng phương pháp người láng giềng gần nhất
                solution.optimizeRouteOrder(i, warehousing);
            }
        }

        // Cập nhật lại fitness
        double newFitness = evaluateFitness(solution, warehousing);
        solution.setFitness(newFitness);
    }

    /**
     * Cập nhật vận tốc và vị trí cho một hạt
     * @param particle Hạt cần cập nhật
     * @param warehousing Kho hàng
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

        // Cho mỗi tuyến đường của robot trong giải pháp
        for (int i = 0; i < newRoutes.size(); i++) {
            ArrayList<Merchandise> currentRoute = newRoutes.get(i);

            // Áp dụng các phép toán PSO để điều chỉnh tuyến đường
            // Với xác suất dựa trên w, giữ một số mặt hàng ở vị trí hiện tại
            for (int j = 0; j < currentRoute.size(); j++) {
                if (random.nextDouble() > w) {
                    // Với xác suất dựa trên c1, kết hợp thông tin từ vị trí tốt nhất cá nhân
                    if (random.nextDouble() < c1 && i < personalBest.getRobotRoutes().size()) {
                        ArrayList<Merchandise> personalBestRoute = personalBest.getRobotRoutes().get(i);
                        if (!personalBestRoute.isEmpty()) {
                            // Lấy một mặt hàng ngẫu nhiên từ vị trí tốt nhất cá nhân
                            int randomIndex = random.nextInt(personalBestRoute.size());
                            Merchandise itemFromPersonalBest = personalBestRoute.get(randomIndex);

                            if (!containsMerchandise(currentRoute, itemFromPersonalBest)) {
                                // Chèn mặt hàng vào vị trí ngẫu nhiên
                                int insertPos = random.nextInt(currentRoute.size() + 1);
                                if (insertPos < currentRoute.size()) {
                                    currentRoute.add(insertPos, itemFromPersonalBest);
                                } else {
                                    currentRoute.add(itemFromPersonalBest);
                                }
                            }
                        }
                    }

                    // Với xác suất dựa trên c2, kết hợp thông tin từ vị trí tốt nhất toàn cục
                    if (random.nextDouble() < c2 && i < globalBestSolution.getRobotRoutes().size()) {
                        ArrayList<Merchandise> globalBestRoute = globalBestSolution.getRobotRoutes().get(i);
                        if (!globalBestRoute.isEmpty()) {
                            // Lấy một mặt hàng ngẫu nhiên từ vị trí tốt nhất toàn cục
                            int randomIndex = random.nextInt(globalBestRoute.size());
                            Merchandise itemFromGlobalBest = globalBestRoute.get(randomIndex);

                            if (!containsMerchandise(currentRoute, itemFromGlobalBest)) {
                                // Chèn mặt hàng vào vị trí ngẫu nhiên
                                int insertPos = random.nextInt(currentRoute.size() + 1);
                                if (insertPos < currentRoute.size()) {
                                    currentRoute.add(insertPos, itemFromGlobalBest);
                                } else {
                                    currentRoute.add(itemFromGlobalBest);
                                }
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

        // Tối ưu hóa thứ tự các mặt hàng trong các tuyến đường sau khi cập nhật
        // Chỉ tối ưu thỉnh thoảng để tăng hiệu suất
        if (random.nextDouble() < 0.3) {
            optimizeRouteOrders(currentSolution, warehousing);
        }
    }

    /**
     * Kiểm tra xem danh sách đã chứa mặt hàng tương tự chưa
     * @param list Danh sách cần kiểm tra
     * @param item Mặt hàng cần tìm
     * @return true nếu đã có mặt hàng tương tự, false nếu chưa
     */
    private boolean containsMerchandise(ArrayList<Merchandise> list, Merchandise item) {
        for (Merchandise m : list) {
            if (m.getName().equals(item.getName()) && m.getQuantity() == item.getQuantity()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Đảm bảo ràng buộc về sức chứa
     * @param route Tuyến đường cần kiểm tra
     * @param capacity Sức chứa của robot
     */
    private void enforceCapacityConstraints(ArrayList<Merchandise> route, int capacity) {
        // Thực thi đơn giản: xóa các mặt hàng từ cuối nếu vượt quá sức chứa
        int totalQuantity = 0;
        for (Merchandise item : route) {
            totalQuantity += item.getQuantity();
        }

        while (totalQuantity > capacity && !route.isEmpty()) {
            Merchandise removedItem = route.remove(route.size() - 1);
            totalQuantity -= removedItem.getQuantity();
        }
    }

    /**
     * Đảm bảo tất cả các mặt hàng yêu cầu đều được phân bổ
     * @param solution Giải pháp cần kiểm tra
     * @param allRequiredItems Danh sách tất cả các mặt hàng yêu cầu
     */
    private void ensureAllItemsAllocated(Solution solution, ArrayList<Merchandise> allRequiredItems) {
        // Kiểm tra mặt hàng nào đang thiếu trong giải pháp hiện tại
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

        // Phân bổ các mặt hàng còn thiếu cho robot có sức chứa còn trống
        for (Merchandise missing : missingItems) {
            boolean allocated = false;
            for (int i = 0; i < solution.getRobots().size(); i++) {
                Robot robot = solution.getRobots().get(i);
                ArrayList<Merchandise> route = solution.getRobotRoutes().get(i);

                int currentLoad = 0;
                for (Merchandise item : route) {
                    currentLoad += item.getQuantity();
                }

                if (currentLoad + missing.getQuantity() <= robot.capacity) {
                    route.add(missing);
                    allocated = true;
                    break;
                }
            }

            // Nếu tất cả robot đều đã đạt sức chứa tối đa, thử tạo không gian bằng cách xóa các mặt hàng ít quan trọng hơn
            if (!allocated) {
                // Tìm robot có mặt hàng ít quan trọng nhất
                int robotIndex = findRobotWithLeastImportantItem(solution);
                if (robotIndex >= 0) {
                    ArrayList<Merchandise> route = solution.getRobotRoutes().get(robotIndex);
                    // Xóa mặt hàng ít quan trọng nhất
                    if (!route.isEmpty()) {
                        route.remove(route.size() - 1);
                        // Thêm mặt hàng còn thiếu
                        route.add(missing);
                    }
                }
            }
        }
    }

    /**
     * Tìm robot có mặt hàng ít quan trọng nhất
     * @param solution Giải pháp chứa các tuyến đường robot
     * @return Chỉ số của robot
     */
    private int findRobotWithLeastImportantItem(Solution solution) {
        int maxItems = -1;
        int robotIndex = -1;

        for (int i = 0; i < solution.getRobotRoutes().size(); i++) {
            ArrayList<Merchandise> route = solution.getRobotRoutes().get(i);
            if (!route.isEmpty() && route.size() > maxItems) {
                maxItems = route.size();
                robotIndex = i;
            }
        }

        return robotIndex;
    }

    /**
     * Đánh giá độ thích nghi (chi phí quãng đường) của một giải pháp
     * @param solution Giải pháp cần đánh giá
     * @param warehousing Kho hàng
     * @return Tổng chi phí quãng đường
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

            Position currentPosition = startPosition;

            for (Merchandise merchandise : route) {
                // Tìm mặt hàng trong kho
                Merchandise warehouseItem = findMerchandiseInWarehouse(merchandise, warehousing);
                if (warehouseItem != null) {
                    // Chọn vị trí tối ưu dựa trên vị trí hiện tại
                    Position optimalPosition = warehouseItem.getOptimalPosition(currentPosition);

                    // Tính khoảng cách từ vị trí hiện tại đến vị trí tối ưu
                    float distance = DistanceCalculator.calculateDistance(currentPosition, optimalPosition);
                    totalDistance += distance;

                    // Cập nhật vị trí hiện tại
                    currentPosition = optimalPosition;
                    robot.setCurrentPosition(currentPosition);
                }
            }

            // Quay lại vị trí xuất phát
            float returnDistance = DistanceCalculator.calculateDistance(currentPosition, startPosition);
            totalDistance += returnDistance;
            robot.setCurrentPosition(startPosition.copy());
        }

        return totalDistance;
    }

    /**
     * Tìm mặt hàng trong kho dựa trên tên
     * @param merchandise Mặt hàng cần tìm
     * @param warehousing Kho hàng
     * @return Mặt hàng tìm thấy hoặc null nếu không tìm thấy
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