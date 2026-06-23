import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/KnowYourColleaguesServlet")
public class KnowYourColleaguesServlet extends HttpServlet {

    protected void doGet(javax.servlet.http.HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        try (Connection con = DBConnection.getConnection()) {

            String sql =
                "SELECT fullname, designation, communication_email, " +
                "date_of_birth, date_of_joining " +
                "FROM users " +
                "WHERE fullname IS NOT NULL " +
                "ORDER BY fullname";

            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            StringBuilder json = new StringBuilder("[");
            boolean first = true;

            while (rs.next()) {

                if (!first) json.append(",");

                String fullname =
                        clean(rs.getString("fullname"));

                String designation =
                        clean(rs.getString("designation"));

                String email =
                        clean(rs.getString("communication_email"));

                String dob =
                        rs.getDate("date_of_birth") != null
                        ? rs.getDate("date_of_birth").toString()
                        : "";

                String doj =
                        rs.getDate("date_of_joining") != null
                        ? rs.getDate("date_of_joining").toString()
                        : "";

                json.append("{")
                    .append("\"fullname\":\"").append(fullname).append("\",")
                    .append("\"designation\":\"").append(designation).append("\",")
                    .append("\"email\":\"").append(email).append("\",")
                    .append("\"dob\":\"").append(dob).append("\",")
                    .append("\"doj\":\"").append(doj).append("\"")
                    .append("}");

                first = false;
            }

            json.append("]");

            out.print(json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            out.print("[]");
        }
    }

    private String clean(String value) {
        if (value == null) return "";

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
