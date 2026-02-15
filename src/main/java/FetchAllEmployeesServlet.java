import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/FetchAllEmployeesServlet")
public class FetchAllEmployeesServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        LocalDate today = LocalDate.now();

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                out.print("{\"error\": \"Database Connection Failed\"}");
                return;
            }

            if ("counts".equals(action)) {
                PreparedStatement psTotal = con.prepareStatement("SELECT COUNT(*) FROM users WHERE role='User'");
                ResultSet rsTotal = psTotal.executeQuery();
                int total = rsTotal.next() ? rsTotal.getInt(1) : 0;

                PreparedStatement psActive = con.prepareStatement("SELECT COUNT(*) FROM attendance WHERE work_date=?");
                psActive.setDate(1, Date.valueOf(today));
                ResultSet rsActive = psActive.executeQuery();
                int active = rsActive.next() ? rsActive.getInt(1) : 0;

                out.print("{\"total\": " + total + ", \"active\": " + active + "}");
            } else {
                Set<LocalDate> holidayDates = new HashSet<>();
                PreparedStatement hps = con.prepareStatement("SELECT holiday_date FROM holidays");
                ResultSet hrs = hps.executeQuery();
                while(hrs.next()) {
                    Date hDate = hrs.getDate("holiday_date");
                    if (hDate != null) holidayDates.add(hDate.toLocalDate());
                }

                // Query optimized for PostgreSQL
                String sql = "SELECT employee_id, fullname, email, designation, wfh_allowed, date_of_joining, " +
                             "(SELECT COUNT(*) FROM attendance WHERE user_email = users.email) as present_count " +
                             "FROM users WHERE role='User'";
                
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();

                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                
                while (rs.next()) {
                    if (!first) json.append(",");

                    // --- INDIVIDUAL WORKING DAYS CALCULATION ---
                    int individualWorkingDays = 0;
                    Date dojSql = rs.getDate("date_of_joining");
                    LocalDate empJoiningDate = (dojSql != null) ? dojSql.toLocalDate() : today;

                    LocalDate tempIter = empJoiningDate;
                    while (!tempIter.isAfter(today)) {
                        boolean isSunday = tempIter.getDayOfWeek().getValue() == 7;
                        boolean isHoliday = holidayDates.contains(tempIter);
                        if (!isSunday && !isHoliday) {
                            individualWorkingDays++;
                        }
                        tempIter = tempIter.plusDays(1);
                    }

                    int presentCount = rs.getInt("present_count");
                    int leavesTaken = individualWorkingDays - presentCount;
                    if (leavesTaken < 0) leavesTaken = 0;

                    // Building JSON with escaped values for safety
                    json.append("{")
                        .append("\"empId\":\"").append(clean(rs.getString("employee_id"))).append("\",")
                        .append("\"fullname\":\"").append(clean(rs.getString("fullname"))).append("\",")
                        .append("\"email\":\"").append(clean(rs.getString("email"))).append("\",")
                        .append("\"designation\":\"").append(clean(rs.getString("designation"))).append("\",")
                        .append("\"doj\":\"").append(rs.getString("date_of_joining") == null ? "" : rs.getString("date_of_joining")).append("\",")
                        .append("\"wfhAllowed\":").append(rs.getBoolean("wfh_allowed")).append(",")
                        .append("\"leavesTaken\":").append(leavesTaken)
                        .append("}");
                    
                    first = false;
                }
                json.append("]");
                out.print(json.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print("{\"error\": \"Database Error: " + e.getMessage().replace("\"", "'") + "\"}");
        }
        out.flush();
    }

    // Helper to prevent JSON breaking characters
    private String clean(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
