import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/MyAwardsServlet")
public class MyAwardsServlet extends HttpServlet {

    // Escapes a string for safe inclusion inside a JSON string literal.
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);

        if (session == null) {
            out.print("[]");
            return;
        }

        String loginEmail = (String) session.getAttribute("userEmail");

        if (loginEmail == null) {
            out.print("[]");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {

            // Step 1: Resolve the logged-in user's communication_email
            // (votes are keyed by communication_email, not the login email).
            String nomineeEmail = loginEmail;

            PreparedStatement p1 = con.prepareStatement(
                    "SELECT communication_email FROM users WHERE email=?");

            p1.setString(1, loginEmail);

            ResultSet r1 = p1.executeQuery();

            if (r1.next() && r1.getString("communication_email") != null) {
                nomineeEmail = r1.getString("communication_email");
            }

            // Step 2: Get every award this person was nominated for, along with
            // every nominator (voter) and the date they nominated.
            PreparedStatement ps = con.prepareStatement(
                    "SELECT a.award_id, a.award_name, a.description, " +
                    "COALESCE(u.fullname, v.voter_email) AS voter_name, v.voted_at " +
                    "FROM employee_award_votes v " +
                    "JOIN award_master a ON a.award_id = v.award_id " +
                    "LEFT JOIN users u ON u.communication_email = v.voter_email " +
                    "WHERE v.nominee_email = ? " +
                    "ORDER BY a.award_name ASC, v.voted_at ASC");

            ps.setString(1, nomineeEmail);

            ResultSet rs = ps.executeQuery();

            LinkedHashMap<Integer, String[]> awardInfo = new LinkedHashMap<>();
            LinkedHashMap<Integer, StringBuilder> awardNominators = new LinkedHashMap<>();
            LinkedHashMap<Integer, Integer> awardCounts = new LinkedHashMap<>();

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");

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
            }

            StringBuilder json = new StringBuilder("[");
            boolean first = true;

            for (Integer awardId : awardInfo.keySet()) {

                if (!first) json.append(",");
                first = false;

                String[] info = awardInfo.get(awardId);

                json.append("{")
                        .append("\"award\":\"").append(esc(info[0])).append("\",")
                        .append("\"description\":\"").append(esc(info[1])).append("\",")
                        .append("\"votes\":").append(awardCounts.get(awardId)).append(",")
                        .append("\"nominators\":[").append(awardNominators.get(awardId)).append("]")
                        .append("}");
            }

            json.append("]");

            out.print(json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            out.print("[]");
        }
    }
}
