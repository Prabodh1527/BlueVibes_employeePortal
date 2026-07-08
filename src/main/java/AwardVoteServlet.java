import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/AwardVoteServlet")
public class AwardVoteServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain");

        HttpSession session = request.getSession(false);

        if(session == null){
            response.getWriter().print("LOGIN_REQUIRED");
            return;
        }
        System.out.println("SESSION EMAIL = " + session.getAttribute("email"));
        System.out.println("SESSION USEREMAIL = " + session.getAttribute("userEmail"));
        System.out.println("SESSION COMMUNICATION = " + session.getAttribute("communication_email"));
        System.out.println("SESSION USER = " + session.getAttribute("user"));

        String voterEmail = (String) session.getAttribute("email");

        String awardId = request.getParameter("awardId");
        String nomineeEmail = request.getParameter("nomineeEmail");

        try(Connection con = DBConnection.getConnection()){

            // Prevent duplicate vote
            PreparedStatement check = con.prepareStatement(
                "SELECT vote_id FROM employee_award_votes WHERE voter_email=? AND award_id=?"
            );

            check.setString(1, voterEmail);
            check.setInt(2, Integer.parseInt(awardId));

            ResultSet rs = check.executeQuery();

            if(rs.next()){
                response.getWriter().print("ALREADY_VOTED");
                return;
            }

            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO employee_award_votes(voter_email,nominee_email,award_id) VALUES(?,?,?)"
            );

            ps.setString(1, voterEmail);
            ps.setString(2, nomineeEmail);
            ps.setInt(3, Integer.parseInt(awardId));

            ps.executeUpdate();

            response.getWriter().print("SUCCESS");

        }
        catch(Exception e){
            e.printStackTrace();
            response.getWriter().print("ERROR");
        }

    }
}
