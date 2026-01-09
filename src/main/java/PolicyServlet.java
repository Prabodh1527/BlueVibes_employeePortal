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
                "api_secret", System.getenv("CLOUDINARY_API_SECRET")
        ));
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");

        try (Connection con = DBConnection.getConnection()) {

            if ("delete".equals(action)) {
                String idStr = request.getParameter("id");
                if (idStr != null) {
                    PreparedStatement ps = con.prepareStatement("DELETE FROM company_policies WHERE id=?");
                    ps.setInt(1, Integer.parseInt(idStr));
                    ps.executeUpdate();
                }
            } else {
                Part filePart = request.getPart("policyFile");

                if (filePart != null && filePart.getSize() > 0) {
                    String originalName = filePart.getSubmittedFileName();

                    Map uploadResult = cloudinary.uploader().upload(
                            filePart.getInputStream(),
                            ObjectUtils.asMap(
                                    "resource_type", "raw",
                                    "folder", "bluevibes/policies",
                                    "public_id", System.currentTimeMillis() + "_" + originalName
                            )
                    );

                    String fileUrl = uploadResult.get("secure_url").toString();

                    PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO company_policies (policy_name, file_path) VALUES (?, ?)");
                    ps.setString(1, originalName);
                    ps.setString(2, fileUrl);
                    ps.executeUpdate();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        response.sendRedirect("policy.html");
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String action = request.getParameter("action");

        if ("view".equals(action)) {
            try (Connection con = DBConnection.getConnection()) {
                String id = request.getParameter("id");
                PreparedStatement ps = con.prepareStatement("SELECT file_path FROM company_policies WHERE id=?");
                ps.setInt(1, Integer.parseInt(id));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    response.sendRedirect(rs.getString("file_path"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        response.setContentType("application/json");
        try (Connection con = DBConnection.getConnection()) {
            ResultSet rs = con.createStatement().executeQuery(
                    "SELECT id, policy_name FROM company_policies ORDER BY id DESC");
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{\"id\":").append(rs.getInt("id"))
                        .append(",\"name\":\"").append(rs.getString("policy_name")).append("\"}");
                first = false;
            }
            json.append("]");
            response.getWriter().print(json.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
