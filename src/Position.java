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
     * Khởi tạo vị trí với các tham số cụ thể
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
     * Tính khoảng cách Manhattan giữa hai vị trí
     * @param other Vị trí khác
     * @return Khoảng cách Manhattan
     */
    public float distanceTo(Position other) {
        // Tính khoảng cách Manhattan (|x1-x2| + |y1-y2|)
        int xDiff = Math.abs(this.x - other.x);
        int yDiff = Math.abs(this.y - other.y);

        // Tính thêm khoảng cách theo tầng
        float tierDistance = 0;
        final float TIER_DISTANCE = 0.5f;  // Khoảng cách giữa các tầng

        try {
            if (this.shelf == other.shelf && this.slot == other.slot) {
                // Nếu cùng kệ và cùng ô, chỉ tính khoảng cách theo tầng
                tierDistance = TIER_DISTANCE * Math.abs(this.tier - other.tier);
            } else {
                // Khác kệ hoặc khác ô, tính tổng khoảng cách di chuyển theo tầng
                tierDistance = TIER_DISTANCE * Math.max(0, (this.tier + other.tier - 2));
            }
        } catch (Exception e) {
            System.out.println("Lỗi khi tính khoảng cách theo tầng: " + e.getMessage());
            // Trả về giá trị an toàn
            tierDistance = 0;
        }

        return xDiff + yDiff + tierDistance;
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
     * Chuyển đổi vị trí thành chỉ số trong mảng kho hàng
     * @return Chỉ số trong mảng
     */
    public int toIndex() {
        try {
            // Đảm bảo các tham số Params không bằng 0 trước khi tính toán
            int tiersParam = Math.max(1, Params.TIERS);
            int slotsParam = Math.max(1, Params.SLOTS);

            return shelf * (tiersParam * slotsParam) + tier * slotsParam + slot;
        } catch (Exception e) {
            System.out.println("Lỗi khi tính chỉ số: " + e.getMessage());
            return 0; // Trả về giá trị mặc định an toàn
        }
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

    /**
     * Đặt giá trị shelf
     * @param shelf Giá trị shelf mới
     */
    public void setShelf(int shelf) {
        this.shelf = shelf;
        calculateCoordinates();
    }

    /**
     * Lấy giá trị tier
     * @return Giá trị tier
     */
    public int getTier() {
        return tier;
    }

    /**
     * Đặt giá trị tier
     * @param tier Giá trị tier mới
     */
    public void setTier(int tier) {
        this.tier = tier;
    }

    /**
     * Lấy giá trị slot
     * @return Giá trị slot
     */
    public int getSlot() {
        return slot;
    }

    /**
     * Đặt giá trị slot
     * @param slot Giá trị slot mới
     */
    public void setSlot(int slot) {
        this.slot = slot;
        // Cập nhật lại tọa độ y
        this.y = this.slot;
    }

    @Override
    public String toString() {
        return "{ " + shelf + " " + tier + " " + slot + " ~ " + x + " " + y + " }";
    }
}