import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;

public class TravelAdminServlet extends HttpServlet {

    // 🔹 GET → fetch all travel requests
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        try {
            Class.forName("org.postgresql.Driver");

            Connection con = DriverManager.getConnection(
                "jdbc:postgresql://dpg-d68bcgn5r7bs73eo41k0-a:5432/bluevibes",
                "bluevibes_user",
                "dHyhOQGsAdkp2bwdifqdewiz6l0YxwAp"
            );

            String query = "SELECT * FROM travel_request ORDER BY id DESC";
            PreparedStatement ps = con.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                if (!first) json.append(",");

                json.append("{")
                        .append("\"id\":").append(rs.getInt("id")).append(",")
                        .append("\"type\":\"").append(rs.getString("type")).append("\",")
                        .append("\"from_location\":\"").append(rs.getString("from_location")).append("\",")
                        .append("\"to_location\":\"").append(rs.getString("to_location")).append("\",")
                        .append("\"from_date\":\"").append(rs.getString("from_date")).append("\",")
                        .append("\"to_date\":\"").append(rs.getString("to_date")).append("\",")
                        .append("\"purpose\":\"").append(rs.getString("purpose")).append("\",")
                        .append("\"amount\":").append(rs.getDouble("amount")).append(",")
                        .append("\"status\":\"").append(rs.getString("status")).append("\"")
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

    // 🔹 POST → approve / reject
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String id = request.getParameter("id");
        String status = request.getParameter("status");

        try {
            Class.forName("org.postgresql.Driver");

            Connection con = DriverManager.getConnection(
                "jdbc:postgresql://dpg-d68bcgn5r7bs73eo41k0-a:5432/bluevibes",
                "bluevibes_user",
                "dHyhOQGsAdkp2bwdifqdewiz6l0YxwAp"
            );

            String query = "UPDATE travel_request SET status=? WHERE id=?";
            PreparedStatement ps = con.prepareStatement(query);

            ps.setString(1, status);
            ps.setInt(2, Integer.parseInt(id));

            ps.executeUpdate();

            response.getWriter().print("updated");

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().print("error");
        }
    }
}
