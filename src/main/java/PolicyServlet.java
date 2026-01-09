import java.io.*;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/PolicyServlet")
@MultipartConfig(maxFileSize = 1024 * 1024 * 10)
public class PolicyServlet extends HttpServlet {

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
                    String dbPath = "uploads/policies/" + uniqueName;

                    String rootPath = getServletContext().getRealPath("/");
                    String fullUploadPath = rootPath + File.separator + "uploads" + File.separator + "policies";

                    File uploadDir = new File(fullUploadPath);
                    if (!uploadDir.exists()) uploadDir.mkdirs();

                    filePart.write(fullUploadPath + File.separator + uniqueName);

                    PreparedStatement ps = con.prepareStatement("INSERT INTO company_policies (policy_name, file_path) VALUES (?, ?)");
                    ps.setString(1, fileName);
                    ps.setString(2, dbPath);
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        response.sendRedirect("policy.html");
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        String fileId = request.getParameter("id");

        // 1. DOWNLOAD LOGIC: If action=view and id is present, stream the file
        if ("view".equals(action) && fileId != null && !fileId.trim().isEmpty()) {
            handleDownload(request, response, fileId);
            return;
        }

        // 2. JSON LOGIC: Send list of files to the JSP
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try (Connection con = DBConnection.getConnection()) {
            ResultSet rs = con.createStatement().executeQuery("SELECT id, policy_name FROM company_policies ORDER BY id DESC");
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                int id = rs.getInt("id");
                json.append("{")
                        .append("\"id\":").append(id).append(",") // Ensure ID is a number
                        .append("\"name\":\"").append(rs.getString("policy_name")).append("\"")
                        .append("}");
                first = false;
            }
            json.append("]");
            response.getWriter().print(json.toString());
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleDownload(HttpServletRequest request, HttpServletResponse response, String id) throws IOException {
        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT policy_name, file_path FROM company_policies WHERE id = ?");
            ps.setInt(1, Integer.parseInt(id));
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String fileName = rs.getString("policy_name");
                String relativePath = rs.getString("file_path");
                String rootPath = getServletContext().getRealPath("/");
                File file = new File(rootPath + File.separator + relativePath);

                if (file.exists() && !file.isDirectory()) {
                    response.reset(); // Clear any previous text
                    String mimeType = getServletContext().getMimeType(file.getAbsolutePath());
                    response.setContentType(mimeType != null ? mimeType : "application/pdf");
                    response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
                    response.setContentLength((int) file.length());

                    try (FileInputStream in = new FileInputStream(file);
                         OutputStream out = response.getOutputStream()) {
                        byte[] buffer = new byte[8192];
                        int n;
                        while ((n = in.read(buffer)) != -1) {
                            out.write(buffer, 0, n);
                        }
                        out.flush();
                    }
                } else {
                    response.sendError(404, "File missing on server disk.");
                }
            }
        } catch (Exception e) { e.printStackTrace(); response.sendError(500); }
    }
}
