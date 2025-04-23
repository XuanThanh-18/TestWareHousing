import java.util.ArrayList;

/**
 * Lớp Robot đại diện cho một robot trong kho hàng
 * Robot có thể di chuyển và thu thập các mặt hàng với giới hạn sức chứa
 */
public class Robot {
    String nameRobot;           // Tên/mã định danh của robot
    int capacity = Params.CAPACITY;  // Sức chứa tối đa của robot
    ArrayList<Merchandise> shoppingCart = new ArrayList<>();  // Giỏ hàng chứa các mặt hàng đã lấy

    /**
     * Khởi tạo một robot mới với tên chỉ định
     * @param nameRobot Tên của robot
     */
    public Robot(String nameRobot) {
        this.nameRobot = nameRobot;
    }

    /**
     * Kiểm tra xem robot có thể lấy thêm mặt hàng không
     * @param merchandise Mặt hàng cần kiểm tra
     * @return true nếu robot có thể lấy mặt hàng, false nếu không
     */
    public boolean canPickUp(Merchandise merchandise) {
        int totalQuantity = 0;
        for (Merchandise item : shoppingCart) {
            totalQuantity += item.getQuantity();
        }
        return totalQuantity + merchandise.getQuantity() <= capacity;
    }

    /**
     * Tính tổng khối lượng hiện tại trong giỏ hàng
     * @return Tổng khối lượng
     */
    public int getCurrentLoad() {
        int totalQuantity = 0;
        for (Merchandise item : shoppingCart) {
            totalQuantity += item.getQuantity();
        }
        return totalQuantity;
    }

    /**
     * Thêm một mặt hàng vào giỏ hàng của robot
     * @param merchandise Mặt hàng cần thêm
     * @return true nếu thêm thành công, false nếu vượt quá sức chứa
     */
    public boolean addToCart(Merchandise merchandise) {
        if (canPickUp(merchandise)) {
            shoppingCart.add(merchandise);
            return true;
        }
        return false;
    }

    /**
     * Xóa một mặt hàng khỏi giỏ hàng
     * @param index Vị trí của mặt hàng cần xóa
     * @return Mặt hàng đã xóa
     */
    public Merchandise removeFromCart(int index) {
        if (index >= 0 && index < shoppingCart.size()) {
            return shoppingCart.remove(index);
        }
        return null;
    }

    /**
     * Tính tổng quãng đường đi của robot từ vị trí counter qua tất cả mặt hàng
     * và quay lại counter
     * @param counter Vị trí counter
     * @param warehousing Kho hàng chứa thông tin vị trí
     * @return Tổng quãng đường
     */
    public float calculateTotalDistance(Position counter, ArrayList<Merchandise> warehousing) {
        if (shoppingCart.isEmpty()) return 0;

        float totalDistance = 0;
        Position currentPosition = counter;

        // Đi đến từng mặt hàng trong giỏ hàng
        for (Merchandise item : shoppingCart) {
            // Tìm vị trí của mặt hàng trong kho
            Merchandise warehouseItem = findItemInWarehouse(item, warehousing);
            if (warehouseItem != null) {
                // Tính khoảng cách đến mặt hàng tiếp theo
                float distance = warehouseItem.distanceTo(new Merchandise("", 0, currentPosition));
                totalDistance += distance;
                // Cập nhật vị trí hiện tại
                currentPosition = warehouseItem.getPosition();
            }
        }

        // Quay lại counter
        Merchandise lastItem = new Merchandise("", 0, currentPosition);
        Merchandise counterItem = new Merchandise("Counter", 0, counter);
        totalDistance += counterItem.distanceTo(lastItem);

        return totalDistance;
    }

    /**
     * Tìm mặt hàng trong kho hàng
     * @param item Mặt hàng cần tìm
     * @param warehousing Kho hàng
     * @return Mặt hàng trong kho (có vị trí) hoặc null nếu không tìm thấy
     */
    private Merchandise findItemInWarehouse(Merchandise item, ArrayList<Merchandise> warehousing) {
        for (Merchandise warehouseItem : warehousing) {
            if (warehouseItem.getName().equals(item.getName())) {
                return warehouseItem;
            }
        }
        return null;
    }

    /**
     * Xóa sạch giỏ hàng
     */
    public void clearCart() {
        shoppingCart.clear();
    }

    @Override
    public String toString() {
        return "{" + nameRobot + " " + shoppingCart + "}";
    }
}