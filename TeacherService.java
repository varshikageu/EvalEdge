import java.sql.*;
import java.time.*;

public class TeacherService {

    // CREATE SLOTS (AUTO 30 MIN)
    public void addSlots(int teacherId, String day, String date, String start, String end) {
        try (Connection con = DBConnection.getConnection()) {

            LocalTime startTime = LocalTime.parse(start);
            LocalTime endTime = LocalTime.parse(end);

            while (startTime.isBefore(endTime)) {
                LocalTime slotEnd = startTime.plusMinutes(30);

                String query = "INSERT INTO slots (teacher_id, day,slot_date, start_time, end_time, is_booked) VALUES (?, ?, ?, ?, ?, FALSE)";
                PreparedStatement ps = con.prepareStatement(query);

                ps.setInt(1, teacherId);
                ps.setString(2, day);
               ps.setDate(3, java.sql.Date.valueOf(date));
                ps.setTime(4, Time.valueOf(startTime));
                ps.setTime(5, Time.valueOf(slotEnd));

                ps.executeUpdate();

                startTime = slotEnd;
            }

            System.out.println("Slots added successfully!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // DELETE SLOT
    public void deleteSlot(int slotId) {
        try (Connection con = DBConnection.getConnection()) {

            String query = "DELETE FROM slots WHERE slot_id=?";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setInt(1, slotId);

            ps.executeUpdate();
            System.out.println("Slot deleted!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // UPDATE SLOT
    public void updateSlot(int slotId, String start, String end) {
        try (Connection con = DBConnection.getConnection()) {

            String query = "UPDATE slots SET start_time=?, end_time=? WHERE slot_id=?";
            PreparedStatement ps = con.prepareStatement(query);

            ps.setTime(1, Time.valueOf(start));
            ps.setTime(2, Time.valueOf(end));
            ps.setInt(3, slotId);

            ps.executeUpdate();
            System.out.println("Slot updated!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public int addTeacher(String name) {
    int id = -1;
    try (Connection con = DBConnection.getConnection()) {

        String query = "INSERT INTO teachers (name) VALUES (?)";
        PreparedStatement ps = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, name);
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            id = rs.getInt(1);
        }

    } catch (Exception e) {
        e.printStackTrace();
    }
    return id;
}
}