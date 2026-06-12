import java.io.IOException;
import java.sql.*;
import java.util.Properties;
import java.util.Random;
import javax.mail.*;
import javax.mail.internet.*;
import javax.servlet.ServletException;
import javax.servlet.http.*;

public class ForgotPasswordServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = request.getParameter("userEmail");
        System.out.println("=== FORGOT PASSWORD SERVLET ACTIVATED ===");
        System.out.println("Target User Email: " + email);
        
        String tempPassword = generateRandomPassword(8);

        Connection con = null;
        try {
            con = DBConnection.getConnection();
            String hashedPassword = PasswordUtil.hashPassword(tempPassword);
            
            PreparedStatement pst = con.prepareStatement("UPDATE users SET password = ? WHERE email = ?");
            pst.setString(1, hashedPassword);
            pst.setString(2, email);
            
            int rowsAffected = pst.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("Database updated successfully. Triggering Brevo pipeline...");
                sendEmail(email, tempPassword, "Your Temporary Password");
                response.sendRedirect("forgotpassword.html?status=sent");
            } else {
                System.out.println("Warning: Email Address '" + email + "' not found in Database 'users' table.");
                response.sendRedirect("forgotpassword.html?status=notfound");
            }
            
        } catch (Exception e) {
            System.err.println("CRITICAL FAILURE IN FORGOT PASSWORD SERVLET:");
            e.printStackTrace();
            response.sendRedirect("forgotpassword.html?status=error");
        } finally {
            if (con != null) {
                try { con.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    private void sendEmail(String to, String tempPassword, String subject) throws MessagingException {
        final String smtpUser = System.getenv("SUPPORT_EMAIL");
        final String smtpKey = System.getenv("SUPPORT_EMAIL_PASSWORD"); 
        
        // Use a fallback to prevent strict Brevo authentication rejection rules
        String senderEmail = System.getenv("BREVO_SENDER_EMAIL");
        if (senderEmail == null || senderEmail.trim().isEmpty()) {
            senderEmail = smtpUser; // Fallback to login user if not specified
        }

        if (smtpUser == null || smtpKey == null) {
            System.err.println("ERROR: SUPPORT_EMAIL or SUPPORT_EMAIL_PASSWORD variables are completely missing in Render Environment!");
            return;
        }

        System.out.println("Initializing SMTP Configurations for smtp-relay.brevo.com...");
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp-relay.brevo.com"); 
        props.put("mail.smtp.port", "587"); 
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true"); 
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        
        // Strict network limits to break out of infinite spinner cycles
        props.put("mail.smtp.connectiontimeout", "6000"); 
        props.put("mail.smtp.timeout", "6000");           

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(smtpUser, smtpKey);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(senderEmail));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setText("Hello,\n\nYour temporary login credentials are:\nPassword: " + tempPassword + "\n\nPlease login and update your password immediately.");

        System.out.println("Handshaking with smtp-relay.brevo.com. Transmitting data packet...");
        Transport.send(message);
        System.out.println("SUCCESS: Message acknowledged and dispatched by Brevo Relay to: " + to);
    }

    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
