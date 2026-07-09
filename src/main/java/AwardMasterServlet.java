import java.io.*;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/AwardMasterServlet")
public class AwardMasterServlet extends HttpServlet {

    // Escapes a string for safe inclusion inside a JSON string literal.
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String action = request.getParameter("action");

        try (Connection con = DBConnection.getConnection()) {

            if ("list".equals(action)) {

                PreparedStatement ps = con.prepareStatement(
                        "SELECT award_id, award_name, description FROM award_master ORDER BY award_name ASC");

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
                            .append(rs.getString("description") == null ? ""
                                    : rs.getString("description").replace("\"", "\\\""))
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
                    "COUNT(v.vote_id) AS votes " +
                    "FROM award_master a " +
                    "LEFT JOIN employee_award_votes v ON a.award_id=v.award_id " +
                    "LEFT JOIN users u ON u.communication_email=v.nominee_email " +
                    "GROUP BY a.award_name,u.fullname " +
                    "ORDER BY a.award_name,votes DESC"
            
                );
            
                ResultSet rs = ps.executeQuery();
            
                StringBuilder json = new StringBuilder("[");
                boolean first = true;
            
                while(rs.next()){
            
                    if(!first){
                        json.append(",");
                    }
            
                    json.append("{")
                        .append("\"award\":\"")
                        .append(rs.getString("award_name").replace("\"","\\\""))
                        .append("\",")
            
                        .append("\"employee\":\"")
                        .append((rs.getString("employee")==null?"-":rs.getString("employee")).replace("\"","\\\""))
                        .append("\",")
            
                        .append("\"votes\":")
                        .append(rs.getInt("votes"))
            
                        .append("}");
            
                    first=false;
                }
            
                json.append("]");
                out.print(json.toString());
            }

            // Every employee with their nomination totals (includes employees with 0 nominations,
            // since this starts from `users` and left-joins the votes).
            else if ("employeeSummary".equals(action)) {

                PreparedStatement ps = con.prepareStatement(
                        "SELECT u.communication_email AS email, u.fullname AS fullname, " +
                        "COUNT(v.vote_id) AS total_nominations, " +
                        "COUNT(DISTINCT v.award_id) AS distinct_awards " +
                        "FROM users u " +
                        "LEFT JOIN employee_award_votes v ON v.nominee_email = u.communication_email " +
                        "GROUP BY u.communication_email, u.fullname " +
                        "ORDER BY total_nominations DESC, u.fullname ASC");

                ResultSet rs = ps.executeQuery();

                StringBuilder json = new StringBuilder("[");
                boolean first = true;

                while (rs.next()) {

                    if (!first) json.append(",");

                    String email = rs.getString("email");
                    String fullname = rs.getString("fullname");

                    json.append("{")
                            .append("\"email\":\"").append(esc(email)).append("\",")
                            .append("\"fullname\":\"").append(esc(fullname == null ? email : fullname)).append("\",")
                            .append("\"totalNominations\":").append(rs.getInt("total_nominations")).append(",")
                            .append("\"distinctAwards\":").append(rs.getInt("distinct_awards"))
                            .append("}");

                    first = false;
                }

                json.append("]");
                out.print(json.toString());
            }

