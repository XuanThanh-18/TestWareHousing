import java.util.ArrayList;

/**
 * Lớp WareHousing quản lý kho hàng và vị trí của các mặt hàng trong kho
 */
public class WareHousing {

    /**
     * Thiết lập kho hàng với vị trí cho các mặt hàng
     * @return Danh sách mặt hàng trong kho với vị trí
     */
    public static ArrayList<Merchandise> setWareHousing() {
        ArrayList<Merchandise> warehousing = new ArrayList<>();
        ArrayList<Merchandise> listMerchandise = Params.WAREHOUSE;

        // Tạo counter tại vị trí 0
        Merchandise counter = new Merchandise("Counter", 0, new Position(1, 1, 1));

        // Đặt các mặt hàng vào kho với vị trí đã được xác định từ file input
        for (Merchandise item : listMerchandise) {
            Merchandise merchandise = new Merchandise();
            merchandise.setName(item.getName());
            merchandise.setQuantity(item.getQuantity());

            // Sử dụng vị trí đã được chỉ định trong file input
            if (item.getPosition() != null) {
                merchandise.setPosition(item.getPosition().copy());
            } else {
                System.out.println("Cảnh báo: Mặt hàng " + item.getName() + " không có vị trí, cung cấp vị trí mặc định.");
                // Đặt vị trí mặc định nếu không có
                merchandise.setPosition(new Position(1, 1, 1));
            }

            warehousing.add(merchandise);
        }

        // Tính toán điểm tiếp cận cho tất cả mặt hàng
        WarehouseMap warehouseMap;
        if (Params.WAREHOUSE_MAP != null) {
            warehouseMap = new WarehouseMap(Params.WAREHOUSE_MAP);
        } else {
            warehouseMap = WarehouseMap.createMapFromWarehouse(Params.SHELVES, Params.SLOTS);
        }

        for (Merchandise merchandise : warehousing) {
            merchandise.calculateAccessPoint(warehouseMap);
        }

        // In thông tin kho hàng
        System.out.println("Đã thiết lập kho hàng với " + warehousing.size() + " món hàng:");
        for (int i = 0; i < warehousing.size(); i++) {
            System.out.println("- " + warehousing.get(i) + " (điểm tiếp cận: " + warehousing.get(i).getAccessPoint() + ")");
        }

        return warehousing;
    }

    /**
     * Tính vị trí của ô trong kệ
     * @param i Chỉ số của ô
     * @return Vị trí (shelf, tier, slot)
     */
    public static Position cellPositionInShelf(int i) {
        // Kiểm tra trước khi tính toán để tránh chia cho 0
        if (Params.TIERS <= 0 || Params.SLOTS <= 0) {
            System.out.println("CẢNH BÁO: TIERS hoặc SLOTS bằng 0, sử dụng giá trị mặc định.");
            return new Position(1, 1, 1);
        }

        // Đảm bảo i luôn lớn hơn hoặc bằng 1
        i = Math.max(1, i);

        try {
            // Cách tính vị trí (shelf, tier, slot) từ chỉ số i
            int shelf = (i - 1) / (Params.TIERS * Params.SLOTS);
            int tier = (i - (shelf * Params.TIERS * Params.SLOTS) - 1) / Params.SLOTS;
            int slot = i - shelf * Params.TIERS * Params.SLOTS - tier * Params.SLOTS - 1;

            // Đảm bảo các giá trị luôn lớn hơn hoặc bằng 1
            shelf = Math.max(1, shelf);
            tier = Math.max(1, tier);
            slot = Math.max(1, slot);

            return new Position(shelf, tier, slot);
        } catch (ArithmeticException e) {
            System.out.println("Lỗi tính toán vị trí ô: " + e.getMessage());
            return new Position(1, 1, 1); // Trả về vị trí mặc định an toàn
        }
    }

    /**
     * Tính chỉ số của ô từ vị trí trong kho
     * @param shelf Kệ
     * @param tier Tầng
     * @param slot Ô
     * @return Chỉ số của ô trong kho
     */
    public static int cellPositionInWarehouse(int shelf, int tier, int slot) {
        // Kiểm tra trước khi tính toán để tránh lỗi không mong muốn
        if (Params.SLOTS <= 0 || Params.TIERS <= 0) {
            System.out.println("CẢNH BÁO: SLOTS hoặc TIERS bằng 0, trả về giá trị mặc định 1.");
            return 1;
        }

        // Đảm bảo các giá trị đầu vào hợp lệ
        shelf = Math.max(0, shelf);
        tier = Math.max(0, tier);
        slot = Math.max(0, slot);

        return shelf * (Params.SLOTS * Params.TIERS) + tier * Params.SLOTS + slot + 1;
    }

    /**
     * Tìm mặt hàng trong kho dựa trên tên
     * @param name Tên mặt hàng
     * @param warehousing Kho hàng
     * @return Mặt hàng tìm thấy hoặc null nếu không tìm thấy
     */
    public static Merchandise findMerchandiseByName(String name, ArrayList<Merchandise> warehousing) {
        for (Merchandise item : warehousing) {
            if (item.getName().equals(name)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Tính tổng số lượng mặt hàng trong kho
     * @param warehousing Kho hàng
     * @return Tổng số lượng
     */
    public static int getTotalQuantity(ArrayList<Merchandise> warehousing) {
        int total = 0;
        for (Merchandise item : warehousing) {
            total += item.getQuantity();
        }
        return total;
    }

    /**
     * In thông tin về kho hàng
     * @param warehousing Kho hàng
     */
    public static void printWarehouseInfo(ArrayList<Merchandise> warehousing) {
        System.out.println("=== THÔNG TIN KHO HÀNG ===");
        System.out.println("Số lượng mặt hàng: " + warehousing.size());
        System.out.println("Tổng số lượng: " + getTotalQuantity(warehousing));

        // Hiển thị số lượng theo loại mặt hàng
        ArrayList<String> types = new ArrayList<>();
        for (Merchandise item : warehousing) {
            if (!types.contains(item.getName())) {
                types.add(item.getName());
            }
        }

        System.out.println("Loại mặt hàng: " + types.size() + " loại");
        for (String type : types) {
            int count = 0;
            for (Merchandise item : warehousing) {
                if (item.getName().equals(type)) {
                    count++;
                }
            }
            System.out.println("- " + type + ": " + count + " vị trí");
        }
    }
}