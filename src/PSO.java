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
                updateVelocityAndPosition(particle);

                // Đánh giá độ thích nghi
                double fitness = evaluateFitness(particle.getSolution(), warehousing);
                particle.getSolution().setFitness(fitness);

                // Cập nhật vị trí tốt nhất của hạt
                if (fitness < particle.getBestFitness()) {
                    particle.setBestSolution(new Solution(particle.getSolution()));
                    particle.setBestFitness(fitness);

                    // Cập nhật vị trí tốt nhất toàn cục nếu cần
                    if (fitness < globalBest.getBestFitness()) {
                        globalBest.setBestSolution(new Solution(particle.getSolution()));
                        globalBest.setBestFitness(fitness);
                    }
                }
            }

            // Áp dụng VNS để cải thiện giải pháp tốt nhất toàn cục sau mỗi N vòng lặp
            if (iteration % 5 == 0) {
                Solution improvedSolution = vns.improve(globalBest.getBestSolution(), warehousing);
                if (improvedSolution.getFitness() < globalBest.getBestFitness()) {
                    globalBest.setBestSolution(improvedSolution);
                    globalBest.setBestFitness(improvedSolution.getFitness());
                }
            }

            // In tiến độ
            if (iteration % 10 == 0) {
                System.out.println("Vòng lặp " + iteration + ": Quãng đường tốt nhất = " + globalBest.getBestFitness());
            }
        }

        System.out.println("PSO đã hoàn thành. Quãng đường tốt nhất: " + globalBest.getBestFitness());
        return globalBest.getBestSolution();
    }

    /**
     * Khởi tạo đàn với các hạt ngẫu nhiên
     * @param warehousing Kho hàng
     * @param require Danh sách mặt hàng cần lấy
     * @param robots Danh sách robot
     */
    private void initializeSwarm(ArrayList<Merchandise> warehousing, ArrayList<Merchandise> require, ArrayList<Robot> robots) {
        // Tạo các hạt ban đầu
        for (int i = 0; i < swarmSize; i++) {
            Particle particle = new Particle();

            // Khởi tạo với một giải pháp ngẫu nhiên
            Solution solution = new Solution(robots);
            solution.initializeRandomSolution(require);

            // Đánh giá độ thích nghi ban đầu
            double fitness = evaluateFitness(solution, warehousing);
            solution.setFitness(fitness);

            particle.setSolution(solution);
            particle.setBestSolution(new Solution(solution));
            particle.setBestFitness(fitness);

            swarm.add(particle);

            // Khởi tạo vị trí tốt nhất toàn cục với hạt đầu tiên
            if (i == 0 || fitness < globalBest.getBestFitness()) {
                globalBest = new Particle();
                globalBest.setBestSolution(new Solution(solution));
                globalBest.setBestFitness(fitness);
            }
        }
    }

    /**
     * Cập nhật vận tốc và vị trí cho một hạt
     * @param particle Hạt cần cập nhật
     */
    private void updateVelocityAndPosition(Particle particle) {
        Solution currentSolution = particle.getSolution();
        Solution personalBest = particle.getBestSolution();
        Solution globalBestSolution = globalBest.getBestSolution();

        // Cho mỗi tuyến đường của robot trong giải pháp
        for (int i = 0; i < currentSolution.getRobotRoutes().size(); i++) {
            ArrayList<Merchandise> currentRoute = currentSolution.getRobotRoutes().get(i);

            // Áp dụng các phép toán PSO để điều chỉnh tuyến đường
            // Với xác suất dựa trên w, giữ một số mặt hàng ở vị trí hiện tại
            for (int j = 0; j < currentRoute.size(); j++) {
                if (random.nextDouble() > w) {
                    // Với xác suất dựa trên c1, kết hợp thông tin từ vị trí tốt nhất cá nhân
                    if (random.nextDouble() < c1 && i < personalBest.getRobotRoutes().size() &&
                            j < personalBest.getRobotRoutes().get(i).size()) {
                        // Thử chèn một mặt hàng từ vị trí tốt nhất cá nhân nếu chưa có trong tuyến đường
                        Merchandise itemFromPersonalBest = personalBest.getRobotRoutes().get(i).get(j);
                        if (!containsMerchandise(currentRoute, itemFromPersonalBest)) {
                            // Tìm vị trí để chèn hoặc thay thế
                            int insertPos = random.nextInt(currentRoute.size() + 1);
                            if (insertPos < currentRoute.size()) {
                                currentRoute.set(insertPos, itemFromPersonalBest);
                            } else {
                                currentRoute.add(itemFromPersonalBest);
                            }
                        }
                    }

                    // Với xác suất dựa trên c2, kết hợp thông tin từ vị trí tốt nhất toàn cục
                    if (random.nextDouble() < c2 && i < globalBestSolution.getRobotRoutes().size() &&
                            j < globalBestSolution.getRobotRoutes().get(i).size()) {
                        Merchandise itemFromGlobalBest = globalBestSolution.getRobotRoutes().get(i).get(j);
                        if (!containsMerchandise(currentRoute, itemFromGlobalBest)) {
                            // Tìm vị trí để chèn hoặc thay thế
                            int insertPos = random.nextInt(currentRoute.size() + 1);
                            if (insertPos < currentRoute.size()) {
                                currentRoute.set(insertPos, itemFromGlobalBest);
                            } else {
                                currentRoute.add(itemFromGlobalBest);
                            }
                        }
                    }
                }
            }

            // Đảm bảo ràng buộc về sức chứa không bị vi phạm
            enforceCapacityConstraints(currentRoute, currentSolution.getRobots().get(i).capacity);
        }

        // Đảm bảo tất cả các mặt hàng được yêu cầu đều được phân bổ cho một số robot
        ensureAllItemsAllocated(currentSolution, globalBestSolution.getAllRequiredItems());
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
                    route.remove(route.size() - 1);
                    // Thêm mặt hàng còn thiếu
                    route.add(missing);
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
        Position counterPosition = new Position(0, 0, -1); // Giả sử counter ở vị trí (0,0,-1)

        for (int i = 0; i < solution.getRobotRoutes().size(); i++) {
            ArrayList<Merchandise> route = solution.getRobotRoutes().get(i);
            if (route.isEmpty()) continue;

            // Bắt đầu từ counter
            Position currentPosition = counterPosition;

            // Tính khoảng cách cho mỗi mặt hàng trong tuyến đường
            for (Merchandise merchandise : route) {
                // Tìm mặt hàng trong kho để lấy vị trí
                Merchandise warehouseItem = findMerchandiseInWarehouse(merchandise, warehousing);
                if (warehouseItem != null) {
                    // Tính khoảng cách từ vị trí hiện tại đến mặt hàng này
                    float distance = DistanceCalculator.calculateDistance(currentPosition, warehouseItem.getPosition());
                    totalDistance += distance;
                    currentPosition = warehouseItem.getPosition();
                }
            }

            // Quay lại counter
            totalDistance += DistanceCalculator.calculateDistance(currentPosition, counterPosition);
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