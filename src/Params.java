import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Lớp Params chứa các tham số và dữ liệu cấu hình cho bài toán
 * Đọc dữ liệu từ file và khởi tạo các tham số
 */
public class Params {
    // Số lượng kệ
    static int SHELVES;
    // Số tầng
    static int TIERS;
    // Số ô trên mỗi tầng
    static int SLOTS;
    // Số lượng robot
    static int ROBOTS;
    // Sức chứa của mỗi robot
    static int CAPACITY;
    // Số lượng mặt hàng cần lấy
    static int REQUIRE_MACHANDISE;
    // Tổng số ô trong kho
    static int COUNT;
    // Danh sách mặt hàng trong kho
    static ArrayList<Merchandise> WAREHOUSE;
    // Danh sách mặt hàng cần lấy
    static ArrayList<Merchandise> REQUIRE;
    // Bản đồ kho hàng
    static int[][] WAREHOUSE_MAP;

    /**
     * Tham số cho thuật toán PSO
     */
    // Kích thước đàn
    static int PSO_SWARM_SIZE = 30;
    // Số vòng lặp tối đa
    static int PSO_MAX_ITERATIONS = 100;
    // Trọng số quán tính
    static double PSO_INERTIA_WEIGHT = 0.7;
    // Hệ số nhận thức
    static double PSO_COGNITIVE_COEFFICIENT = 1.5;
    // Hệ số xã hội
    static double PSO_SOCIAL_COEFFICIENT = 1.5;

    /**
     * Tham số cho thuật toán VNS
     */
    // Số vòng lặp tối đa
    static int VNS_MAX_ITERATIONS = 30;
    // Số lượng lân cận tối đa
    static int VNS_MAX_NEIGHBORHOODS = 3;

    /**
     * Đọc tham số từ file
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
            if (parts.length > 2) {
                SHELVES = Integer.parseInt(parts[0]);
                TIERS = Integer.parseInt(parts[1]);
                SLOTS = Integer.parseInt(parts[2]);
                REQUIRE_MACHANDISE = Integer.parseInt(parts[3]);
                ROBOTS = Integer.parseInt(parts[4]);
                CAPACITY = Integer.parseInt(parts[5]);
                COUNT = TIERS * SHELVES * SLOTS;
            }

            // Đọc bản đồ kho hàng nếu có
            boolean readingMap = false;
            boolean readingWarehouse = false;
            boolean readingRequire = false;
            ArrayList<int[]> mapRows = new ArrayList<>();

            while ((line = br.readLine()) != null) {
                // Kiểm tra các phần của file
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

                // Nếu đang đọc bản đồ
                if (readingMap) {
                    String[] mapRow = line.trim().split(" ");
                    int[] row = new int[mapRow.length];
                    for (int i = 0; i < mapRow.length; i++) {
                        row[i] = Integer.parseInt(mapRow[i]);
                    }
                    mapRows.add(row);
                }
                // Nếu đang đọc thông tin kho hàng
                else if (readingWarehouse) {
                    parts = line.split(" ");
                    if (parts.length >= 2) {
                        name = parts[0];
                        quantity = Integer.parseInt(parts[1]);
                        Merchandise merchandise = new Merchandise(name, quantity);

                        // Nếu có thông tin vị trí
                        if (parts.length >= 5) {
                            int shelf = Integer.parseInt(parts[2]);
                            int tier = Integer.parseInt(parts[3]);
                            int slot = Integer.parseInt(parts[4]);
                            Position position = new Position(shelf, tier, slot);
                            merchandise.setPosition(position);
                        }

                        warehouse.add(merchandise);
                    }
                }
                // Nếu đang đọc thông tin mặt hàng cần lấy
                else if (readingRequire) {
                    parts = line.split(" ");
                    if (parts.length >= 2) {
                        name = parts[0];
                        quantity = Integer.parseInt(parts[1]);
                        Merchandise merchandise = new Merchandise(name, quantity);
                        require.add(merchandise);
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
            } else {
                // Nếu không có bản đồ trong file, tạo bản đồ mặc định
                createDefaultMap();
            }

            // Kiểm tra số lượng mặt hàng yêu cầu
            // XÓA CẢNH BÁO VÀ CHỈ CẬP NHẬT THAM SỐ
            REQUIRE_MACHANDISE = require.size();
            ROBOTS = Math.max(1, ROBOTS); // Đảm bảo có ít nhất 1 robot

            WAREHOUSE = warehouse;
            REQUIRE = require;

            // In thông tin cấu hình
            System.out.println("Đã đọc file thành công:");
            System.out.println("- Số kệ: " + SHELVES);
            System.out.println("- Số tầng: " + TIERS);
            System.out.println("- Số ô trên mỗi tầng: " + SLOTS);
            System.out.println("- Số lượng mặt hàng cần lấy: " + REQUIRE_MACHANDISE);
            System.out.println("- Số lượng robot: " + ROBOTS);
            System.out.println("- Sức chứa mỗi robot: " + CAPACITY);

            // In thông tin bản đồ
            if (WAREHOUSE_MAP != null) {
                System.out.println("- Bản đồ kho hàng: " + WAREHOUSE_MAP.length + "x" + WAREHOUSE_MAP[0].length);
            }

        } catch (FileNotFoundException e) {
            throw new RuntimeException("Không tìm thấy file: " + pathname, e);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi đọc file: " + pathname, e);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Lỗi định dạng số trong file: " + pathname, e);
        }
    }

    /**
     * Tạo bản đồ mặc định cho kho hàng
     */
    private static void createDefaultMap() {
        // Mỗi kệ chiếm 1 dòng và có 1 dòng lối đi
        int rows = SHELVES * 2 + 1;
        int cols = SLOTS;

        WAREHOUSE_MAP = new int[rows][cols];

        // Thiết lập các kệ hàng (dòng lẻ)
        for (int i = 1; i < rows; i += 2) {
            for (int j = 0; j < cols; j++) {
                WAREHOUSE_MAP[i][j] = 1; // Kệ hàng
            }
        }

        System.out.println("Đã tạo bản đồ kho hàng mặc định " + rows + "x" + cols);
    }

    /**
     * Tính số ô hàng (cell) trong kho
     * @return Tổng số ô trong kho
     */
    public static int calculateTotalCells() {
        return SHELVES * TIERS * SLOTS;
    }

    /**
     * In danh sách mặt hàng trong kho
     */
    public static void printWarehouse() {
        System.out.println("Danh sách mặt hàng trong kho:");
        for (Merchandise m : WAREHOUSE) {
            System.out.println("- " + m.getName() + ": " + m.getQuantity() + " đơn vị tại vị trí " + m.getPosition());
        }
    }

    /**
     * In danh sách mặt hàng cần lấy
     */
    public static void printRequiredItems() {
        System.out.println("Danh sách mặt hàng cần lấy:");
        for (Merchandise m : REQUIRE) {
            System.out.println("- " + m.getName() + ": " + m.getQuantity() + " đơn vị");
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

        System.out.println("Bản đồ kho hàng:");
        for (int i = 0; i < WAREHOUSE_MAP.length; i++) {
            for (int j = 0; j < WAREHOUSE_MAP[i].length; j++) {
                if (WAREHOUSE_MAP[i][j] == 0) {
                    System.out.print("_ "); // Ô đi được
                } else {
                    System.out.print("■ "); // Kệ hàng
                }
            }
            System.out.println();
        }
    }
}