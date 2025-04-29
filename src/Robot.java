import java.util.ArrayList;

/**
 * Lớp Robot đại diện cho một robot trong kho hàng
 * Robot có thể di chuyển và thu thập các mặt hàng với giới hạn sức chứa
 */
public class Robot {
    String nameRobot;           // Tên/mã định danh của robot
    int capacity = Params.CAPACITY;  // Sức chứa tối đa của robot
    ArrayList<Merchandise> shoppingCart = new ArrayList<>();  // Giỏ hàng chứa các mặt hàng đã lấy
    private Position startPosition; // Vị trí xuất phát của robot
    private Position currentPosition; // Vị trí hiện tại của robot

    /**
     * Khởi tạo một robot mới với tên chỉ định
     * @param nameRobot Tên của robot
     */
    public Robot(String nameRobot) {
        this.nameRobot = nameRobot;
        this.startPosition = new Position(0, 0, 0); // Mặc định là [0,0]
        this.currentPosition = this.startPosition.copy(); // Ban đầu, vị trí hiện tại trùng với vị trí xuất phát
    }

    /**
     * Khởi tạo một robot mới với tên và vị trí xuất phát
     * @param nameRobot Tên của robot
     * @param startPosition Vị trí xuất phát
     */
    public Robot(String nameRobot, Position startPosition) {
        this.nameRobot = nameRobot;
        this.startPosition = startPosition;
        this.currentPosition = startPosition.copy(); // Ban đầu, vị trí hiện tại trùng với vị trí xuất phát
    }

    /**
     * Đặt vị trí xuất phát cho robot
     * @param position Vị trí xuất phát mới
     */
    public void setStartPosition(Position position) {
        this.startPosition = position;
        this.currentPosition = position.copy(); // Cập nhật cả vị trí hiện tại khi đặt vị trí xuất phát
    }

    /**
     * Lấy vị trí xuất phát của robot
     * @return Vị trí xuất phát
     */
    public Position getStartPosition() {
        return startPosition;
    }

    /**
     * Lấy vị trí hiện tại của robot
     * @return Vị trí hiện tại
     */
    public Position getCurrentPosition() {
        return currentPosition;
    }

    /**
     * Đặt vị trí hiện tại của robot
     * @param position Vị trí hiện tại mới
     */
    public void setCurrentPosition(Position position) {
        this.currentPosition = position;
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
     * Tính tổng quãng đường đi của robot từ vị trí xuất phát qua tất cả mặt hàng
     * và quay lại vị trí xuất phát
     * @param warehousing Kho hàng chứa thông tin vị trí
     * @return Tổng quãng đường
     */
    public float calculateTotalDistance(ArrayList<Merchandise> warehousing) {
        if (shoppingCart.isEmpty()) return 0;

        float totalDistance = 0;
        Position currentPos = startPosition.copy();

        // Đặt vị trí hiện tại của DistanceCalculator về vị trí xuất phát
        DistanceCalculator.setCurrentRobotPosition(currentPos);

        // Đi đến từng mặt hàng trong giỏ hàng
        for (Merchandise item : shoppingCart) {
            // Tìm vị trí của mặt hàng trong kho
            Merchandise warehouseItem = findItemInWarehouse(item, warehousing);
            if (warehouseItem != null) {
                // Tính khoảng cách đến mặt hàng tiếp theo
                float distance = DistanceCalculator.calculateDistance(currentPos, warehouseItem.getPosition());
                totalDistance += distance;
                // Cập nhật vị trí hiện tại
                currentPos = warehouseItem.getPosition();
                // Cập nhật vị trí hiện tại của robot
                this.setCurrentPosition(currentPos);
            }
        }

        // Quay lại vị trí xuất phát
        float returnDistance = DistanceCalculator.calculateDistance(currentPos, startPosition);
        totalDistance += returnDistance;

        // Cập nhật vị trí hiện tại của robot về vị trí xuất phát sau khi hoàn thành
        this.setCurrentPosition(startPosition.copy());

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
        return "{" + nameRobot + " start:" + startPosition + " current:" + currentPosition + " cart:" + shoppingCart + "}";
    }
}