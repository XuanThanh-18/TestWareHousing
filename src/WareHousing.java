import java.util.ArrayList;

/**
 * Lớp WareHousing quản lý kho hàng và vị trí của các mặt hàng trong kho
 * Phiên bản hoàn chỉnh cho cấu trúc kho mới: mỗi ô hàng có 1 điểm tiếp cận duy nhất
 */
public class WareHousing {

    /**
     * Thiết lập kho hàng chính từ dữ liệu Params
     * Đây là phương thức chính được gọi từ Individual.java
     * @return Danh sách mặt hàng trong kho với vị trí và điểm tiếp cận
     */
    public static ArrayList<Merchandise> setWareHousing() {
        System.out.println("=== ĐANG THIẾT LẬP KHO HÀNG ===");

        if (Params.WAREHOUSE == null || Params.WAREHOUSE.isEmpty()) {
            System.out.println("CẢNH BÁO: Không có dữ liệu kho hàng từ Params. Tạo kho mẫu.");
            return createSampleWarehouse();
        }

        ArrayList<Merchandise> warehousing = new ArrayList<>();

        // Tạo bản đồ kho hàng để tính điểm tiếp cận
        WarehouseMap warehouseMap = createWarehouseMap();

        // Xử lý từng mặt hàng từ Params.WAREHOUSE
        for (Merchandise sourceItem : Params.WAREHOUSE) {
            try {
                // Tạo mặt hàng mới cho kho
                Merchandise warehouseItem = new Merchandise();
                warehouseItem.setName(sourceItem.getName());
                warehouseItem.setQuantity(sourceItem.getQuantity());

                // Xử lý vị trí
                if (sourceItem.getPosition() != null) {
                    warehouseItem.setPosition(sourceItem.getPosition().copy());
                } else {
                    // Tạo vị trí mặc định nếu không có
                    Position defaultPosition = generateDefaultPosition(warehousing.size() + 1);
                    warehouseItem.setPosition(defaultPosition);
                    System.out.println("Cảnh báo: Mặt hàng " + sourceItem.getName() +
                            " không có vị trí, gán vị trí mặc định: " + defaultPosition);
                }

                // Tính toán điểm tiếp cận cho mặt hàng
                warehouseItem.calculateAccessPoint(warehouseMap);

                // Kiểm tra tính hợp lệ của vị trí
                if (validateItemPosition(warehouseItem, warehouseMap)) {
                    warehousing.add(warehouseItem);
                } else {
                    System.out.println("CẢNH BÁO: Vị trí của mặt hàng " + warehouseItem.getName() +
                            " không hợp lệ, bỏ qua.");
                }

            } catch (Exception e) {
                System.out.println("Lỗi khi xử lý mặt hàng " + sourceItem.getName() + ": " + e.getMessage());
            }
        }

        // Sao chép các vị trí thay thế từ dữ liệu gốc
        copyAlternativePositions(warehousing, Params.WAREHOUSE, warehouseMap);

        // Kiểm tra và báo cáo kết quả
        validateAndReport(warehousing);

        System.out.println("✓ Hoàn thành thiết lập kho hàng với " + warehousing.size() + " mặt hàng");
        return warehousing;
    }

    /**
     * Tạo bản đồ kho hàng từ thông tin cấu hình
     */
    private static WarehouseMap createWarehouseMap() {
        if (Params.WAREHOUSE_MAP != null) {
            return new WarehouseMap(Params.WAREHOUSE_MAP);
        } else {
            return WarehouseMap.createMapFromWarehouse(Params.SHELVES, Params.SLOTS);
        }
    }

    /**
     * Tạo vị trí mặc định cho mặt hàng dựa trên chỉ số
     */
    private static Position generateDefaultPosition(int index) {
        if (Params.TIERS <= 0 || Params.SLOTS <= 0) {
            return new Position(1, 1, 1);
        }

        // Tính toán vị trí dựa trên chỉ số
        int totalSlotsPerShelf = Params.TIERS * Params.SLOTS;
        int shelf = ((index - 1) / totalSlotsPerShelf) + 1;
        int remaining = (index - 1) % totalSlotsPerShelf;
        int tier = (remaining / Params.SLOTS) + 1;
        int slot = (remaining % Params.SLOTS) + 1;

        // Đảm bảo không vượt quá giới hạn
        shelf = Math.min(shelf, Params.SHELVES);
        tier = Math.min(tier, Params.TIERS);
        slot = Math.min(slot, Params.SLOTS);

        return new Position(shelf, tier, slot);
    }

