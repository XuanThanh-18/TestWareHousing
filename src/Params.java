import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Lớp Params chứa các tham số và dữ liệu cấu hình cho bài toán
 * Phiên bản đã sửa lỗi để đảm bảo lấy đủ hàng theo yêu cầu
 */
public class Params {
    // Tham số kho hàng
    static int SHELVES;              // Số khối kệ
    static int TIERS;                // Số tầng trong mỗi khối kệ (thường là 2)
    static int SLOTS;                // Số ô trên mỗi tầng (độ rộng kho)
    static int ROBOTS;               // Số robot
    static int CAPACITY;             // Sức chứa của mỗi robot
    static int REQUIRE_MACHANDISE;   // Số lượng mặt hàng cần lấy
    static int COUNT;                // Tổng số ô trong kho

    // Dữ liệu kho hàng
    static ArrayList<Merchandise> WAREHOUSE;    // Danh sách hàng trong kho
    static ArrayList<Merchandise> REQUIRE;      // Danh sách hàng cần lấy
    static int[][] WAREHOUSE_MAP;               // Bản đồ kho hàng

    /**
     * Tham số cho thuật toán PSO
     */
    static int PSO_SWARM_SIZE = 30;
    static int PSO_MAX_ITERATIONS = 100;
    static double PSO_INERTIA_WEIGHT = 0.7;
    static double PSO_COGNITIVE_COEFFICIENT = 1.5;
    static double PSO_SOCIAL_COEFFICIENT = 1.5;

    /**
     * Tham số cho thuật toán VNS
     */
    static int VNS_MAX_ITERATIONS = 200;
    static int VNS_MAX_NEIGHBORHOODS = 3;

