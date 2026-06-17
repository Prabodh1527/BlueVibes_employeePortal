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

    // Mail Settings
    private final String SMTP_HOST = "smtp.gmail.com"; 
    private final String SMTP_PORT = "587";

    // Database Configuration Parameters (Update these to match your actual DB environment)
    private final String DB_URL = "jdbc:mysql://localhost:3306/your_database_name";
    private final String DB_USER = "root";
    private final String DB_PASSWORD = "your_db_password";

    // Helper method to establish connection without relying on com.bluevibes.util
    private Connection getConnection() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver"); // Force load driver for web container environment
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"success\": false, \"error\": \"Session expired or invalid login.\"}");
            return;
        }

        String username = (String) session.getAttribute("username"); 
        
        String systemSenderEmail = null;
        String systemSenderPassword = null;
        String targetRecipientEmail = null;

        String recipientQuery = "SELECT email FROM communication_email WHERE username = ?";
        String senderQuery = "SELECT config_value FROM system_config WHERE config_key = ?";
        
        // Using our internal connection helper block
        try (Connection conn = getConnection()) {
            
            // 1. Get Recipient Communication Target
            try (PreparedStatement psRec = conn.prepareStatement(recipientQuery)) {
                psRec.setString(1, username);
                try (ResultSet rsRec = psRec.executeQuery()) {
                    if (rsRec.next()) {
                        targetRecipientEmail = rsRec.getString("email");
                    }
                }
            }
            
            // 2. Get Dynamic Corporate SMTP Mailer Address
            try (PreparedStatement psSenderEmail = conn.prepareStatement(senderQuery)) {
                psSenderEmail.setString(1, "smtp_sender_email");
                try (ResultSet rsSE = psSenderEmail.executeQuery()) {
                    if (rsSE.next()) {
                        systemSenderEmail = rsSE.getString("config_value");
                    }
                }
            }

            // 3. Get App Specific Credentials Token String Pass
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
            out.print("{\"success\": false, \"error\": \"Internal Database extraction error: " + e.getMessage() + "\"}");
            return;
        }

        // Operational Assertions
        if (targetRecipientEmail == null || targetRecipientEmail.trim().isEmpty()) {
            out.print("{\"success\": false, \"error\": \"No recipient mapping matching this user session.\"}");
            return;
        }
        if (systemSenderEmail == null || systemSenderPassword == null || systemSenderEmail.trim().isEmpty()) {
            out.print("{\"success\": false, \"error\": \"Missing system configuration SMTP parameters in DB rows.\"}");
            return;
        }

        // 4. Capture Excel payload stream
        String base64ExcelData = request.getParameter("excelData");
        if (base64ExcelData == null || base64ExcelData.isEmpty()) {
            out.print("{\"success\": false, \"error\": \"Bad Request: Missing spreadsheet payload context parameters.\"}");
            return;
        }

        final String finalSenderEmail = systemSenderEmail;
        final String finalSenderPassword = systemSenderPassword;

        try {
            byte[] excelBytes = Base64.getDecoder().decode(base64ExcelData);

            // 5. Build Mail Properties Structure Context Configuration
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
            message.setFrom(new InternetAddress(finalSenderEmail, "BlueVibes Automated Portal"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(targetRecipientEmail));
            message.setSubject("BlueVibes | Weekly Status Report - " + username);

            Multipart multipart = new MimeMultipart();

            // Text email body component
            MimeBodyPart textBodyPart = new MimeBodyPart();
            String emailMessageContent = "Dear Worker,\n\n"
                    + "Please find attached your newly generated Weekly Status Report spreadsheet logs.\n\n"
                    + "Best regards,\n"
                    + "BlueVibes Engine Automated Mail Gateway Portal System Node Container.";
            textBodyPart.setText(emailMessageContent);
            multipart.addBodyPart(textBodyPart);

            // Binary Excel attachment mapping component
            MimeBodyPart attachmentBodyPart = new MimeBodyPart();
            attachmentBodyPart.setContent(excelBytes, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            attachmentBodyPart.setFileName(username + "_Weekly_Status_Report.xlsx");
            multipart.addBodyPart(attachmentBodyPart);

            message.setContent(multipart);

            // 6. Push stream directly across mail server routes
            Transport.send(message);

            out.print("{\"success\": true}");

        } catch (Exception mailError) {
            mailError.printStackTrace();
            out.print("{\"success\": false, \"error\": \"SMTP transmission dropped or error encountered: " + mailError.getMessage() + "\"}");
        } finally {
            out.flush();
            out.close();
        }
    }
}
