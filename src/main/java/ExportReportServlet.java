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

    // Hardcoded target routing email requested for auditing delivery
    private static final String AUDITOR_EMAIL = "prasanthram@bluegitalllp.com";

    private static final String DB_URL = "jdbc:postgresql://dpg-d6vrvov5r7bs73f04bpg-a.oregon-postgres.render.com:5432/bluevibes_db_new?sslmode=require";
    private static final String DB_USER = "bluevibes_db_new_user";
    private static final String DB_PASSWORD = "jc0bxNz8YFBiM7BZoa80yWd8T30jb9MD";

    private Connection getConnection() throws Exception {
        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        System.out.println("===> ExportReportServlet INVOKED. Processing inbound JSON packet...");
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String username = "Employee"; 
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("username") != null) {
            username = (String) session.getAttribute("username"); 
        }
        System.out.println("===> Processing report delivery request for user: " + username);

        String systemSenderEmail = null;
        String systemSenderPassword = null;
        String targetRecipientEmail = null;

        String recipientQuery = "SELECT email FROM communication_email WHERE username = ?";
        String senderQuery = "SELECT config_value FROM system_config WHERE config_key = ?";
        
        try (Connection conn = getConnection()) {
            System.out.println("===> Database connection established successfully.");
            
            try (PreparedStatement psRec = conn.prepareStatement(recipientQuery)) {
                psRec.setString(1, username);
                try (ResultSet rsRec = psRec.executeQuery()) {
                    if (rsRec.next()) {
                        targetRecipientEmail = rsRec.getString("email");
                    }
                }
            }
            
            // Fallback strategy if session username mapping isn't found in tables
            if (targetRecipientEmail == null) {
                System.out.println("===> Target email not found for user, attempting fallback to first communication record.");
                try (PreparedStatement psFallback = conn.prepareStatement("SELECT email FROM communication_email LIMIT 1")) {
                    try (ResultSet rsFE = psFallback.executeQuery()) {
                        if (rsFE.next()) { targetRecipientEmail = rsFE.getString("email"); }
                    }
                }
            }
            System.out.println("===> Primary User Routing Confirmed: " + targetRecipientEmail);
            
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
            System.err.println("!!! DATABASE FAILURE IN SERVLET LOOP: " + e.getMessage());
            e.printStackTrace();
            out.print("{\"success\":false,\"error\":\"Database validation error: " + e.getMessage() + "\"}");
            return;
        }

        // Configuration Gatekeepers
        if (systemSenderEmail == null || systemSenderPassword == null || systemSenderEmail.trim().isEmpty()) {
            System.err.println("!!! CONFIGURATION ERROR: System SMTP key configurations are missing from database.");
            out.print("{\"success\":false,\"error\":\"SMTP accounts missing from system_config configuration maps.\"}");
            return;
        }

        // Read raw data payload stream directly 
        StringBuilder jsonBuffer = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                jsonBuffer.append(line);
            }
        }
        String payload = jsonBuffer.toString();
        System.out.println("===> Data payload safely ingested. Character size: " + payload.length());

        // Build spreadsheet dataset format dynamically on the server instance
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("Task ID,Task Description,Customer,Status,% Completed,Start Date,End Date,Comments\n");

        try {
            int arrayStart = payload.indexOf("[");
            int arrayEnd = payload.lastIndexOf("]");
            if (arrayStart != -1 && arrayEnd != -1) {
                payload = payload.substring(arrayStart, arrayEnd + 1);
            }

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

                // Clean delimiter tokens to prevent layout breakage inside standard cell blocks
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
            System.out.println("===> CSV translation matrix compiled successfully.");
        } catch (Exception ex) {
            System.err.println("!!! PARSING RUNTIME ERROR: " + ex.getMessage());
            out.print("{\"success\":false,\"error\":\"Failed compiling JSON payload structure matrix.\"}");
            return;
        }

        final String authEmail = systemSenderEmail;
        final String authPassword = systemSenderPassword;

        try {
            System.out.println("===> Handshaking with Gmail SMTP Gateway at port 587...");
            byte[] fileBytes = csvBuilder.toString().getBytes(StandardCharsets.UTF_8);

            // Modern Explicit TLS Connection Protocols Configuration
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

            Message message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(authEmail, "BlueVibes System Gateway"));

            // Compile the comma-separated dual recipient string logic safely
            StringBuilder recipientsList = new StringBuilder(AUDITOR_EMAIL);
            if (targetRecipientEmail != null && !targetRecipientEmail.trim().isEmpty()) {
                recipientsList.append(",").append(targetRecipientEmail.trim());
            }
            
            System.out.println("===> Preparing multi-recipient broadcast routing payload target to: " + recipientsList.toString());
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientsList.toString()));
            message.setSubject("BlueVibes | Weekly Status Report - " + username);

            Multipart multipart = new MimeMultipart();

            // Part 1: Body Text Context
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText("Hello,\n\nPlease find the processed Weekly Status Report data sheet attached directly to this system message log.");
            multipart.addBodyPart(textPart);

            // Part 2: File Attachment Wrapper (Transmitted cleanly as UTF-8 Spreadsheet)
            MimeBodyPart attachPart = new MimeBodyPart();
            attachPart.setContent(fileBytes, "text/csv; charset=UTF-8");
            attachPart.setFileName(username + "_Weekly_Status_Report.csv");
            multipart.addBodyPart(attachPart);

            message.setContent(multipart);
            Transport.send(message);

            System.out.println("🚀 SUCCESS! Report attachments successfully routed and delivered.");
            out.print("{\"success\":true}");
        } catch (Exception mailError) {
            System.err.println("!!! SMTP TRANSMISSION EXCEPTION: " + mailError.getMessage());
            mailError.printStackTrace();
            out.print("{\"success\":false,\"error\":\"JavaMail engine transmission exception: " + mailError.getMessage() + "\"}");
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
