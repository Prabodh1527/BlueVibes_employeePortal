import java.io.*;
import java.sql.*;
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.*;
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

        HttpSession session = request.getSession(false);
        String sessionRole = (session != null) ? (String) session.getAttribute("userRole") : null;

        if (session == null || !"Admin".equalsIgnoreCase(sessionRole)) {
            response.setStatus(401);
            out.print("[]");
            return;
        }

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
            sql.append(" AND (wr.end_date IS NULL OR wr.end_date >= CAST(? AS DATE))");
        }
        if (toDate != null && !toDate.isEmpty()) {
            sql.append(" AND (wr.start_date IS NULL OR wr.start_date <= CAST(? AS DATE))");
        }

        sql.append(" ORDER BY wr.created_at ASC");

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
                        .append("\"userName\":\"").append(escapeJson(rs.getString("fullname"))).append("\",")
                        .append("\"taskId\":\"").append(escapeJson(rs.getString("task_id"))).append("\",")
                        .append("\"taskDesc\":\"").append(escapeJson(rs.getString("task_description"))).append("\",")
                        .append("\"customer\":\"").append(escapeJson(rs.getString("customer"))).append("\",")
                        .append("\"status\":\"").append(escapeJson(rs.getString("status"))).append("\",")
                        .append("\"percent\":").append(rs.getInt("percentage_completed")).append(",")
                        .append("\"startDate\":\"").append(rs.getString("start_date") != null ? rs.getString("start_date") : "").append("\",")
                        .append("\"endDate\":\"").append(rs.getString("end_date") != null ? rs.getString("end_date") : "").append("\",")
                        .append("\"comments\":\"").append(escapeJson(rs.getString("comments"))).append("\"")
                        .append("}");

                first = false;
            }

            json.append("]");

            // ✅ ONLY ADDITION (trigger mail)
            String action = request.getParameter("action");
            if ("mail".equals(action)) {
                sendEmail(json.toString());
            }

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
            sb.append("'").append(arr[i].trim().replace("'", "''")).append("'");
            if (i < arr.length - 1) sb.append(",");
        }
        return sb.toString();
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }

    // ✅ ONLY ADDITION (email method)
    private void sendEmail(String content) {

        final String from = "gprabodhchandra@gmail.com";
        final String password = "votvwrdaqdabjwul";
        final String to = "prasanthrambharadwaj@gmail.com";

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(from, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(to));
            message.setSubject("Weekly Report Export");

            message.setText(content);

            Transport.send(message);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
