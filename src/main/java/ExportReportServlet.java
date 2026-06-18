import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/ExportReportServlet")
public class ExportReportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Database Connection Parameters
    private static final String DB_URL = "jdbc:postgresql://dpg-d6vrvov5r7bs73f04bpg-a.oregon-postgres.render.com:5432/bluevibes_db_new?sslmode=require&sslfactory=org.postgresql.ssl.NonValidatingFactory";
    private static final String DB_USER = "bluevibes_db_new_user";
    private static final String DB_PASSWORD = "jc0bxNz8YFBiM7BZoa80yWd8T30jB9MD";

    // Brevo SMTP Relay Configuration Parameters
    private static final String SMTP_HOST = "smtp-relay.brevo.com";
    private static final String SMTP_PORT = "587"; 
    private static final String SMTP_USER = "ae8702001@smtp-brevo.com";
    private static final String SMTP_PASS = "jc0bxNz8YFBiM7BZoa80yWd8T30jB9MD"; // Ensure this matches your Brevo Master key/password

    private Connection getConnection() throws Exception {
        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        String userEmail = "prasanthram@bluegitalllp.com"; // Default safe constraint fallback
        
        if (session != null && session.getAttribute("email") != null) {
            userEmail = (String) session.getAttribute("email");
        }

        // 1. Build CSV/Excel dataset from PostgreSQL records
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("Task ID,Task Description,Customer,Status,% Completed,Start Date,End Date,Comments\n");

        String query = "SELECT task_id, task_description, customer, status, percentage_completed, start_date, end_date, comments " +
                       "FROM user_weekly_reports WHERE user_email = ? ORDER BY report_id DESC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setString(1, userEmail);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    csvContent.append("\"").append(rs.getString("task_id")).append("\",")
                              .append("\"").append(rs.getString("task_description")).append("\",")
                              .append("\"").append(rs.getString("customer")).append("\",")
                              .append("\"").append(rs.getString("status")).append("\",")
                              .append(rs.getInt("percentage_completed")).append(",")
                              .append("\"").append(rs.getDate("start_date") != null ? rs.getDate("start_date").toString() : "").append("\",")
                              .append("\"").append(rs.getDate("end_date") != null ? rs.getDate("end_date").toString() : "").append("\",")
                              .append("\"").append(rs.getString("comments") != null ? rs.getString("comments") : "").append("\"\n");
                }
            }

            byte[] reportBytes = csvContent.toString().getBytes("UTF-8");

            // 2. Dispatch the spreadsheet to Brevo's SMTP Cluster over Port 587
            sendMailWithAttachment(userEmail, reportBytes);

            // 3. Provide a copy directly to the user's browser for download
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=Weekly_Status_Report.csv");
            response.setContentLength(reportBytes.length);
            response.getOutputStream().write(reportBytes);
            response.getOutputStream().flush();

        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
            out.print("<script>alert('Failed to transmit spreadsheet via Brevo: " + e.getMessage() + "'); window.history.back();</script>");
        }
    }

    private void sendMailWithAttachment(String recipientEmail, byte[] attachmentData) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true"); // Mandates TLS security protocols over 587
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASS);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SMTP_USER, "BlueVibes System Admin"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
        message.setSubject("🚀 Weekly Status Report Sheets Upload Notification");

        // Set email body text
        MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText("Hello,\n\nPlease find attached your Weekly Status Report export from the corporate audit ledger ecosystem.");

        // Attach spreadsheet
        MimeBodyPart attachmentBodyPart = new MimeBodyPart();
        DataSource source = new ByteArrayDataSource(attachmentData, "text/csv");
        attachmentBodyPart.setDataHandler(new DataHandler(source));
        attachmentBodyPart.setFileName("My_Weekly_Status_Report.csv");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);
        multipart.addBodyPart(attachmentBodyPart);

        message.setContent(multipart);
        Transport.send(message);
        System.out.println("Email successfully routed through Brevo server pipeline!");
    }
}
