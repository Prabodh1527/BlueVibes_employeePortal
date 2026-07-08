import java.io.*;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/AwardMasterServlet")
public class AwardMasterServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String action = request.getParameter("action");

        try (Connection con = DBConnection.getConnection()) {

            if ("list".equals(action)) {

                PreparedStatement ps = con.prepareStatement(
                        "SELECT award_id, award_name, award_description FROM award_master ORDER BY award_name ASC");

                ResultSet rs = ps.executeQuery();

                StringBuilder json = new StringBuilder("[");
                boolean first = true;

                while (rs.next()) {

                    if (!first)
                        json.append(",");

                    json.append("{")
                            .append("\"id\":").append(rs.getInt("award_id")).append(",")
                            .append("\"name\":\"")
                            .append(rs.getString("award_name").replace("\"", "\\\""))
                            .append("\",")
                            .append("\"description\":\"")
                            .append(rs.getString("award_description") == null ? ""
                                    : rs.getString("award_description").replace("\"", "\\\""))
                            .append("\"")
                            .append("}");

                    first = false;
                }

                json.append("]");
                out.print(json.toString());
            }

            else if ("results".equals(action)) {

                PreparedStatement ps = con.prepareStatement(
            
                    "SELECT " +
                    "a.award_name, " +
                    "COALESCE(u.fullname,'-') AS employee, " +
                    "COUNT(v.id) AS votes " +
                    "FROM award_master a " +
                    "LEFT JOIN employee_award_votes v " +
                    "ON a.award_id = v.award_id " +
                    "LEFT JOIN users u " +
                    "ON u.email = v.nominee_email " +
                    "GROUP BY a.award_name, u.fullname " +
                    "ORDER BY a.award_name, votes DESC"
            
                );
            
                ResultSet rs = ps.executeQuery();
            
                StringBuilder json = new StringBuilder("[");
                boolean first = true;
            
                while (rs.next()) {
            
                    if (!first)
                        json.append(",");
            
                    json.append("{")
                        .append("\"award\":\"")
                        .append(rs.getString("award_name").replace("\"","\\\\\""))
                        .append("\",")
                        .append("\"employee\":\"")
                        .append((rs.getString("employee")==null?"-":rs.getString("employee")).replace("\"","\\\\\""))
                        .append("\",")
                        .append("\"votes\":")
                        .append(rs.getInt("votes"))
                        .append("}");
            
                    first = false;
                }
            
                json.append("]");
                out.print(json.toString());
            }
            catch (Exception e) {
                e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");

        try (Connection con = DBConnection.getConnection()) {

            if ("add".equals(action)) {

                String name = request.getParameter("awardName");
                String description = request.getParameter("awardDescription");

                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO award_master (award_name,award_description) VALUES (?,?)");

                ps.setString(1, name);
                ps.setString(2, description);

                ps.executeUpdate();
            }

            else if ("update".equals(action)) {

                int id = Integer.parseInt(request.getParameter("id"));

                String name = request.getParameter("awardName");
                String description = request.getParameter("awardDescription");

                PreparedStatement ps = con.prepareStatement(
                        "UPDATE award_master SET award_name=?,award_description=? WHERE award_id=?");

                ps.setString(1, name);
                ps.setString(2, description);
                ps.setInt(3, id);

                ps.executeUpdate();
            }

            else if ("delete".equals(action)) {

                int id = Integer.parseInt(request.getParameter("id"));

                PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM award_master WHERE award_id=?");

                ps.setInt(1, id);

                ps.executeUpdate();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
