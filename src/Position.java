/**
 * Lớp Position đại diện cho vị trí trong không gian 3D của kho hàng
 * Vị trí bao gồm kệ (shelf), tầng (tier) và ô (slot)
 * Các tọa độ x, y được tính toán dựa trên giá trị shelf, tier và slot
 */
public class Position {
    private int shelf;  // Kệ hàng
    private int tier;   // Tầng
    private int slot;   // Ô
    public int x;       // Tọa độ x trên lưới
    public int y;       // Tọa độ y trên lưới
    /**
     * Khởi tạo vị trí mặc định (0,0,0)
     */
    public Position() {
        this.shelf = 0;
        this.tier = 0;
        this.slot = 0;
        calculateCoordinates();
    }

    /**
     * Khởi tạo Position với các tham số cụ thể
     * @param shelf Kệ hàng
     * @param tier Tầng
     * @param slot Ô
     */
    public Position(int shelf, int tier, int slot) {
        this.shelf = shelf;
        this.tier = tier;
        this.slot = slot;
        calculateCoordinates();
    }

    /**
     * Tính toán tọa độ x, y dựa trên vị trí shelf, tier, slot
     */
    private void calculateCoordinates() {
        // Tính toán tọa độ x, y dựa trên vị trí shelf, tier, slot
        this.y = slot;
        this.x = 2 * shelf - 1;
    }

    /**
     * Kiểm tra xem vị trí hiện tại có trùng với vị trí khác không
     * @param other Vị trí khác
     * @return true nếu trùng, false nếu không
     */
    public boolean equals(Position other) {
        return this.shelf == other.shelf &&
                this.tier == other.tier &&
                this.slot == other.slot;
    }

    /**
     * Tạo một bản sao của vị trí hiện tại
     * @return Bản sao của vị trí
     */
    public Position copy() {
        return new Position(this.shelf, this.tier, this.slot);
    }

    /**
     * Lấy giá trị shelf
     * @return Giá trị shelf
     */
    public int getShelf() {
        return shelf;
    }
    public int getTier() {
        return tier;
    }
    public int getSlot() {
        return slot;
    }
    @Override
    public String toString() {

        return "{ " + shelf + " " + tier + " " + slot + " ~ " + x + " " + y + " }";
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + shelf;
        result = prime * result + tier;
        result = prime * result + slot;
        return result;
    }
}