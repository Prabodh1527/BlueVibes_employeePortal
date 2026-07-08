import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/KnowColleaguesServlet")
public class KnowColleaguesServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        String loggedInEmail = (String) session.getAttribute("email");
        
        String sql =
            "SELECT fullname, communication_email, designation, " +
            "date_of_birth, date_of_joining " +
            "FROM users " +
            /*"WHERE communication_email <> ? " +*/
            "ORDER BY date_of_joining";

        try (Connection con = DBConnection.getConnection()) {

            PreparedStatement ps = con.prepareStatement(sql);
            //ps.setString(1, loggedInEmail);
        
            ResultSet rs = ps.executeQuery();
        
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
        
            while (rs.next()) {
        
                if (!first) {
                    json.append(",");
                }
        
                first = false;
        
                json.append("{")
                    .append("\"fullname\":\"")
                    .append(escape(rs.getString("fullname")))
                    .append("\",")
        
                    .append("\"communication_email\":\"")
                    .append(escape(rs.getString("communication_email")))
                    .append("\",")
        
                    .append("\"designation\":\"")
                    .append(escape(rs.getString("designation")))
                    .append("\",")
        
                    .append("\"date_of_birth\":\"")
                    .append(rs.getString("date_of_birth") == null ? "" : rs.getString("date_of_birth"))
                    .append("\",")
        
                    .append("\"date_of_joining\":\"")
                    .append(rs.getString("date_of_joining") == null ? "" : rs.getString("date_of_joining"))
                    .append("\"")
        
                    .append("}");
            }
        
            json.append("]");
            out.print(json.toString());
        
            rs.close();
            ps.close();
        
        } catch (Exception e) {
            e.printStackTrace();
            out.print("[]");
        }
    }

    private String escape(String value){

        if(value == null){
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
