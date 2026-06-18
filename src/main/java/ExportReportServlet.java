import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/ExportReportServlet")
public class ExportReportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String DB_URL = "jdbc:postgresql://dpg-d6vrvov5r7bs73f04bpg-a.oregon-postgres.render.com:5432/bluevibes_db_new?sslmode=require&sslfactory=org.postgresql.ssl.NonValidatingFactory";
    private static final String DB_USER = "bluevibes_db_new_user";
    private static final String DB_PASSWORD = "jc0bxNz8YFBiM7BZoa80yWd8T30jB9MD";

    private Connection getConnection() throws Exception {
        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    // Changing to doGet to support direct browser link triggers for downloads
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        String userEmail = null;

        if (session != null) {
            if (session.getAttribute("email") != null) userEmail = (String) session.getAttribute("email");
            else if (session.getAttribute("username") != null) userEmail = (String) session.getAttribute("username");
            else if (session.getAttribute("user") != null) userEmail = (String) session.getAttribute("user");
            else if (session.getAttribute("employeeName") != null) userEmail = (String) session.getAttribute("employeeName");
        }

        if (userEmail == null || userEmail.trim().isEmpty()) {
            userEmail = "Employee"; 
        }

        StringBuilder csvBuilder = new StringBuilder();
        // CSV Headers mapping to UI requirements
        csvBuilder.append("Report ID,Task ID,Task Description,Customer,Status,% Completed,Start Date,End Date,Comments\n");

        // Pulling directly from your active table and column names
        String query = "SELECT report_id, task_id, task_description, customer, status, percentage_completed, start_date, end_date, comments " +
                       "FROM user_weekly_reports WHERE user_email = ? ORDER BY report_id DESC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setString(1, userEmail);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int rId = rs.getInt("report_id");
                    String tId = rs.getString("task_id");
                    String desc = rs.getString("task_description");
                    String customer = rs.getString("customer");
                    String status = rs.getString("status");
                    int percent = rs.getInt("percentage_completed");
                    String sDate = rs.getDate("start_date") != null ? rs.getDate("start_date").toString() : "-";
                    String eDate = rs.getDate("end_date") != null ? rs.getDate("end_date").toString() : "-";
                    String comments = rs.getString("comments");

                    // Sanitize fields to prevent breakages in CSV layout formatting
                    if (tId == null) tId = "N/A";
                    if (desc == null) desc = "No Description";
                    if (customer == null) customer = "Internal";
                    if (status == null) status = "InProgress";
                    if (comments == null) comments = "-";

                    desc = desc.replace(",", " ").replace("\n", " ").trim();
                    customer = customer.replace(",", " ").trim();
                    comments = comments.replace(",", " ").replace("\"", "'").replace("\n", " ").trim();

                    csvBuilder.append(rId).append(",")
                              .append(tId).append(",")
                              .append(desc).append(",")
                              .append(customer).append(",")
                              .append(status).append(",")
                              .append(percent).append(",")
                              .append(sDate).append(",")
                              .append(eDate).append(",")
                              .append(comments).append("\n");
                }
            }
        } catch (Exception e) {
            System.err.println("!!! DB EXPORT ERROR: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database Data Generation Error: " + e.getMessage());
            return;
        }

        // Send file directly back as an attachment down to user's local disk
        byte[] csvBytes = csvBuilder.toString().getBytes(StandardCharsets.UTF_8);
        
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + userEmail.split("@")[0] + "_Weekly_Report.csv\"");
        response.setContentLength(csvBytes.length);

        try (OutputStream os = response.getOutputStream()) {
            os.write(csvBytes);
            os.flush();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        doGet(request, response);
    }
}
