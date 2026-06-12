package com.bluevibes; // Verify that this matches your actual project package folder!

import java.io.IOException;
import java.sql.*;
import java.util.Properties;
import java.util.Random;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

// IMPORTANT: Ensure this matches the action URL inside your forgotpassword.html form!
@WebServlet("/ForgotPasswordServlet")
public class ForgotPasswordServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Captures "userEmail" parameter from your front-end form input fields
        String email = request.getParameter("userEmail");
        System.out.println("FORGOT PASSWORD HIT");
        System.out.println("EMAIL = " + email);
        
        // 1. Generate the plain-text random 8-character string for the user's email
        String tempPassword = generateRandomPassword(8);

        try (Connection con = DBConnection.getConnection()) {
            // 2. Check if user exists inside Render DB
            PreparedStatement psCheck = con.prepareStatement("SELECT fullname FROM users WHERE email = ?");
            psCheck.setString(1, email);
            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {
                String fullName = rs.getString("fullname");

                // CRITICAL SECURITY FIX: Hash the temporary password before saving it to the DB!
                // This stops your login system from breaking due to plain-text entries.
                String hashedTempPassword = PasswordUtil.hashPassword(tempPassword);

                // 3. Update DB with the secure BCrypt version of the temporary password
                PreparedStatement psUpdate = con.prepareStatement("UPDATE users SET password = ? WHERE email = ?");
                psUpdate.setString(1, hashedTempPassword);
                psUpdate.setString(2, email);
                psUpdate.executeUpdate();

                // 4. Send the PLAIN-TEXT temporary password via email to their inbox
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
        // DYNAMIC ENV VARIABLES: Grabs credentials from the operating system or hosting environment
        final String fromEmail = System.getenv("SUPPORT_EMAIL");
        final String appPassword = System.getenv("SUPPORT_EMAIL_PASSWORD"); 
        
        System.out.println("USERNAME= " + fromEmail);
        System.out.println("ENTERED sendEmail()");

        // Fail-safe protection check if configurations are missing in the runtime environment
        if (fromEmail == null || appPassword == null) {
            System.err.println("ERROR: SUPPORT_EMAIL or SUPPORT_EMAIL_PASSWORD environment variables are missing!");
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        props.put("mail.debug", "true");

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
            
            // Clean, personalized string message showing the user their plain text key
            message.setText("Hello " + name + ",\n\nYour temporary password for SQUAD is: " + pass +
                    "\n\nPlease login and change it immediately inside your profile dashboard settings.");
            System.out.println("BEFORE MAIL SEND EXECUTION");

            Transport.send(message);
            System.out.println("MAIL SENT SUCCESSFULLY TO " + toEmail);
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