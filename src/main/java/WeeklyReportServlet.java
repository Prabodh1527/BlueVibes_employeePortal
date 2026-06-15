package com.bluevibes.servlet;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.bluevibes.util.ExcelEmailSender;

@WebServlet("/WeeklyReportServlet")
public class WeeklyReportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // Keep your existing standard doGet logic untouched here...
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String action = request.getParameter("action");

        if ("exportEmail".equals(action)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            try {
                // Read current employee username session context
                String userEmail = (String) request.getSession().getAttribute("userEmail");
                if (userEmail == null || userEmail.trim().isEmpty()) {
                    userEmail = "gprabodhchandra@gmail.com"; // System production fallback identifier
                }

                // Since we removed multipart dependencies, we now build the spreadsheet 
                // data payload dynamically directly via our backend/email logic
                boolean mailSuccess = ExcelEmailSender.sendExcelEmail(userEmail);

                if (mailSuccess) {
                    response.getWriter().write("{\"success\":true}");
                } else {
                    response.getWriter().write("{\"success\":false,\"message\":\"Brevo outbound delivery API processing rejected the packet payload.\"}");
                }

            } catch (Exception e) {
                e.printStackTrace();
                response.getWriter().write("{\"success\":false,\"message\":\"Internal error pipeline stack fault: " + e.getMessage() + "\"}");
            }
            return;
        }

        // Your existing normal Form submission save block goes here...
    }
}
