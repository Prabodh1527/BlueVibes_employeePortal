import java.io.BufferedReader;
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

    private static final String SMTP_HOST = "smtp.gmail.com"; 
    private static final String SMTP_PORT = "587";

    private static final String DB_URL = "jdbc:postgresql://dpg-d6vrvov5r7bs73f04bpg-a.oregon-postgres.render.com:5432/bluevibes_db_new?sslmode=require";
    private static final String DB_USER = "bluevibes_db_new_user";
    private static final String DB_PASSWORD = "jc0bxNz8YFBiM7BZoa80yWd8T30jb9MD";

    private Connection getConnection() throws Exception {
        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String username = "Employee"; 
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("username") != null) {
            username = (String) session.getAttribute("username"); 
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
            
            if (targetRecipientEmail == null) {
                try (PreparedStatement psFallback = conn.prepareStatement("SELECT email FROM communication_email LIMIT 1")) {
                    try (ResultSet rsFE = psFallback.executeQuery()) {
                        if (rsFE.next()) { targetRecipientEmail = rsFE.getString("email"); }
                    }
                }
            }
            
            try (PreparedStatement psSenderEmail = conn.prepareStatement(senderQuery)) {
                psSenderEmail.setString(1, "smtp_sender_email");
                try (ResultSet rsSE = psSenderEmail.executeQuery()) {
                    if (rsSE.next()) { systemSenderEmail = rsSE.getString("config_value"); }
                }
            }

            try (PreparedStatement psSenderPass = conn.prepareStatement(senderQuery)) {
                psSenderPass.setString(1, "smtp_sender_password");
                try (ResultSet rsSP = psSenderPass.executeQuery()) {
                    if (rsSP.next()) { systemSenderPassword = rsSP.getString("config_value"); }
                }
            }
        } catch (Exception e) {
            out.print("{\"success\":false,\"error\":\"Database connection error: " + e.getMessage() + "\"}");
            return;
        }

        if (targetRecipientEmail == null || targetRecipientEmail.trim().isEmpty()) {
            out.print("{\"success\":false,\"error\":\"Recipient profile missing from email tables.\"}");
            return;
        }
        if (systemSenderEmail == null || systemSenderPassword == null || systemSenderEmail.trim().isEmpty()) {
            out.print("{\"success\":false,\"error\":\"SMTP accounts not configured in system_config table rows.\"}");
            return;
        }

        // READ RAW JSON FROM REQ BODY (Prevents Render payload size drops)
        StringBuilder jsonBuffer = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                jsonBuffer.append(line);
            }
        }

        String rawJson = jsonBuffer.toString();
        String base64ExcelData = "";
        
        // Native string parsing to extract excelData content out of the raw JSON
        if (rawJson.contains("\"excelData\":\"")) {
            int startIdx = rawJson.indexOf("\"excelData\":\"") + 13;
            int endIdx = rawJson.indexOf("\"", startIdx);
            if (startIdx > 12 && endIdx > startIdx) {
                base64ExcelData = rawJson.substring(startIdx, endIdx);
            }
        }

        if (base64ExcelData.trim().isEmpty()) {
            out.print("{\"success\":false,\"error\":\"Excel spreadsheet data extraction failed or body missing.\"}");
            return;
        }

        final String authEmail = systemSenderEmail;
        final String authPassword = systemSenderPassword;

        try {
            byte[] excelBytes = Base64.getDecoder().decode(base64ExcelData.trim());

            Properties props = new Properties();
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");

            Session mailSession = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(authEmail, authPassword);
                }
            });

            Message message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(authEmail, "BlueVibes System Gateway"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(targetRecipientEmail));
            message.setSubject("BlueVibes | Weekly Status Report - " + username);

            Multipart multipart = new MimeMultipart();

            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText("Please find your processed Weekly Status Report document attached to this mail log.");
            multipart.addBodyPart(textPart);

            MimeBodyPart attachPart = new MimeBodyPart();
            attachPart.setContent(excelBytes, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            attachPart.setFileName(username + "_Weekly_Status_Report.xlsx");
            multipart.addBodyPart(attachPart);

            message.setContent(multipart);
            Transport.send(message);

            out.print("{\"success\":true}");
        } catch (Exception mailError) {
            out.print("{\"success\":false,\"error\":\"JavaMail transmission exception: " + mailError.getMessage() + "\"}");
        } finally {
            out.flush();
            out.close();
        }
    }
}