    /**
     * Kiểm tra tính hợp lệ của vị trí mặt hàng
     */
    private static boolean validateItemPosition(Merchandise item, WarehouseMap map) {
        if (item.getPosition() == null) {
            return false;
        }

        Position pos = item.getPosition();

        // Kiểm tra phạm vi hợp lệ
        if (pos.getShelf() < 1 || pos.getShelf() > Params.SHELVES ||
                pos.getTier() < 1 || pos.getTier() > Params.TIERS ||
                pos.getSlot() < 1 || pos.getSlot() > Params.SLOTS) {
            return false;
        }

        // Kiểm tra xem có thể tiếp cận được không
        return item.isAccessible(map);
    }

    /**
     * Sao chép các vị trí thay thế từ dữ liệu gốc
     */
    private static void copyAlternativePositions(ArrayList<Merchandise> warehousing,
                                                 ArrayList<Merchandise> sourceWarehouse,
                                                 WarehouseMap map) {
        for (Merchandise warehouseItem : warehousing) {
            for (Merchandise sourceItem : sourceWarehouse) {
                if (warehouseItem.getName().equals(sourceItem.getName())) {
                    // Sao chép các vị trí thay thế nếu có
                    for (Position altPos : sourceItem.getAllPositions()) {
                        if (!altPos.equals(warehouseItem.getPosition())) {
                            warehouseItem.addAlternativePosition(altPos.copy());
                        }
                    }
                    break;
                }
            }
        }
    }

    /**
     * Kiểm tra và báo cáo kết quả thiết lập kho hàng
     */
    private static void validateAndReport(ArrayList<Merchandise> warehousing) {
        System.out.println("\n=== BÁO CÁO THIẾT LẬP KHO HÀNG ===");

        int totalQuantity = 0;
        int accessibleItems = 0;
        ArrayList<String> itemTypes = new ArrayList<>();

        for (Merchandise item : warehousing) {
            totalQuantity += item.getQuantity();

            if (item.getAccessPoint() != null) {
                accessibleItems++;
            }

            if (!itemTypes.contains(item.getName())) {
                itemTypes.add(item.getName());
            }
        }

        System.out.println("- Tổng số mặt hàng: " + warehousing.size());
        System.out.println("- Tổng số lượng: " + totalQuantity);
        System.out.println("- Số loại mặt hàng: " + itemTypes.size());
        System.out.println("- Mặt hàng có thể tiếp cận: " + accessibleItems + "/" + warehousing.size());

        // In chi tiết từng mặt hàng
        System.out.println("\n=== CHI TIẾT MẶT HÀNG ===");
        for (int i = 0; i < warehousing.size(); i++) {
            Merchandise item = warehousing.get(i);
            String accessStatus = (item.getAccessPoint() != null) ? "✓" : "✗";
            System.out.printf("%-2d. %-15s: %3d đơn vị tại %s %s\n",
                    (i+1), item.getName(), item.getQuantity(),
                    item.getPosition(), accessStatus);

            // Hiển thị điểm tiếp cận
            if (item.getAccessPoint() != null) {
                System.out.println("    Điểm tiếp cận: " + item.getAccessPoint());
            }

            // Hiển thị vị trí thay thế nếu có
            if (item.getAllPositions().size() > 1) {
                System.out.println("    Có " + (item.getAllPositions().size() - 1) + " vị trí thay thế");
            }
        }
    }

    /**
     * Tạo kho hàng mẫu cho test khi không có dữ liệu
     */
    public static ArrayList<Merchandise> createSampleWarehouse() {
        System.out.println("Tạo kho hàng mẫu...");
        ArrayList<Merchandise> warehousing = new ArrayList<>();

        // Tạo bản đồ mẫu
        WarehouseMap map = WarehouseMap.createMapFromWarehouse(4, 6); // 4 kệ, 6 ô mỗi tầng

        // Tạo các mặt hàng mẫu
        Merchandise[] sampleItems = {
                new Merchandise("Laptop", 10, new Position(1, 1, 1)),
                new Merchandise("Mouse", 5, new Position(1, 1, 2)),
                new Merchandise("Keyboard", 8, new Position(1, 2, 1)),
                new Merchandise("Monitor", 3, new Position(1, 2, 2)),
                new Merchandise("Printer", 2, new Position(2, 1, 1)),
                new Merchandise("Scanner", 4, new Position(2, 1, 2)),
                new Merchandise("Tablet", 6, new Position(2, 2, 1)),
                new Merchandise("Charger", 7, new Position(2, 2, 2))
        };

        for (Merchandise item : sampleItems) {
            item.calculateAccessPoint(map);
            warehousing.add(item);
        }

        System.out.println("✓ Đã tạo kho mẫu với " + warehousing.size() + " mặt hàng");
        return warehousing;
    }

