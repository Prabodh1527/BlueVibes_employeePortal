import java.io.*;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/WeeklyReportServlet")
public class WeeklyReportServlet extends HttpServlet {

    // ================= SAVE / UPDATE / DELETE =================
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        String userEmail = (String) session.getAttribute("userEmail");
        if (userEmail == null) {
            response.sendRedirect("index.html");
            return;
        }

        String action = request.getParameter("action");

        // DELETE ROW
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

        String insertSql =
                "INSERT INTO user_weekly_reports " +
                "(user_email, task_id, task_description, customer, status, percentage_completed, start_date, end_date, comments) " +
                "VALUES (?,?,?,?,?,?,?,?,?)";

        String updateSql =
                "UPDATE user_weekly_reports SET " +
                "task_id=?, task_description=?, customer=?, status=?, percentage_completed=?, start_date=?, end_date=?, comments=? " +
                "WHERE report_id=? AND user_email=?";

        try (Connection con = DBConnection.getConnection()) {

            for (int i = 0; i < taskDescs.length; i++) {

                if (taskDescs[i] == null || taskDescs[i].trim().isEmpty()) continue;

                int percent = 0;
                try { percent = Integer.parseInt(percents[i]); } catch (Exception ignored) {}

                if ("0".equals(reportIds[i])) {
                    try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                        ps.setString(1, userEmail);
                        ps.setString(2, taskIds[i]);
                        ps.setString(3, taskDescs[i]);
                        ps.setString(4, customers[i]);
                        ps.setString(5, statuses[i]);
                        ps.setInt(6, percent);
                        ps.setString(7, startDates[i].isEmpty() ? null : startDates[i]);
                        ps.setString(8, endDates[i].isEmpty() ? null : endDates[i]);
                        ps.setString(9, comments[i]);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                        ps.setString(1, taskIds[i]);
                        ps.setString(2, taskDescs[i]);
                        ps.setString(3, customers[i]);
                        ps.setString(4, statuses[i]);
                        ps.setInt(5, percent);
                        ps.setString(6, startDates[i].isEmpty() ? null : startDates[i]);
                        ps.setString(7, endDates[i].isEmpty() ? null : endDates[i]);
                        ps.setString(8, comments[i]);
                        ps.setInt(9, Integer.parseInt(reportIds[i]));
                        ps.setString(10, userEmail);
                        ps.executeUpdate();
                    }
                }
            }

            response.sendRedirect("weeklyreport.html?status=success");

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("weeklyreport.html?status=error");
        }
    }

    // ================= FETCH USER REPORTS =================
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String action = request.getParameter("action");
        if (!"fetchMyReports".equals(action)) return;

        HttpSession session = request.getSession(false);
        String userEmail = (String) session.getAttribute("userEmail");
        if (userEmail == null) return;

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String sql =
                "SELECT * FROM user_weekly_reports WHERE user_email=? ORDER BY created_at DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, userEmail);
            ResultSet rs = ps.executeQuery();

            StringBuilder json = new StringBuilder("[");
            boolean first = true;

            while (rs.next()) {
                if (!first) json.append(",");

                json.append("{")
                        .append("\"reportId\":").append(rs.getInt("report_id")).append(",")
                        .append("\"taskId\":\"").append(rs.getString("task_id")).append("\",")
                        .append("\"taskDesc\":\"").append(rs.getString("task_description")).append("\",")
                        .append("\"customer\":\"").append(rs.getString("customer")).append("\",")
                        .append("\"status\":\"").append(rs.getString("status")).append("\",")
                        .append("\"percent\":").append(rs.getInt("percentage_completed")).append(",")
                        .append("\"startDate\":\"").append(rs.getString("start_date")).append("\",")
                        .append("\"endDate\":\"").append(rs.getString("end_date")).append("\",")
                        .append("\"comments\":\"")
                        .append(rs.getString("comments") == null ? "" :
                                rs.getString("comments").replace("\"", "\\\""))
                        .append("\"}");

                first = false;
            }

            json.append("]");
            out.print(json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            out.print("[]");
        }
    }

    // ================= DELETE =================
    private void deleteReport(HttpServletRequest request, HttpServletResponse response, String email)
            throws IOException {

        String id = request.getParameter("id");
        response.setContentType("application/json");

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps =
                     con.prepareStatement("DELETE FROM user_weekly_reports WHERE report_id=? AND user_email=?")) {

            ps.setInt(1, Integer.parseInt(id));
            ps.setString(2, email);
            ps.executeUpdate();
            response.getWriter().print("{\"success\":true}");

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().print("{\"success\":false}");
        }
    }
}
