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

    /**
     * Tính khoảng cách từ mặt hàng này đến mặt hàng khác
     * @param other Mặt hàng khác
     * @return Khoảng cách giữa hai mặt hàng
     */
    public float distanceTo(Merchandise other) {
        if ((this.position.getShelf() == other.position.getShelf()) && (this.position.getSlot() == other.position.getSlot()))
            return LAYERDISTANCE * (Math.abs(this.position.getTier() - other.position.getTier()));

        return Math.min(this.position.x + other.position.x, 2 * (K + 2) - (this.position.x + other.position.x)) +
                Math.abs(this.position.y - other.position.y) +
                LAYERDISTANCE * (this.position.getTier() + other.position.getTier() - 2);
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

    /**
     * Tạo một bản sao của mặt hàng
     * @return Bản sao của mặt hàng
     */
    public Merchandise copy() {
        Merchandise copy = new Merchandise(this.name, this.quantity);
        if (this.position != null) {
            copy.setPosition(this.position.copy());
        }
        return copy;
    }
}