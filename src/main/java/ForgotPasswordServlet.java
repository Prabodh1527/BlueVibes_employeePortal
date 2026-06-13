import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Random;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet; // REQUIRED FOR ROUTING
import javax.servlet.http.*;

// Explicitly maps the servlet path so your HTML form can find it
@WebServlet("/ForgotPasswordServlet")
public class ForgotPasswordServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ForgotPasswordServlet.class.getName());

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = request.getParameter("userEmail");
        
        // This log will print instantly before anything else can fail!
        LOGGER.info("=== FORGOT PASSWORD SERVLET CRITICAL ENTRY POINT TRIGGERED ===");
        LOGGER.info("User Email Received from HTML Form: " + email);
        
        String tempPassword = generateRandomPassword(8);
        Connection con = null;

        try {
            LOGGER.info("Attempting to handshake with the database layer...");
            con = DBConnection.getConnection();
            
            if (con == null) {
                LOGGER.severe("DATABASE ERROR: DBConnection.getConnection() returned NULL!");
                throw new SQLException("Could not establish a database connection handle.");
            }

            String hashedPassword = PasswordUtil.hashPassword(tempPassword);
            PreparedStatement pst = con.prepareStatement("UPDATE users SET password = ? WHERE email = ?");
            pst.setString(1, hashedPassword);
            pst.setString(2, email);
            
            int rowsAffected = pst.executeUpdate();
            LOGGER.info("Database query completed. Rows affected: " + rowsAffected);
            
            if (rowsAffected > 0) {
                LOGGER.info("User verified. Opening Brevo HTTP API tunnel...");
                sendEmailViaAPI(email, tempPassword, "Your Temporary Password");
                response.sendRedirect("forgotpassword.html?status=sent");
            } else {
                LOGGER.warning("Warning: Email '" + email + "' does not exist in the database records.");
                response.sendRedirect("forgotpassword.html?status=notfound");
            }
            
        } catch (Exception e) {
            // This prints the exact line number and code library that is causing the crash
            LOGGER.log(Level.SEVERE, "!! CODESYSTEM CRASH ROUTINE DETECTED !! Error Message: " + e.getMessage(), e);
            response.sendRedirect("forgotpassword.html?status=error");
        } finally {
            if (con != null) {
                try { con.close(); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error closing connection", e); }
            }
        }
    }

    private void sendEmailViaAPI(String toEmail, String tempPassword, String subject) throws Exception {
        String apiKey = System.getenv("BREVO_API_KEY");
        String senderEmail = System.getenv("BREVO_SENDER_EMAIL");

        if (apiKey == null || senderEmail == null) {
            LOGGER.severe("CONFIGURATION ERROR: BREVO_API_KEY or BREVO_SENDER_EMAIL environment variable is missing!");
            throw new RuntimeException("Missing environment keys.");
        }

        URL url = new URL("https://api.brevo.com/v3/smtp/email");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("api-key", apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String jsonPayload = "{"
                + "\"sender\":{\"email\":\"" + senderEmail.trim() + "\",\"name\":\"Support Team\"},"
                + "\"to\":[{\"email\":\"" + toEmail.trim() + "\"}],"
                + "\"subject\":\"" + subject + "\","
                + "\"textContent\":\"Hello,\\n\\nYour temporary login credentials are:\\nPassword: " + tempPassword + "\\n\\nPlease login and update your credentials immediately.\""
                + "}";

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 201 || responseCode == 200) {
            LOGGER.info("API SUCCESS: Brevo accepted the email payload. Code: " + responseCode);
        } else {
            LOGGER.severe("API ERROR: Brevo rejected the email payload. Code: " + responseCode);
            throw new RuntimeException("Brevo API rejection code: " + responseCode);
        }
        conn.disconnect();
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
