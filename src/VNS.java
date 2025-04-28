import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Lớp VNS (Variable Neighborhood Search) thực hiện thuật toán tìm kiếm lân cận biến đổi
 * để cải thiện giải pháp tìm được từ PSO
 */
public class VNS {
    private final int MAX_ITERATIONS;
    private final int MAX_NEIGHBORHOODS;
    private Random random;

    /**
     * Khởi tạo VNS với các tham số mặc định
     */
    public VNS() {
        this.MAX_ITERATIONS = Params.VNS_MAX_ITERATIONS;
        this.MAX_NEIGHBORHOODS = Params.VNS_MAX_NEIGHBORHOODS;
        this.random = new Random();
    }

    /**
     * Cải thiện một giải pháp sử dụng thuật toán VNS
     * @param initialSolution Giải pháp ban đầu
     * @param warehousing Kho hàng
     * @return Giải pháp tốt hơn
     */
    public Solution improve(Solution initialSolution, ArrayList<Merchandise> warehousing) {
        Solution currentSolution = new Solution(initialSolution);
        Solution bestSolution = new Solution(initialSolution);
        int k = 1;  // Bắt đầu với lân cận đầu tiên
        int iterations = 0;

        // In thông tin khởi tạo
        System.out.println("  Bắt đầu VNS với chi phí ban đầu: " + bestSolution.getFitness());

        while (iterations < MAX_ITERATIONS) {
            // Shake - tạo một giải pháp trong lân cận thứ k
            Solution newSolution = shake(currentSolution, k);

            // Tìm kiếm cục bộ để cải thiện giải pháp mới
            Solution localOptimum = localSearch(newSolution, warehousing);

            // Nếu tối ưu cục bộ tốt hơn giải pháp tốt nhất hiện tại
            if (localOptimum.getFitness() < bestSolution.getFitness()) {
                bestSolution = new Solution(localOptimum);
                currentSolution = new Solution(localOptimum);
                k = 1;  // Đặt lại lân cận
                System.out.println("  VNS - Vòng lặp " + iterations + ": Tìm thấy giải pháp tốt hơn với chi phí " +
                        bestSolution.getFitness());
            } else {
                // Chuyển sang lân cận tiếp theo
                k = (k % MAX_NEIGHBORHOODS) + 1;
            }

            iterations++;
        }

        System.out.println("  VNS hoàn thành sau " + iterations + " vòng lặp. Chi phí tốt nhất: " +
                bestSolution.getFitness());
        return bestSolution;
    }

    /**
     * Tạo một giải pháp trong lân cận thứ k bằng cách "shake"
     * @param solution Giải pháp hiện tại
     * @param k Chỉ số lân cận
     * @return Giải pháp mới
     */
    private Solution shake(Solution solution, int k) {
        Solution shaken = new Solution(solution);

        switch (k) {
            case 1:
                // Lân cận 1: Hoán đổi các mặt hàng giữa hai robot ngẫu nhiên
                swapItemsBetweenRobots(shaken);
                break;
            case 2:
                // Lân cận 2: Đảo ngược một phần của tuyến đường robot
                reverseSubroute(shaken);
                break;
            case 3:
                // Lân cận 3: Phân phối lại các mặt hàng giữa các robot
                redistributeItems(shaken);
                break;
        }

        return shaken;
    }

    /**
     * Hoán đổi các mặt hàng giữa hai robot ngẫu nhiên
     * @param solution Giải pháp cần thay đổi
     */
    private void swapItemsBetweenRobots(Solution solution) {
        if (solution.getRobots().size() < 2) return;

        // Chọn hai robot khác nhau
        int robot1Index = random.nextInt(solution.getRobots().size());
        int robot2Index;
        do {
            robot2Index = random.nextInt(solution.getRobots().size());
        } while (robot1Index == robot2Index);

        ArrayList<Merchandise> route1 = solution.getRobotRoutes().get(robot1Index);
        ArrayList<Merchandise> route2 = solution.getRobotRoutes().get(robot2Index);

        if (route1.isEmpty() || route2.isEmpty()) return;

        // Chọn các mặt hàng ngẫu nhiên để hoán đổi
        int item1Index = random.nextInt(route1.size());
        int item2Index = random.nextInt(route2.size());

        // Hoán đổi mặt hàng
        Merchandise temp = route1.get(item1Index);
        route1.set(item1Index, route2.get(item2Index));
        route2.set(item2Index, temp);

        // Đảm bảo ràng buộc về sức chứa không bị vi phạm
        enforceCapacityConstraints(solution, robot1Index);
        enforceCapacityConstraints(solution, robot2Index);
    }

    /**
     * Đảo ngược một phần của tuyến đường robot ngẫu nhiên
     * @param solution Giải pháp cần thay đổi
     */
    private void reverseSubroute(Solution solution) {
        // Chọn một robot ngẫu nhiên
        if (solution.getRobots().isEmpty()) return;

        int robotIndex = random.nextInt(solution.getRobots().size());
        ArrayList<Merchandise> route = solution.getRobotRoutes().get(robotIndex);

        if (route.size() < 2) return;

        // Chọn một đoạn đường con để đảo ngược
        int start = random.nextInt(route.size() - 1);
        int end = start + 1 + random.nextInt(route.size() - start - 1);

        // Đảo ngược đoạn đường con
        while (start < end) {
            Merchandise temp = route.get(start);
            route.set(start, route.get(end));
            route.set(end, temp);
            start++;
            end--;
        }
    }

