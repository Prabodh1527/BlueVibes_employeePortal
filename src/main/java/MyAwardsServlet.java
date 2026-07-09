import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/MyAwardsServlet")
public class MyAwardsServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);

        if(session == null){
            out.print("[]");
            return;
        }

        String loginEmail = (String) session.getAttribute("userEmail");

        if(loginEmail == null){
            loginEmail = (String) session.getAttribute("email");
        }

        if(loginEmail == null){
            out.print("[]");
            return;
        }

        try(Connection con = DBConnection.getConnection()){

            // Get communication email using login email
            String communicationEmail = loginEmail;

            PreparedStatement psUser = con.prepareStatement(
                "SELECT communication_email FROM users WHERE email=?"
            );

            psUser.setString(1, loginEmail);

            ResultSet rsUser = psUser.executeQuery();

            if(rsUser.next()){
                communicationEmail = rsUser.getString("communication_email");
            }

            rsUser.close();
            psUser.close();

            PreparedStatement ps = con.prepareStatement(

                "SELECT a.award_name, a.description, COUNT(v.vote_id) AS total_votes " +
                "FROM employee_award_votes v " +
                "JOIN award_master a ON a.award_id = v.award_id " +
                "WHERE v.nominee_email = ? " +
                "GROUP BY a.award_name, a.description " +
                "ORDER BY total_votes DESC"

            );

            ps.setString(1, communicationEmail);

            ResultSet rs = ps.executeQuery();

            StringBuilder json = new StringBuilder("[");
            boolean first = true;

            while(rs.next()){

                if(!first){
                    json.append(",");
                }

                first = false;

                json.append("{")
                    .append("\"award\":\"")
                    .append(rs.getString("award_name").replace("\"","\\\""))
                    .append("\",")

                    .append("\"description\":\"")
                    .append(rs.getString("description")==null ? "" :
                            rs.getString("description").replace("\"","\\\""))
                    .append("\",")

                    .append("\"votes\":")
                    .append(rs.getInt("total_votes"))

                    .append("}");
            }

            json.append("]");

            rs.close();
            ps.close();

            out.print(json.toString());

        }
        catch(Exception e){
            e.printStackTrace();
            out.print("[]");
        }
    }
}
