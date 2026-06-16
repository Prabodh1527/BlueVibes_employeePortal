import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/WeeklyReportServlet")
public class WeeklyReportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    // Fallback database configuration settings
    private static final String DB_URL = System.getenv("DB_URL") != null ? System.getenv("DB_URL") : "jdbc:postgresql://localhost:5432/postgres";
    private static final String DB_USER = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "postgres";
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "password";

    private String targetTableName = null;

    @Override
    public void init() throws ServletException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // Safely determines which table exists in your PostgreSQL database at runtime
    private synchronized String resolveTableName(Connection conn) throws SQLException {
        if (targetTableName != null) return targetTableName;

        String[] prospectiveNames = {"weekly_report", "weeklyreport", "reports", "weekly_reports"};
        for (String tableName : prospectiveNames) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM " + tableName + " LIMIT 1")) {
                ps.executeQuery();
                targetTableName = tableName;
                System.out.println("WeeklyReportServlet successfully bound to active table: " + targetTableName);
                return targetTableName;
            } catch (SQLException e) {
                // Table does not exist, check the next candidate down the list
            }
        }

        // Emergency fallback: Create the table automatically if absolutely nothing is found
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS weekly_report (" +
                "report_id SERIAL PRIMARY KEY, task_id INT, task_desc VARCHAR(255), " +
                "customer VARCHAR(255), status VARCHAR(50), percent INT, " +
                "start_date DATE, end_date DATE, comments TEXT)");
            targetTableName = "weekly_report";
            return targetTableName;
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        JSONArray jsonArray = new JSONArray();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            // Gracefully notify frontend to avoid breaking background AJAX handshakes
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"session_expired\"}");
            return;
        }

        String action = request.getParameter("action");

        try (Connection conn = getConnection()) {
            String tableName = resolveTableName(conn);

            if ("fetchMyReports".equals(action)) {
                String sql = "SELECT * FROM " + tableName + " ORDER BY start_date DESC, report_id DESC";
                try (PreparedStatement ps = conn.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    
                    while (rs.next()) {
                        JSONObject obj = new JSONObject();
                        obj.put("reportId", rs.getInt("report_id"));
                        obj.put("taskId", rs.getInt("task_id"));
                        obj.put("taskDesc", rs.getString("task_desc"));
                        obj.put("customer", rs.getString("customer"));
                        obj.put("status", rs.getString("status"));
                        obj.put("percent", rs.getInt("percent"));
                        obj.put("startDate", rs.getDate("start_date") != null ? rs.getDate("start_date").toString() : "");
                        obj.put("endDate", rs.getDate("end_date") != null ? rs.getDate("end_date").toString() : "");
                        obj.put("comments", rs.getString("comments"));
                        jsonArray.put(obj);
                    }
                }
                out.print(jsonArray.toString());
            } else {
                out.print("[]");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.sendRedirect("index.html?status=session_expired");
            return;
        }

        String action = request.getParameter("action");

        try (Connection conn = getConnection()) {
            String tableName = resolveTableName(conn);

            if ("delete".equals(action)) {
                response.setContentType("application/json");
                String idParam = request.getParameter("id");
                if (idParam != null) {
                    String sql = "DELETE FROM " + tableName + " WHERE report_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, Integer.parseInt(idParam));
                        int rows = ps.executeUpdate();
                        response.getWriter().print("{\"success\":" + (rows > 0) + "}");
                    }
                }
                return;
            }

            // Extract multi-row array inputs securely sent via URLSearchParams 
            String[] reportIds = request.getParameterValues("reportId");
            String[] taskIds = request.getParameterValues("taskId");
            String[] taskDescs = request.getParameterValues("taskDesc");
            String[] customers = request.getParameterValues("customer");
            String[] statuses = request.getParameterValues("status");
            String[] percents = request.getParameterValues("percent");
            String[] startDates = request.getParameterValues("startDate");
            String[] endDates = request.getParameterValues("endDate");
            String[] commentsArray = request.getParameterValues("comments");

            if (reportIds != null && reportIds.length > 0) {
                String insertSql = "INSERT INTO " + tableName + " (task_id, task_desc, customer, status, percent, start_date, end_date, comments) VALUES (?, ?, ?, ?, ?, CAST(? AS DATE), CAST(? AS DATE), ?)";
                String updateSql = "UPDATE " + tableName + " SET task_id=?, task_desc=?, customer=?, status=?, percent=?, start_date=CAST(? AS DATE), end_date=CAST(? AS DATE), comments=? WHERE report_id=?";

                for (int i = 0; i < reportIds.length; i++) {
                    int rId = Integer.parseInt(reportIds[i]);
                    
                    if (rId == 0) { // New entry addition
                        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                            ps.setInt(1, (taskIds != null && i < taskIds.length) ? Integer.parseInt(taskIds[i]) : 0);
                            ps.setString(2, (taskDescs != null && i < taskDescs.length) ? taskDescs[i] : "");
                            ps.setString(3, (customers != null && i < customers.length) ? customers[i] : "");
                            ps.setString(4, (statuses != null && i < statuses.length) ? statuses[i] : "Open");
                            ps.setInt(5, (percents != null && i < percents.length) ? Integer.parseInt(percents[i]) : 0);
                            ps.setString(6, (startDates != null && i < startDates.length && !startDates[i].isEmpty()) ? startDates[i] : null);
                            ps.setString(7, (endDates != null && i < endDates.length && !endDates[i].isEmpty()) ? endDates[i] : null);
                            ps.setString(8, (commentsArray != null && i < commentsArray.length) ? commentsArray[i] : "");
                            ps.executeUpdate();
                        }
                    } else { // Modifying an existing record
                        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                            ps.setInt(1, (taskIds != null && i < taskIds.length) ? Integer.parseInt(taskIds[i]) : 0);
                            ps.setString(2, (taskDescs != null && i < taskDescs.length) ? taskDescs[i] : "");
                            ps.setString(3, (customers != null && i < customers.length) ? customers[i] : "");
                            ps.setString(4, (statuses != null && i < statuses.length) ? statuses[i] : "Open");
                            ps.setInt(5, (percents != null && i < percents.length) ? Integer.parseInt(percents[i]) : 0);
                            ps.setString(6, (startDates != null && i < startDates.length && !startDates[i].isEmpty()) ? startDates[i] : null);
                            ps.setString(7, (endDates != null && i < endDates.length && !endDates[i].isEmpty()) ? endDates[i] : null);
                            ps.setString(8, (commentsArray != null && i < commentsArray.length) ? commentsArray[i] : "");
                            ps.setInt(9, rId);
                            ps.executeUpdate();
                        }
                    }
                }
            }
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().print("{\"success\":true}");
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().print("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
