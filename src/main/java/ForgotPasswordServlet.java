import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.Properties;
import java.util.Random;

// Using javax libraries for Tomcat 9 compatibility
import javax.mail.*;
import javax.mail.internet.*;
import javax.servlet.ServletException;
import javax.servlet.http.*;

public class ForgotPasswordServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = request.getParameter("userEmail");
        System.out.println("FORGOT PASSWORD HIT");
        System.out.println("EMAIL = " + email);
        
        String tempPassword = generateRandomPassword(8);
        HttpSession mySession = request.getSession();

        Connection con = null;
        try {
            // Bypass package visibility blocks using Reflection to get the DB Connection
            Class<?> dbClass = Class.forName("com.bluevibes.DBConnection");
            Method getConnectionMethod = dbClass.getMethod("getConnection");
            con = (Connection) getConnectionMethod.invoke(null);

            // Bypass package visibility blocks using Reflection to hash the password
            Class<?> cryptoClass = Class.forName("com.bluevibes.PasswordUtil");
            Method hashMethod = cryptoClass.getMethod("hashPassword", String.class);
            String hashedPassword = (String) hashMethod.invoke(null, tempPassword);
            
            PreparedStatement pst = con.prepareStatement("UPDATE users SET password = ? WHERE email = ?");
            pst.setString(1, hashedPassword);
            pst.setString(2, email);
            
            int rowsAffected = pst.executeUpdate();
            
            if (rowsAffected > 0) {
                sendEmail(email, tempPassword, "Your Temporary Password");
                response.sendRedirect("forgotpassword.html?status=sent");
            } else {
                response.sendRedirect("forgotpassword.html?status=notfound");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("forgotpassword.html?status=error");
        } finally {
            if (con != null) {
                try { con.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    private void sendEmail(String to, String tempPassword, String subject) throws MessagingException {
        final String fromEmail = System.getenv("SUPPORT_EMAIL");
        final String appPassword = System.getenv("SUPPORT_EMAIL_PASSWORD"); 

        if (fromEmail == null || appPassword == null) {
            System.err.println("ERROR: SUPPORT_EMAIL or SUPPORT_EMAIL_PASSWORD variables missing on Render!");
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, appPassword);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromEmail));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setText("Hello,\n\nYour temporary login credentials are:\nPassword: " + tempPassword + "\n\nPlease login and update your password immediately.");

        Transport.send(message);
        System.out.println("Email sent successfully to: " + to);
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
