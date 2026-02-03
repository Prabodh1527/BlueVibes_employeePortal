import java.io.*;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/AdminReportServlet")
public class AdminReportServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String emailsParam = request.getParameter("emails");
        String fromDate = request.getParameter("from");
        String toDate = request.getParameter("to");

        StringBuilder sql = new StringBuilder(
                "SELECT wr.*, u.fullname FROM user_weekly_reports wr " +
                "JOIN users u ON wr.user_email = u.email WHERE 1=1 "
        );

        if (emailsParam != null && !emailsParam.isEmpty()) {
            sql.append(" AND wr.user_email IN (").append(formatEmailList(emailsParam)).append(")");
        }
        if (fromDate != null && !fromDate.isEmpty()) {
            sql.append(" AND DATE(wr.start_date) >= ?");
        }
        if (toDate != null && !toDate.isEmpty()) {
            sql.append(" AND DATE(wr.end_date) <= ?");
        }

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            int idx = 1;
            if (fromDate != null && !fromDate.isEmpty()) ps.setString(idx++, fromDate);
            if (toDate != null && !toDate.isEmpty()) ps.setString(idx++, toDate);

            ResultSet rs = ps.executeQuery();
            StringBuilder json = new StringBuilder("[");
            boolean first = true;

            while (rs.next()) {
                if (!first) json.append(",");

                json.append("{")
                        .append("\"userName\":\"").append(rs.getString("fullname")).append("\",")
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

    private String formatEmailList(String emails) {
        String[] arr = emails.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            sb.append("'").append(arr[i].trim()).append("'");
            if (i < arr.length - 1) sb.append(",");
        }
        return sb.toString();
    }
}
