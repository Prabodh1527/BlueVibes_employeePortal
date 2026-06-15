import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/WeeklyReportServlet")
public class WeeklyReportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // Keep your existing standard fetchMyReports logic here...
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String action = request.getParameter("action");

        if ("exportEmail".equals(action)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            try {
                String userEmail = (String) request.getSession().getAttribute("userEmail");
                if (userEmail == null || userEmail.trim().isEmpty()) {
                    userEmail = "gprabodhchandra@gmail.com"; 
                }

                // Fire the utility layer data builder and mail sender directly
                boolean mailSuccess = ExcelEmailSender.sendExcelEmail(userEmail);

                if (mailSuccess) {
                    response.getWriter().write("{\"success\":true}");
                } else {
                    response.getWriter().write("{\"success\":false,\"message\":\"Brevo outbound server rejected transaction validation.\"}");
                }

            } catch (Exception e) {
                e.printStackTrace();
                response.getWriter().write("{\"success\":false,\"message\":\"Internal pipeline stack fault: " + e.getMessage() + "\"}");
            }
            return;
        }

        // Keep your existing database form save routine untouched down here...
    }
}
