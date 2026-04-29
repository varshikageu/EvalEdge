public class Main {
    public static void main(String[] args) {
        try {
            DashboardServer.start();
            System.out.println("🚀 EvalEdge running at: http://localhost:8080/");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}