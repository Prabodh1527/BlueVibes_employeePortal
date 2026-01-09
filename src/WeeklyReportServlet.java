import java.io.*;
import java.sql.*;
import java.util.Arrays;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/WeeklyReportServlet")
public class WeeklyReportServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        String email = (String) session.getAttribute("userEmail");
        String action = request.getParameter("action");

        if (email == null) return;

        if ("fetchMyReports".equals(action)) {
            response.setContentType("application/json");
            try (Connection con = DBConnection.getConnection()) {
                PreparedStatement ps = con.prepareStatement("SELECT * FROM user_weekly_reports WHERE user_email = ? ORDER BY report_id ASC");
                ps.setString(1, email);
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
                            .append("\"comments\":\"").append(rs.getString("comments") != null ? rs.getString("comments").replace("\"", "\\\"").replace("\n", " ") : "").append("\"")
                            .append("}");
                    first = false;
                }
                json.append("]");
                response.getWriter().write(json.toString());
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        String email = (String) session.getAttribute("userEmail");
        String action = request.getParameter("action");

        if (email == null) {
            response.sendRedirect("index.html");
            return;
        }

        if ("delete".equals(action)) {
            response.setContentType("application/json");
            try (Connection con = DBConnection.getConnection()) {
                int id = Integer.parseInt(request.getParameter("id"));
                PreparedStatement ps = con.prepareStatement("DELETE FROM user_weekly_reports WHERE report_id = ? AND user_email = ?");
                ps.setInt(1, id);
                ps.setString(2, email);
                ps.executeUpdate();
                response.getWriter().write("{\"success\": true}");
            } catch (Exception e) { e.printStackTrace(); }
            return;
        }

        // Standard Save/Update logic
        String[] reportIds = request.getParameterValues("reportId");
        String[] taskIds = request.getParameterValues("taskId");
        String[] taskDescs = request.getParameterValues("taskDesc");
        String[] customers = request.getParameterValues("customer");
        String[] statuses = request.getParameterValues("status");
        String[] percents = request.getParameterValues("percent");
        String[] startDates = request.getParameterValues("startDate");
        String[] endDates = request.getParameterValues("endDate");
        String[] comments = request.getParameterValues("comments");

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            String insertSql = "INSERT INTO user_weekly_reports (user_email, task_id, task_description, customer, status, percentage_completed, start_date, end_date, comments) VALUES (?,?,?,?,?,?,?,?,?)";
            String updateSql = "UPDATE user_weekly_reports SET task_id=?, task_description=?, customer=?, status=?, percentage_completed=?, start_date=?, end_date=?, comments=? WHERE report_id=? AND user_email=?";

            if (taskDescs != null) {
                for (int i = 0; i < taskDescs.length; i++) {
                    // Skip if the description is empty
                    if (taskDescs[i] == null || taskDescs[i].trim().isEmpty()) continue;

                    int currentId = 0;
                    if (reportIds != null && i < reportIds.length) {
                        try { currentId = Integer.parseInt(reportIds[i]); } catch (Exception e) { currentId = 0; }
                    }

                    int percVal = 0;
                    if (percents != null && i < percents.length) {
                        try { percVal = Integer.parseInt(percents[i]); } catch (Exception e) { percVal = 0; }
                    }

                    if (currentId == 0) {
                        try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                            ps.setString(1, email);
                            ps.setString(2, (taskIds != null && i < taskIds.length) ? taskIds[i] : "");
                            ps.setString(3, taskDescs[i]);
                            ps.setString(4, (customers != null && i < customers.length) ? customers[i] : "");
                            ps.setString(5, (statuses != null && i < statuses.length) ? statuses[i] : "");
                            ps.setInt(6, percVal);
                            ps.setString(7, (startDates != null && i < startDates.length && !startDates[i].isEmpty()) ? startDates[i] : null);
                            ps.setString(8, (endDates != null && i < endDates.length && !endDates[i].isEmpty()) ? endDates[i] : null);
                            ps.setString(9, (comments != null && i < comments.length) ? comments[i] : "");
                            ps.executeUpdate();
                        }
                    } else {
                        try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                            ps.setString(1, (taskIds != null && i < taskIds.length) ? taskIds[i] : "");
                            ps.setString(2, taskDescs[i]);
                            ps.setString(3, (customers != null && i < customers.length) ? customers[i] : "");
                            ps.setString(4, (statuses != null && i < statuses.length) ? statuses[i] : "");
                            ps.setInt(5, percVal);
                            ps.setString(6, (startDates != null && i < startDates.length && !startDates[i].isEmpty()) ? startDates[i] : null);
                            ps.setString(7, (endDates != null && i < endDates.length && !endDates[i].isEmpty()) ? endDates[i] : null);
                            ps.setString(8, (comments != null && i < comments.length) ? comments[i] : "");
                            ps.setInt(9, currentId);
                            ps.setString(10, email);
                            ps.executeUpdate();
                        }
                    }
                }
            }
            con.commit();
            response.sendRedirect("weeklyreport.html?status=success");
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("weeklyreport.html?status=error");
        }
    }
}