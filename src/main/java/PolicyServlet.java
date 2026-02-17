import java.io.*;
import java.sql.*;
import java.util.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@WebServlet("/PolicyServlet")
@MultipartConfig(maxFileSize = 1024 * 1024 * 10)
public class PolicyServlet extends HttpServlet {

    private Cloudinary cloudinary;

    @Override
    public void init() {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", System.getenv("CLOUDINARY_CLOUD_NAME"),
                "api_key", System.getenv("CLOUDINARY_API_KEY"),
                "api_secret", System.getenv("CLOUDINARY_API_SECRET"),
                "secure", true
        ));
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        File tempFile = null;

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) throw new SQLException("DB Connection Failed");

            if ("delete".equals(action)) {
                String idStr = request.getParameter("id");
                if (idStr != null && !idStr.isEmpty()) {
                    try (PreparedStatement ps = con.prepareStatement("DELETE FROM company_policies WHERE id=?")) {
                        ps.setInt(1, Integer.parseInt(idStr));
                        ps.executeUpdate();
                    }
                }
            } else {
                Part filePart = request.getPart("policyFile");
                if (filePart == null || filePart.getSize() == 0) {
                    response.sendRedirect("policy.html");
                    return;
                }

                String originalName = filePart.getSubmittedFileName();
                tempFile = File.createTempFile("policy_", ".pdf");

                try (InputStream is = filePart.getInputStream();
                     FileOutputStream fos = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                    }
                }

                Map uploadResult = cloudinary.uploader().upload(tempFile, ObjectUtils.asMap(
                        "folder", "bluevibes/policies",
                        "resource_type", "raw",
                        "overwrite", true
                ));

                String fileUrl = uploadResult.get("secure_url").toString();

                String sql = "INSERT INTO company_policies (policy_name, file_path) VALUES (?, ?) ";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, originalName);
                    ps.setString(2, fileUrl);
                    ps.executeUpdate();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
        response.sendRedirect("policy.html");
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String action = request.getParameter("action");

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) return;

            if ("view".equals(action)) {
                String id = request.getParameter("id");
                if (id != null && !id.isEmpty()) {
                    try (PreparedStatement ps = con.prepareStatement("SELECT file_path FROM company_policies WHERE id=?")) {
                        ps.setInt(1, Integer.parseInt(id));
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                String rawUrl = rs.getString("file_path");
                                String downloadUrl = rawUrl.replace("/upload/", "/upload/fl_attachment/");
                                response.sendRedirect(downloadUrl);
                                return;
                            }
                        }
                    }
                }
            }

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, policy_name FROM company_policies ORDER BY id DESC")) {

                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    json.append("{")
                        .append("\"id\":").append(rs.getInt("id"))
                        .append(",\"policy_name\":\"").append(cleanJson(rs.getString("policy_name"))).append("\"")
                        .append("}");
                    first = false;
                }
                json.append("]");
                response.getWriter().print(json.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().print("[]");
        }
    }

    private String cleanJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
