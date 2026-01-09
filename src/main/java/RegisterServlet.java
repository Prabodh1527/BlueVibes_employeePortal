import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/RegisterServlet")
public class RegisterServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String action = request.getParameter("action");
        if (action == null) action = "add";

        try (Connection con = DBConnection.getConnection()) {

            if ("add".equals(action)) {
                String empid = request.getParameter("empid");
                String fullname = request.getParameter("fullname");
                String email = request.getParameter("email");
                String designation = request.getParameter("designation");
                String doj = request.getParameter("doj");
                String gender = request.getParameter("sex");

                int calculatedLeaves = 10;
                try {
                    LocalDate joiningDate = LocalDate.parse(doj);
                    int monthJoined = joiningDate.getMonthValue();
                    calculatedLeaves = (int) Math.ceil(((13 - monthJoined) / 12.0) * 10);
                } catch (Exception e) {
                    calculatedLeaves = 10;
                }

                String sql = "INSERT INTO users (employee_id, fullname, email, password, role, gender, designation, date_of_joining, leave_balance) VALUES (?, ?, ?, ?, 'User', ?, ?, ?, ?)";
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setString(1, empid);
                ps.setString(2, fullname);
                ps.setString(3, email);
                ps.setString(4, "dummypassword");
                ps.setString(5, gender);
                ps.setString(6, designation);
                ps.setString(7, doj);
                ps.setInt(8, calculatedLeaves);
                ps.executeUpdate();
                response.sendRedirect("usermanagement.html?status=registered");

            } else if ("update_basic".equals(action)) {
                String originalEmail = request.getParameter("originalEmail");
                String newEmail = request.getParameter("email");
                String empid = request.getParameter("empid");
                String fullname = request.getParameter("fullname");
                String designation = request.getParameter("designation");

                String sql = "UPDATE users SET email = ?, employee_id = ?, fullname = ?, designation = ? WHERE email = ?";
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setString(1, newEmail);
                ps.setString(2, empid);
                ps.setString(3, fullname);
                ps.setString(4, designation);
                ps.setString(5, originalEmail);
                ps.executeUpdate();
                response.getWriter().write("success");

            } else if ("delete".equals(action)) {
                String empid = request.getParameter("empid");
                String getEmailSql = "SELECT email FROM users WHERE employee_id = ?";
                PreparedStatement psEmail = con.prepareStatement(getEmailSql);
                psEmail.setString(1, empid);
                ResultSet rsEmail = psEmail.executeQuery();

                if (rsEmail.next()) {
                    String userEmail = rsEmail.getString("email");
                    String[] tables = {"leaves", "attendance", "user_weekly_reports", "user_certifications", "user_experience", "user_qualifications", "user_payslips", "notifications", "broadcasts"};
                    for (String tableName : tables) {
                        try {
                            PreparedStatement psDel = con.prepareStatement("DELETE FROM " + tableName + " WHERE user_email = ?");
                            psDel.setString(1, userEmail);
                            psDel.executeUpdate();
                        } catch (Exception e) {}
                    }
                    PreparedStatement psUser = con.prepareStatement("DELETE FROM users WHERE employee_id = ? AND role != 'Admin'");
                    psUser.setString(1, empid);
                    int rowsDeleted = psUser.executeUpdate();
                    if (rowsDeleted > 0) response.getWriter().write("deleted");
                    else response.getWriter().write("error");
                }

            } else if ("reset_all".equals(action)) {
                // --- THE COMPLETE SYSTEM WIPE ---
                // Tables to be cleared (User data only)
                String[] userRelatedTables = {
                        "attendance",
                        "leaves",
                        "user_weekly_reports",
                        "user_certifications",
                        "user_experience",
                        "user_qualifications",
                        "user_payslips",
                        "notifications",
                        "broadcasts",
                        "announcements"
                };

                for (String table : userRelatedTables) {
                    try {
                        con.prepareStatement("DELETE FROM " + table).executeUpdate();
                    } catch (Exception e) {}
                }

                // Delete all users except for the Admin account
                con.prepareStatement("DELETE FROM users WHERE role != 'Admin'").executeUpdate();

                // Note: Policies and Master tables are NOT in the list above, so they stay!
                response.getWriter().write("reset_success");

            } else if ("toggle_wfh".equals(action)) {
                String email = request.getParameter("email");
                boolean status = Boolean.parseBoolean(request.getParameter("status"));
                String sql = "UPDATE users SET wfh_allowed = ? WHERE email = ?";
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setBoolean(1, status);
                ps.setString(2, email);
                ps.executeUpdate();
                response.getWriter().write("success");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().println("Error: " + e.getMessage());
        }
    }
}
