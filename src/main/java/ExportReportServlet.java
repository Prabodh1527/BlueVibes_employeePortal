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

    private final String SMTP_HOST = "smtp.gmail.com"; 
    private final String SMTP_PORT = "587";

    // =========================================================================
    // UPDATE THESE WITH YOUR LIVE ENVIRONMENT ACCESS CREDENTIALS
    // =========================================================================
    private final String DB_URL = "jdbc:mysql://localhost:3306/your_live_db_name";
    private final String DB_USER = "root";
    private final String DB_PASSWORD = "your_live_db_password";
    // =========================================================================

    private Connection getConnection() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String username = "Employee"; // Default fallback to prevent crash if session fails
        
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("username") != null) {
            username = (String) session.getAttribute("username"); 
        } else {
            // Log that session is missing but continue execution to send the email anyway
            System.out.println("[WARNING] ExportReportServlet received request with an empty session context.");
        }

        String systemSenderEmail = null;
        String systemSenderPassword = null;
        String targetRecipientEmail = null;

        String recipientQuery = "SELECT email FROM communication_email WHERE username = ?";
        String senderQuery = "SELECT config_value FROM system_config WHERE config_key = ?";
        
        try (Connection conn = getConnection()) {
            
            try (PreparedStatement psRec = conn.prepareStatement(recipientQuery)) {
                psRec.setString(1, username);
                try (ResultSet rsRec = psRec.executeQuery()) {
                    if (rsRec.next()) {
                        targetRecipientEmail = rsRec.getString("email");
                    }
                }
            }
            
            // If recipient mapping is not found because of fallback username, get any valid email to test
            if (targetRecipientEmail == null) {
                try (PreparedStatement psFallbackEmail = conn.prepareStatement("SELECT email FROM communication_email LIMIT 1")) {
                    try (ResultSet rsFE = psFallbackEmail.executeQuery()) {
                        if (rsFE.next()) { targetRecipientEmail = rsFE.getString("email"); }
                    }
                }
            }
            
            try (PreparedStatement psSenderEmail = conn.prepareStatement(senderQuery)) {
                psSenderEmail.setString(1, "smtp_sender_email");
                try (ResultSet rsSE = psSenderEmail.executeQuery()) {
                    if (rsSE.next()) {
                        systemSenderEmail = rsSE.getString("config_value");
                    }
                }
            }

            try (PreparedStatement psSenderPass = conn.prepareStatement(senderQuery)) {
                psSenderPass.setString(1, "smtp_sender_password");
                try (ResultSet rsSP = psSenderPass.executeQuery()) {
                    if (rsSP.next()) {
                        systemSenderPassword = rsSE.getString("config_value");
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_OK); // Force 200 so UI can capture the text description safely
            out.print("{\"success\": false, \"error\": \"Database Error Processing: " + e.getMessage() + "\"}");
            return;
        }

        if (targetRecipientEmail == null || targetRecipientEmail.trim().isEmpty()) {
            out.print("{\"success\": false, \"error\": \"Target recipient address missing from database tables.\"}");
            return;
        }
        if (systemSenderEmail == null || systemSenderPassword == null || systemSenderEmail.trim().isEmpty()) {
            out.print("{\"success\": false, \"error\": \"SMTP variables (smtp_sender_email/smtp_sender_password) missing from system_config rows.\"}");
            return;
        }

        String base64ExcelData = request.getParameter("excelData");
        if (base64ExcelData == null || base64ExcelData.isEmpty()) {
            out.print("{\"success\": false, \"error\": \"Payload delivery error: excelData parameter was received empty by backend.\"}");
            return;
        }

        final String finalSenderEmail = systemSenderEmail;
        final String finalSenderPassword = systemSenderPassword;

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
            message.setFrom(new InternetAddress(finalSenderEmail, "BlueVibes System Gateway"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(targetRecipientEmail));
            message.setSubject("BlueVibes | Weekly Status Report - " + username);

            Multipart multipart = new MimeMultipart();

            MimeBodyPart textBodyPart = new MimeBodyPart();
            textBodyPart.setText("Please find your processed Weekly Status Report document attached to this mail log.");
            multipart.addBodyPart(textBodyPart);

            MimeBodyPart attachmentBodyPart = new MimeBodyPart();
            attachmentBodyPart.setContent(excelBytes, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            attachmentBodyPart.setFileName(username + "_Weekly_Status_Report.xlsx");
            multipart.addBodyPart(attachmentBodyPart);

            message.setContent(multipart);
            Transport.send(message);

            out.print("{\"success\": true}");

        } catch (Exception mailError) {
            mailError.printStackTrace();
            out.print("{\"success\": false, \"error\": \"JavaMail Exception Processing Error: " + mailError.getMessage() + "\"}");
        } finally {
            out.flush();
            out.close();
        }
    }
}
