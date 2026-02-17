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
        String cloudName = System.getenv("CLOUDINARY_CLOUD_NAME");
        String apiKey = System.getenv("CLOUDINARY_API_KEY");
        String apiSecret = System.getenv("CLOUDINARY_API_SECRET");

        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    // ================= UPLOAD & DELETE (Admin Side) =================
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        
        // ADDED DELETE LOGIC
        if ("delete".equals(action)) {
            String idStr = request.getParameter("id");
            if (idStr != null && !idStr.isEmpty()) {
                try (Connection con = DBConnection.getConnection()) {
                    try (PreparedStatement ps = con.prepareStatement("DELETE FROM employee_payslips WHERE id=?")) {
                        ps.setInt(1, Integer.parseInt(idStr));
                        ps.executeUpdate();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            return;
        }

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
                            "overwrite", true
                    )
            );

            String fileUrl = uploadResult.get("secure_url").toString();

            // FIXED: Using table name 'employee_payslips' and columns 'user_email', 'month_year', 'file_url'
            con.setAutoCommit(false);
            try {
                try (PreparedStatement del = con.prepareStatement("DELETE FROM employee_payslips WHERE user_email=? AND month_year=?")) {
                    del.setString(1, userEmail);
                    del.setString(2, monthYear);
                    del.executeUpdate();
                }

                try (PreparedStatement ps = con.prepareStatement("INSERT INTO employee_payslips (user_email, month_year, file_url) VALUES (?, ?, ?)")) {
                    ps.setString(1, userEmail);
                    ps.setString(2, monthYear);
                    ps.setString(3, fileUrl);
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

                // FIXED: Table 'employee_payslips', Column 'file_url'
                try (PreparedStatement ps = con.prepareStatement("SELECT file_url FROM employee_payslips WHERE id=?")) {
                    ps.setInt(1, Integer.parseInt(id));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String fileUrl = rs.getString("file_url");
                            // Logic to force a download via Cloudinary transformation
                            String downloadUrl = fileUrl.replace("/upload/", "/upload/fl_attachment/");
                            response.sendRedirect(downloadUrl);
                            return;
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

                // FIXED: Table 'employee_payslips'
                String sql = "SELECT id, month_year, uploaded_at FROM employee_payslips WHERE user_email=? ORDER BY uploaded_at DESC";
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

