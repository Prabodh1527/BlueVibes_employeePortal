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

@WebServlet("/PayslipServlet")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 2,
        maxFileSize = 1024 * 1024 * 10,
        maxRequestSize = 1024 * 1024 * 50
)
public class PayslipServlet extends HttpServlet {

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

        if ("upload".equals(action)) {
            String userEmail = request.getParameter("userEmail");
            String monthYear = request.getParameter("monthYear");
            Part filePart = request.getPart("payslipFile");

            String originalFileName = getFileName(filePart);
            String cleanEmail = userEmail.replace("@", "_").replace(".", "_");
            String cloudName = "payslip_" + cleanEmail + "_" + monthYear;

            try {
                Map uploadResult = cloudinary.uploader().upload(filePart.getInputStream(),
                        ObjectUtils.asMap(
                                "public_id", cloudName,
                                "resource_type", "raw"
                        ));

                String cloudUrl = uploadResult.get("secure_url").toString();

                try (Connection con = DBConnection.getConnection()) {
                    PreparedStatement del = con.prepareStatement(
                            "DELETE FROM user_payslips WHERE user_email=? AND month_year=?");
                    del.setString(1, userEmail);
                    del.setString(2, monthYear);
                    del.executeUpdate();

                    PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO user_payslips (user_email, month_year, file_path, file_name) VALUES (?, ?, ?, ?)");
                    ps.setString(1, userEmail);
                    ps.setString(2, monthYear);
                    ps.setString(3, cloudUrl);
                    ps.setString(4, originalFileName);
                    ps.executeUpdate();

                    response.sendRedirect("genpayslip.html?status=success");
                }
            } catch (Exception e) {
                e.printStackTrace();
                response.sendRedirect("genpayslip.html?status=error");
            }
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try (Connection con = DBConnection.getConnection()) {

            if ("history".equals(action)) {
                String targetEmail = request.getParameter("userEmail");
                PreparedStatement ps = con.prepareStatement(
                        "SELECT id, month_year, file_name, uploaded_at FROM user_payslips WHERE user_email = ? ORDER BY uploaded_at DESC");
                ps.setString(1, targetEmail);
                ResultSet rs = ps.executeQuery();

                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    json.append("{")
                            .append("\"id\":\"").append(rs.getInt("id")).append("\",")
                            .append("\"month_year\":\"").append(rs.getString("month_year")).append("\",")
                            .append("\"file_name\":\"").append(rs.getString("file_name")).append("\",")
                            .append("\"uploaded_at\":\"").append(rs.getTimestamp("uploaded_at").toString().split(" ")[0]).append("\"")
                            .append("}");
                    first = false;
                }
                json.append("]");
                response.getWriter().write(json.toString());
            }

            else if ("view".equals(action)) {
                String id = request.getParameter("id");
                PreparedStatement ps = con.prepareStatement("SELECT file_path FROM user_payslips WHERE id = ?");
                ps.setInt(1, Integer.parseInt(id));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    response.sendRedirect(rs.getString("file_path"));
                }
            }

            else if ("list".equals(action)) {
                String currentUser = (String) request.getSession().getAttribute("userEmail");
                PreparedStatement ps = con.prepareStatement(
                        "SELECT month_year, file_path FROM user_payslips WHERE user_email = ? ORDER BY id DESC");
                ps.setString(1, currentUser);
                ResultSet rs = ps.executeQuery();

                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    json.append(String.format("{\"monthYear\":\"%s\", \"path\":\"%s\"}",
                            rs.getString("month_year"), rs.getString("file_path")));
                    first = false;
                }
                json.append("]");
                response.getWriter().write(json.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("[]");
        }
    }

    private String getFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        for (String token : contentDisp.split(";")) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf("=") + 2, token.length() - 1);
            }
        }
        return "payslip.pdf";
    }
}
