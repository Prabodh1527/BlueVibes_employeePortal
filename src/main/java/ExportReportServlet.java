import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/ExportReportServlet")
public class ExportReportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Mail server global settings
    private final String SMTP_HOST = "smtp.gmail.com"; 
    private final String SMTP_PORT = "587";

    // =========================================================================
    // UPDATE THESE STRINGS WITH YOUR RENDER LIVE DATABASE ACCESS CREDENTIALS
    // =========================================================================
    private final String DB_URL = "jdbc:mysql://localhost:3306/your_live_db_name";
    private final String DB_USER = "root";
    private final String DB_PASSWORD = "your_live_db_password";
    // =========================================================================

    // Self-contained connection provider method bypasses external project utilities
    private Connection getConnection() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        // 1. Session check - sends explicit 419/401 token mapping on login dropping
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"success\": false, \"error\": \"AUTH_EXPIRED\"}");
            out.flush();
            out.close();
            return;
        }

        String username = (String) session.getAttribute("username"); 
        
        String systemSenderEmail = null;
        String systemSenderPassword = null;
        String targetRecipientEmail = null;

        String recipientQuery = "SELECT email FROM communication_email WHERE username = ?";
        String senderQuery = "SELECT config_value FROM system_config WHERE config_key = ?";
        
        // 2. Query configurations dynamically from local rows
        try (Connection conn = getConnection()) {
            
            // Get user's dynamic recipient email destination
            try (PreparedStatement psRec = conn.prepareStatement(recipientQuery)) {
                psRec.setString(1, username);
                try (ResultSet rsRec = psRec.executeQuery()) {
                    if (rsRec.next()) {
                        targetRecipientEmail = rsRec.getString("email");
                    }
                }
            }
            
            // Get corporate system sender email configuration
            try (PreparedStatement psSenderEmail = conn.prepareStatement(senderQuery)) {
                psSenderEmail.setString(1, "smtp_sender_email");
                try (ResultSet rsSE = psSenderEmail.executeQuery()) {
                    if (rsSE.next()) {
                        systemSenderEmail = rsSE.getString("config_value");
                    }
                }
            }

            // Get corporate system sender password configuration
            try (PreparedStatement psSenderPass = conn.prepareStatement(senderQuery)) {
                psSenderPass.setString(1, "smtp_sender_password");
                try (ResultSet rsSP = psSenderPass.executeQuery()) {
                    if (rsSP.next()) {
                        systemSenderPassword = rsSP.getString("config_value");
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // Keeps user in place on page
            out.print("{\"success\": false, \"error\": \"Database Error: Could not load authentication parameters. " + e.getMessage() + "\"}");
            return;
        }

        // Validate values fetched from database
        if (targetRecipientEmail == null || targetRecipientEmail.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\": false, \"error\": \"Database missing target recipient mapping inside communication_email for user: " + username + "\"}");
            return;
        }
        if (systemSenderEmail == null || systemSenderPassword == null || systemSenderEmail.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\": false, \"error\": \"System configurations missing. Ensure smtp_sender_email and smtp_sender_password records exist inside system_config.\"}");
            return;
        }

        // 3. Receive file payload data
        String base64ExcelData = request.getParameter("excelData");
        if (base64ExcelData == null || base64ExcelData.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\": false, \"error\": \"Local spreadsheet generated, but mail delivery failed: Excel spreadsheet input stream parameter payload is empty.\"}");
            return;
        }

        final String finalSenderEmail = systemSenderEmail;
        final String finalSenderPassword = systemSenderPassword;

        // 4. Assemble and dispatch email
        try {
            byte[] excelBytes = Base64.getDecoder().decode(base64ExcelData);

            Properties properties = new Properties();
            properties.put("mail.smtp.host", SMTP_HOST);
            properties.put("mail.smtp.port", SMTP_PORT);
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.ssl.protocols", "TLSv1.2");

            Session mailSession = Session.getInstance(properties, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(finalSenderEmail, finalSenderPassword);
                }
            });

            Message message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(finalSenderEmail, "BlueVibes Corporate Portal"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(targetRecipientEmail));
            message.setSubject("BlueVibes | Weekly Status Report Archive - " + username);

            Multipart multipart = new MimeMultipart();

            // Main Body Text
            MimeBodyPart textBodyPart = new MimeBodyPart();
            String emailMessageContent = "Dear " + username + ",\n\n"
                    + "Your Weekly Status Report spreadsheet has been processed successfully.\n\n"
                    + "Please find your compliance archive copy attached directly to this email message.\n\n"
                    + "Best regards,\n"
                    + "BlueVibes Automated Reporting Gateway Engine Notification Node.";
            textBodyPart.setText(emailMessageContent);
            multipart.addBodyPart(textBodyPart);

            // Reconstructed Excel Binary File Attachment
            MimeBodyPart attachmentBodyPart = new MimeBodyPart();
            attachmentBodyPart.setContent(excelBytes, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            attachmentBodyPart.setFileName(username + "_Weekly_Status_Report.xlsx");
            multipart.addBodyPart(attachmentBodyPart);

            message.setContent(multipart);
            
            // Connect and send
            Transport.send(message);

            // Success json response packet
            out.print("{\"success\": true}");

        } catch (Exception mailError) {
            mailError.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); 
            out.print("{\"success\": false, \"error\": \"Local file saved, but connection to corporate mail engine dropped: " + mailError.getMessage() + "\"}");
        } finally {
            out.flush();
            out.close();
        }
    }
}
