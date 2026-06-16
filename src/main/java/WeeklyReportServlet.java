import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
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

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        HttpSession session = request.getSession(false);
        
        // FIXED: Checked against "userEmail" to match login authentication properties
        if (session == null || session.getAttribute("userEmail") == null) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().print("[]");
            return;
        }

        if ("fetchMyReports".equals(action)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();
            String sessionEmail = (String) session.getAttribute("userEmail"); // FIXED
            StringBuilder json = new StringBuilder("[");
            
            Connection con = null;
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                con = DBConnection.getConnection();
                
                String sql = "SELECT r.id, r.task_id, r.task_desc, r.customer, r.status, r.percent_completed, r.start_date, r.end_date, r.comments " +
                             "FROM weekly_reports r " +
                             "JOIN users u ON r.user_email = u.email OR r.user_email = u.communication_mail " +
                             "WHERE u.email = ? OR u.communication_mail = ? " +
                             "ORDER BY r.id DESC";
                             
                ps = con.prepareStatement(sql);
                ps.setString(1, sessionEmail);
                ps.setString(2, sessionEmail);
                rs = ps.executeQuery();

                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    json.append("{")
                        .append("\"reportId\":\"").append(rs.getInt("id")).append("\",")
                        .append("\"taskId\":\"").append(rs.getString("task_id")).append("\",")
                        .append("\"taskDesc\":\"").append(rs.getString("task_desc")).append("\",")
                        .append("\"customer\":\"").append(rs.getString("customer")).append("\",")
                        .append("\"status\":\"").append(rs.getString("status")).append("\",")
                        .append("\"percent\":\"").append(rs.getInt("percent_completed")).append("\",")
                        .append("\"startDate\":\"").append(rs.getDate("start_date")).append("\",")
                        .append("\"endDate\":\"").append(rs.getDate("end_date")).append("\",")
                        .append("\"comments\":\"").append(rs.getString("comments") != null ? rs.getString("comments").replace("\"", "\\\"") : "").append("\"")
                        .append("}");
                    first = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try { if (rs != null) rs.close(); } catch(Exception e){}
                try { if (ps != null) ps.close(); } catch(Exception e){}
                try { if (con != null) con.close(); } catch(Exception e){}
            }
            json.append("]");
            out.print(json.toString());
            out.flush();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        HttpSession session = request.getSession(false);
        
        // FIXED: Checked against "userEmail" to stop unexpected route ejections
        if (session == null || session.getAttribute("userEmail") == null) {
            response.sendRedirect("index.html?status=session_expired");
            return;
        }

        if ("delete".equals(action)) {
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            String id = request.getParameter("id");
            
            Connection con = null;
            PreparedStatement ps = null;
            try {
                con = DBConnection.getConnection();
                ps = con.prepareStatement("DELETE FROM weekly_reports WHERE id = ?");
                ps.setInt(1, Integer.parseInt(id));
                int rows = ps.executeUpdate();
                out.print(rows > 0 ? "{\"success\":true}" : "{\"success\":false}");
            } catch (Exception e) {
                out.print("{\"success\":false}");
            } finally {
                try { if (ps != null) ps.close(); } catch(Exception e){}
                try { if (con != null) con.close(); } catch(Exception e){}
            }
            return;
        }

        String sessionEmail = (String) session.getAttribute("userEmail"); // FIXED
        String finalPersistedEmail = sessionEmail; 
        
        Connection con = null;
        PreparedStatement psUser = null;
        ResultSet rsUser = null;
        
        try {
            con = DBConnection.getConnection();
            
            String userSql = "SELECT email FROM users WHERE email = ? OR communication_mail = ?";
            psUser = con.prepareStatement(userSql);
            psUser.setString(1, sessionEmail);
            psUser.setString(2, sessionEmail);
            rsUser = psUser.executeQuery();
            if (rsUser.next()) {
                String primaryEmail = rsUser.getString("email");
                if (primaryEmail != null && !primaryEmail.trim().isEmpty()) {
                    finalPersistedEmail = primaryEmail.trim();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (rsUser != null) rsUser.close(); } catch(Exception e){}
            try { if (psUser != null) psUser.close(); } catch(Exception e){}
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

        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false);

            if (taskIds != null) {
                for (int i = 0; i < taskIds.length; i++) {
                    String rid = reportIds[i];
                    if ("0".equals(rid)) {
                        String ins = "INSERT INTO weekly_reports (user_email, task_id, task_desc, customer, status, percent_completed, start_date, end_date, comments) VALUES (?,?,?,?,?,?,?,?,?)";
                        try (PreparedStatement ps = con.prepareStatement(ins)) {
                            ps.setString(1, finalPersistedEmail);
                            ps.setString(2, taskIds[i]);
                            ps.setString(3, taskDescs[i]);
                            ps.setString(4, customers[i]);
                            ps.setString(5, statuses[i]);
                            ps.setInt(6, Integer.parseInt(percents[i]));
                            ps.setDate(7, java.sql.Date.valueOf(startDates[i]));
                            ps.setDate(8, java.sql.Date.valueOf(endDates[i]));
                            ps.setString(9, commentsArray != null && commentsArray.length > i ? commentsArray[i] : "");
                            ps.executeUpdate();
                        }
                    } else {
                        String upd = "UPDATE weekly_reports SET task_id=?, task_desc=?, customer=?, status=?, percent_completed=?, start_date=?, end_date=?, comments=? WHERE id=?";
                        try (PreparedStatement ps = con.prepareStatement(upd)) {
                            ps.setString(1, taskIds[i]);
                            ps.setString(2, taskDescs[i]);
                            ps.setString(3, customers[i]);
                            ps.setString(4, statuses[i]);
                            ps.setInt(5, Integer.parseInt(percents[i]));
                            ps.setDate(6, java.sql.Date.valueOf(startDates[i]));
                            ps.setDate(7, java.sql.Date.valueOf(endDates[i]));
                            ps.setString(8, commentsArray != null && commentsArray.length > i ? commentsArray[i] : "");
                            ps.setInt(9, Integer.parseInt(rid));
                            ps.executeUpdate();
                        }
                    }
                }
            }
            con.commit();
            response.sendRedirect("weeklyreport.html?status=success");
        } catch (Exception e) {
            if (con != null) { try { con.rollback(); } catch (Exception ex) {} }
            e.printStackTrace();
            response.sendRedirect("weeklyreport.html?status=error");
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
    }
}
