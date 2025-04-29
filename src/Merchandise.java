import java.util.ArrayList;

/**
 * Lớp Merchandise đại diện cho một mặt hàng trong kho
 * Chứa thông tin về tên, số lượng và vị trí của mặt hàng
 */
public class Merchandise {
    public final int K = Params.SLOTS;
    public final float LAYERDISTANCE = 0.5f;
    private String name;
    private int quantity;
    private Position position = new Position();
    private ArrayList<Position> alternativePositions = new ArrayList<>();

    public void addAlternativePosition(Position pos) {
        if (pos != null) {
            alternativePositions.add(pos);
        }
    }

    // Phương thức lấy tất cả vị trí (vị trí chính + vị trí thay thế)
    public ArrayList<Position> getAllPositions() {
        ArrayList<Position> allPositions = new ArrayList<>();
        allPositions.add(position); // Vị trí chính
        allPositions.addAll(alternativePositions); // Vị trí thay thế
        return allPositions;
    }
    /**
     * Khởi tạo một mặt hàng với tên, số lượng và vị trí
     * @param name Tên mặt hàng
     * @param quantity Số lượng
     * @param position Vị trí
     */
    public Merchandise(String name, int quantity, Position position) {
        this.name = name;
        this.quantity = quantity;
        this.position = position;
    }

    /**
     * Khởi tạo một mặt hàng với tên và số lượng
     * @param name Tên mặt hàng
     * @param quantity Số lượng
     */
    public Merchandise(String name, int quantity) {
        this.name = name;
        this.quantity = quantity;
    }

    /**
     * Lấy vị trí của mặt hàng
     * @return Vị trí
     */
    public Position getPosition() {
        return position;
    }

    /**
     * Đặt vị trí cho mặt hàng
     * @param position Vị trí mới
     */
    public void setPosition(Position position) {
        this.position = position;
    }

    /**
     * Khởi tạo một mặt hàng rỗng
     */
    public Merchandise() {
    }

    @Override
    public String toString() {
        return "{" + name + " " + quantity + "~" + position.getShelf() + " " + position.getTier() + " " + position.getSlot() + "} ";
    }

    /**
     * Lấy tên mặt hàng
     * @return Tên mặt hàng
     */
    public String getName() {
        return name;
    }

    /**
     * Đặt tên cho mặt hàng
     * @param name Tên mới
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Lấy số lượng của mặt hàng
     * @return Số lượng
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Đặt số lượng cho mặt hàng
     * @param quantity Số lượng mới
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /**
     * So sánh hai mặt hàng
     * @param other Mặt hàng khác
     * @return true nếu hai mặt hàng có cùng tên và số lượng
     */
    public boolean equals(Merchandise other) {
        return this.name.equals(other.name) && this.quantity == other.quantity;
    }

    // Phương thức chọn vị trí tối ưu dựa trên vị trí hiện tại
    public Position getOptimalPosition(Position currentPosition) {
        if (alternativePositions.isEmpty()) {
            return position; // Nếu không có vị trí thay thế, trả về vị trí chính
        }

        // Tìm vị trí gần nhất với vị trí hiện tại
        Position bestPosition = position;
        float minDistance = DistanceCalculator.calculateManhattanDistance(currentPosition, position);

        for (Position altPos : alternativePositions) {
            float distance = DistanceCalculator.calculateManhattanDistance(currentPosition, altPos);
            if (distance < minDistance) {
                minDistance = distance;
                bestPosition = altPos;
            }
        }

        return bestPosition;
    }
}