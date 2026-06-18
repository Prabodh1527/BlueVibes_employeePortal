import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
    private static final String AUDITOR_EMAIL = "prasanthram@bluegitalllp.com";

    // Fixed External URL and capitalization typo ('B' instead of 'b')
    private static final String DB_URL = "jdbc:postgresql://dpg-d6vrvov5r7bs73f04bpg-a.oregon-postgres.render.com:5432/bluevibes_db_new?sslmode=require&sslfactory=org.postgresql.ssl.NonValidatingFactory";
    private static final String DB_USER = "bluevibes_db_new_user";
    private static final String DB_PASSWORD = "jc0bxNz8YFBiM7BZoa80yWd8T30JB9MD";

    private Connection getConnection() throws Exception {
        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(false);
        String username = null;

        if (session != null) {
            if (session.getAttribute("username") != null) username = (String) session.getAttribute("username");
            else if (session.getAttribute("user") != null) username = (String) session.getAttribute("user");
            else if (session.getAttribute("employeeName") != null) username = (String) session.getAttribute("employeeName");
            else if (session.getAttribute("email") != null) username = (String) session.getAttribute("email");
        }

        if (username == null || username.trim().isEmpty()) {
            username = "Employee"; 
        }

        String systemSenderEmail = null;
        String systemSenderPassword = null;
        String targetRecipientEmail = null;

        try (Connection conn = getConnection()) {
            String recipientQuery = "SELECT email FROM communication_email WHERE username = ?";
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
            
            String senderQuery = "SELECT config_value FROM system_config WHERE config_key = ?";
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
            System.err.println("!!! DB ACCESS ERROR: " + e.getMessage());
            out.print("{\"success\":false,\"error\":\"Database Connection Failure: " + e.getMessage() + "\"}");
            out.flush();
            return;
        }

        if (systemSenderEmail == null || systemSenderPassword == null || systemSenderEmail.trim().isEmpty()) {
            out.print("{\"success\":false,\"error\":\"SMTP settings are missing inside your system_config database table.\"}");
            out.flush();
            return;
        }

        StringBuilder jsonBuffer = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                jsonBuffer.append(line);
            }
        }
        String payload = jsonBuffer.toString();

        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("Task ID,Task Description,Customer,Status,% Completed,Start Date,End Date,Comments\n");

        try {
            int searchIdx = 0;
            while ((searchIdx = payload.indexOf("{", searchIdx)) != -1) {
                int endBox = payload.indexOf("}", searchIdx);
                if (endBox == -1) break;
                
                String objectBlock = payload.substring(searchIdx, endBox + 1);
                
                String taskId = extractValue(objectBlock, "taskId");
                String taskDesc = extractValue(objectBlock, "taskDesc");
                String customer = extractValue(objectBlock, "customer");
                String status = extractValue(objectBlock, "status");
                String percent = extractValue(objectBlock, "percent");
                String startDate = extractValue(objectBlock, "startDate");
                String endDate = extractValue(objectBlock, "endDate");
                String comments = extractValue(objectBlock, "comments");

                if (taskId.isEmpty()) taskId = "N/A";
                if (taskDesc.isEmpty()) taskDesc = "Unassigned Task";
                if (customer.isEmpty()) customer = "Internal";
                if (status.isEmpty()) status = "InProgress";
                if (percent.isEmpty()) percent = "0";
                if (startDate.isEmpty()) startDate = "-";
                if (endDate.isEmpty()) endDate = "-";
                if (comments.isEmpty()) comments = "-";

                comments = comments.replace(",", " ").replace("\"", "'").replace("\n", " ").trim();
                taskDesc = taskDesc.replace(",", " ").trim();
                customer = customer.replace(",", " ").trim();
                status = status.replace(",", " ").trim();

                csvBuilder.append(taskId).append(",")
                          .append(taskDesc).append(",")
                          .append(customer).append(",")
                          .append(status).append(",")
                          .append(percent).append(",")
                          .append(startDate).append(",")
                          .append(endDate).append(",")
                          .append(comments).append("\n");

                searchIdx = endBox + 1;
            }
        } catch (Exception ex) {
            out.print("{\"success\":false,\"error\":\"JSON compilation matrix syntax failure: " + ex.getMessage() + "\"}");
            out.flush();
            return;
        }

        final String authEmail = systemSenderEmail;
        final String authPassword = systemSenderPassword;

        try {
            byte[] fileBytes = csvBuilder.toString().getBytes(StandardCharsets.UTF_8);

            Properties props = new Properties();
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
            props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
            props.put("mail.smtp.connectiontimeout", "15000"); 
            props.put("mail.smtp.timeout", "25000");           

            Session mailSession = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(authEmail, authPassword);
                }
            });

            MimeMessage message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(authEmail, "BlueVibes System Gateway"));

            String finalRecipients = AUDITOR_EMAIL; 
            if (targetRecipientEmail != null && !targetRecipientEmail.trim().isEmpty() && !targetRecipientEmail.equalsIgnoreCase("null")) {
                finalRecipients = AUDITOR_EMAIL + "," + targetRecipientEmail.trim();
            }
            
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(finalRecipients));
            message.setSubject("BlueVibes | Weekly Status Report - " + username);

            Multipart multipart = new MimeMultipart();

            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText("Hello,\n\nPlease find the generated spreadsheet attached below.");
            multipart.addBodyPart(textPart);

            MimeBodyPart attachPart = new MimeBodyPart();
            attachPart.setContent(fileBytes, "text/csv; charset=UTF-8");
            attachPart.setFileName(username + "_Weekly_Status_Report.csv");
            multipart.addBodyPart(attachPart);

            message.setContent(multipart);
            Transport.send(message);

            out.print("{\"success\":true}");
        } catch (Exception mailError) {
            System.err.println("!!! SMTP ENGINE FAILURE: " + mailError.getMessage());
            out.print("{\"success\":false,\"error\":\"Mail Transmission Failure: " + mailError.getMessage() + "\"}");
        } finally {
            out.flush();
            out.close();
        }
    }

    private String extractValue(String block, String key) {
        String matchToken = "\"" + key + "\":\"";
        int start = block.indexOf(matchToken);
        if (start != -1) {
            start += matchToken.length();
            int end = block.indexOf("\"", start);
            return (end != -1) ? block.substring(start, end) : "";
        }
        matchToken = "\"" + key + "\":";
        start = block.indexOf(matchToken);
        if (start != -1) {
            start += matchToken.length();
            int end = block.indexOf(",", start);
            if (end == -1) end = block.indexOf("}", start);
            return (end != -1) ? block.substring(start, end).replace("\"", "").trim() : "";
        }
        return "";
    }
}