    /**
     * Tính vị trí của ô trong kệ từ chỉ số tuyến tính
     * @param i Chỉ số của ô (bắt đầu từ 1)
     * @return Vị trí (shelf, tier, slot)
     */
    public static Position cellPositionInShelf(int i) {
        if (Params.TIERS <= 0 || Params.SLOTS <= 0) {
            System.out.println("CẢNH BÁO: TIERS hoặc SLOTS không hợp lệ, sử dụng vị trí mặc định.");
            return new Position(1, 1, 1);
        }

        // Đảm bảo i >= 1
        i = Math.max(1, i);

        try {
            int totalSlotsPerShelf = Params.TIERS * Params.SLOTS;

            // Tính shelf (kệ)
            int shelf = ((i - 1) / totalSlotsPerShelf) + 1;

            // Tính vị trí trong kệ
            int positionInShelf = (i - 1) % totalSlotsPerShelf;

            // Tính tier (tầng)
            int tier = (positionInShelf / Params.SLOTS) + 1;

            // Tính slot (ô)
            int slot = (positionInShelf % Params.SLOTS) + 1;

            return new Position(shelf, tier, slot);

        } catch (ArithmeticException e) {
            System.out.println("Lỗi tính toán vị trí ô: " + e.getMessage());
            return new Position(1, 1, 1);
        }
    }

