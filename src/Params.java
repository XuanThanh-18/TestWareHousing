import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Lớp Params chứa các tham số và dữ liệu cấu hình cho bài toán
 * Cập nhật để tương thích với cấu trúc kho mới: mỗi ô hàng có 1 điểm tiếp cận duy nhất
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
    static int VNS_MAX_ITERATIONS = 30;
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
                        quantity = Integer.parseInt(parts[1]);

                        // Kiểm tra xem có đủ số lượng trong kho không
                        boolean sufficientQuantity = false;
                        for (Merchandise item : warehouse) {
                            if (item.getName().equals(name) && item.getQuantity() >= quantity) {
                                sufficientQuantity = true;
                                break;
                            }
                        }

                        if (sufficientQuantity) {
                            Merchandise merchandise = new Merchandise(name, quantity);
                            require.add(merchandise);
                        } else {
                            System.out.println("Cảnh báo: Không đủ số lượng cho sản phẩm " + name +
                                    " (yêu cầu: " + quantity + ")");
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

                System.out.println("Đã đọc bản đồ kho hàng từ file: " + rows + "x" + cols);
            } else {
                // Tạo bản đồ mặc định theo cấu trúc mới
                createDefaultMapForNewStructure();
            }

            // Cập nhật tham số
            REQUIRE_MACHANDISE = require.size();
            ROBOTS = Math.max(1, ROBOTS);
            WAREHOUSE = warehouse;
            REQUIRE = require;

            // In thông tin cấu hình
            printConfigurationInfo();

        } catch (FileNotFoundException e) {
            throw new RuntimeException("Không tìm thấy file: " + pathname, e);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi đọc file: " + pathname, e);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Lỗi định dạng số trong file: " + pathname, e);
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

        System.out.println("Đã tạo bản đồ kho hàng mặc định (cấu trúc mới): " + rows + "x" + cols);
    }

    /**
     * In thông tin cấu hình
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

        // Thống kê mặt hàng
        int totalWarehouseQuantity = 0;
        int totalRequiredQuantity = 0;

        for (Merchandise item : WAREHOUSE) {
            totalWarehouseQuantity += item.getQuantity();
        }

        for (Merchandise item : REQUIRE) {
            totalRequiredQuantity += item.getQuantity();
        }

        System.out.println("- Tổng số lượng hàng trong kho: " + totalWarehouseQuantity);
        System.out.println("- Tổng số lượng hàng cần lấy: " + totalRequiredQuantity);
        System.out.println("- Tỷ lệ sử dụng kho: " + String.format("%.1f", (totalRequiredQuantity * 100.0 / totalWarehouseQuantity)) + "%");
    }

    /**
     * Tính số ô hàng trong kho
     */
    public static int calculateTotalCells() {
        return SHELVES * TIERS * SLOTS;
    }

    /**
     * In danh sách mặt hàng trong kho
     */
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

    /**
     * In danh sách mặt hàng cần lấy
     */
    public static void printRequiredItems() {
        System.out.println("\n=== DANH SÁCH MẶT HÀNG CẦN LẤY ===");
        for (int i = 0; i < REQUIRE.size(); i++) {
            Merchandise m = REQUIRE.get(i);
            System.out.println((i+1) + ". " + m.getName() + ": " + m.getQuantity() + " đơn vị");
        }
    }

    /**
     * In bản đồ kho hàng
     */
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

    /**
     * Kiểm tra tính hợp lệ của cấu hình
     */
    public static boolean validateConfiguration() {
        if (SHELVES <= 0 || TIERS <= 0 || SLOTS <= 0) {
            System.out.println("CẢNH BÁO: Tham số kho hàng không hợp lệ");
            return false;
        }

        if (ROBOTS <= 0 || CAPACITY <= 0) {
            System.out.println("CẢNH BÁO: Tham số robot không hợp lệ");
            return false;
        }

        if (WAREHOUSE == null || WAREHOUSE.isEmpty()) {
            System.out.println("CẢNH BÁO: Không có mặt hàng nào trong kho");
            return false;
        }

        if (REQUIRE == null || REQUIRE.isEmpty()) {
            System.out.println("CẢNH BÁO: Không có mặt hàng nào cần lấy");
            return false;
        }

        return true;
    }

    /**
     * In thông tin tổng quan
     */
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