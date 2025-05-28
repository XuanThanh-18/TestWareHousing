import java.util.ArrayList;

/**
 * Lớp Merchandise đại diện cho một mặt hàng trong kho
 * Được đơn giản hóa cho cấu trúc kho mới: mỗi ô hàng có 1 điểm tiếp cận duy nhất
 */
public class Merchandise {
    private String name;
    private int quantity;
    private Position position = new Position();
    private ArrayList<Position> alternativePositions = new ArrayList<>();
    private Position accessPoint; // Điểm tiếp cận duy nhất của mặt hàng này

    // Getter/Setter cho accessPoint
    public Position getAccessPoint() {
        return accessPoint;
    }

    public void setAccessPoint(Position accessPoint) {
        this.accessPoint = accessPoint;
    }

    // Tính và cập nhật điểm tiếp cận duy nhất
    public void calculateAccessPoint(WarehouseMap map) {
        if (this.position != null) {
            int[] coords = map.positionToCoordinates(this.position);
            int[] accessCoords = map.findUniqueAccessPoint(coords[0], coords[1]);
            this.accessPoint = map.coordinatesToPosition(accessCoords[0], accessCoords[1]);
        }
    }

    public void addAlternativePosition(Position pos) {
        if (pos != null) {
            alternativePositions.add(pos);
        }
    }

    // Lấy tất cả vị trí (vị trí chính + vị trí thay thế)
    public ArrayList<Position> getAllPositions() {
        ArrayList<Position> allPositions = new ArrayList<>();
        allPositions.add(position);
        allPositions.addAll(alternativePositions);
        return allPositions;
    }

    // Constructors
    public Merchandise(String name, int quantity, Position position) {
        this.name = name;
        this.quantity = quantity;
        this.position = position;
    }

    public Merchandise(String name, int quantity) {
        this.name = name;
        this.quantity = quantity;
    }

    public Merchandise() {
    }

    // Getters và Setters
    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    // So sánh hai mặt hàng
    public boolean equals(Merchandise other) {
        return this.name.equals(other.name) && this.quantity == other.quantity;
    }

    /**
     * Chọn vị trí tối ưu dựa trên vị trí hiện tại
     * Đơn giản hóa vì mỗi vị trí chỉ có 1 điểm tiếp cận
     */
    public Position getOptimalPosition(Position currentPosition) {
        if (alternativePositions.isEmpty()) {
            return position;
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

    /**
     * Lấy điểm tiếp cận duy nhất cho vị trí chính
     */
    public Position getUniqueAccessPoint(WarehouseMap map) {
        if (accessPoint == null) {
            calculateAccessPoint(map);
        }
        return accessPoint;
    }

    /**
     * Kiểm tra xem mặt hàng có thể tiếp cận được không
     */
    public boolean isAccessible(WarehouseMap map) {
        if (position == null) return false;

        int[] coords = map.positionToCoordinates(position);
        int[] accessCoords = map.findUniqueAccessPoint(coords[0], coords[1]);

        return map.isWalkable(accessCoords[0], accessCoords[1]);
    }

    @Override
    public String toString() {
        return "{" + name + " " + quantity + "~" +
                position.getShelf() + " " + position.getTier() + " " + position.getSlot() +
                (accessPoint != null ? " AP:" + accessPoint.getShelf() + "," +
                        accessPoint.getTier() + "," + accessPoint.getSlot() : "") + "}";
    }

    /**
     * In thông tin chi tiết mặt hàng
     */
    public void printDetailedInfo(WarehouseMap map) {
        System.out.println("=== THÔNG TIN MẶT HÀNG ===");
        System.out.println("Tên: " + name);
        System.out.println("Số lượng: " + quantity);
        System.out.println("Vị trí: " + position);

        if (accessPoint == null) {
            calculateAccessPoint(map);
        }
        System.out.println("Điểm tiếp cận: " + accessPoint);

        int[] coords = map.positionToCoordinates(position);
        System.out.println("Tọa độ trên bản đồ: [" + coords[0] + ", " + coords[1] + "]");
        System.out.println("Có thể tiếp cận: " + isAccessible(map));

        if (!alternativePositions.isEmpty()) {
            System.out.println("Vị trí thay thế: " + alternativePositions.size() + " vị trí");
            for (int i = 0; i < alternativePositions.size(); i++) {
                System.out.println("  - Vị trí " + (i+1) + ": " + alternativePositions.get(i));
            }
        }
    }
}