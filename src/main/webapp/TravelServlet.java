import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;

public class TravelServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        String from = request.getParameter("from_date");
        String to = request.getParameter("to_date");
        String purpose = request.getParameter("purpose");
        String type = request.getParameter("transfer_type");
        String advance = request.getParameter("advance_amount");
        String location = request.getParameter("location"); // ✅ ADDED

        try {
            Class.forName("org.postgresql.Driver");

            Connection con = DriverManager.getConnection(
                "jdbc:postgresql://dpg-d68bcgn5r7bs73eo41k0-a:5432/bluevibes",
                "bluevibes_user",
                "dHyhOQGsAdkp2bwdifqdewiz6l0YxwAp"
            );

            // ✅ UPDATED QUERY (location added)
            String query = "INSERT INTO travel_request (email, from_date, to_date, purpose, location, transfer_type, advance_amount, status) VALUES (?, ?, ?, ?, ?, ?, ?, 'Pending')";

            PreparedStatement ps = con.prepareStatement(query);

            ps.setString(1, email);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));
            ps.setString(4, purpose);
            ps.setString(5, location); // ✅ ADDED
            ps.setString(6, type);
            ps.setDouble(7, Double.parseDouble(advance)); // shifted index

            ps.executeUpdate();

            response.getWriter().print("success");

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().print("error");
        }
    }
}
