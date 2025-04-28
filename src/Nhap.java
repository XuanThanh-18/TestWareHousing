import java.util.ArrayList;
import java.util.List;

public class Nhap {
    public static void main(String[] args) {
        Params.ReadParams();
        WarehouseMap warehouseMap;
        if (Params.WAREHOUSE_MAP != null) {
            warehouseMap = new WarehouseMap(Params.WAREHOUSE_MAP);
        } else {
            warehouseMap = WarehouseMap.createMapFromWarehouse(Params.SHELVES, Params.SLOTS);
        }
        if (Params.REQUIRE == null || Params.REQUIRE.isEmpty()) {
            System.out.println("CẢNH BÁO: Không có mặt hàng nào cần lấy. Hãy kiểm tra file input.");
            return;
        }
        // In bản đồ kho hàng
        System.out.println("\n=== BẢN ĐỒ KHO HÀNG ===");
        warehouseMap.printMap();

        // Vẽ đường đi
        ArrayList<int[]> path = warehouseMap.findShortestPath(warehouseMap.positionToCoordinates(start)[0],
                warehouseMap.positionToCoordinates(start)[1],warehouseMap.positionToCoordinates(end)[0],
                warehouseMap.positionToCoordinates(end)[1]);
        if (path == null || path.isEmpty()) {
            System.out.println("Không có đường đi nào để hiển thị.");
            return;
        }
        System.out.println("\n=== ĐƯỜNG ĐI ===");
        warehouseMap.printPathOnMap(path);

        System.out.println("Khoảng cách thực tế: " + warehouseMap.calculateActualDistance(start, end));
    }
}