    /**
     * Đọc tham số từ file
     * Cập nhật để tương thích với cấu trúc kho mới
     */
    public static void ReadParams() {
        String pathname = "src/resources/data_test_small.txt";
        ArrayList<Merchandise> warehouse = new ArrayList<>();
        ArrayList<Merchandise> require = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(pathname))) {
            String line;
            String name;
            int quantity;

            // Đọc dòng đầu tiên chứa các tham số
            line = br.readLine();
            String[] parts = line.split(" ");
            if (parts.length >= 6) {
                SHELVES = Integer.parseInt(parts[0]);           // Số khối kệ
                TIERS = Integer.parseInt(parts[1]);             // Số tầng mỗi khối
                SLOTS = Integer.parseInt(parts[2]);             // Số ô mỗi tầng
                REQUIRE_MACHANDISE = Integer.parseInt(parts[3]); // Số mặt hàng cần lấy
                ROBOTS = Integer.parseInt(parts[4]);            // Số robot
                CAPACITY = Integer.parseInt(parts[5]);          // Sức chứa robot
                COUNT = TIERS * SHELVES * SLOTS;               // Tổng số ô
            }

            // Đọc bản đồ kho hàng, kho hàng và yêu cầu
            boolean readingMap = false;
            boolean readingWarehouse = false;
            boolean readingRequire = false;
            ArrayList<int[]> mapRows = new ArrayList<>();

            while ((line = br.readLine()) != null) {
                // Xử lý các section markers
                if (line.trim().equals("MAP_START")) {
                    readingMap = true;
                    continue;
                } else if (line.trim().equals("MAP_END")) {
                    readingMap = false;
                    continue;
                } else if (line.trim().equals("WAREHOUSE_START")) {
                    readingWarehouse = true;
                    continue;
                } else if (line.trim().equals("WAREHOUSE_END")) {
                    readingWarehouse = false;
                    continue;
                } else if (line.trim().equals("REQUIRE_START")) {
                    readingRequire = true;
                    continue;
                } else if (line.trim().equals("REQUIRE_END")) {
                    readingRequire = false;
                    continue;
                }

                // Đọc bản đồ
                if (readingMap) {
                    String[] mapRow = line.trim().split(" ");
                    int[] row = new int[mapRow.length];
                    for (int i = 0; i < mapRow.length; i++) {
                        row[i] = Integer.parseInt(mapRow[i]);
                    }
                    mapRows.add(row);
                }
                // Đọc thông tin kho hàng
                else if (readingWarehouse) {
                    parts = line.trim().split(" ");
                    if (parts.length >= 5) {
                        name = parts[0];
                        quantity = Integer.parseInt(parts[1]);
                        int shelf = Integer.parseInt(parts[2]);
                        int tier = Integer.parseInt(parts[3]);
                        int slot = Integer.parseInt(parts[4]);

                        Position position = new Position(shelf, tier, slot);
                        Merchandise merchandise = new Merchandise(name, quantity, position);

                        // Kiểm tra xem sản phẩm đã tồn tại chưa
                        boolean found = false;
                        for (Merchandise existing : warehouse) {
                            if (existing.getName().equals(name)) {
                                // Thêm vị trí thay thế
                                existing.addAlternativePosition(position.copy());
                                // Cộng dồn số lượng
                                existing.setQuantity(existing.getQuantity() + quantity);
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            warehouse.add(merchandise);
                        }
                    }
                }
                // Đọc yêu cầu
                else if (readingRequire) {
                    parts = line.trim().split(" ");
                    if (parts.length >= 2) {
                        name = parts[0];
                        int requestedQuantity = Integer.parseInt(parts[1]);

                        // Kiểm tra tổng số lượng có sẵn trong kho
                        int availableQuantity = 0;
                        for (Merchandise item : warehouse) {
                            if (item.getName().equals(name)) {
                                availableQuantity += item.getQuantity();
                            }
                        }

                        if (availableQuantity >= requestedQuantity) {
                            // Tạo merchandise với số lượng yêu cầu
                            Merchandise requiredMerchandise = new Merchandise(name, requestedQuantity);
                            require.add(requiredMerchandise);
                            System.out.println("✓ Thêm yêu cầu: " + name + " x " + requestedQuantity +
                                    " (có sẵn: " + availableQuantity + ")");
                        } else {
                            // Nếu không đủ hàng, thêm số lượng có sẵn và cảnh báo
                            if (availableQuantity > 0) {
                                Merchandise requiredMerchandise = new Merchandise(name, availableQuantity);
                                require.add(requiredMerchandise);
                                System.out.println("⚠ Cảnh báo: Chỉ có " + availableQuantity + "/" + requestedQuantity +
                                        " cho " + name + " - Đã điều chỉnh yêu cầu");
                            } else {
                                System.out.println("✗ Lỗi: Không có " + name + " trong kho (yêu cầu: " + requestedQuantity + ")");
                            }
                        }
                    }
                }
            }

            // Tạo bản đồ kho hàng từ dữ liệu đọc được
            if (!mapRows.isEmpty()) {
                int rows = mapRows.size();
                int cols = mapRows.get(0).length;
                WAREHOUSE_MAP = new int[rows][cols];

                for (int i = 0; i < rows; i++) {
                    System.arraycopy(mapRows.get(i), 0, WAREHOUSE_MAP[i], 0, cols);
                }

                System.out.println("✓ Đã đọc bản đồ kho hàng từ file: " + rows + "x" + cols);
            } else {
                // Tạo bản đồ mặc định theo cấu trúc mới
                createDefaultMapForNewStructure();
            }

            // Cập nhật tham số
            REQUIRE_MACHANDISE = require.size();
            ROBOTS = Math.max(1, ROBOTS);
            WAREHOUSE = warehouse;
            REQUIRE = require;

            // Kiểm tra và điều chỉnh sức chứa robot nếu cần
            validateAndAdjustCapacity();

            // In thông tin cấu hình
            printConfigurationInfo();

            // Kiểm tra tính khả thi của bài toán
            validateProblemFeasibility();

        } catch (FileNotFoundException e) {
            throw new RuntimeException("Không tìm thấy file: " + pathname, e);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi đọc file: " + pathname, e);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Lỗi định dạng số trong file: " + pathname, e);
        }
    }

    /**
     * Kiểm tra và điều chỉnh sức chứa robot để đảm bảo có thể lấy đủ hàng
     */
    private static void validateAndAdjustCapacity() {
        if (REQUIRE == null || REQUIRE.isEmpty()) {
            return;
        }

        int totalRequiredQuantity = 0;
        for (Merchandise item : REQUIRE) {
            totalRequiredQuantity += item.getQuantity();
        }

        int totalCapacity = ROBOTS * CAPACITY;

        if (totalRequiredQuantity > totalCapacity) {
            System.out.println("⚠ CẢNH BÁO: Tổng yêu cầu (" + totalRequiredQuantity +
                    ") > Tổng sức chứa robot (" + totalCapacity + ")");

            // Tự động điều chỉnh sức chứa
            int newCapacity = (int) Math.ceil((double) totalRequiredQuantity / ROBOTS) + 5; // +5 để dự phòng
            System.out.println("🔧 Tự động điều chỉnh sức chứa robot từ " + CAPACITY + " lên " + newCapacity);
            CAPACITY = newCapacity;
        }
    }

    /**
     * Kiểm tra tính khả thi của bài toán
     */
    private static void validateProblemFeasibility() {
        System.out.println("\n=== KIỂM TRA TÍNH KHẢ THI ===");

        boolean feasible = true;

        // Kiểm tra mỗi mặt hàng yêu cầu
        for (Merchandise reqItem : REQUIRE) {
            int availableQuantity = 0;
            for (Merchandise warehouseItem : WAREHOUSE) {
                if (warehouseItem.getName().equals(reqItem.getName())) {
                    availableQuantity += warehouseItem.getQuantity();
                }
            }

            if (availableQuantity < reqItem.getQuantity()) {
                System.out.println("✗ " + reqItem.getName() + ": Yêu cầu " + reqItem.getQuantity() +
                        ", có sẵn " + availableQuantity);
                feasible = false;
            } else {
                System.out.println("✓ " + reqItem.getName() + ": Yêu cầu " + reqItem.getQuantity() +
                        ", có sẵn " + availableQuantity);
            }
        }

        // Kiểm tra sức chứa
        int totalRequired = 0;
        for (Merchandise item : REQUIRE) {
            totalRequired += item.getQuantity();
        }
        int totalCapacity = ROBOTS * CAPACITY;

        if (totalRequired > totalCapacity) {
            System.out.println("✗ Sức chứa: Yêu cầu " + totalRequired + ", có sẵn " + totalCapacity);
            feasible = false;
        } else {
            System.out.println("✓ Sức chứa: Yêu cầu " + totalRequired + ", có sẵn " + totalCapacity);
        }

        if (feasible) {
            System.out.println("🎉 Bài toán có thể giải được!");
        } else {
            System.out.println("⚠ Bài toán có thể không giải được hoàn toàn!");
        }
    }

    /**
     * Tạo bản đồ mặc định cho cấu trúc kho mới
     */
    private static void createDefaultMapForNewStructure() {
        // Cấu trúc: mỗi khối kệ có 2 hàng, cách nhau bởi lối đi
        // Tổng số hàng = số khối kệ * 3 + 1 (mỗi khối: 2 hàng kệ + 1 hàng lối đi)
        int rows = SHELVES * 3 + 1;
        int cols = SLOTS;

        WAREHOUSE_MAP = new int[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (i % 3 == 0) {
                    // Lối đi ngang
                    WAREHOUSE_MAP[i][j] = 0;
                } else {
                    // Hàng kệ
                    if (j == 0 || j == cols - 1) {
                        WAREHOUSE_MAP[i][j] = 0; // Lối đi hai bên
                    } else {
                        WAREHOUSE_MAP[i][j] = 1; // Kệ hàng
                    }
                }
            }
        }

        System.out.println("✓ Đã tạo bản đồ kho hàng mặc định (cấu trúc mới): " + rows + "x" + cols);
    }

    /**
     * In thông tin cấu hình - Phiên bản cải tiến
     */
    private static void printConfigurationInfo() {
        System.out.println("\n=== THÔNG TIN CẤU HÌNH KHO HÀNG ===");
        System.out.println("Cấu trúc kho (mới): Mỗi ô hàng có 1 điểm tiếp cận duy nhất");
        System.out.println("- Số khối kệ: " + SHELVES);
        System.out.println("- Số tầng mỗi khối: " + TIERS);
        System.out.println("- Số ô mỗi tầng: " + SLOTS);
        System.out.println("- Tổng số ô trong kho: " + COUNT);
        System.out.println("- Số robot: " + ROBOTS);
        System.out.println("- Sức chứa mỗi robot: " + CAPACITY);
        System.out.println("- Số loại mặt hàng trong kho: " + WAREHOUSE.size());
        System.out.println("- Số loại mặt hàng cần lấy: " + REQUIRE_MACHANDISE);

        if (WAREHOUSE_MAP != null) {
            System.out.println("- Kích thước bản đồ: " + WAREHOUSE_MAP.length + "x" + WAREHOUSE_MAP[0].length);
        }

        // Thống kê mặt hàng - chi tiết hơn
        int totalWarehouseQuantity = 0;
        int totalRequiredQuantity = 0;
        int totalWarehouseTypes = WAREHOUSE.size();
        int totalRequiredTypes = REQUIRE.size();

        for (Merchandise item : WAREHOUSE) {
            totalWarehouseQuantity += item.getQuantity();
        }

        for (Merchandise item : REQUIRE) {
            totalRequiredQuantity += item.getQuantity();
        }

        System.out.println("- Tổng số lượng hàng trong kho: " + totalWarehouseQuantity);
        System.out.println("- Tổng số lượng hàng cần lấy: " + totalRequiredQuantity);
        System.out.println("- Loại hàng trong kho: " + totalWarehouseTypes);
        System.out.println("- Loại hàng cần lấy: " + totalRequiredTypes);
        System.out.println("- Tỷ lệ sử dụng kho: " + String.format("%.1f", (totalRequiredQuantity * 100.0 / totalWarehouseQuantity)) + "%");

        // Kiểm tra sức chứa
        int totalCapacity = ROBOTS * CAPACITY;
        System.out.println("- Tổng sức chứa robot: " + totalCapacity);
        System.out.println("- Tỷ lệ sử dụng sức chứa: " + String.format("%.1f", (totalRequiredQuantity * 100.0 / totalCapacity)) + "%");

        if (totalRequiredQuantity > totalCapacity) {
            System.out.println("⚠ CẢNH BÁO: Vượt quá sức chứa robot!");
        }
    }

    /**
     * In danh sách mặt hàng cần lấy - Phiên bản chi tiết
     */
    public static void printRequiredItems() {
        System.out.println("\n=== DANH SÁCH MẶT HÀNG CẦN LẤY (CHI TIẾT) ===");

        if (REQUIRE == null || REQUIRE.isEmpty()) {
            System.out.println("Không có mặt hàng nào cần lấy!");
            return;
        }

        int totalQuantity = 0;
        for (int i = 0; i < REQUIRE.size(); i++) {
            Merchandise reqItem = REQUIRE.get(i);

            // Tìm trong kho để kiểm tra có đủ hàng không
            int availableQuantity = 0;
            ArrayList<Position> availablePositions = new ArrayList<>();

            for (Merchandise warehouseItem : WAREHOUSE) {
                if (warehouseItem.getName().equals(reqItem.getName())) {
                    availableQuantity += warehouseItem.getQuantity();
                    availablePositions.add(warehouseItem.getPosition());
                    // Thêm các vị trí thay thế nếu có
                    for (Position altPos : warehouseItem.getAllPositions()) {
                        if (!altPos.equals(warehouseItem.getPosition())) {
                            availablePositions.add(altPos);
                        }
                    }
                }
            }

            String status = (availableQuantity >= reqItem.getQuantity()) ? "✓" : "⚠";
            System.out.printf("%s %2d. %-20s: %3d đơn vị (có sẵn: %3d, tại %d vị trí)\n",
                    status, (i+1), reqItem.getName(), reqItem.getQuantity(),
                    availableQuantity, availablePositions.size());

            totalQuantity += reqItem.getQuantity();
        }

        System.out.println("─".repeat(70));
        System.out.println("TỔNG SỐ LƯỢNG CẦN LẤY: " + totalQuantity + " đơn vị");
        System.out.println("TỔNG SỨC CHỨA ROBOT: " + (ROBOTS * CAPACITY) + " đơn vị");

        if (totalQuantity > ROBOTS * CAPACITY) {
            System.out.println("⚠ CẢNH BÁO: Cần tăng số robot hoặc sức chứa!");
        } else {
            System.out.println("✓ Sức chứa đủ để thực hiện nhiệm vụ");
        }
    }

    /**
     * Kiểm tra tính hợp lệ của cấu hình - Phiên bản nâng cao
     */
    public static boolean validateConfiguration() {
        boolean isValid = true;
        ArrayList<String> errors = new ArrayList<>();

        // Kiểm tra tham số cơ bản
        if (SHELVES <= 0 || TIERS <= 0 || SLOTS <= 0) {
            errors.add("Tham số kho hàng không hợp lệ: SHELVES=" + SHELVES + ", TIERS=" + TIERS + ", SLOTS=" + SLOTS);
            isValid = false;
        }

        if (ROBOTS <= 0 || CAPACITY <= 0) {
            errors.add("Tham số robot không hợp lệ: ROBOTS=" + ROBOTS + ", CAPACITY=" + CAPACITY);
            isValid = false;
        }

        if (WAREHOUSE == null || WAREHOUSE.isEmpty()) {
            errors.add("Không có mặt hàng nào trong kho");
            isValid = false;
        }

        if (REQUIRE == null || REQUIRE.isEmpty()) {
            errors.add("Không có mặt hàng nào cần lấy");
            isValid = false;
        }

        // Kiểm tra tính khả thi
        if (REQUIRE != null && WAREHOUSE != null) {
            for (Merchandise reqItem : REQUIRE) {
                int availableQuantity = 0;
                for (Merchandise warehouseItem : WAREHOUSE) {
                    if (warehouseItem.getName().equals(reqItem.getName())) {
                        availableQuantity += warehouseItem.getQuantity();
                    }
                }

                if (availableQuantity < reqItem.getQuantity()) {
                    errors.add("Không đủ " + reqItem.getName() + ": yêu cầu " + reqItem.getQuantity() +
                            ", có sẵn " + availableQuantity);
                    isValid = false;
                }
            }
        }

        // In lỗi nếu có
        if (!errors.isEmpty()) {
            System.out.println("\n❌ CÁC LỖI CẤU HÌNH:");
            for (int i = 0; i < errors.size(); i++) {
                System.out.println((i+1) + ". " + errors.get(i));
            }
        }

        return isValid;
    }

    /**
     * Các phương thức còn lại giữ nguyên...
     */
    public static int calculateTotalCells() {
        return SHELVES * TIERS * SLOTS;
    }

    public static void printWarehouse() {
        System.out.println("\n=== DANH SÁCH MẶT HÀNG TRONG KHO ===");
        for (int i = 0; i < WAREHOUSE.size(); i++) {
            Merchandise m = WAREHOUSE.get(i);
            System.out.println((i+1) + ". " + m.getName() + ": " + m.getQuantity() +
                    " đơn vị tại " + m.getPosition());

            // Hiển thị vị trí thay thế nếu có
            if (!m.getAllPositions().isEmpty() && m.getAllPositions().size() > 1) {
                System.out.println("   Vị trí thay thế: " + (m.getAllPositions().size() - 1) + " vị trí");
            }
        }
    }

    public static void printWarehouseMap() {
        if (WAREHOUSE_MAP == null) {
            System.out.println("Bản đồ kho hàng chưa được khởi tạo");
            return;
        }

        System.out.println("\n=== BẢN ĐỒ KHO HÀNG (CẤU TRÚC MỚI) ===");
        System.out.println("Ký hiệu: · = lối đi, ■ = kệ hàng");

        // In header với số cột
        System.out.print("   ");
        for (int j = 0; j < WAREHOUSE_MAP[0].length; j++) {
            System.out.printf("%2d", j);
        }
        System.out.println();

        // In từng hàng
        for (int i = 0; i < WAREHOUSE_MAP.length; i++) {
            System.out.printf("%2d ", i);
            for (int j = 0; j < WAREHOUSE_MAP[i].length; j++) {
                if (WAREHOUSE_MAP[i][j] == 0) {
                    System.out.print(" ·"); // Ô đi được
                } else {
                    System.out.print(" ■"); // Kệ hàng
                }
            }

            // Ghi chú cho từng hàng
            if (i == 0) {
                System.out.print("  <- Lối đi chính/Counter");
            } else if (i % 3 == 0) {
                System.out.print("  <- Lối đi ngang");
            } else if (i % 3 == 1) {
                System.out.print("  <- Kệ hàng (tầng trên)");
            } else {
                System.out.print("  <- Kệ hàng (tầng dưới)");
            }

            System.out.println();
        }
    }

    public static void printSummary() {
        System.out.println("\n=== TỔNG QUAN BÀI TOÁN ===");
        System.out.println("Cấu trúc kho: " + SHELVES + " khối kệ x " + TIERS + " tầng x " + SLOTS + " ô");
        System.out.println("Số robot: " + ROBOTS + " (sức chứa: " + CAPACITY + " mỗi robot)");
        System.out.println("Mặt hàng: " + WAREHOUSE.size() + " loại trong kho, " + REQUIRE.size() + " loại cần lấy");

        // Tính tổng tải trọng cần thiết
        int totalRequiredLoad = 0;
        for (Merchandise item : REQUIRE) {
            totalRequiredLoad += item.getQuantity();
        }

        int totalCapacity = ROBOTS * CAPACITY;
        System.out.println("Tải trọng: " + totalRequiredLoad + "/" + totalCapacity +
                " (" + String.format("%.1f", (totalRequiredLoad * 100.0 / totalCapacity)) + "% sức chứa)");

        if (totalRequiredLoad > totalCapacity) {
            System.out.println("CẢNH BÁO: Tổng tải trọng vượt quá sức chứa robot!");
        }
    }
}