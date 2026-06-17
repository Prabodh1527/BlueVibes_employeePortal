import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
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

// Import your database connection helper safely
import com.bluevibes.util.DBConnection; 

@WebServlet("/ExportReportServlet")
public class ExportReportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Infrastructure Configuration
    private final String SMTP_HOST = "smtp.gmail.com"; 
    private final String SMTP_PORT = "587";

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

        // DB Query Logic Structures
        String recipientQuery = "SELECT email FROM communication_email WHERE username = ?";
        String senderQuery = "SELECT config_value FROM system_config WHERE config_key = ?";
        
        try (Connection conn = DBConnection.getConnection()) {
            
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
            out.print("{\"success\": false, \"error\": \"Database connection lookup error: " + e.getMessage() + "\"}");
            return;
        }

        // Configuration checks
        if (targetRecipientEmail == null || targetRecipientEmail.trim().isEmpty()) {
            out.print("{\"success\": false, \"error\": \"Recipient email record not found for this username.\"}");
            return;
        }
        if (systemSenderEmail == null || systemSenderPassword == null || systemSenderEmail.trim().isEmpty()) {
            out.print("{\"success\": false, \"error\": \"SMTP sender variables missing from the system_config table.\"}");
            return;
        }

        // 4. Capture incoming spreadsheet stream parameter mapping from frontend UI
        String base64ExcelData = request.getParameter("excelData");
        if (base64ExcelData == null || base64ExcelData.isEmpty()) {
            out.print("{\"success\": false, \"error\": \"Bad Request: Missing spreadsheet data payload.\"}");
            return;
        }

        final String finalSenderEmail = systemSenderEmail;
        final String finalSenderPassword = systemSenderPassword;

        try {
            byte[] excelBytes = Base64.getDecoder().decode(base64ExcelData);

            // 5. Build JavaMail Runtime Environment Setup
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
            message.setFrom(new InternetAddress(finalSenderEmail, "BlueVibes Automated Reporting Engine"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(targetRecipientEmail));
            message.setSubject("BlueVibes | Weekly Status Report Archive Summary - " + username);

            Multipart multipart = new MimeMultipart();

            // Text email part mapping
            MimeBodyPart textBodyPart = new MimeBodyPart();
            String emailMessageContent = "Dear Worker,\n\n"
                    + "Please find attached your newly generated Weekly Status Report spreadsheet logs.\n\n"
                    + "Best regards,\n"
                    + "BlueVibes Engine Automated Mail Gateway Node Instance Portal Server Context Base Framework Layer.";
            textBodyPart.setText(emailMessageContent);
            multipart.addBodyPart(textBodyPart);

            // Binary Excel attachment part mapping
            MimeBodyPart attachmentBodyPart = new MimeBodyPart();
            attachmentBodyPart.setContent(excelBytes, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            attachmentBodyPart.setFileName(username + "_Weekly_Status_Report.xlsx");
            multipart.addBodyPart(attachmentBodyPart);

            message.setContent(multipart);

            // 6. Direct execution to server network sockets
            Transport.send(message);

            out.print("{\"success\": true}");

        } catch (Exception mailError) {
            mailError.printStackTrace();
            out.print("{\"success\": false, \"error\": \"SMTP Engine runtime delivery exception encountered: " + mailError.getMessage() + "\"}");
        } finally {
            out.flush();
            out.close();
        }
    }
}
