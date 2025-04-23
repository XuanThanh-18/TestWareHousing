import java.util.ArrayList;

/**
 * Lớp Test thực hiện thuật toán tìm đường đi ngắn nhất cho robot
 * sử dụng PSO kết hợp VNS trên bản đồ kho hàng
 */
public class Test {
    public static void main(String[] args) {
        // Đọc tham số từ file
        Params.ReadParams();
        Params.printWarehouseMap();
        // Khởi tạo kho hàng
        ArrayList<Merchandise> warehousing = WareHousing.setWareHousing();

        // Định nghĩa vị trí counter
        Position counterPosition = new Position(0, 0, -1);

        // Tạo bản đồ kho hàng từ dữ liệu đọc được
        WarehouseMap warehouseMap;
        if (Params.WAREHOUSE_MAP != null) {
            warehouseMap = new WarehouseMap(Params.WAREHOUSE_MAP);
        } else {
            warehouseMap = WarehouseMap.createMapFromWarehouse(Params.SHELVES, Params.SLOTS);
        }

        // In bản đồ kho hàng
        warehouseMap.printMap();

        // Khởi tạo trình tính khoảng cách với bản đồ
        DistanceCalculator.initialize(warehouseMap);

        // Tạo một cá thể
        Individual individual = new Individual();

        // Thực hiện thuật toán PSO-VNS
        System.out.println("==================================================");
        System.out.println("THUẬT TOÁN PSO-VNS TÌM ĐƯỜNG ĐI NGẮN NHẤT CHO ROBOT");
        System.out.println("==================================================");

        // Thực thi thuật toán PSO-VNS
        float psoVnsDistance = individual.solvePsoVns(counterPosition, warehousing);

        System.out.println("\n==================================================");
        System.out.println("TỔNG CHI PHÍ QUÃNG ĐƯỜNG: " + psoVnsDistance);
        System.out.println("==================================================");

        // In thông tin chi tiết đường đi cho mỗi robot
        System.out.println("\nCHI TIẾT ĐƯỜNG ĐI CỦA CÁC ROBOT:");
        for (Robot robot : individual.robots) {
            System.out.println("\n--- ROBOT " + robot.nameRobot + " ---");

            if (robot.shoppingCart.isEmpty()) {
                System.out.println("Không có mặt hàng nào được phân công");
                continue;
            }

            // Hiển thị đường đi chi tiết
            System.out.println("1. Bắt đầu từ COUNTER tại " + counterPosition);
            Position currentPos = counterPosition;
            float totalDistance = 0;

            // Lưu trữ đường đi chi tiết để hiển thị trên bản đồ
            ArrayList<Position> robotPath = new ArrayList<>();
            robotPath.add(counterPosition);

            for (int i = 0; i < robot.shoppingCart.size(); i++) {
                Merchandise item = robot.shoppingCart.get(i);

                // Tìm vị trí trong kho
                Merchandise warehouseItem = null;
                for (Merchandise w : warehousing) {
                    if (w.getName().equals(item.getName())) {
                        warehouseItem = w;
                        break;
                    }
                }

                if (warehouseItem != null) {
                    // Tính đường đi thực tế
                    ArrayList<int[]> pathCoords = DistanceCalculator.findPath(currentPos, warehouseItem.getPosition());

                    // Tính khoảng cách
                    float distance = DistanceCalculator.calculateDistance(currentPos, warehouseItem.getPosition());
                    totalDistance += distance;

                    // Hiển thị thông tin mặt hàng
                    System.out.println((i + 2) + ". Đi đến " + warehouseItem.getName() +
                            " (số lượng: " + warehouseItem.getQuantity() + ") tại vị trí " +
                            warehouseItem.getPosition() + " (+" + distance + " đơn vị)");

                    // In ra đường đi chi tiết (nếu có)
                    if (!pathCoords.isEmpty()) {
                        System.out.println("   Đường đi chi tiết:");
                        for (int j = 0; j < pathCoords.size(); j++) {
                            int[] coord = pathCoords.get(j);
                            Position pathPos = warehouseMap.coordinatesToPosition(coord[0], coord[1]);
                            System.out.println("   - Bước " + (j+1) + ": " + pathPos);
                        }
                    }

                    // Cập nhật vị trí hiện tại
                    currentPos = warehouseItem.getPosition();
                    robotPath.add(currentPos);
                }
            }

            // Đường đi về counter
            ArrayList<int[]> returnPathCoords = DistanceCalculator.findPath(currentPos, counterPosition);
            float returnDistance = DistanceCalculator.calculateDistance(currentPos, counterPosition);
            totalDistance += returnDistance;

            System.out.println((robot.shoppingCart.size() + 2) + ". Quay về COUNTER tại " +
                    counterPosition + " (+" + returnDistance + " đơn vị)");

            // In ra đường đi chi tiết khi quay về
            if (!returnPathCoords.isEmpty()) {
                System.out.println("   Đường đi chi tiết khi quay về:");
                for (int j = 0; j < returnPathCoords.size(); j++) {
                    int[] coord = returnPathCoords.get(j);
                    Position pathPos = warehouseMap.coordinatesToPosition(coord[0], coord[1]);
                    System.out.println("   - Bước " + (j+1) + ": " + pathPos);
                }
            }

            robotPath.add(counterPosition); // Thêm điểm cuối là counter
            System.out.println("\nTổng quãng đường của ROBOT " + robot.nameRobot + ": " + totalDistance);

            // Hiển thị đường đi trên bản đồ
            System.out.println("\nĐường đi của ROBOT " + robot.nameRobot + " trên bản đồ:");

            // Chuyển đổi danh sách Position thành tọa độ trên bản đồ
            ArrayList<int[]> fullPath = new ArrayList<>();
            for (Position pos : robotPath) {
                int[] coords = warehouseMap.positionToCoordinates(pos);
                fullPath.add(coords);
            }

            // In đường đi trên bản đồ
            warehouseMap.printPathOnMap(fullPath);
        }
    }
}