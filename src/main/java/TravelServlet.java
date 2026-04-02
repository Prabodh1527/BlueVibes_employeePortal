import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;

public class TravelServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 🔹 get data from frontend
        String type = request.getParameter("type");
        String fromLoc = request.getParameter("from");
        String toLoc = request.getParameter("to");
        String fromDate = request.getParameter("from_date");
        String toDate = request.getParameter("to_date");
        String purpose = request.getParameter("purpose");
        String amount = request.getParameter("amount");
        String remarks = request.getParameter("remarks");

        try {
            Class.forName("org.postgresql.Driver");

            Connection con = DriverManager.getConnection(
                "jdbc:postgresql://dpg-d68bcgn5r7bs73eo41k0-a:5432/bluevibes",
                "bluevibes_user",
                "dHyhOQGsAdkp2bwdifqdewiz6l0YxwAp"
            );

            String query = "INSERT INTO travel_request " +
                    "(type, from_location, to_location, from_date, to_date, purpose, amount, remarks, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'Pending')";

            PreparedStatement ps = con.prepareStatement(query);

            ps.setString(1, type);
            ps.setString(2, fromLoc);
            ps.setString(3, toLoc);
            ps.setDate(4, Date.valueOf(fromDate));
            ps.setDate(5, Date.valueOf(toDate));
            ps.setString(6, purpose);
            ps.setDouble(7, Double.parseDouble(amount));
            ps.setString(8, remarks);

            ps.executeUpdate();

            response.getWriter().print("Saved Successfully");

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().print("Error");
        }
    }
}
