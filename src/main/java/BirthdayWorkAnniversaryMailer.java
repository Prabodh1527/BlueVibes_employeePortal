import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class BirthdayWorkAnniversaryMailer {

private static final String API_KEY =
"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";

private static final String SENDER_EMAIL =
"gprabodhchandra@gmail.com";

private static final String BIRTHDAY_IMAGE =
"https://bluevibes-employeeportal.onrender.com/birthday.jpg";

private static final String ANNIVERSARY_IMAGE =
"https://bluevibes-employeeportal.onrender.com/work_anniversary.jpg";

public static void sendBirthdayMail(
        String recipientEmail,
        String employeeName)
        throws Exception {

    String html =

    "<html><body style='font-family:Arial,sans-serif;background:#f4f6f9;padding:20px;'>"

    +

    "<div style='max-width:650px;margin:auto;background:#ffffff;padding:30px;border-radius:12px;'>"

    +

    "<h1 style='text-align:center;color:#0f2c59;'>🎉 Happy Birthday</h1>"

    +

    "<h2 style='text-align:center;color:#0284c7;font-size:30px;'>"
    + employeeName +
    "</h2>"

    +

    "<p style='text-align:center;font-size:16px;color:#555;'>"

    +

    "The entire BlueVibes team wishes you a wonderful birthday filled with happiness, success, good health and memorable moments."

    +

    "</p>"

    +

    "<img src='" + BIRTHDAY_IMAGE + "' style='width:100%;max-width:550px;display:block;margin:auto;border-radius:8px;'>"

    +

    "<br>"

    +

    "<p style='text-align:center;color:#666;'>"

    +

    "Warm Regards,<br><b>BlueVibes Team</b>"

    +

    "</p>"

    +

    "</div></body></html>";

    sendEmail(
            recipientEmail,
            "🎉 Happy Birthday " + employeeName + "!",
            html);
}

public static void sendAnniversaryMail(
        String recipientEmail,
        String employeeName)
        throws Exception {

    String html =

    "<html><body style='font-family:Arial,sans-serif;background:#f4f6f9;padding:20px;'>"

    +

    "<div style='max-width:650px;margin:auto;background:#ffffff;padding:30px;border-radius:12px;'>"

    +

    "<h1 style='text-align:center;color:#0f2c59;'>🏆 Happy Work Anniversary</h1>"

    +

    "<h2 style='text-align:center;color:#0284c7;font-size:30px;'>"
    + employeeName +
    "</h2>"

    +

    "<p style='text-align:center;font-size:16px;color:#555;'>"

    +

    "Thank you for your dedication, commitment and valuable contributions to BlueVibes. Wishing you continued success and many more milestones ahead."

    +

    "</p>"

    +

    "<img src='" + ANNIVERSARY_IMAGE + "' style='width:100%;max-width:550px;display:block;margin:auto;border-radius:8px;'>"

    +

    "<br>"

    +

    "<p style='text-align:center;color:#666;'>"

    +

    "Warm Regards,<br><b>BlueVibes Team</b>"

    +

    "</p>"

    +

    "</div></body></html>";

    sendEmail(
            recipientEmail,
            "🏆 Happy Work Anniversary " + employeeName + "!",
            html);
}

private static void sendEmail(
        String recipientEmail,
        String subject,
        String htmlContent)
        throws Exception {

    URL url =
    new URL("https://api.brevo.com/v3/smtp/email");

    HttpURLConnection conn =
    (HttpURLConnection) url.openConnection();

    conn.setRequestMethod("POST");

    conn.setRequestProperty(
            "api-key",
            API_KEY);

    conn.setRequestProperty(
            "Content-Type",
            "application/json");

    conn.setRequestProperty(
            "Accept",
            "application/json");

    conn.setDoOutput(true);

    String jsonPayload =

    "{"

    +

    "\"sender\":{"

    +

    "\"name\":\"BlueVibes Portal\","

    +

    "\"email\":\"" + SENDER_EMAIL + "\""

    +

    "},"

    +

    "\"to\":[{\"email\":\""

    + recipientEmail +

    "\"}],"

    +

    "\"subject\":\""

    + subject +

    "\","

    +

    "\"htmlContent\":\""

    + htmlContent.replace("\"","\\\"")

    +

    "\""

    +

    "}";

    try(OutputStream os =
        conn.getOutputStream()) {

        byte[] input =
        jsonPayload.getBytes(
        StandardCharsets.UTF_8);

        os.write(
        input,
        0,
        input.length);
    }

    System.out.println(
    "MAIL STATUS = "
    + conn.getResponseCode());

    conn.disconnect();
}

}
