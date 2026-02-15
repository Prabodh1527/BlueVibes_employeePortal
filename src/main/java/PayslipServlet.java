import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.sql.*;
import java.util.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@WebServlet("/PayslipServlet")
@MultipartConfig(maxFileSize = 1024 * 1024 * 10)
public class PayslipServlet extends HttpServlet {

    private Cloudinary cloudinary;

    @Override
    public void init() {
        // Safe check for environment variables
        String cloudName = System.getenv("CLOUDINARY_CLOUD_NAME");
        String apiKey = System.getenv("CLOUDINARY_API_KEY");
        String apiSecret = System.getenv("CLOUDINARY_API_SECRET");

        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }

    // ================= UPLOAD (Admin Side) =================
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        if (!"upload".equals(action)) return;

        String userEmail = request.getParameter("userEmail");
        String monthYear = request.getParameter("monthYear");
        Part filePart = request.getPart("payslipFile");

        if (filePart == null || userEmail == null || monthYear == null) {
            response.sendRedirect("genpayslip.html?status=error");
            return;
        }

        File tempFile = null;

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) throw new SQLException("DB Connection Failed");

            tempFile = File.createTempFile("payslip_", ".pdf");

            try (InputStream is = filePart.getInputStream();
                 FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            // Upload to Cloudinary
            Map uploadResult = cloudinary.uploader().upload(
                    tempFile,
                    ObjectUtils.asMap(
                            "resource_type", "raw",
                            "folder", "bluevibes/payslips",
                            "public_id", (userEmail + "_" + monthYear).replaceAll("[^a-zA-Z0-9_-]", "_"),
                            "overwrite", true
                    )
            );

            String fileUrl = uploadResult.get("secure_url").toString();
            String originalName = filePart.getSubmittedFileName();

            // PostgreSQL Transaction: Delete old entry if exists, then insert new
            con.setAutoCommit(false);
            try {
                try (PreparedStatement del = con.prepareStatement("DELETE FROM user_payslips WHERE user_email=? AND month_year=?")) {
                    del.setString(1, userEmail);
                    del.setString(2, monthYear);
                    del.executeUpdate();
                }

                try (PreparedStatement ps = con.prepareStatement("INSERT INTO user_payslips (user_email, month_year, file_path, file_name) VALUES (?, ?, ?, ?)")) {
                    ps.setString(1, userEmail);
                    ps.setString(2, monthYear);
                    ps.setString(3, fileUrl);
                    ps.setString(4, originalName);
                    ps.executeUpdate();
                }
                con.commit();
                response.sendRedirect("genpayslip.html?status=success");
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("genpayslip.html?status=error");
        } finally {
            if (tempFile != null && tempFile.exists()) tempFile.delete();
        }
    }

    // ================= VIEW / HISTORY (User Side) =================
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String action = request.getParameter("action");

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) return;

            if ("view".equals(action)) {
                String id = request.getParameter("id");
                if (id == null) return;

                try (PreparedStatement ps = con.prepareStatement("SELECT file_path, file_name FROM user_payslips WHERE id=?")) {
                    ps.setInt(1, Integer.parseInt(id));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String fileUrl = rs.getString("file_path");
                            String fileName = rs.getString("file_name");

                            URL url = new URL(fileUrl);
                            URLConnection conn = url.openConnection();

                            response.setContentType("application/pdf");
                            response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");

                            try (InputStream in = conn.getInputStream();
                                 OutputStream out = response.getOutputStream()) {
                                byte[] buffer = new byte[4096];
                                int len;
                                while ((len = in.read(buffer)) != -1) {
                                    out.write(buffer, 0, len);
                                }
                            }
                        }
                    }
                }
            }

            else if ("history".equals(action)) {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                String email = request.getParameter("userEmail");
                if (email == null) {
                    response.getWriter().write("[]");
                    return;
                }

                String sql = "SELECT id, month_year, file_name, uploaded_at FROM user_payslips WHERE user_email=? ORDER BY uploaded_at DESC";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, email);
                    try (ResultSet rs = ps.executeQuery()) {
                        StringBuilder json = new StringBuilder("[");
                        boolean first = true;
                        while (rs.next()) {
                            if (!first) json.append(",");
                            
                            Timestamp uploadedAt = rs.getTimestamp("uploaded_at");
                            String dateOnly = (uploadedAt != null) ? uploadedAt.toString().split(" ")[0] : "N/A";

                            json.append("{")
                                    .append("\"id\":").append(rs.getInt("id"))
                                    .append(",\"month_year\":\"").append(clean(rs.getString("month_year")))
                                    .append("\",\"file_name\":\"").append(clean(rs.getString("file_name")))
                                    .append("\",\"uploaded_at\":\"").append(dateOnly)
                                    .append("\"}");
                            first = false;
                        }
                        json.append("]");
                        response.getWriter().write(json.toString());
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("[]");
        }
    }

    private String clean(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
