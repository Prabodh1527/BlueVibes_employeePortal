import java.io.IOException;
import java.io.PrintWriter;
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

@WebServlet("/WeeklyReportServlet")
public class WeeklyReportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String DB_URL = "jdbc:postgresql://dpg-d6vrvov5r7bs73f04bpg-a.oregon-postgres.render.com:5432/bluevibes_db_new?sslmode=require&sslfactory=org.postgresql.ssl.NonValidatingFactory";
    private static final String DB_USER = "bluevibes_db_new_user";
    private static final String DB_PASSWORD = "jc0bxNz8YFBiM7BZoa80yWd8T30jB9MD";

    private Connection getConnection() throws Exception {
        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(false);
        String userEmail = null;

        if (session != null) {
            userEmail = (String) session.getAttribute("userEmail");
            /*if (session.getAttribute("email") != null) userEmail = (String) session.getAttribute("email");
            else if (session.getAttribute("username") != null) userEmail = (String) session.getAttribute("username");
            else if (session.getAttribute("user") != null) userEmail = (String) session.getAttribute("user");
            else if (session.getAttribute("employeeName") != null) userEmail = (String) session.getAttribute("employeeName");*/
        }

        /*if (userEmail == null || userEmail.trim().isEmpty()) {
            userEmail = "Employee"; 
        }*/
        if(userEmail==null){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"session_expired\"}");
            return;
        }
        
        String action = request.getParameter("action");

        if ("fetchMyReports".equalsIgnoreCase(action)) {
            // Mapped directly to report_id, user_email, and percentage_completed
            String fetchQuery = "SELECT report_id, task_id, task_description, customer, status, percentage_completed, start_date, end_date, comments " +
                                "FROM user_weekly_reports WHERE user_email = ? ORDER BY report_id DESC";
            
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(fetchQuery)) {
                
                ps.setString(1, userEmail);
                try (ResultSet rs = ps.executeQuery()) {
                    StringBuilder jsonResult = new StringBuilder("[");
                    boolean first = true;
                    
                    while (rs.next()) {
                        if (!first) { jsonResult.append(","); }
                        first = false;
                        
                        jsonResult.append("{")
                                  .append("\"reportId\":").append(rs.getInt("report_id")).append(",")
                                  .append("\"taskId\":\"").append(rs.getString("task_id")).append("\",")
                                  .append("\"taskDesc\":\"").append(rs.getString("task_description")).append("\",")
                                  .append("\"customer\":\"").append(rs.getString("customer")).append("\",")
                                  .append("\"status\":\"").append(rs.getString("status")).append("\",")
                                  .append("\"percent\":").append(rs.getInt("percentage_completed")).append(",")
                                  .append("\"startDate\":\"").append(rs.getDate("start_date")).append("\",")
                                  .append("\"endDate\":\"").append(rs.getDate("end_date")).append("\",")
                                  .append("\"comments\":\"").append(rs.getString("comments") != null ? rs.getString("comments").replace("\"", "\\\"") : "").append("\"")
                                  .append("}");
                    }
                    jsonResult.append("]");
                    out.print(jsonResult.toString());
                }
            } catch (Exception e) {
                System.err.println("!!! DB FETCH ERROR: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"error\":\"Failed to fetch records: " + e.getMessage() + "\"}");
            }
        }
        out.flush();
        out.close();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        String userEmail = null;

        /*if (session != null) {
            if (session.getAttribute("email") != null) userEmail = (String) session.getAttribute("email");
            else if (session.getAttribute("username") != null) userEmail = (String) session.getAttribute("username");
            else if (session.getAttribute("user") != null) userEmail = (String) session.getAttribute("user");
            else if (session.getAttribute("employeeName") != null) userEmail = (String) session.getAttribute("employeeName");
        }

        if (userEmail == null || userEmail.trim().isEmpty()) {
            userEmail = "Employee"; 
        }*/
        if (session != null) {
            userEmail = (String) session.getAttribute("userEmail");
            /*if (session.getAttribute("email") != null) userEmail = (String) session.getAttribute("email");
            else if (session.getAttribute("username") != null) userEmail = (String) session.getAttribute("username");
            else if (session.getAttribute("user") != null) userEmail = (String) session.getAttribute("user");
            else if (session.getAttribute("employeeName") != null) userEmail = (String) session.getAttribute("employeeName");*/
        }

        /*if (userEmail == null || userEmail.trim().isEmpty()) {
            userEmail = "Employee"; 
        }*/
        if(userEmail==null){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"session_expired\"}");
            return;
        }
        
        String action = request.getParameter("action");

        if ("delete".equalsIgnoreCase(action)) {
            String targetId = request.getParameter("id");
            // Mapped to report_id and user_email
            String deleteSQL = "DELETE FROM user_weekly_reports WHERE report_id = ? AND user_email = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(deleteSQL)) {
                ps.setInt(1, Integer.parseInt(targetId));
                ps.setString(2, userEmail);
                int rows = ps.executeUpdate();
                out.print("{\"success\":" + (rows > 0) + "}");
            } catch (Exception ex) {
                out.print("{\"success\":false,\"error\":\"" + ex.getMessage() + "\"}");
            }
            out.flush();
            return;
        }

        System.out.println("Content-Type = " + request.getContentType());
        System.out.println("taskId = " + java.util.Arrays.toString(request.getParameterValues("taskId")));
        System.out.println("taskDesc = " + java.util.Arrays.toString(request.getParameterValues("taskDesc")));

        String[] reportIds = request.getParameterValues("reportId");
        String[] taskIds = request.getParameterValues("taskId");
        System.out.println("Task IDs received = " + taskIds.length);
        String[] taskDescs = request.getParameterValues("taskDesc");
        System.out.println("Task Descriptions received = " + taskDescs.length);
        String[] customers = request.getParameterValues("customer");
        String[] statuses = request.getParameterValues("status");
        String[] percents = request.getParameterValues("percent");
        String[] startDates = request.getParameterValues("startDate");
        String[] endDates = request.getParameterValues("endDate");
        String[] commentsArray = request.getParameterValues("comments");

        if (taskIds == null || taskIds.length == 0) {
            out.print("{\"success\":false,\"error\":\"No report metadata row streams found.\"}");
            out.flush();
            return;
        }

        // Mapped to accurate column structure positions
        String insertSQL = "INSERT INTO user_weekly_reports (user_email, task_id, task_description, customer, status, percentage_completed, start_date, end_date, comments) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String updateSQL = "UPDATE user_weekly_reports SET task_id=?, task_description=?, customer=?, status=?, percentage_completed=?, start_date=?, end_date=?, comments=? WHERE report_id=? AND user_email=?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (int i = 0; i < taskIds.length; i++) {
                    if (taskDescs[i] == null || taskDescs[i].trim().isEmpty()) continue;

                    int rId = (reportIds != null && i < reportIds.length) ? Integer.parseInt(reportIds[i]) : 0;
                    System.out.println("Row = " + i +
                    " ReportId=" + rId +
                    " TaskId=" + taskIds[i] +
                    " Desc=" + taskDescs[i]);
                    
                    if (rId == 0) {
                        try (PreparedStatement psInsert = conn.prepareStatement(insertSQL)) {
                            psInsert.setString(1, userEmail);
                            psInsert.setString(2, taskIds[i]);
                            psInsert.setString(3, taskDescs[i]);
                            psInsert.setString(4, (customers != null && i < customers.length) ? customers[i] : "");
                            psInsert.setString(5, (statuses != null && i < statuses.length) ? statuses[i] : "InProgress");
                            psInsert.setInt(6, (percents != null && i < percents.length) ? Integer.parseInt(percents[i]) : 0);
                            psInsert.setDate(7, java.sql.Date.valueOf(startDates[i]));
                            psInsert.setDate(8, java.sql.Date.valueOf(endDates[i]));
                            psInsert.setString(9, (commentsArray != null && i < commentsArray.length) ? commentsArray[i] : "");
                            int rows = psInsert.executeUpdate();

                            System.out.println(
                                "Inserted taskId=" + taskIds[i] +
                                " rowsAffected=" + rows
                            );
                        }
                    } else {
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
                            psUpdate.setString(10, userEmail);
                            int rows = psUpdate.executeUpdate();

                            System.out.println(
                                "Updated reportId=" + rId +
                                " rowsAffected=" + rows
                            );
                        }
                    }
                }
                System.out.println("ABOUT TO COMMIT");
                conn.commit();
                System.out.println("COMMIT SUCCESS");
                out.print("{\"success\":true}");
            } catch(Exception batchError){

                System.out.println("========== ERROR ==========");
                batchError.printStackTrace();
            
                conn.rollback();
            
                throw batchError;
            }
        } catch (Exception ex) {

            System.err.println("========== WEEKLY REPORT ERROR ==========");
            ex.printStackTrace();
            System.err.println("=========================================");
        
            out.print("{\"success\":false}");
        } finally {
            out.flush();
            out.close();
        }
    }
}
