import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/GetFullUserAuditServlet")
public class GetFullUserAuditServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = request.getParameter("email");
        StringBuilder json = new StringBuilder();

        try (Connection con = DBConnection.getConnection()) {
            // 1. Fetch Main User Data
            PreparedStatement ps = con.prepareStatement("SELECT * FROM users WHERE email = ?");
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                json.append("{ \"user\": {");
                json.append("\"fullname\":\"").append(rs.getString("fullname")).append("\",");
                json.append("\"email\":\"").append(rs.getString("email")).append("\",");
                json.append("\"gender\":\"").append(rs.getString("gender")).append("\",");
                json.append("\"dob\":\"").append(rs.getString("date_of_birth") != null ? rs.getString("date_of_birth") : "").append("\",");
                json.append("\"aadhar\":\"").append(rs.getString("aadhar") != null ? rs.getString("aadhar") : "").append("\",");
                json.append("\"pan\":\"").append(rs.getString("pan") != null ? rs.getString("pan") : "").append("\",");
                json.append("\"phone\":\"").append(rs.getString("phone") != null ? rs.getString("phone") : "").append("\",");
                json.append("\"m_father\":\"").append(rs.getString("mobile_father") != null ? rs.getString("mobile_father") : "").append("\",");
                json.append("\"m_mother\":\"").append(rs.getString("mobile_mother") != null ? rs.getString("mobile_mother") : "").append("\",");
                json.append("\"m_guardian\":\"").append(rs.getString("mobile_guardian") != null ? rs.getString("mobile_guardian") : "").append("\",");
                json.append("\"p_addr\":\"").append(rs.getString("perm_address") != null ? rs.getString("perm_address") : "").append("\",");
                json.append("\"c_addr\":\"").append(rs.getString("comm_address") != null ? rs.getString("comm_address") : "").append("\",");
                json.append("\"designation\":\"").append(rs.getString("designation")).append("\",");
                json.append("\"doj\":\"").append(rs.getString("date_of_joining") != null ? rs.getString("date_of_joining") : "").append("\",");
                json.append("\"pf_num\":\"").append(rs.getString("pf_number") != null ? rs.getString("pf_number") : "").append("\"");
                json.append("},");

                // 2. Fetch Qualifications
                json.append("\"quals\": [");
                PreparedStatement psQ = con.prepareStatement("SELECT * FROM user_qualifications WHERE user_email = ?");
                psQ.setString(1, email);
                ResultSet rsQ = psQ.executeQuery();
                boolean firstQ = true;
                while (rsQ.next()) {
                    if (!firstQ) json.append(",");
                    json.append("{\"name\":\"").append(rsQ.getString("qualification")).append("\",\"year\":\"").append(rsQ.getString("passing_year")).append("\",\"grade\":\"").append(rsQ.getString("grade")).append("\",\"perc\":\"").append(rsQ.getString("percentage")).append("\"}");
                    firstQ = false;
                }
                json.append("],");

                // 3. Fetch Experience
                json.append("\"exps\": [");
                PreparedStatement psE = con.prepareStatement("SELECT * FROM user_experience WHERE user_email = ?");
                psE.setString(1, email);
                ResultSet rsE = psE.executeQuery();
                boolean firstE = true;
                while (rsE.next()) {
                    if (!firstE) json.append(",");
                    json.append("{\"company\":\"").append(rsE.getString("company_name")).append("\",\"desig\":\"").append(rsE.getString("designation")).append("\",\"joined\":\"").append(rsE.getString("joined_date")).append("\",\"left\":\"").append(rsE.getString("left_date")).append("\"}");
                    firstE = false;
                }
                json.append("],");

                // 4. NEW: Fetch Certifications
                json.append("\"certs\": [");
                PreparedStatement psC = con.prepareStatement("SELECT * FROM user_certifications WHERE user_email = ?");
                psC.setString(1, email);
                ResultSet rsC = psC.executeQuery();
                boolean firstC = true;
                while (rsC.next()) {
                    if (!firstC) json.append(",");
                    json.append("{\"name\":\"").append(rsC.getString("certification_name")).append("\",")
                            .append("\"date\":\"").append(rsC.getString("issue_date") != null ? rsC.getString("issue_date") : "").append("\",")
                            .append("\"path\":\"").append(rsC.getString("certificate_path") != null ? rsC.getString("certificate_path") : "").append("\"}");
                    firstC = false;
                }
                json.append("] }");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        out.print(json.toString());
        out.flush();
    }
}