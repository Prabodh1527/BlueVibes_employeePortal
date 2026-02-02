import java.io.*;
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
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", System.getenv("CLOUDINARY_CLOUD_NAME"),
                "api_key", System.getenv("CLOUDINARY_API_KEY"),
                "api_secret", System.getenv("CLOUDINARY_API_SECRET")
        ));
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 🔴 ADDED FOR DEBUG (ONLY CHANGE)
        System.out.println("PayslipServlet doPost HIT");

        String action = request.getParameter("action");

        if (!"upload".equals(action)) return;

        String userEmail = request.getParameter("userEmail");
        String monthYear = request.getParameter("monthYear");
        Part filePart = request.getPart("payslipFile");

        try (Connection con = DBConnection.getConnection()) {

            Map uploadResult = cloudinary.uploader().upload(
                    filePart.getInputStream(),
                    ObjectUtils.asMap(
                            "resource_type", "raw",
                            "folder", "bluevibes/payslips",
                            "public_id", userEmail + "_" + monthYear
                    )
            );

            String fileUrl = uploadResult.get("secure_url").toString();
            String originalName = filePart.getSubmittedFileName();

            PreparedStatement del = con.prepareStatement(
                    "DELETE FROM user_payslips WHERE user_email=? AND month_year=?");
            del.setString(1, userEmail);
            del.setString(2, monthYear);
            del.executeUpdate();

            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO user_payslips (user_email, month_year, file_path, file_name) VALUES (?, ?, ?, ?)");
            ps.setString(1, userEmail);
            ps.setString(2, monthYear);
            ps.setString(3, fileUrl);
            ps.setString(4, originalName);
            ps.executeUpdate();

            response.sendRedirect("genpayslip.html?status=success");

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("genpayslip.html?status=error");
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String action = request.getParameter("action");

        try (Connection con = DBConnection.getConnection()) {

            if ("view".equals(action)) {
                String id = request.getParameter("id");
                PreparedStatement ps = con.prepareStatement(
                        "SELECT file_path FROM user_payslips WHERE id=?");
                ps.setInt(1, Integer.parseInt(id));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    response.sendRedirect(rs.getString("file_path"));
                }
            }

            else if ("history".equals(action)) {
                String email = request.getParameter("userEmail");
                PreparedStatement ps = con.prepareStatement(
                        "SELECT id, month_year, file_name, uploaded_at FROM user_payslips WHERE user_email=? ORDER BY uploaded_at DESC");
                ps.setString(1, email);
                ResultSet rs = ps.executeQuery();

                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    json.append("{\"id\":").append(rs.getInt("id"))
                            .append(",\"month_year\":\"").append(rs.getString("month_year"))
                            .append("\",\"file_name\":\"").append(rs.getString("file_name"))
                            .append("\",\"uploaded_at\":\"")
                            .append(rs.getTimestamp("uploaded_at").toString().split(" ")[0])
                            .append("\"}");
                    first = false;
                }
                json.append("]");
                response.getWriter().write(json.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
