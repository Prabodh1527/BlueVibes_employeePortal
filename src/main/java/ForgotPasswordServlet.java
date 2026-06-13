import java.io.IOException;
import java.sql.*;
import java.util.Properties;
import java.util.Random;
import javax.mail.*;
import javax.mail.internet.*;
import javax.servlet.ServletException;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.servlet.http.*;

public class ForgotPasswordServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ForgotPasswordServlet.class.getName());

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = request.getParameter("userEmail");
        LOGGER.info("=== FORGOT PASSWORD SERVLET ACTIVATED ===");
        LOGGER.info("Target User Email: " + email);
        
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
                LOGGER.info("Database updated successfully. Triggering secure Brevo pipeline...");
                sendEmail(email, tempPassword, "Your Temporary Password");
                response.sendRedirect("forgotpassword.html?status=sent");
            } else {
                LOGGER.warning("Warning: Email Address '" + email + "' not found in Database 'users' table.");
                response.sendRedirect("forgotpassword.html?status=notfound");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "CRITICAL FAILURE IN FORGOT PASSWORD SERVLET:", e);
            response.sendRedirect("forgotpassword.html?status=error");
        } finally {
            if (con != null) {
                try { con.close(); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error closing connection", e); }
            }
        }
    }

    private void sendEmail(String to, String tempPassword, String subject) throws MessagingException {
        final String smtpUser = System.getenv("SUPPORT_EMAIL");
        final String smtpKey = System.getenv("SUPPORT_EMAIL_PASSWORD"); 
        final String senderEmail = System.getenv("BREVO_SENDER_EMAIL");

        if (smtpUser == null || smtpKey == null || senderEmail == null) {
            LOGGER.severe("ERROR: Missing Environment Variables!");
            throw new MessagingException("Missing email provider configuration variables.");
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp-relay.brevo.com"); 
        props.put("mail.smtp.port", "465"); 
        props.put("mail.smtp.auth", "true");
        
        // Strict SSL rules to force JavaMail over the secure port
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.ssl.required", "true");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        
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

        Transport.send(message);
        LOGGER.info("SUCCESS: Message securely dispatched via Brevo Relay to: " + to);
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