    /**
     * Tính chỉ số tuyến tính từ vị trí trong kho
     * @param shelf Kệ
     * @param tier Tầng
     * @param slot Ô
     * @return Chỉ số của ô trong kho (bắt đầu từ 1)
     */
    public static int cellPositionInWarehouse(int shelf, int tier, int slot) {
        if (Params.SLOTS <= 0 || Params.TIERS <= 0) {
            System.out.println("CẢNH BÁO: SLOTS hoặc TIERS không hợp lệ, trả về 1.");
            return 1;
        }

        // Đảm bảo các giá trị >= 1
        shelf = Math.max(1, shelf);
        tier = Math.max(1, tier);
        slot = Math.max(1, slot);

        try {
            // Tính vị trí tuyến tính
            int totalSlotsPerShelf = Params.TIERS * Params.SLOTS;
            int positionInShelf = (tier - 1) * Params.SLOTS + (slot - 1);
            return (shelf - 1) * totalSlotsPerShelf + positionInShelf + 1;

        } catch (ArithmeticException e) {
            System.out.println("Lỗi tính toán chỉ số ô: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Tìm mặt hàng trong kho dựa trên tên
     * @param name Tên mặt hàng
     * @param warehousing Kho hàng
     * @return Mặt hàng tìm thấy hoặc null nếu không tìm thấy
     */
    public static Merchandise findMerchandiseByName(String name, ArrayList<Merchandise> warehousing) {
        if (name == null || name.trim().isEmpty() || warehousing == null) {
            return null;
        }

        for (Merchandise item : warehousing) {
            if (item.getName() != null && item.getName().equals(name.trim())) {
                return item;
            }
        }
        return null;
    }

    /**
     * Tìm tất cả mặt hàng cùng tên trong kho
     * @param name Tên mặt hàng
     * @param warehousing Kho hàng
     * @return Danh sách mặt hàng cùng tên
     */
    public static ArrayList<Merchandise> findAllMerchandiseByName(String name, ArrayList<Merchandise> warehousing) {
        ArrayList<Merchandise> found = new ArrayList<>();

        if (name == null || name.trim().isEmpty() || warehousing == null) {
            return found;
        }

        for (Merchandise item : warehousing) {
            if (item.getName() != null && item.getName().equals(name.trim())) {
                found.add(item);
            }
        }

        return found;
    }

    /**
     * Tính tổng số lượng mặt hàng trong kho
     * @param warehousing Kho hàng
     * @return Tổng số lượng
     */
    public static int getTotalQuantity(ArrayList<Merchandise> warehousing) {
        if (warehousing == null) {
            return 0;
        }

        int total = 0;
        for (Merchandise item : warehousing) {
            total += item.getQuantity();
        }
        return total;
    }

    /**
     * Tính tổng số lượng của một loại mặt hàng cụ thể
     * @param name Tên mặt hàng
     * @param warehousing Kho hàng
     * @return Tổng số lượng của mặt hàng đó
     */
    public static int getTotalQuantityByName(String name, ArrayList<Merchandise> warehousing) {
        if (name == null || warehousing == null) {
            return 0;
        }

        int total = 0;
        for (Merchandise item : warehousing) {
            if (item.getName() != null && item.getName().equals(name)) {
                total += item.getQuantity();
            }
        }
        return total;
    }

    /**
     * In thông tin tổng quan về kho hàng
     * @param warehousing Kho hàng
     */
    public static void printWarehouseInfo(ArrayList<Merchandise> warehousing) {
        if (warehousing == null || warehousing.isEmpty()) {
            System.out.println("=== KHO HÀNG TRỐNG ===");
            return;
        }

        System.out.println("=== THÔNG TIN KHO HÀNG ===");
        System.out.println("Số vị trí mặt hàng: " + warehousing.size());
        System.out.println("Tổng số lượng: " + getTotalQuantity(warehousing));

        // Thống kê theo loại mặt hàng
        ArrayList<String> itemTypes = new ArrayList<>();
        for (Merchandise item : warehousing) {
            if (!itemTypes.contains(item.getName())) {
                itemTypes.add(item.getName());
            }
        }

        System.out.println("Số loại mặt hàng: " + itemTypes.size());

        System.out.println("\n=== CHI TIẾT THEO LOẠI ===");
        for (String type : itemTypes) {
            int positions = 0;
            int totalQty = 0;

            for (Merchandise item : warehousing) {
                if (item.getName().equals(type)) {
                    positions++;
                    totalQty += item.getQuantity();
                }
            }

            System.out.printf("- %-15s: %3d vị trí, %4d đơn vị\n", type, positions, totalQty);
        }

        // Thống kê theo kệ
        System.out.println("\n=== PHÂN BỔ THEO KỆ ===");
        for (int shelf = 1; shelf <= Params.SHELVES; shelf++) {
            int itemsInShelf = 0;
            int quantityInShelf = 0;

            for (Merchandise item : warehousing) {
                if (item.getPosition().getShelf() == shelf) {
                    itemsInShelf++;
                    quantityInShelf += item.getQuantity();
                }
            }

            if (itemsInShelf > 0) {
                System.out.printf("Kệ %d: %d mặt hàng, %d đơn vị\n", shelf, itemsInShelf, quantityInShelf);
            }
        }
    }

    /**
     * Kiểm tra tính hợp lệ của kho hàng
     * @param warehousing Kho hàng cần kiểm tra
     * @return true nếu hợp lệ, false nếu không
     */
    public static boolean validateWarehouse(ArrayList<Merchandise> warehousing) {
        if (warehousing == null) {
            System.out.println("CẢNH BÁO: Kho hàng null");
            return false;
        }

        if (warehousing.isEmpty()) {
            System.out.println("CẢNH BÁO: Kho hàng trống");
            return false;
        }

        boolean isValid = true;

        for (int i = 0; i < warehousing.size(); i++) {
            Merchandise item = warehousing.get(i);

            if (item == null) {
                System.out.println("CẢNH BÁO: Mặt hàng tại vị trí " + i + " là null");
                isValid = false;
                continue;
            }

            if (item.getName() == null || item.getName().trim().isEmpty()) {
                System.out.println("CẢNH BÁO: Mặt hàng tại vị trí " + i + " có tên trống");
                isValid = false;
            }

            if (item.getQuantity() <= 0) {
                System.out.println("CẢNH BÁO: Mặt hàng " + item.getName() + " có số lượng <= 0");
                isValid = false;
            }

            if (item.getPosition() == null) {
                System.out.println("CẢNH BÁO: Mặt hàng " + item.getName() + " không có vị trí");
                isValid = false;
            }
        }

        return isValid;
    }

    /**
     * Sao chép kho hàng (deep copy)
     * @param original Kho hàng gốc
     * @return Bản sao của kho hàng
     */
    public static ArrayList<Merchandise> copyWarehouse(ArrayList<Merchandise> original) {
        if (original == null) {
            return new ArrayList<>();
        }

        ArrayList<Merchandise> copy = new ArrayList<>();

        for (Merchandise item : original) {
            if (item != null) {
                Merchandise newItem = new Merchandise(
                        item.getName(),
                        item.getQuantity(),
                        item.getPosition() != null ? item.getPosition().copy() : new Position()
                );

                // Sao chép điểm tiếp cận
                if (item.getAccessPoint() != null) {
                    newItem.setAccessPoint(item.getAccessPoint().copy());
                }

                // Sao chép vị trí thay thế
                for (Position altPos : item.getAllPositions()) {
                    if (!altPos.equals(item.getPosition())) {
                        newItem.addAlternativePosition(altPos.copy());
                    }
                }

                copy.add(newItem);
            }
        }

        return copy;
    }

    /**
     * Tối ưu hóa kho hàng - loại bỏ duplicate và sắp xếp
     * @param warehousing Kho hàng cần tối ưu
     * @return Kho hàng đã được tối ưu
     */
    public static ArrayList<Merchandise> optimizeWarehouse(ArrayList<Merchandise> warehousing) {
        if (warehousing == null || warehousing.isEmpty()) {
            return new ArrayList<>();
        }

        ArrayList<Merchandise> optimized = new ArrayList<>();

        // Loại bỏ null items
        for (Merchandise item : warehousing) {
            if (item != null && item.getName() != null && !item.getName().trim().isEmpty()) {
                optimized.add(item);
            }
        }

        // Sắp xếp theo shelf, tier, slot
        optimized.sort((a, b) -> {
            if (a.getPosition() == null && b.getPosition() == null) return 0;
            if (a.getPosition() == null) return 1;
            if (b.getPosition() == null) return -1;

            int shelfCompare = Integer.compare(a.getPosition().getShelf(), b.getPosition().getShelf());
            if (shelfCompare != 0) return shelfCompare;

            int tierCompare = Integer.compare(a.getPosition().getTier(), b.getPosition().getTier());
            if (tierCompare != 0) return tierCompare;

            return Integer.compare(a.getPosition().getSlot(), b.getPosition().getSlot());
        });

        return optimized;
    }

    /**
     * Xuất thông tin kho hàng ra định dạng CSV
     * @param warehousing Kho hàng
     * @return Chuỗi CSV
     */
    public static String exportToCSV(ArrayList<Merchandise> warehousing) {
        if (warehousing == null || warehousing.isEmpty()) {
            return "Name,Quantity,Shelf,Tier,Slot,AccessPoint\n";
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Name,Quantity,Shelf,Tier,Slot,AccessPoint\n");

        for (Merchandise item : warehousing) {
            csv.append(item.getName()).append(",");
            csv.append(item.getQuantity()).append(",");
            csv.append(item.getPosition().getShelf()).append(",");
            csv.append(item.getPosition().getTier()).append(",");
            csv.append(item.getPosition().getSlot()).append(",");

            if (item.getAccessPoint() != null) {
                csv.append("\"").append(item.getAccessPoint().toString()).append("\"");
            } else {
                csv.append("null");
            }

            csv.append("\n");
        }

        return csv.toString();
    }

    /**
     * Lấy thống kê nhanh về kho hàng
     * @param warehousing Kho hàng
     * @return Chuỗi thống kê
     */
    public static String getWarehouseStats(ArrayList<Merchandise> warehousing) {
        if (warehousing == null || warehousing.isEmpty()) {
            return "Kho hàng trống";
        }

        int totalItems = warehousing.size();
        int totalQuantity = getTotalQuantity(warehousing);
        int uniqueTypes = 0;
        ArrayList<String> types = new ArrayList<>();

        for (Merchandise item : warehousing) {
            if (!types.contains(item.getName())) {
                types.add(item.getName());
                uniqueTypes++;
            }
        }

        return String.format("Kho hàng: %d vị trí, %d đơn vị, %d loại mặt hàng",
                totalItems, totalQuantity, uniqueTypes);
    }
}