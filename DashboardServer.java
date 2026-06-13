import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DashboardServer {

    private static final int PORT = 8080;
    private static final DashboardService svc = new DashboardService();

    public static void start() throws Exception {

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
System.out.println("teacher-slots route active");
        // Serve index.html at root
        server.createContext("/",            DashboardServer::serveIndex);
        // Serve StudentDashboard.html at /dashboard
        server.createContext("/dashboard",   DashboardServer::serveDashboard);
server.createContext("/teacher-dashboard", DashboardServer::serveTeacherDashboard);
        // New endpoints
        
        server.createContext("/student-bookings", DashboardServer::handleStudentBookings);
        server.createContext("/update-booking-feedback", DashboardServer::handleUpdateBookingFeedback);
        server.createContext("/teacher-slots", DashboardServer::handleTeacherSlots);
        server.createContext("/teacher-bookings", DashboardServer::handleTeacherBookings);
        server.createContext("/add-teacher", DashboardServer::handleAddTeacher);
        server.createContext("/add-student", DashboardServer::handleAddStudent);
        server.createContext("/add-slots",   DashboardServer::handleAddSlots);
        server.createContext("/book-slot",   DashboardServer::handleBookSlot);
        server.createContext("/slots",       DashboardServer::handleGetSlots);

        // Existing endpoints
        server.createContext("/login",       DashboardServer::handleLogin);
        server.createContext("/pbl-detail",  DashboardServer::handlePblDetail);

        server.setExecutor(null);
        server.start();

        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   EvalEdge is running!                       ║");
        System.out.println("║   Open: http://localhost:8080/               ║");
        System.out.println("║   Press Ctrl+C to stop the server.           ║");
        System.out.println("╚══════════════════════════════════════════════╝");
    }

    // ── Serve index.html at / ─────────────────────────────────────────────────
    private static void serveIndex(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("GET")) { ex.sendResponseHeaders(405, -1); return; }

       String path = ex.getRequestURI().getPath();
if (!path.equals("/") && !path.equals("/index.html")) {
    ex.sendResponseHeaders(404, -1);
    return;
}

        File htmlFile = new File("index.html");
        if (!htmlFile.exists()) htmlFile = new File("../index.html");

        if (!htmlFile.exists()) {
            String msg = "index.html not found in: " + new File(".").getAbsolutePath();
            ex.sendResponseHeaders(404, msg.length());
            ex.getResponseBody().write(msg.getBytes());
            ex.getResponseBody().close();
            return;
        }

        byte[] bytes = Files.readAllBytes(htmlFile.toPath());
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(200, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    // ── Serve StudentDashboard.html at /dashboard ─────────────────────────────
    private static void serveDashboard(HttpExchange ex) throws IOException {
    if (!ex.getRequestMethod().equals("GET")) { 
        ex.sendResponseHeaders(405, -1); 
        return; 
    }

    File htmlFile = new File("StudentDashboard.html");
    if (!htmlFile.exists()) htmlFile = new File("../StudentDashboard.html");

    if (!htmlFile.exists()) {
        String msg = "StudentDashboard.html not found in: " + new File(".").getAbsolutePath();
        ex.sendResponseHeaders(404, msg.length());
        ex.getResponseBody().write(msg.getBytes());
        ex.getResponseBody().close();
        return;
    }

    byte[] bytes = Files.readAllBytes(htmlFile.toPath());
    ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
    ex.sendResponseHeaders(200, bytes.length);
    ex.getResponseBody().write(bytes);
    ex.getResponseBody().close();
}
private static void serveTeacherDashboard(HttpExchange ex) throws IOException {
    if (!ex.getRequestMethod().equals("GET")) { 
        ex.sendResponseHeaders(405, -1); 
        return; 
    }

    File htmlFile = new File("TeacherDashboard.html");
    if (!htmlFile.exists()) htmlFile = new File("../TeacherDashboard.html");

    if (!htmlFile.exists()) {
        String msg = "TeacherDashboard.html not found in: " + new File(".").getAbsolutePath();
        ex.sendResponseHeaders(404, msg.length());
        ex.getResponseBody().write(msg.getBytes());
        ex.getResponseBody().close();
        return;
    }

    byte[] bytes = Files.readAllBytes(htmlFile.toPath());
    ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
    ex.sendResponseHeaders(200, bytes.length);
    ex.getResponseBody().write(bytes);
    ex.getResponseBody().close();
}
    // ── POST /login ───────────────────────────────────────────────────────────
    private static void handleLogin(HttpExchange ex) throws IOException {
        setCORS(ex);
        if (ex.getRequestMethod().equals("OPTIONS")) { ex.sendResponseHeaders(200, -1); return; }

        try {
            Map<String, String> params;
            if (ex.getRequestMethod().equalsIgnoreCase("GET")) {
                String query = ex.getRequestURI().getQuery();
                params = parseQuery(query != null ? query : "");
            } else {
                String body = new String(ex.getRequestBody().readAllBytes());
                params = parseQuery(body);
            }

            int studentId   = Integer.parseInt(params.getOrDefault("studentId", "0"));
            String password = params.getOrDefault("password", "");

            Map<String, String> profile = svc.loginStudent(studentId, password);
            if (profile == null) {
                sendJSON(ex, 401, "{\"error\":\"Invalid credentials\"}");
                return;
            }

            List<Map<String, String>> pbls = svc.getStudentPBLs(studentId);
            sendJSON(ex, 200, buildLoginJSON(profile, pbls));

        } catch (Exception e) {
            sendJSON(ex, 500, "{\"error\":\"" + escJson(e.getMessage()) + "\"}");
        }
    }

    // ── GET /pbl-detail ───────────────────────────────────────────────────────
    private static void handlePblDetail(HttpExchange ex) throws IOException {
        setCORS(ex);
        if (ex.getRequestMethod().equals("OPTIONS")) { ex.sendResponseHeaders(200, -1); return; }

        try {
            String query = ex.getRequestURI().getQuery();
            Map<String, String> params = parseQuery(query != null ? query : "");

            int studentId = Integer.parseInt(params.getOrDefault("studentId", "0"));
            int pblId     = Integer.parseInt(params.getOrDefault("pblId",     "0"));

            List<Map<String, String>>               phases = svc.getPhaseDetails(studentId, pblId);
            Map<Integer, List<Map<String, String>>> meets  = svc.getMeetAttendance(studentId, pblId);

            sendJSON(ex, 200, buildDetailJSON(phases, meets));

        } catch (Exception e) {
            sendJSON(ex, 500, "{\"error\":\"" + escJson(e.getMessage()) + "\"}");
        }
    }

    // ── POST /add-teacher ─────────────────────────────────────────────────────
    private static void handleAddTeacher(HttpExchange ex) throws IOException {
        setCORS(ex);
        if (ex.getRequestMethod().equals("OPTIONS")) { ex.sendResponseHeaders(200, -1); return; }
        try {
            String body = new String(ex.getRequestBody().readAllBytes());
            Map<String, String> params = parseQuery(body);
            String name = params.getOrDefault("name", "");
            TeacherService ts = new TeacherService();
            int id = ts.addTeacher(name);
            sendJSON(ex, 200, "{\"teacher_id\":" + id + "}");
        } catch (Exception e) {
            sendJSON(ex, 500, "{\"error\":\"" + escJson(e.getMessage()) + "\"}");
        }
    }

    // ── POST /add-student ─────────────────────────────────────────────────────
    private static void handleAddStudent(HttpExchange ex) throws IOException {
        setCORS(ex);
        if (ex.getRequestMethod().equals("OPTIONS")) { ex.sendResponseHeaders(200, -1); return; }
        try {
            String body = new String(ex.getRequestBody().readAllBytes());
            Map<String, String> params = parseQuery(body);
            String name = params.getOrDefault("name", "");
            StudentService ss = new StudentService();
            int id = ss.addStudent(name);
            sendJSON(ex, 200, "{\"student_id\":" + id + "}");
        } catch (Exception e) {
            sendJSON(ex, 500, "{\"error\":\"" + escJson(e.getMessage()) + "\"}");
        }
    }

    // ── POST /add-slots ───────────────────────────────────────────────────────
    private static void handleAddSlots(HttpExchange ex) throws IOException {
        setCORS(ex);
        if (ex.getRequestMethod().equals("OPTIONS")) { ex.sendResponseHeaders(200, -1); return; }
        try {
            String body = new String(ex.getRequestBody().readAllBytes());
            Map<String, String> params = parseQuery(body);
            int teacherId = Integer.parseInt(params.getOrDefault("teacherId", "0"));
            String day    = params.getOrDefault("day",   "");
           String date = params.getOrDefault("date", "");
            String start  = params.getOrDefault("start", "");
            String end    = params.getOrDefault("end",   "");
            TeacherService ts = new TeacherService();
            ts.addSlots(teacherId, day, date, start, end);
            sendJSON(ex, 200, "{\"status\":\"ok\"}");
        } catch (Exception e) {
            sendJSON(ex, 500, "{\"error\":\"" + escJson(e.getMessage()) + "\"}");
        }
    }

    // ── POST /book-slot ───────────────────────────────────────────────────────
    private static void handleBookSlot(HttpExchange ex) throws IOException {
        setCORS(ex);
        if (ex.getRequestMethod().equals("OPTIONS")) { ex.sendResponseHeaders(200, -1); return; }
        try {
            String body = new String(ex.getRequestBody().readAllBytes());
            Map<String, String> params = parseQuery(body);
            int studentId = Integer.parseInt(params.getOrDefault("studentId", "0"));
            int slotId    = Integer.parseInt(params.getOrDefault("slotId",    "0"));
            BookingService bs = new BookingService();
            bs.bookSlot(studentId, slotId);
            sendJSON(ex, 200, "{\"status\":\"booked\"}");
        } catch (Exception e) {
            sendJSON(ex, 500, "{\"error\":\"" + escJson(e.getMessage()) + "\"}");
        }
    }

    // ── GET /slots ────────────────────────────────────────────────────────────
    private static void handleGetSlots(HttpExchange ex) throws IOException {
        setCORS(ex);
        if (ex.getRequestMethod().equals("OPTIONS")) { ex.sendResponseHeaders(200, -1); return; }
        try {
            String query = ex.getRequestURI().getQuery();
            Map<String, String> params = parseQuery(query != null ? query : "");
            int teacherId = Integer.parseInt(params.getOrDefault("teacherId", "0"));
            String day    = params.getOrDefault("day", "");

            Connection con = DBConnection.getConnection();
           String sql = "SELECT slot_id, day, slot_date, start_time, end_time FROM slots WHERE teacher_id=? AND day=? AND is_booked=FALSE";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, teacherId);
            ps.setString(2, day);
            ResultSet rs = ps.executeQuery();

            StringBuilder sb = new StringBuilder("{\"slots\":[");
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                sb.append("{\"slot_id\":").append(rs.getInt("slot_id"))
             .append(",\"day\":\"").append(rs.getString("day")).append("\"")
             .append(",\"slot_date\":\"").append(rs.getString("slot_date")).append("\"")
                  .append(",\"start_time\":\"").append(rs.getTime("start_time")).append("\"")
                  .append(",\"end_time\":\"").append(rs.getTime("end_time")).append("\"}");
                first = false;
            }
            sb.append("]}");
            con.close();
            sendJSON(ex, 200, sb.toString());

        } catch (Exception e) {
            sendJSON(ex, 500, "{\"error\":\"" + escJson(e.getMessage()) + "\"}");
        }
    }
