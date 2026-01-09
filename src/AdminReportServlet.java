import java.io.*;
import java.sql.*;
import java.util.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/AdminReportServlet")
public class AdminReportServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String emailsParam = request.getParameter("emails");
        String fromDate = request.getParameter("from");
        String toDate = request.getParameter("to");

        // Log parameters for debugging in your console
        System.out.println("Admin Filter - Emails: " + emailsParam + " | From: " + fromDate + " | To: " + toDate);

        StringBuilder sql = new StringBuilder(
                "SELECT wr.*, u.fullname FROM user_weekly_reports wr " +
                        "JOIN users u ON wr.user_email = u.email WHERE 1=1 "
        );

        if (emailsParam != null && !emailsParam.isEmpty()) {
            sql.append(" AND wr.user_email IN (").append(formatEmailList(emailsParam)).append(")");
        }

        // Use DATE() casting to ensure string comparison works with DB Date types
        if (fromDate != null && !fromDate.isEmpty()) {
            sql.append(" AND DATE(wr.start_date) >= ?");
        }
        if (toDate != null && !toDate.isEmpty()) {
            sql.append(" AND DATE(wr.end_date) <= ?");
        }

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            // The order of these must strictly match the order in the StringBuilder above
            if (fromDate != null && !fromDate.isEmpty()) {
                ps.setString(paramIndex++, fromDate);
            }
            if (toDate != null && !toDate.isEmpty()) {
                ps.setString(paramIndex++, toDate);
            }

            ResultSet rs = ps.executeQuery();
            StringBuilder jsonBuilder = new StringBuilder("[");
            boolean first = true;

            while (rs.next()) {
                if (!first) jsonBuilder.append(",");

                // Sanitizing comments for JSON safety
                String rawComments = rs.getString("comments");
                String cleanComments = (rawComments != null) ?
                        rawComments.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "") : "";

                String row = String.format(
                        "{\"userName\":\"%s\", \"taskId\":\"%s\", \"taskDesc\":\"%s\", \"customer\":\"%s\", \"status\":\"%s\", \"percent\":%d, \"startDate\":\"%s\", \"endDate\":\"%s\", \"comments\":\"%s\"}",
                        rs.getString("fullname"),
                        rs.getString("task_id") != null ? rs.getString("task_id") : "N/A",
                        rs.getString("task_description"),
                        rs.getString("customer"),
                        rs.getString("status"),
                        rs.getInt("percentage_completed"),
                        rs.getString("start_date"),
                        rs.getString("end_date"),
                        cleanComments
                );

                jsonBuilder.append(row);
                first = false;
            }
            jsonBuilder.append("]");
            out.print(jsonBuilder.toString());

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Database Error: " + e.getMessage() + "\"}");
        }
    }

    private String formatEmailList(String emails) {
        String[] emailArray = emails.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < emailArray.length; i++) {
            sb.append("'").append(emailArray[i].trim()).append("'");
            if (i < emailArray.length - 1) sb.append(",");
        }
        return sb.toString();
    }
}