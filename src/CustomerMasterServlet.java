import java.io.*;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/CustomerMasterServlet")
public class CustomerMasterServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        String action = request.getParameter("action");

        if ("list".equals(action)) {
            try (Connection con = DBConnection.getConnection()) {
                PreparedStatement ps = con.prepareStatement("SELECT id, customer_name FROM customer_master ORDER BY customer_name ASC");
                ResultSet rs = ps.executeQuery();

                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    json.append(String.format("{\"id\":%d, \"name\":\"%s\"}",
                            rs.getInt("id"),
                            rs.getString("customer_name")));
                    first = false;
                }
                json.append("]");
                out.print(json.toString());
            } catch (Exception e) {
                e.printStackTrace();
                out.print("[]");
            }
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");

        try (Connection con = DBConnection.getConnection()) {
            if ("add".equals(action)) {
                String customerName = request.getParameter("customerName");
                PreparedStatement ps = con.prepareStatement("INSERT INTO customer_master (customer_name) VALUES (?)");
                ps.setString(1, customerName);
                ps.executeUpdate();

            } else if ("update".equals(action)) {
                // NEW: Handle update request from frontend
                int id = Integer.parseInt(request.getParameter("id"));
                String customerName = request.getParameter("customerName");
                PreparedStatement ps = con.prepareStatement("UPDATE customer_master SET customer_name = ? WHERE id = ?");
                ps.setString(1, customerName);
                ps.setInt(2, id);
                ps.executeUpdate();

            } else if ("delete".equals(action)) {
                int id = Integer.parseInt(request.getParameter("id"));
                PreparedStatement ps = con.prepareStatement("DELETE FROM customer_master WHERE id = ?");
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}