            // Full nomination breakdown for one employee, grouped by award, with every
            // nominator (voter) and the date they nominated.
            else if ("employeeDetail".equals(action)) {

                String email = request.getParameter("email");

                String fullname = email;

                PreparedStatement nameStmt = con.prepareStatement(
                        "SELECT fullname FROM users WHERE communication_email=?");
                nameStmt.setString(1, email);
                ResultSet nameRs = nameStmt.executeQuery();
                if (nameRs.next() && nameRs.getString("fullname") != null) {
                    fullname = nameRs.getString("fullname");
                }

                PreparedStatement ps = con.prepareStatement(
                        "SELECT a.award_id, a.award_name, a.description, " +
                        "v.voter_email, COALESCE(u.fullname, v.voter_email) AS voter_name, v.voted_at " +
                        "FROM employee_award_votes v " +
                        "JOIN award_master a ON a.award_id = v.award_id " +
                        "LEFT JOIN users u ON u.communication_email = v.voter_email " +
                        "WHERE v.nominee_email = ? " +
                        "ORDER BY a.award_name ASC, v.voted_at ASC");
                ps.setString(1, email);

                ResultSet rs = ps.executeQuery();

                java.util.LinkedHashMap<Integer, String[]> awardInfo = new java.util.LinkedHashMap<>();
                java.util.LinkedHashMap<Integer, StringBuilder> awardNominators = new java.util.LinkedHashMap<>();
                java.util.LinkedHashMap<Integer, Integer> awardCounts = new java.util.LinkedHashMap<>();

                int totalNominations = 0;
                java.util.HashSet<Integer> distinctAwards = new java.util.HashSet<>();

                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy");

                while (rs.next()) {

                    int awardId = rs.getInt("award_id");
                    String awardName = rs.getString("award_name");
                    String description = rs.getString("description");
                    String voterName = rs.getString("voter_name");
                    Timestamp votedAt = rs.getTimestamp("voted_at");
                    String dateStr = votedAt == null ? "" : sdf.format(votedAt);

                    awardInfo.putIfAbsent(awardId, new String[]{awardName, description == null ? "" : description});

                    StringBuilder nomBuilder = awardNominators.get(awardId);
                    if (nomBuilder == null) {
                        nomBuilder = new StringBuilder();
                        awardNominators.put(awardId, nomBuilder);
                    } else {
                        nomBuilder.append(",");
                    }

                    nomBuilder.append("{")
                            .append("\"name\":\"").append(esc(voterName)).append("\",")
                            .append("\"date\":\"").append(esc(dateStr)).append("\"")
                            .append("}");

                    awardCounts.merge(awardId, 1, Integer::sum);

                    totalNominations++;
                    distinctAwards.add(awardId);
                }

                StringBuilder json = new StringBuilder("{");
                json.append("\"email\":\"").append(esc(email)).append("\",")
                        .append("\"fullname\":\"").append(esc(fullname)).append("\",")
                        .append("\"totalNominations\":").append(totalNominations).append(",")
                        .append("\"distinctAwards\":").append(distinctAwards.size()).append(",")
                        .append("\"awards\":[");

                boolean firstAward = true;
                for (Integer awardId : awardInfo.keySet()) {

                    if (!firstAward) json.append(",");

                    String[] info = awardInfo.get(awardId);

                    json.append("{")
                            .append("\"awardId\":").append(awardId).append(",")
                            .append("\"awardName\":\"").append(esc(info[0])).append("\",")
                            .append("\"description\":\"").append(esc(info[1])).append("\",")
                            .append("\"count\":").append(awardCounts.get(awardId)).append(",")
                            .append("\"nominators\":[").append(awardNominators.get(awardId)).append("]")
                            .append("}");

                    firstAward = false;
                }

                json.append("]}");
                out.print(json.toString());
            }

        }catch (Exception e) {
                e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        System.out.println("ACTION = " + action);
        System.out.println("NAME = " + request.getParameter("awardName"));
        System.out.println("DESC = " + request.getParameter("awardDescription"));

        try (Connection con = DBConnection.getConnection()) {

            if ("add".equals(action)) {

                String name = request.getParameter("awardName");
                String description = request.getParameter("awardDescription");

                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO award_master (award_name,description) VALUES (?,?)");

                ps.setString(1, name);
                ps.setString(2, description);

                ps.executeUpdate();
                System.out.println("INSERT SUCCESS");
            }

            else if ("update".equals(action)) {

                int id = Integer.parseInt(request.getParameter("id"));

                String name = request.getParameter("awardName");
                String description = request.getParameter("awardDescription");

                PreparedStatement ps = con.prepareStatement(
                        "UPDATE award_master SET award_name=?,description=? WHERE award_id=?");

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
            else if ("resetVotes".equals(action)) {

                PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM employee_award_votes"
                );
            
                ps.executeUpdate();
            
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