private static void handleTeacherBookings(HttpExchange ex) throws IOException {
    setCORS(ex);
    if (ex.getRequestMethod().equals("OPTIONS")) { 
        ex.sendResponseHeaders(200, -1); 
        return; 
    }

    try {
        String query = ex.getRequestURI().getQuery();
        Map<String, String> params = parseQuery(query != null ? query : "");

        int teacherId = Integer.parseInt(params.getOrDefault("teacherId", "0"));

        Connection con = DBConnection.getConnection();

       String sql =
"SELECT b.booking_id, s.slot_id, s.day, s.start_time, s.end_time, " +
"st.name AS student_name, b.remarks, b.feedback, b.attendance " +
"FROM bookings b " +
"JOIN slots s ON b.slot_id = s.slot_id " +
"JOIN students st ON b.student_id = st.student_id " +
"WHERE s.teacher_id = ?";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, teacherId);

        ResultSet rs = ps.executeQuery();

        StringBuilder sb = new StringBuilder("{\"bookings\":[");

        boolean first = true;
        while (rs.next()) {
            if (!first) sb.append(",");

            sb.append("{")
              .append("\"booking_id\":").append(rs.getInt("booking_id")).append(",")
              .append("\"slot_id\":").append(rs.getInt("slot_id")).append(",")
              .append("\"day\":\"").append(rs.getString("day")).append("\",")
              .append("\"start_time\":\"").append(rs.getTime("start_time")).append("\",")
              .append("\"end_time\":\"").append(rs.getTime("end_time")).append("\",")
              .append("\"student_name\":\"").append(rs.getString("student_name")).append("\",")
.append("\"remarks\":\"").append(escJson(rs.getString("remarks"))).append("\",")
.append("\"feedback\":\"").append(escJson(rs.getString("feedback"))).append("\",")
.append("\"attendance\":").append(rs.getBoolean("attendance"))
              .append("}");

            first = false;
        }

        sb.append("]}");

        con.close();

        sendJSON(ex, 200, sb.toString());

    } catch (Exception e) {
        sendJSON(ex, 500, "{\"error\":\"" + e.getMessage() + "\"}");
    }
}private static void handleTeacherSlots(HttpExchange ex) throws IOException {
    setCORS(ex);
    if (ex.getRequestMethod().equals("OPTIONS")) {
        ex.sendResponseHeaders(200, -1);
        return;
    }

    try {
        String query = ex.getRequestURI().getQuery();
        Map<String, String> params = parseQuery(query != null ? query : "");

        int teacherId = Integer.parseInt(params.getOrDefault("teacherId", "0"));

        Connection con = DBConnection.getConnection();
String sql = "SELECT slot_id, day, slot_date, start_time, end_time, is_booked " +
             "FROM slots WHERE teacher_id=? ORDER BY slot_date, start_time";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, teacherId);

        ResultSet rs = ps.executeQuery();

        StringBuilder sb = new StringBuilder("{\"slots\":[");

        boolean first = true;
        while (rs.next()) {
            if (!first) sb.append(",");

            sb.append("{")
              .append("\"slot_id\":").append(rs.getInt("slot_id")).append(",")
              .append("\"day\":\"").append(rs.getString("day")).append("\",")
              .append("\"slot_date\":\"").append(rs.getDate("slot_date")).append("\",")
              .append("\"start_time\":\"").append(rs.getTime("start_time")).append("\",")
              .append("\"end_time\":\"").append(rs.getTime("end_time")).append("\",")
              .append("\"is_booked\":").append(rs.getBoolean("is_booked"))
              .append("}");

            first = false;
        }

        sb.append("]}");

        con.close();

        sendJSON(ex, 200, sb.toString());

    } catch (Exception e) {
        sendJSON(ex, 500, "{\"error\":\"" + e.getMessage() + "\"}");
    }
}
private static void handleUpdateBookingFeedback(HttpExchange ex) throws IOException {
    try {
        String body = new String(ex.getRequestBody().readAllBytes());
        Map<String, String> params = parseQuery(body);

        int bookingId = Integer.parseInt(params.get("bookingId"));
boolean attendance = Boolean.parseBoolean(params.get("attendance"));        String remarks = params.get("remarks");
        String feedback = params.get("feedback");

        Connection con = DBConnection.getConnection();

String sql = "UPDATE bookings SET attendance=?, remarks=?, feedback=? WHERE booking_id=?";        PreparedStatement ps = con.prepareStatement(sql);

        ps.setBoolean(1, attendance);
        ps.setString(2, remarks);
        ps.setString(3, feedback);
        ps.setInt(4, bookingId);

        ps.executeUpdate();

        sendJSON(ex, 200, "{\"status\":\"updated\"}");

    } catch (Exception e) {
        sendJSON(ex, 500, "{\"error\":\"" + e.getMessage() + "\"}");
    }
}
private static void handleStudentBookings(HttpExchange ex) throws IOException {
    try {
        String query = ex.getRequestURI().getQuery();
        Map<String, String> params = parseQuery(query);

        int studentId = Integer.parseInt(params.get("studentId"));

        Connection con = DBConnection.getConnection();

        String sql =
            "SELECT b.booking_id, s.day, s.slot_date, s.start_time, s.end_time, " +
            "b.attendance, b.remarks, b.feedback " +
            "FROM bookings b " +
            "JOIN slots s ON b.slot_id = s.slot_id " +
            "WHERE b.student_id=?";

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, studentId);

        ResultSet rs = ps.executeQuery();

        StringBuilder sb = new StringBuilder("{\"bookings\":[");
        boolean first = true;

        while (rs.next()) {
            if (!first) sb.append(",");

            sb.append("{")
              .append("\"booking_id\":").append(rs.getInt("booking_id")).append(",")
              .append("\"date\":\"").append(rs.getString("slot_date")).append("\",")
              .append("\"day\":\"").append(rs.getString("day")).append("\",")
              .append("\"time\":\"").append(rs.getTime("start_time")).append(" - ").append(rs.getTime("end_time")).append("\",")
              .append("\"attended\":").append(rs.getBoolean("attendance")).append(",")
              .append("\"remarks\":\"").append(escJson(rs.getString("remarks"))).append("\",")
              .append("\"feedback\":\"").append(escJson(rs.getString("feedback"))).append("\"")
              .append("}");

            first = false;
        }

        sb.append("]}");

        sendJSON(ex, 200, sb.toString());

    } catch (Exception e) {
        sendJSON(ex, 500, "{\"error\":\"" + e.getMessage() + "\"}");
    }
}
    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void setCORS(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendJSON(HttpExchange ex, int code, String json) throws IOException {
        byte[] bytes = json.getBytes("UTF-8");
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(code, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    private static Map<String, String> parseQuery(String query) throws Exception {
        Map<String, String> map = new LinkedHashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(URLDecoder.decode(kv[0], "UTF-8"),
                        URLDecoder.decode(kv[1], "UTF-8"));
            }
        }
        return map;
    }

    private static String buildLoginJSON(Map<String, String> profile,
                                         List<Map<String, String>> pbls) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"student\":").append(mapToJson(profile));
        sb.append(",\"pbls\":[");
        for (int i = 0; i < pbls.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(mapToJson(pbls.get(i)));
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String buildDetailJSON(List<Map<String, String>> phases,
                                          Map<Integer, List<Map<String, String>>> meets) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"phases\":[");
        for (int i = 0; i < phases.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(mapToJson(phases.get(i)));
        }
        sb.append("],\"meets\":{");
        boolean first = true;
        for (Map.Entry<Integer, List<Map<String, String>>> e : meets.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":[");
            List<Map<String, String>> list = e.getValue();
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(mapToJson(list.get(i)));
            }
            sb.append("]");
            first = false;
        }
        sb.append("}}");
        return sb.toString();
    }

    private static String mapToJson(Map<String, String> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escJson(e.getKey())).append("\":");
            sb.append("\"").append(escJson(e.getValue())).append("\"");
            first = false;
        }
        return sb.append("}").toString();
    }

    private static String escJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}