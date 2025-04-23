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
        Merchandise counter = new Merchandise("Counter", 0, cellPositionInShelf(0));

        // Gán vị trí cho mỗi mặt hàng trong kho
        for (int i = 0; i < listMerchandise.size(); i++) {
            Merchandise merchandise = new Merchandise();
            merchandise.setName(listMerchandise.get(i).getName());
            merchandise.setQuantity(listMerchandise.get(i).getQuantity());

            // Tính vị trí trong kho
            Position position = cellPositionInShelf(i + 1);
            merchandise.setPosition(position);

            warehousing.add(merchandise);
        }

        // In thông tin kho hàng
        System.out.println("Đã thiết lập kho hàng với " + warehousing.size() + " món hàng:");
        for (int i = 0; i < Math.min(5, warehousing.size()); i++) {
            System.out.println("- " + warehousing.get(i));
        }
        if (warehousing.size() > 5) {
            System.out.println("... và " + (warehousing.size() - 5) + " món hàng khác");
        }

        return warehousing;
    }

    /**
     * Tính vị trí của ô trong kệ
     * @param i Chỉ số của ô
     * @return Vị trí (shelf, tier, slot)
     */
    public static Position cellPositionInShelf(int i) {
        // Cách tính vị trí (shelf, tier, slot) từ chỉ số i
        int shelf = (i - 1) / (Params.TIERS * Params.SLOTS);
        int tier = (i - (shelf * Params.TIERS * Params.SLOTS) - 1) / Params.SLOTS;
        int slot = i - shelf * Params.TIERS * Params.SLOTS - tier * Params.SLOTS - 1;

        Position position = new Position(shelf, tier, slot);
        return position;
    }

    /**
     * Tính chỉ số của ô từ vị trí trong kho
     * @param shelf Kệ
     * @param tier Tầng
     * @param slot Ô
     * @return Chỉ số của ô trong kho
     */
    public static int cellPositionInWarehouse(int shelf, int tier, int slot) {
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