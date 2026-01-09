import java.io.IOException;
import java.sql.*;
import java.util.Properties;
import java.util.Random;
import javax.mail.*;
import javax.mail.internet.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/ForgotPasswordServlet")
public class ForgotPasswordServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = request.getParameter("userEmail");
        String tempPassword = generateRandomPassword(8);

        try (Connection con = DBConnection.getConnection()) {
            // 1. Check if user exists
            PreparedStatement psCheck = con.prepareStatement("SELECT fullname FROM users WHERE email = ?");
            psCheck.setString(1, email);
            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {
                String fullName = rs.getString("fullname");

                // 2. Update DB with temporary password
                PreparedStatement psUpdate = con.prepareStatement("UPDATE users SET password = ? WHERE email = ?");
                psUpdate.setString(1, tempPassword);
                psUpdate.setString(2, email);
                psUpdate.executeUpdate();

                // 3. Send the Email
                sendEmail(email, fullName, tempPassword);
                response.sendRedirect("forgotpassword.html?status=sent");
            } else {
                response.sendRedirect("forgotpassword.html?status=notfound");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("forgotpassword.html?status=error");
        }
    }

    private void sendEmail(String toEmail, String name, String pass) {
        // --- ENTER YOUR DETAILS HERE ---
        final String fromEmail = "gprabodhchandra@gmail.com";
        final String appPassword = "gugkmhmxclagqtnp"; // NO SPACES

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, appPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("SQUAD | Your Temporary Password");
            message.setText("Hello " + name + ",\n\nYour temporary password for SQUAD is: " + pass +
                    "\n\nPlease login and change it immediately in your profile.");

            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }
}