    /**
     * Phân phối lại các mặt hàng giữa các robot
     * @param solution Giải pháp cần thay đổi
     */
    private void redistributeItems(Solution solution) {
        // Thu thập tất cả các mặt hàng từ tất cả các robot
        ArrayList<Merchandise> allItems = new ArrayList<>();
        for (ArrayList<Merchandise> route : solution.getRobotRoutes()) {
            allItems.addAll(route);
            route.clear();
        }

        // Xáo trộn các mặt hàng
        Collections.shuffle(allItems, random);

        // Phân phối lại các mặt hàng cho robot
        for (Merchandise item : allItems) {
            // Tìm robot có tải trọng ít nhất
            int leastLoadedRobot = findLeastLoadedRobot(solution);
            solution.getRobotRoutes().get(leastLoadedRobot).add(item);
        }

        // Đảm bảo ràng buộc về sức chứa được tôn trọng cho tất cả các robot
        for (int i = 0; i < solution.getRobots().size(); i++) {
            enforceCapacityConstraints(solution, i);
        }
    }

    /**
     * Tìm robot có tải trọng thấp nhất
     * @param solution Giải pháp chứa thông tin về robot
     * @return Chỉ số của robot có tải trọng thấp nhất
     */
    private int findLeastLoadedRobot(Solution solution) {
        int minLoad = Integer.MAX_VALUE;
        int robotIndex = 0;

        for (int i = 0; i < solution.getRobots().size(); i++) {
            Robot robot = solution.getRobots().get(i);
            ArrayList<Merchandise> route = solution.getRobotRoutes().get(i);

            int load = 0;
            for (Merchandise item : route) {
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
     * @param solution Giải pháp chứa thông tin robot
     * @param robotIndex Chỉ số robot cần kiểm tra
     */
    private void enforceCapacityConstraints(Solution solution, int robotIndex) {
        Robot robot = solution.getRobots().get(robotIndex);
        ArrayList<Merchandise> route = solution.getRobotRoutes().get(robotIndex);

        int totalQuantity = 0;
        for (Merchandise item : route) {
            totalQuantity += item.getQuantity();
        }

        // Loại bỏ các mặt hàng từ cuối cho đến khi tôn trọng sức chứa
        while (totalQuantity > robot.capacity && !route.isEmpty()) {
            Merchandise removedItem = route.remove(route.size() - 1);
            totalQuantity -= removedItem.getQuantity();

            // Thử phân bổ lại cho các robot khác
            boolean reallocated = false;
            for (int i = 0; i < solution.getRobots().size(); i++) {
                if (i == robotIndex) continue;

                Robot otherRobot = solution.getRobots().get(i);
                ArrayList<Merchandise> otherRoute = solution.getRobotRoutes().get(i);

                int otherLoad = 0;
                for (Merchandise m : otherRoute) {
                    otherLoad += m.getQuantity();
                }

                if (otherLoad + removedItem.getQuantity() <= otherRobot.capacity) {
                    otherRoute.add(removedItem);
                    reallocated = true;
                    break;
                }
            }

            // Nếu không thể phân bổ lại, thông báo
            if (!reallocated) {
                System.out.println("  Cảnh báo: Mặt hàng " + removedItem.getName() +
                        " không thể phân bổ do hạn chế về sức chứa");
            }
        }
    }

    /**
     * Tìm kiếm cục bộ để cải thiện giải pháp
     * @param solution Giải pháp ban đầu
     * @param warehousing Kho hàng
     * @return Giải pháp được cải thiện
     */
    private Solution localSearch(Solution solution, ArrayList<Merchandise> warehousing) {
        Solution currentSolution = new Solution(solution);
        boolean improved = true;
        int iterations = 0;
        final int MAX_LOCAL_ITERATIONS = 20;  // Giới hạn số vòng lặp cục bộ

        while (improved && iterations < MAX_LOCAL_ITERATIONS) {
            improved = false;
            iterations++;

            // 1. Tối ưu hóa thứ tự các mặt hàng trong mỗi tuyến đường robot
            for (int robotIndex = 0; robotIndex < currentSolution.getRobots().size(); robotIndex++) {
                ArrayList<Merchandise> route = currentSolution.getRobotRoutes().get(robotIndex);
                if (route.size() < 2) continue;

                // Tạo một bản sao của giải pháp hiện tại
                Solution newSolution = new Solution(currentSolution);

                // Tối ưu thứ tự bằng thuật toán người láng giềng gần nhất
                newSolution.optimizeRouteOrder(robotIndex, warehousing);

                // Đánh giá giải pháp mới
                double oldFitness = currentSolution.getFitness();
                double newFitness = evaluateFitness(newSolution, warehousing);
                newSolution.setFitness(newFitness);

                // Nếu tốt hơn, cập nhật giải pháp hiện tại
                if (newFitness < oldFitness) {
                    currentSolution = newSolution;
                    improved = true;
                }
            }

            // 2. Thử 2-opt cho mỗi tuyến đường
            if (!improved) {
                for (int robotIndex = 0; robotIndex < currentSolution.getRobots().size(); robotIndex++) {
                    ArrayList<Merchandise> route = currentSolution.getRobotRoutes().get(robotIndex);
                    if (route.size() < 2) continue;

                    for (int i = 0; i < route.size() - 1; i++) {
                        for (int j = i + 1; j < route.size(); j++) {
                            // Tạo một bản sao của giải pháp hiện tại
                            Solution newSolution = new Solution(currentSolution);
                            ArrayList<Merchandise> newRoute = newSolution.getRobotRoutes().get(robotIndex);

                            // Thực hiện hoán đổi 2-opt: đảo ngược đoạn [i+1, j]
                            reverse(newRoute, i + 1, j);

                            // Đánh giá giải pháp mới
                            double oldFitness = currentSolution.getFitness();
                            double newFitness = evaluateFitness(newSolution, warehousing);
                            newSolution.setFitness(newFitness);

                            // Nếu tốt hơn, cập nhật giải pháp hiện tại
                            if (newFitness < oldFitness) {
                                currentSolution = newSolution;
                                improved = true;
                                break;
                            }
                        }
                        if (improved) break;
                    }
                    if (improved) break;
                }
            }

            // 3. Thử di chuyển một mặt hàng từ robot này sang robot khác
            if (!improved) {
                for (int fromRobot = 0; fromRobot < currentSolution.getRobots().size(); fromRobot++) {
                    ArrayList<Merchandise> fromRoute = currentSolution.getRobotRoutes().get(fromRobot);
                    if (fromRoute.isEmpty()) continue;

                    for (int itemIndex = 0; itemIndex < fromRoute.size(); itemIndex++) {
                        Merchandise item = fromRoute.get(itemIndex);

                        for (int toRobot = 0; toRobot < currentSolution.getRobots().size(); toRobot++) {
                            if (fromRobot == toRobot) continue;

                            Solution newSolution = new Solution(currentSolution);
                            ArrayList<Merchandise> newFromRoute = newSolution.getRobotRoutes().get(fromRobot);
                            ArrayList<Merchandise> newToRoute = newSolution.getRobotRoutes().get(toRobot);

                            // Xóa mặt hàng khỏi robot nguồn
                            Merchandise movingItem = newFromRoute.remove(itemIndex);

                            // Thử chèn nó ở các vị trí khác nhau trong tuyến đường của robot đích
                            for (int insertPos = 0; insertPos <= newToRoute.size(); insertPos++) {
                                // Chèn mặt hàng
                                newToRoute.add(insertPos, movingItem);

                                // Kiểm tra xem ràng buộc về sức chứa có bị vi phạm không
                                int totalQuantity = 0;
                                for (Merchandise m : newToRoute) {
                                    totalQuantity += m.getQuantity();
                                }

                                // Nếu hợp lệ, đánh giá giải pháp mới
                                if (totalQuantity <= newSolution.getRobots().get(toRobot).capacity) {
                                    double oldFitness = currentSolution.getFitness();
                                    double newFitness = evaluateFitness(newSolution, warehousing);
                                    newSolution.setFitness(newFitness);

                                    // Nếu tốt hơn, cập nhật giải pháp hiện tại
                                    if (newFitness < oldFitness) {
                                        currentSolution = newSolution;
                                        improved = true;
                                        break;
                                    }
                                }

                                // Xóa mặt hàng để thử vị trí chèn tiếp theo
                                newToRoute.remove(insertPos);
                            }

                            // Nếu không tìm thấy cải thiện, khôi phục giải pháp ban đầu
                            if (!improved) {
                                newFromRoute.add(itemIndex, movingItem);
                            } else {
                                break;
                            }
                        }

                        if (improved) break;
                    }

                    if (improved) break;
                }
            }
        }

        return currentSolution;
    }

    /**
     * Đảo ngược một đoạn của mảng
     * @param route Tuyến đường cần đảo ngược
     * @param start Vị trí bắt đầu
     * @param end Vị trí kết thúc
     */
    private void reverse(ArrayList<Merchandise> route, int start, int end) {
        while (start < end) {
            Merchandise temp = route.get(start);
            route.set(start, route.get(end));
            route.set(end, temp);
            start++;
            end--;
        }
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
            Position startPosition = robot.getStartPosition(); // Lấy vị trí xuất phát của robot

            // Bắt đầu từ vị trí xuất phát
            Position currentPosition = startPosition;

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

            // Quay lại vị trí xuất phát
            totalDistance += DistanceCalculator.calculateDistance(currentPosition, startPosition);
        }

        return totalDistance;
    }

    /**
     * Tìm mặt hàng trong kho
     * @param merchandise Mặt hàng cần tìm
     * @param warehousing Kho hàng
     * @return Mặt hàng trong kho kèm vị trí, hoặc null nếu không tìm thấy
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