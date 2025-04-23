/**
 * Lớp Particle (Hạt) đại diện cho một hạt trong thuật toán PSO
 * Mỗi hạt chứa một giải pháp và thông tin về vị trí tốt nhất của hạt
 */
public class Particle {
    private Solution solution;           // Giải pháp hiện tại
    private Solution bestSolution;       // Giải pháp tốt nhất của hạt
    private double bestFitness;          // Độ thích nghi tốt nhất của hạt

    /**
     * Khởi tạo một hạt mới
     */
    public Particle() {
        this.bestFitness = Double.MAX_VALUE;
    }

    /**
     * Lấy giải pháp hiện tại của hạt
     * @return Giải pháp hiện tại
     */
    public Solution getSolution() {
        return solution;
    }

    /**
     * Đặt giải pháp hiện tại cho hạt
     * @param solution Giải pháp mới
     */
    public void setSolution(Solution solution) {
        this.solution = solution;
    }

    /**
     * Lấy giải pháp tốt nhất của hạt
     * @return Giải pháp tốt nhất
     */
    public Solution getBestSolution() {
        return bestSolution;
    }

    /**
     * Đặt giải pháp tốt nhất cho hạt
     * @param bestSolution Giải pháp tốt nhất mới
     */
    public void setBestSolution(Solution bestSolution) {
        this.bestSolution = bestSolution;
    }

    /**
     * Lấy độ thích nghi tốt nhất của hạt
     * @return Độ thích nghi tốt nhất
     */
    public double getBestFitness() {
        return bestFitness;
    }

    /**
     * Đặt độ thích nghi tốt nhất cho hạt
     * @param bestFitness Độ thích nghi tốt nhất mới
     */
    public void setBestFitness(double bestFitness) {
        this.bestFitness = bestFitness;
    }

    /**
     * Cập nhật vị trí tốt nhất của hạt nếu vị trí hiện tại tốt hơn
     * @param currentFitness Độ thích nghi hiện tại
     * @return true nếu vị trí được cập nhật, false nếu không
     */
    public boolean updateBestPosition(double currentFitness) {
        if (currentFitness < bestFitness) {
            bestFitness = currentFitness;
            bestSolution = new Solution(solution);
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Particle{fitness=" + (solution != null ? solution.getFitness() : "null") +
                ", bestFitness=" + bestFitness + "}";
    }
}