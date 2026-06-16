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

@WebServlet("/WeeklyReportServlet")
public class WeeklyReportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
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

    private void applyCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
            response.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept, Authorization");
        }
    }

    private synchronized String resolveTableName(Connection conn) throws SQLException {
        if (targetTableName != null) return targetTableName;
        String[] prospectiveNames = {"user_weekly_reports", "weekly_report", "weeklyreport", "weekly_reports", "reports"};
        for (String name : prospectiveNames) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM " + name + " LIMIT 1")) {
                ps.executeQuery();
                targetTableName = name;
                return targetTableName;
            } catch (SQLException e) { /* keep looking */ }
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS weekly_report (report_id SERIAL PRIMARY KEY, task_id INT, task_desc VARCHAR(255), customer VARCHAR(255), status VARCHAR(50), percent INT, start_date DATE, end_date DATE, comments TEXT)");
            targetTableName = "weekly_report";
            return targetTableName;
        }
    }

    private Connection getConnection() throws SQLException {
        return DBConnection.getConnection();
    }

    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        applyCorsHeaders(request, response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        applyCorsHeaders(request, response);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userEmail") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"session_expired\"}");
            return;
        }

        String action = request.getParameter("action");
        try (Connection conn = getConnection()) {
            String tableName = resolveTableName(conn);
            if ("fetchMyReports".equals(action)) {
                String userEmail = (String) session.getAttribute("userEmail");
                StringBuilder json = new StringBuilder("[");

                String sql = "SELECT * FROM " + tableName +
                             " WHERE user_email = ? " +
                             " ORDER BY start_date DESC, report_id DESC";
                
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, userEmail);
                
                ResultSet rs = ps.executeQuery(); {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(",");
                        first = false;
                        json.append("{")
                            .append("\"reportId\":").append(rs.getInt("report_id")).append(",")
                            .append("\"taskId\":").append(rs.getInt("task_id")).append(",")
                            .append("\"taskDesc\":\"").append(rs.getString("task_description") != null ? rs.getString("task_description").replace("\"", "\\\"") : "").append("\",")
                            .append("\"customer\":\"").append(rs.getString("customer") != null ? rs.getString("customer").replace("\"", "\\\"") : "").append("\",")
                            .append("\"status\":\"").append(rs.getString("status") != null ? rs.getString("status").replace("\"", "\\\"") : "").append("\",")
                            .append("\"percent\":").append(rs.getInt("percentage_completed")).append(",")
                            .append("\"startDate\":\"").append(rs.getDate("start_date") != null ? rs.getDate("start_date").toString() : "").append("\",")
                            .append("\"endDate\":\"").append(rs.getDate("end_date") != null ? rs.getDate("end_date").toString() : "").append("\",")
                            .append("\"comments\":\"").append(rs.getString("comments") != null ? rs.getString("comments").replace("\"", "\\\"") : "").append("\"")
                            .append("}");
                    }
                }
                json.append("]");
                out.print(json.toString());
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        applyCorsHeaders(request, response);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userEmail") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"session_expired\"}");
            return;
        }

        String action = request.getParameter("action");
        try (Connection conn = getConnection()) {
            String tableName = resolveTableName(conn);

            if ("delete".equals(action)) {
                String idParam = request.getParameter("id");
                if (idParam != null) {
                    String sql = "DELETE FROM " + tableName + " WHERE report_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, Integer.parseInt(idParam));
                        int rows = ps.executeUpdate();
                        out.print("{\"success\":" + (rows > 0) + "}");
                    }
                }
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
            String[] commentsArray = request.getParameterValues("comments");

            if (reportIds != null) {
                String insertSql = "INSERT INTO " + tableName + " (task_id, task_description, customer, status, percentage_completed, start_date, end_date, comments) VALUES (?, ?, ?, ?, ?, CAST(? AS DATE), CAST(? AS DATE), ?)";
                String updateSql = "UPDATE " + tableName + " SET task_id=?, task_description=?, customer=?, status=?, percentage_completed=?, start_date=CAST(? AS DATE), end_date=CAST(? AS DATE), comments=? WHERE report_id=?";

                for (int i = 0; i < reportIds.length; i++) {
                    int rId = Integer.parseInt(reportIds[i]);
                    String sql = (rId == 0) ? insertSql : updateSql;
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, (taskIds != null && i < taskIds.length) ? Integer.parseInt(taskIds[i]) : 0);
                        ps.setString(2, (taskDescs != null && i < taskDescs.length) ? taskDescs[i] : "");
                        ps.setString(3, (customers != null && i < customers.length) ? customers[i] : "");
                        ps.setString(4, (statuses != null && i < statuses.length) ? statuses[i] : "Open");
                        ps.setInt(5, (percents != null && i < percents.length) ? Integer.parseInt(percents[i]) : 0);
                        ps.setString(6, (startDates != null && i < startDates.length && !startDates[i].isEmpty()) ? startDates[i] : null);
                        ps.setString(7, (endDates != null && i < endDates.length && !endDates[i].isEmpty()) ? endDates[i] : null);
                        ps.setString(8, (commentsArray != null && i < commentsArray.length) ? commentsArray[i] : "");
                        if (rId != 0) ps.setInt(9, rId);
                        ps.executeUpdate();
                    }
                }
            }
            out.print("{\"success\":true}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
