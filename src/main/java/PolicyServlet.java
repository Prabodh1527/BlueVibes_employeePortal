import java.io.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
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
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", System.getenv("CLOUDINARY_CLOUD_NAME"));
        config.put("api_key", System.getenv("CLOUDINARY_API_KEY"));
        config.put("api_secret", System.getenv("CLOUDINARY_API_SECRET"));
        cloudinary = new Cloudinary(config);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");

        try (Connection con = DBConnection.getConnection()) {

            if ("delete".equals(action)) {
                String idStr = request.getParameter("id");
                if (idStr != null) {
                    PreparedStatement ps = con.prepareStatement("DELETE FROM company_policies WHERE id = ?");
                    ps.setInt(1, Integer.parseInt(idStr));
                    ps.executeUpdate();
                }
            } else {
                Part filePart = request.getPart("policyFile");
                if (filePart != null && filePart.getSize() > 0) {

                    String fileName = filePart.getSubmittedFileName();
                    String uniqueName = System.currentTimeMillis() + "_" + fileName;

                    Map uploadResult = cloudinary.uploader().upload(filePart.getInputStream(),
                            ObjectUtils.asMap(
                                    "public_id", "policy_" + uniqueName,
                                    "resource_type", "raw"
                            ));

                    String cloudUrl = uploadResult.get("secure_url").toString();

                    PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO company_policies (policy_name, file_path) VALUES (?, ?)");
                    ps.setString(1, fileName);
                    ps.setString(2, cloudUrl);
                    ps.executeUpdate();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        response.sendRedirect("policy.html");
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        String fileId = request.getParameter("id");

        if ("view".equals(action) && fileId != null && !fileId.trim().isEmpty()) {
            handleDownload(response, fileId);
            return;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try (Connection con = DBConnection.getConnection()) {
            ResultSet rs = con.createStatement().executeQuery(
                    "SELECT id, policy_name FROM company_policies ORDER BY id DESC");

            StringBuilder json = new StringBuilder("[");
            boolean first = true;

            while (rs.next()) {
                if (!first) json.append(",");
                int id = rs.getInt("id");
                json.append("{")
                        .append("\"id\":").append(id).append(",")
                        .append("\"name\":\"").append(rs.getString("policy_name")).append("\"")
                        .append("}");
                first = false;
            }

            json.append("]");
            response.getWriter().print(json.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleDownload(HttpServletResponse response, String id) throws IOException {
        try (Connection con = DBConnection.getConnection()) {

            PreparedStatement ps = con.prepareStatement(
                    "SELECT policy_name, file_path FROM company_policies WHERE id = ?");
            ps.setInt(1, Integer.parseInt(id));
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                response.sendRedirect(rs.getString("file_path"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(500);
        }
    }
}
