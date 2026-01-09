import java.io.*;
import java.sql.*;
import java.util.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/UpdateProfileServlet")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 1, // 1 MB
        maxFileSize = 1024 * 1024 * 10,      // 10 MB
        maxRequestSize = 1024 * 1024 * 15    // 15 MB
)
public class UpdateProfileServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        String email = (String) session.getAttribute("userEmail");

        if (email == null) {
            response.sendRedirect("index.html");
            return;
        }

        // Retrieve Parameters
        String dob = request.getParameter("dob");
        String aadhar = request.getParameter("aadhar");
        String pan = request.getParameter("pan");
        String phone = request.getParameter("phone");
        String mFather = request.getParameter("mobile_father");
        String mMother = request.getParameter("mobile_mother");
        String mGuardian = request.getParameter("mobile_guardian");
        String pAddr = request.getParameter("perm_address");
        String cAddr = request.getParameter("comm_address");
        String pfNum = request.getParameter("pf_number");

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            // Update Main Users Table
            String sqlUser = "UPDATE users SET date_of_birth=?, aadhar=?, pan=?, phone=?, mobile_father=?, mobile_mother=?, mobile_guardian=?, perm_address=?, comm_address=?, pf_number=? WHERE email=?";
            PreparedStatement psUser = con.prepareStatement(sqlUser);
            psUser.setString(1, dob);
            psUser.setString(2, aadhar);
            psUser.setString(3, pan);
            psUser.setString(4, phone);
            psUser.setString(5, mFather);
            psUser.setString(6, mMother);
            psUser.setString(7, mGuardian);
            psUser.setString(8, pAddr);
            psUser.setString(9, cAddr);
            psUser.setString(10, pfNum);
            psUser.setString(11, email);
            psUser.executeUpdate();

            // Refresh Qualifications
            PreparedStatement delQual = con.prepareStatement("DELETE FROM user_qualifications WHERE user_email=?");
            delQual.setString(1, email);
            delQual.executeUpdate();

            String[] qualNames = request.getParameterValues("qual_name");
            String[] qualYears = request.getParameterValues("qual_year");
            String[] qualGrades = request.getParameterValues("qual_grade");
            String[] qualPercs = request.getParameterValues("qual_perc");

            if (qualNames != null) {
                // Targeted columns to avoid 'id' field error
                String insQual = "INSERT INTO user_qualifications (user_email, qualification, passing_year, grade, percentage) VALUES (?,?,?,?,?)";
                PreparedStatement psQual = con.prepareStatement(insQual);
                for (int i = 0; i < qualNames.length; i++) {
                    if (qualNames[i] != null && !qualNames[i].trim().isEmpty()) {
                        psQual.setString(1, email);
                        psQual.setString(2, qualNames[i]);
                        psQual.setString(3, qualYears[i]);
                        psQual.setString(4, qualGrades[i]);
                        psQual.setString(5, qualPercs[i]);
                        psQual.executeUpdate();
                    }
                }
            }

            // Refresh Experience
            PreparedStatement delExp = con.prepareStatement("DELETE FROM user_experience WHERE user_email=?");
            delExp.setString(1, email);
            delExp.executeUpdate();

            String[] expComps = request.getParameterValues("exp_company");
            String[] expDesigs = request.getParameterValues("exp_desig");
            String[] expJoins = request.getParameterValues("exp_joined");
            String[] expLefts = request.getParameterValues("exp_left");

            if (expComps != null) {
                // FIXED: Targeted columns to avoid 'id' field error
                String insExp = "INSERT INTO user_experience (user_email, company_name, designation, joined_date, left_date) VALUES (?,?,?,?,?)";
                PreparedStatement psExp = con.prepareStatement(insExp);
                for (int i = 0; i < expComps.length; i++) {
                    if (expComps[i] != null && !expComps[i].trim().isEmpty()) {
                        psExp.setString(1, email);
                        psExp.setString(2, expComps[i]);
                        psExp.setString(3, expDesigs[i]);
                        psExp.setString(4, expJoins[i]);
                        psExp.setString(5, expLefts[i]);
                        psExp.executeUpdate();
                    }
                }
            }

            // Save Certifications (FIXED Image Upload persistence)
            Map<String, String> existingPaths = new HashMap<>();
            PreparedStatement psPaths = con.prepareStatement("SELECT certification_name, certificate_path FROM user_certifications WHERE user_email=?");
            psPaths.setString(1, email);
            ResultSet rsPaths = psPaths.executeQuery();
            while(rsPaths.next()) existingPaths.put(rsPaths.getString("certification_name"), rsPaths.getString("certificate_path"));

            PreparedStatement delCert = con.prepareStatement("DELETE FROM user_certifications WHERE user_email=?");
            delCert.setString(1, email);
            delCert.executeUpdate();

            String[] certNames = request.getParameterValues("cert_name");
            String[] certDates = request.getParameterValues("cert_date");
            List<Part> fileParts = new ArrayList<>();
            for (Part part : request.getParts()) if (part.getName().equals("cert_image")) fileParts.add(part);

            if (certNames != null) {
                String uploadPath = getServletContext().getRealPath("") + File.separator + "uploads" + File.separator + "certs";
                File uploadDir = new File(uploadPath);
                if (!uploadDir.exists()) uploadDir.mkdirs();

                // Targeted functional columns to avoid 'id' field error
                String insCert = "INSERT INTO user_certifications (user_email, certification_name, issue_date, certificate_path) VALUES (?,?,?,?)";
                PreparedStatement psCert = con.prepareStatement(insCert);

                for (int i = 0; i < certNames.length; i++) {
                    if (certNames[i] != null && !certNames[i].trim().isEmpty()) {
                        String finalPath = existingPaths.getOrDefault(certNames[i], "");

                        if (i < fileParts.size()) {
                            Part filePart = fileParts.get(i);
                            if (filePart.getSize() > 0) {
                                String fileName = email.replace("@", "_").replace(".", "_") + "_" + i + "_" + getFileName(filePart);
                                filePart.write(uploadPath + File.separator + fileName);
                                finalPath = "uploads" + "/" + "certs" + "/" + fileName;
                            }
                        }

                        psCert.setString(1, email);
                        psCert.setString(2, certNames[i]);
                        psCert.setString(3, certDates[i]);
                        psCert.setString(4, finalPath);
                        psCert.executeUpdate();
                    }
                }
            }

            con.commit();
            response.sendRedirect("LoadProfileServlet");

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().println("Update Error: " + e.getMessage());
        }
    }

    private String getFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        for (String token : contentDisp.split(";")) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf("=") + 2, token.length() - 1);
            }
        }
        return "";
    }
}
