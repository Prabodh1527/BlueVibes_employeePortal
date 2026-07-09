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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
    
        PrintWriter out = response.getWriter();
    
        HttpSession session = request.getSession(false);
    
        if(session==null){
            out.print("[]");
            return;
        }
    
        String loginEmail=(String)session.getAttribute("userEmail");
    
        if(loginEmail==null){
            out.print("[]");
            return;
        }
    
        try(Connection con=DBConnection.getConnection()){
    
            // Step 1 : Get communication email of logged in user
            String nomineeEmail=loginEmail;
    
            PreparedStatement p1=con.prepareStatement(
                "SELECT communication_email FROM users WHERE email=?"
            );
    
            p1.setString(1,loginEmail);
    
            ResultSet r1=p1.executeQuery();
    
            if(r1.next()){
                nomineeEmail=r1.getString("communication_email");
            }
    
            // Step 2 : Get awards
            PreparedStatement ps=con.prepareStatement(
    
                "SELECT a.award_name,a.description,COUNT(*) total_votes " +
                "FROM employee_award_votes v " +
                "INNER JOIN award_master a ON a.award_id=v.award_id " +
                "WHERE v.nominee_email=? " +
                "GROUP BY a.award_name,a.description " +
                "ORDER BY total_votes DESC"
    
            );
    
            ps.setString(1,nomineeEmail);
    
            ResultSet rs=ps.executeQuery();
    
            StringBuilder json=new StringBuilder("[");
            boolean first=true;
    
            while(rs.next()){
    
                if(!first) json.append(",");
    
                first=false;
    
                String award=rs.getString("award_name");
                String desc=rs.getString("description");
    
                if(award==null) award="";
                if(desc==null) desc="";
    
                award=award.replace("\"","\\\"");
                desc=desc.replace("\"","\\\"");
    
                json.append("{")
                    .append("\"award\":\"").append(award).append("\",")
                    .append("\"description\":\"").append(desc).append("\",")
                    .append("\"votes\":").append(rs.getInt("total_votes"))
                    .append("}");
            }
    
            json.append("]");
    
            out.print(json.toString());
    
        }catch(Exception e){
            e.printStackTrace();
            out.print("[]");
        }
    }
}
