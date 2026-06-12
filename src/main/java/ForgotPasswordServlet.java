package com.bluevibes; // Matches your project package structure

import java.io.IOException;
import java.sql.*;
import java.util.Properties;
import java.util.Random;

// Replaced jakarta libraries with javax for Tomcat 9 compatibility
import javax.mail.*;
import javax.mail.internet.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/ForgotPasswordServlet")
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
            // Database update logic
            con = DBConnection.getConnection();
            String hashedPassword = PasswordUtil.hashPassword(tempPassword);
            
            PreparedStatement pst = con.prepareStatement("UPDATE employee SET password = ? WHERE email = ?");
            pst.setString(1, hashedPassword);
            pst.setString(2, email);
            
            int rowsAffected = pst.executeUpdate();
            
            if (rowsAffected > 0) {
                // Trigger the email transmission using secure SSL Port 465
                sendEmail(email, tempPassword, "Your Temporary Password");
                mySession.setAttribute("status", "success");
            } else {
                mySession.setAttribute("status", "emailNotFound");
            }
            
            response.sendRedirect("forgotpassword.jsp");
            
        } catch (Exception e) {
            e.printStackTrace();
            mySession.setAttribute("status", "error");
            response.sendRedirect("forgotpassword.jsp");
        } finally {
            if (con != null) {
                try { con.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    private void sendEmail(String to, String tempPassword, String subject) throws MessagingException {
        // Mail server properties configured for SSL Port 465 bypass
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        // Update these credentials with your actual sender details/App Password
        final String username = "your-email@gmail.com"; 
        final String password = "your-app-password"; 

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setText("Hello,\n\nYour temporary login credentials are:\nPassword: " + tempPassword + "\n\nPlease login and update your password immediately.");

        Transport.send(message);
        System.out.println("Email sent successfully to: " + to);
    }

    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}