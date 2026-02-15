import java.io.*;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/WeeklyReportServlet")
public class WeeklyReportServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        String userEmail = (session != null) ? (String) session.getAttribute("userEmail") : null;
        
        if (userEmail == null) {
            response.sendRedirect("index.html");
            return;
        }

        String action = request.getParameter("action");
        if ("delete".equals(action)) {
            deleteReport(request, response, userEmail);
            return;
        }

        String[] reportIds = request.getParameterValues("reportId");
        String[] taskIds = request.getParameterValues("taskId");
        String[] taskDescs = request.getParameterValues("taskDesc");
        String[] customers = request.getParameterValues("customer");
        String[] statuses = request.getParameterValues("status");
        String[] percents = request.getParameterValues("percent");
        String[] startDates = request.getParameterValues("startDate");
        String[] endDates = request.getParameterValues("endDate");
        String[] comments = request.getParameterValues("comments");

        // PostgreSQL fix: use explicit DATE casting
        String insertSql = "INSERT INTO user_weekly_reports " +
                "(user_email, task_id, task_description, customer, status, percentage_completed, start_date, end_date, comments) " +
                "VALUES (?,?,?,?,?,?,CAST(? AS DATE),CAST(? AS DATE),?)";

        String updateSql = "UPDATE user_weekly_reports SET " +
                "task_id=?, task_description=?, customer=?, status=?, percentage_completed=?, " +
                "start_date=CAST(? AS DATE), end_date=CAST(? AS DATE), comments=? " +
                "WHERE report_id=? AND user_email=?";

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) throw new SQLException("Database connection failed");
            
            if (taskDescs != null) {
                for (int i = 0; i < taskDescs.length; i++) {
                    if (taskDescs[i] == null || taskDescs[i].trim().isEmpty()) continue;

                    int percentValue = 0;
                    try { percentValue = Integer.parseInt(percents[i]); } catch (Exception ignored) {}

                    if (reportIds != null && i < reportIds.length && "0".equals(reportIds[i])) {
                        try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                            ps.setString(1, userEmail);
                            ps.setString(2, taskIds[i]);
                            ps.setString(3, taskDescs[i]);
                            ps.setString(4, customers[i]);
                            ps.setString(5, statuses[i]);
                            ps.setInt(6, percentValue);
                            ps.setString(7, (startDates[i] == null || startDates[i].isEmpty()) ? null : startDates[i]);
                            ps.setString(8, (endDates[i] == null || endDates[i].isEmpty()) ? null : endDates[i]);
                            ps.setString(9, comments[i]);
                            ps.executeUpdate();
                        }
                    } else if (reportIds != null && i < reportIds.length) {
                        try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                            ps.setString(1, taskIds[i]);
                            ps.setString(2, taskDescs[i]);
                            ps.setString(3, customers[i]);
                            ps.setString(4, statuses[i]);
                            ps.setInt(5, percentValue);
                            ps.setString(6, (startDates[i] == null || startDates[i].isEmpty()) ? null : startDates[i]);
                            ps.setString(7, (endDates[i] == null || endDates[i].isEmpty()) ? null : endDates[i]);
                            ps.setString(8, comments[i]);
                            ps.setInt(9, Integer.parseInt(reportIds[i]));
                            ps.setString(10, userEmail);
                            ps.executeUpdate();
                        }
                    }
                }
            }
            response.sendRedirect("weeklyreport.html?status=success");
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("weeklyreport.html?status=error");
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String action = request.getParameter("action");
        if (!"fetchMyReports".equals(action)) return;

        HttpSession session = request.getSession(false);
        String userEmail = (session != null) ? (String) session.getAttribute("userEmail") : null;
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        if (userEmail == null) {
            response.getWriter().print("[]");
            return;
        }

        String sql = "SELECT * FROM user_weekly_reports WHERE user_email=? ORDER BY created_at ASC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userEmail);
            try (ResultSet rs = ps.executeQuery()) {
                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    json.append("{")
                        .append("\"reportId\":").append(rs.getInt("report_id")).append(",")
                        .append("\"taskId\":\"").append(clean(rs.getString("task_id"))).append("\",")
                        .append("\"taskDesc\":\"").append(clean(rs.getString("task_description"))).append("\",")
                        .append("\"customer\":\"").append(clean(rs.getString("customer"))).append("\",")
                        .append("\"status\":\"").append(clean(rs.getString("status"))).append("\",")
                        .append("\"percent\":").append(rs.getInt("percentage_completed")).append(",")
                        .append("\"startDate\":\"").append(rs.getString("start_date") != null ? rs.getString("start_date") : "").append("\",")
                        .append("\"endDate\":\"").append(rs.getString("end_date") != null ? rs.getString("end_date") : "").append("\",")
                        .append("\"comments\":\"").append(clean(rs.getString("comments"))).append("\"")
                        .append("}");
                    first = false;
                }
                json.append("]");
                response.getWriter().print(json.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().print("[]");
        }
    }

    private void deleteReport(HttpServletRequest request, HttpServletResponse response, String email) throws IOException {
        String id = request.getParameter("id");
        try (Connection con = DBConnection.getConnection()) {
            String sql = "DELETE FROM user_weekly_reports WHERE report_id=? AND user_email=?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, Integer.parseInt(id));
                ps.setString(2, email);
                ps.executeUpdate();
                response.getWriter().print("{\"success\":true}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().print("{\"success\":false}");
        }
    }

    private String clean(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }
}
