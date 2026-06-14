import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class BirthdayEmailSender {
    private static final Logger LOGGER = Logger.getLogger(BirthdayEmailSender.class.getName());

    public static void sendBirthdayEmail(String toEmail, String employeeName) throws Exception {
        String apiKey = System.getenv("SUPPORT_EMAIL_PASSWORD"); 
        String senderEmail = System.getenv("SUPPORT_EMAIL");
        
        // UPDATED: This now points directly to your newly uploaded clean graphic asset link
        String imageUrl = "https://i.ibb.co/9dg844p/Whats-App-Image-2026-06-14-at-17-57-12.jpg"; 

        if (apiKey == null || senderEmail == null) {
            LOGGER.severe("CONFIGURATION ERROR: Birthday system missing Render environment credentials!");
            return;
        }

        URL url = new URL("https://api.brevo.com/v3/smtp/email");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("api-key", apiKey.trim());
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        // Frame your image beautifully below the custom greeting name
        String htmlContent = "<html><body style='text-align: center; font-family: Arial, sans-serif; background-color: #f4f6f9; padding: 20px;'>"
                + "<div style='max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 30px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.05); text-align: center;'>"
                + "<h1 style='color: #0f2c59; font-size: 28px; margin-bottom: 5px;'>🎉 Happy Birthday, " + employeeName + "! 🎉</h1>"
                + "<p style='color: #555555; font-size: 16px; margin-top: 0;'>The entire team is wishing you an extraordinary day!</p>"
                + "<br/>"
                + "<img src='" + imageUrl + "' alt='Happy Birthday Card' style='width:100%; max-width:500px; border-radius:6px; display: block; margin: 0 auto;' />"
                + "<br/>"
                + "<p style='color: #888888; font-size: 14px; margin-top: 20px;'>Warmest Regards,<br/><span style='color: #0f2c59; font-weight: bold; font-size: 16px;'>BlueVibes Team</span></p>"
                + "</div>"
                + "</body></html>";

        String jsonPayload = "{"
                + "\"sender\":{\"email\":\"" + senderEmail.trim() + "\",\"name\":\"BlueVibes HR Team\"},"
                + "\"to\":[{\"email\":\"" + toEmail.trim() + "\"}],"
                + "\"subject\":\"✨ Happy Birthday " + employeeName + "! ✨\","
                + "\"htmlContent\":\"" + htmlContent.replace("\"", "\\\"") + "\""
                + "}";

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 201 || responseCode == 200) {
            LOGGER.info("Personalized birthday graphic successfully delivered to: " + toEmail);
        } else {
            LOGGER.severe("Brevo rejected birthday payload delivery request. HTTP Code: " + responseCode);
        }
        conn.disconnect();
    }
}
