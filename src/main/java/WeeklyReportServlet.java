import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/WeeklyReportServlet")
public class WeeklyReportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String DB_URL = "jdbc:postgresql://dpg-d6vrvov5r7bs73f04bpg-a.oregon-postgres.render.com:5432/bluevibes_db_new?sslmode=require";
    private static final String DB_USER = "bluevibes_db_new_user";
    private static final String DB_PASSWORD = "jc0bxNz8YFBiM7BZoa80yWd8T30jb9MD";

    private Connection getConnection() throws Exception {
        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    // 1. FETCH OPERATION (Handles loading previous entries on window.onload)
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"Session expired or invalid.\"}");
            return;
        }
        
        String username = (String) session.getAttribute("username");
        String action = request.getParameter("action");

        if ("fetchMyReports".equalsIgnoreCase(action)) {
            System.out.println("===> Fetching weekly reports database array matrix for user: " + username);
            
            // Query selects all active report tracks matching the session user string ordered chronologically
            String fetchQuery = "SELECT id, task_id, task_description, customer, status, percent_completed, start_date, end_date, comments " +
                                "FROM weekly_reports WHERE username = ? ORDER BY id DESC";
            
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(fetchQuery)) {
                
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    StringBuilder jsonResult = new StringBuilder("[");
                    boolean first = true;
                    
                    while (rs.next()) {
                        if (!first) { jsonResult.append(","); }
                        first = false;
                        
                        jsonResult.append("{")
                                  .append("\"reportId\":").append(rs.getInt("id")).append(",")
                                  .append("\"taskId\":\"").append(rs.getString("task_id")).append("\",")
                                  .append("\"taskDesc\":\"").append(rs.getString("task_description")).append("\",")
                                  .append("\"customer\":\"").append(rs.getString("customer")).append("\",")
                                  .append("\"status\":\"").append(rs.getString("status")).append("\",")
                                  .append("\"percent\":").append(rs.getInt("percent_completed")).append(",")
                                  .append("\"startDate\":\"").append(rs.getDate("start_date")).append("\",")
                                  .append("\"endDate\":\"").append(rs.getDate("end_date")).append("\",")
                                  .append("\"comments\":\"").append(rs.getString("comments") != null ? rs.getString("comments").replace("\"", "\\\"") : "").append("\"")
                                  .append("}");
                    }
                    jsonResult.append("]");
                    out.print(jsonResult.toString());
                }
            } catch (Exception e) {
                System.err.println("!!! DB FETCH RUNTIME ERROR: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"error\":\"Failed to fetch records: " + e.getMessage() + "\"}");
            }
        }
        out.flush();
        out.close();
    }

    // 2. SAVE AND DELETE OPERATIONS (Handles form data post processing submissions)
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"success\":false,\"error\":\"Session missing inside secure endpoint.\"}");
            return;
        }
        
        String username = (String) session.getAttribute("username");
        String action = request.getParameter("action");

        // Sub-Operation: Hard Delete Entry Row
        if ("delete".equalsIgnoreCase(action)) {
            String targetId = request.getParameter("id");
            System.out.println("===> Requesting row deletion inside table matrix. Record Serial ID: " + targetId);
            
            String deleteSQL = "DELETE FROM weekly_reports WHERE id = ? AND username = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(deleteSQL)) {
                ps.setInt(1, Integer.parseInt(targetId));
                ps.setString(2, username);
                int rows = ps.executeUpdate();
                out.print("{\"success\":" + (rows > 0) + "}");
            } catch (Exception ex) {
                out.print("{\"success\":false,\"error\":\"" + ex.getMessage() + "\"}");
            }
            out.flush();
            return;
        }

        // Sub-Operation: Save / Update Form Rows Dataset Array Block
        System.out.println("===> Processing batch update payload transaction mapping loop for: " + username);
        
        String[] reportIds = request.getParameterValues("reportId");
        String[] taskIds = request.getParameterValues("taskId");
        String[] taskDescs = request.getParameterValues("taskDesc");
        String[] customers = request.getParameterValues("customer");
        String[] statuses = request.getParameterValues("status");
        String[] percents = request.getParameterValues("percent");
        String[] startDates = request.getParameterValues("startDate");
        String[] endDates = request.getParameterValues("endDate");
        String[] commentsArray = request.getParameterValues("comments");

        if (taskIds == null || taskIds.length == 0) {
            out.print("{\"success\":false,\"error\":\"Empty matrix rows provided inside form array payload.\"}");
            out.flush();
            return;
        }

        String insertSQL = "INSERT INTO weekly_reports (username, task_id, task_description, customer, status, percent_completed, start_date, end_date, comments) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String updateSQL = "UPDATE weekly_reports SET task_id=?, task_description=?, customer=?, status=?, percent_completed=?, start_date=?, end_date=?, comments=? WHERE id=? AND username=?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Enable batch transaction management loop safely
            
            try {
                for (int i = 0; i < taskIds.length; i++) {
                    // Skip entirely empty row structures
                    if (taskDescs[i] == null || taskDescs[i].trim().isEmpty()) continue;

                    int rId = (reportIds != null && i < reportIds.length) ? Integer.parseInt(reportIds[i]) : 0;
                    
                    if (rId == 0) {
                        // Create and execute a brand new record insert
                        try (PreparedStatement psInsert = conn.prepareStatement(insertSQL)) {
                            psInsert.setString(1, username);
                            psInsert.setString(2, taskIds[i]);
                            psInsert.setString(3, taskDescs[i]);
                            psInsert.setString(4, (customers != null && i < customers.length) ? customers[i] : "");
                            psInsert.setString(5, (statuses != null && i < statuses.length) ? statuses[i] : "InProgress");
                            psInsert.setInt(6, (percents != null && i < percents.length) ? Integer.parseInt(percents[i]) : 0);
                            psInsert.setDate(7, java.sql.Date.valueOf(startDates[i]));
                            psInsert.setDate(8, java.sql.Date.valueOf(endDates[i]));
                            psInsert.setString(9, (commentsArray != null && i < commentsArray.length) ? commentsArray[i] : "");
                            psInsert.executeUpdate();
                        }
                    } else {
                        // Update an existing row record layout matched via unique DB ID serial key
                        try (PreparedStatement psUpdate = conn.prepareStatement(updateSQL)) {
                            psUpdate.setString(1, taskIds[i]);
                            psUpdate.setString(2, taskDescs[i]);
                            psUpdate.setString(3, (customers != null && i < customers.length) ? customers[i] : "");
                            psUpdate.setString(4, (statuses != null && i < statuses.length) ? statuses[i] : "InProgress");
                            psUpdate.setInt(5, (percents != null && i < percents.length) ? Integer.parseInt(percents[i]) : 0);
                            psUpdate.setDate(6, java.sql.Date.valueOf(startDates[i]));
                            psUpdate.setDate(7, java.sql.Date.valueOf(endDates[i]));
                            psUpdate.setString(8, (commentsArray != null && i < commentsArray.length) ? commentsArray[i] : "");
                            psUpdate.setInt(9, rId);
                            psUpdate.setString(10, username);
                            psUpdate.executeUpdate();
                        }
                    }
                }
                conn.commit(); // Push all changes cleanly downstream
                System.out.println("🚀 Batch update transaction loop completed successfully.");
                out.print("{\"success\":true}");
            } catch (Exception batchError) {
                conn.rollback(); // Undo any partial mutations if array iteration faults out halfway
                throw batchError;
            }
        } catch (Exception ex) {
            System.err.println("!!! TRANSACTION REJECTION ERROR: " + ex.getMessage());
            ex.printStackTrace();
            out.print("{\"success\":false,\"error\":\"Database Transaction execution error: " + ex.getMessage() + "\"}");
        } finally {
            out.flush();
            out.close();
        }
    }
}
