import java.io.IOException;
import java.sql.*;
import java.util.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/LoadProfileServlet")
public class LoadProfileServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        String email = (session != null) ? (String) session.getAttribute("userEmail") : null;

        if (email == null) {
            response.sendRedirect("index.html");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                response.getWriter().println("Database Connection Failed");
                return;
            }

            // 1. Fetch Master User Data
            String userSql = "SELECT * FROM users WHERE email = ?";
            try (PreparedStatement ps = con.prepareStatement(userSql)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Map<String, String> user = new HashMap<>();
                        user.put("employee_id", clean(rs.getString("employee_id")));
                        user.put("fullname", clean(rs.getString("fullname")));
                        user.put("email", clean(rs.getString("email")));
                        user.put("gender", clean(rs.getString("gender")));
                        user.put("dob", rs.getString("date_of_birth") != null ? rs.getString("date_of_birth") : "");
                        user.put("aadhar", clean(rs.getString("aadhar")));
                        user.put("pan", clean(rs.getString("pan")));
                        user.put("pf_num", clean(rs.getString("pf_number")));
                        user.put("phone", clean(rs.getString("phone")));
                        user.put("m_father", clean(rs.getString("mobile_father")));
                        user.put("m_mother", clean(rs.getString("mobile_mother")));
                        user.put("m_guardian", clean(rs.getString("mobile_guardian")));
                        user.put("p_addr", clean(rs.getString("perm_address")));
                        user.put("c_addr", clean(rs.getString("comm_address")));
                        user.put("designation", rs.getString("designation") != null ? rs.getString("designation") : "Not Assigned");
                        user.put("doj", rs.getString("date_of_joining") != null ? rs.getString("date_of_joining") : "");

                        request.setAttribute("user", user);

                        // 2. Fetch Qualifications
                        List<Map<String, String>> quals = new ArrayList<>();
                        try (PreparedStatement psQual = con.prepareStatement("SELECT * FROM user_qualifications WHERE user_email = ?")) {
                            psQual.setString(1, email);
                            try (ResultSet rsQual = psQual.executeQuery()) {
                                while (rsQual.next()) {
                                    Map<String, String> q = new HashMap<>();
                                    q.put("name", rsQual.getString("qualification"));
                                    q.put("year", rsQual.getString("passing_year"));
                                    q.put("grade", rsQual.getString("grade"));
                                    q.put("perc", rsQual.getString("percentage"));
                                    quals.add(q);
                                }
                            }
                        }
                        request.setAttribute("quals", quals);

                        // 3. Fetch Experience
                        List<Map<String, String>> exps = new ArrayList<>();
                        try (PreparedStatement psExp = con.prepareStatement("SELECT * FROM user_experience WHERE user_email = ?")) {
                            psExp.setString(1, email);
                            try (ResultSet rsExp = psExp.executeQuery()) {
                                while (rsExp.next()) {
                                    Map<String, String> e = new HashMap<>();
                                    e.put("company", rsExp.getString("company_name"));
                                    e.put("desig", rsExp.getString("designation"));
                                    e.put("joined", rsExp.getString("joined_date"));
                                    e.put("left", rsExp.getString("left_date"));
                                    exps.add(e);
                                }
                            }
                        }
                        request.setAttribute("exps", exps);

                        // 4. Fetch Certifications
                        List<Map<String, String>> userCerts = new ArrayList<>();
                        try (PreparedStatement psUCert = con.prepareStatement("SELECT * FROM user_certifications WHERE user_email = ?")) {
                            psUCert.setString(1, email);
                            try (ResultSet rsUCert = psUCert.executeQuery()) {
                                while (rsUCert.next()) {
                                    Map<String, String> c = new HashMap<>();
                                    c.put("name", rsUCert.getString("certification_name"));
                                    c.put("date", rsUCert.getString("issue_date"));
                                    c.put("path", rsUCert.getString("certificate_path") != null ? rsUCert.getString("certificate_path") : "");
                                    userCerts.add(c);
                                }
                            }
                        }
                        request.setAttribute("userCerts", userCerts);

                        // 5. Fetch Master Lists
                        request.setAttribute("masterQuals", fetchMasterList(con, "master_qualifications", "qualification_name"));
                        request.setAttribute("masterOrgs", fetchMasterList(con, "master_organizations", "organization_name"));
                        request.setAttribute("masterCerts", fetchMasterList(con, "master_certifications", "certification_name"));
                        request.setAttribute("masterDesigs", fetchMasterList(con, "designation_master", "designation_name"));

                        request.getRequestDispatcher("profile.jsp").forward(request, response);
                    } else {
                        response.sendRedirect("index.html");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().println("Error loading profile: " + e.getMessage());
        }
    }

    private List<Map<String, String>> fetchMasterList(Connection con, String table, String column) throws SQLException {
        List<Map<String, String>> list = new ArrayList<>();
        String sql = "SELECT " + column + " FROM " + table + " ORDER BY " + column + " ASC";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, String> m = new HashMap<>();
                m.put("name", rs.getString(column));
                list.add(m);
            }
        }
        return list;
    }

    private String clean(String input) {
        return (input == null) ? "" : input;
    }
}
