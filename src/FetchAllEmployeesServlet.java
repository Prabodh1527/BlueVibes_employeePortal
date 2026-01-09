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
                while(hrs.next()) holidayDates.add(hrs.getDate("holiday_date").toLocalDate());

                // UPDATED SQL: Added date_of_joining so we can calculate leaves correctly for each person
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

                    // Default to today if DOJ is missing to prevent errors
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
                    // --------------------------------------------

                    int presentCount = rs.getInt("present_count");
                    int leavesTaken = individualWorkingDays - presentCount;
                    if (leavesTaken < 0) leavesTaken = 0;

                    json.append("{");
                    json.append("\"empId\":\"").append(rs.getString("employee_id")).append("\",");
                    json.append("\"fullname\":\"").append(rs.getString("fullname")).append("\",");
                    json.append("\"email\":\"").append(rs.getString("email")).append("\",");
                    json.append("\"designation\":\"").append(rs.getString("designation")).append("\",");
                    json.append("\"doj\":\"").append(rs.getString("date_of_joining")).append("\","); // Added DOJ field
                    json.append("\"wfhAllowed\":").append(rs.getBoolean("wfh_allowed")).append(",");
                    json.append("\"leavesTaken\":").append(leavesTaken);
                    json.append("}");
                    first = false;
                }
                json.append("]");
                out.print(json.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print("{\"error\": \"Database Error\"}");
        }
        out.flush();
    }
}