import java.io.*;
import java.sql.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/WeeklyReportServlet")
public class WeeklyReportServlet extends HttpServlet {

    // SAVE / UPDATE / DELETE
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");

        // DELETE ROW
        if ("delete".equals(action)) {
            deleteReport(request, response);
            return;
        }

        // SAVE / UPDATE
        HttpSession session = request.getSession(false);
        String userEmail = (String) session.getAttribute("userEmail");

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

            for (int i = 0; i < taskIds.length; i++) {

                if ("0".equals(reportIds[i])) {
                    // INSERT
                    try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                        ps.setString(1, userEmail);
                        ps.setString(2, taskIds[i]);
                        ps.setString(3, taskDescs[i]);
                        ps.setString(4, customers[i]);
                        ps.setString(5, statuses[i]);
                        ps.setInt(6, Integer.parseInt(percents[i]));
                        ps.setString(7, startDates[i]);
                        ps.setString(8, endDates[i]);
                        ps.setString(9, comments[i]);
                        ps.executeUpdate();
                    }
                } else {
                    // UPDATE
                    try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                        ps.setString(1, taskIds[i]);
                        ps.setString(2, taskDescs[i]);
                        ps.setString(3, customers[i]);
                        ps.setString(4, statuses[i]);
                        ps.setInt(5, Integer.parseInt(percents[i]));
                        ps.setString(6, startDates[i]);
                        ps.setString(7, endDates[i]);
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

    // FETCH USER REPORTS
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        if (!"fetchMyReports".equals(action)) return;

        HttpSession session = request.getSession(false);
        String userEmail = (String) session.getAttribute("userEmail");

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
                    .append("\"}")
                ;
                first = false;
            }

            json.append("]");
            out.print(json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            out.print("[]");
        }
    }

    // DELETE
    private void deleteReport(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String id = request.getParameter("id");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps =
                     con.prepareStatement("DELETE FROM user_weekly_reports WHERE report_id=?")) {

            ps.setInt(1, Integer.parseInt(id));
            ps.executeUpdate();
            out.print("{\"success\":true}");

        } catch (Exception e) {
            e.printStackTrace();
            out.print("{\"success\":false}");
        }
    }
}
