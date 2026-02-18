import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/AttendanceServlet")
public class AttendanceServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        String sessionUser = (session != null) ? (String) session.getAttribute("userEmail") : null;

        if (sessionUser == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String adminEmail = request.getParameter("adminEmail");
        String targetEmail = (adminEmail != null && !adminEmail.isEmpty()) ? adminEmail : sessionUser;
        String action = request.getParameter("action");

        int reqMonth = (request.getParameter("month") != null) ? Integer.parseInt(request.getParameter("month")) : LocalDate.now().getMonthValue();
        int reqYear = (request.getParameter("year") != null) ? Integer.parseInt(request.getParameter("year")) : LocalDate.now().getYear();
        LocalDate today = LocalDate.now();

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                out.print("{\"success\": false, \"error\": \"Connection Failed\"}");
                return;
            }

            Map<Integer, String> holidayDetails = new HashMap<>();
            Set<Integer> holidayDaysList = new HashSet<>();

            String holidayQuery = "SELECT EXTRACT(DAY FROM holiday_date)::int, reason FROM holidays " +
                                 "WHERE EXTRACT(MONTH FROM holiday_date)=? AND EXTRACT(YEAR FROM holiday_date)=?";
            
            try (PreparedStatement hps = con.prepareStatement(holidayQuery)) {
                hps.setInt(1, reqMonth);
                hps.setInt(2, reqYear);
                try (ResultSet hrs = hps.executeQuery()) {
                    while(hrs.next()) {
                        int day = hrs.getInt(1);
                        String reason = hrs.getString("reason");
                        holidayDaysList.add(day);
                        holidayDetails.put(day, (reason != null) ? reason : "Holiday");
                    }
                }
            }

            if ("status".equals(action)) {
                String sql = "SELECT punch_in, punch_out FROM attendance WHERE user_email=? AND work_date=?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, targetEmail);
                    ps.setDate(2, Date.valueOf(today));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            boolean isOut = rs.getTimestamp("punch_out") != null;
                            out.print("{\"punchedIn\": true, \"punchedOut\": " + isOut + "}");
                        } else {
                            out.print("{\"punchedIn\": false}");
                        }
                    }
                }
            }
            else if ("in".equals(action)) {
                String allowedIp = "127.0.0.1";
                boolean isWfhAllowed = false;

                try (PreparedStatement ips = con.prepareStatement("SELECT setting_value FROM network_settings WHERE setting_key='office_ip'")) {
                    try (ResultSet rsIp = ips.executeQuery()) {
                        if (rsIp.next()) { allowedIp = rsIp.getString("setting_value"); }
                    }
                }

                try (PreparedStatement ups = con.prepareStatement("SELECT wfh_allowed FROM users WHERE email=?")) {
                    ups.setString(1, sessionUser);
                    try (ResultSet rsUser = ups.executeQuery()) {
                        if (rsUser.next()) { isWfhAllowed = rsUser.getBoolean("wfh_allowed"); }
                    }
                }

                String clientIp = request.getHeader("X-Forwarded-For");
                if (clientIp != null && clientIp.contains(",")) { clientIp = clientIp.split(",")[0].trim(); }
                if (clientIp == null) { clientIp = request.getRemoteAddr(); }
                
                boolean isLocal = clientIp.equals("127.0.0.1") || clientIp.equals("0:0:0:0:0:0:0:1");

                if (isWfhAllowed || clientIp.equals(allowedIp) || isLocal) {
                    String sql = "INSERT INTO attendance (user_email, work_date, punch_in) VALUES (?, ?, CURRENT_TIMESTAMP)";
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, sessionUser);
                        ps.setDate(2, Date.valueOf(today));
                        ps.executeUpdate();
                        out.print("{\"success\": true}");
                    }
                } else {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    out.print("{\"success\": false, \"message\": \"Access Denied: WFH not enabled and not on Office Wi-Fi.\"}");
                }
            }
            else if ("get_office_ip".equals(action)) {
                try (PreparedStatement ips = con.prepareStatement("SELECT setting_value FROM network_settings WHERE setting_key='office_ip'")) {
                    try (ResultSet rsIp = ips.executeQuery()) {
                        String current = rsIp.next() ? rsIp.getString("setting_value") : "Not Set";
                        out.print("{\"ip\": \"" + current + "\"}");
                    }
                }
            }
            else if ("update_office_ip".equals(action)) {
                String newIp = request.getParameter("new_ip");
                String sql = "INSERT INTO network_settings (setting_key, setting_value) VALUES ('office_ip', ?) " +
                             "ON CONFLICT (setting_key) DO UPDATE SET setting_value=EXCLUDED.setting_value";
                try (PreparedStatement ups = con.prepareStatement(sql)) {
                    ups.setString(1, newIp);
                    ups.executeUpdate();
                    out.print("{\"success\": true}");
                }
            }
            else if ("out".equals(action)) {
                String sql = "UPDATE attendance SET punch_out=CURRENT_TIMESTAMP WHERE user_email=? AND work_date=? AND punch_out IS NULL";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, sessionUser);
                    ps.setDate(2, Date.valueOf(today));
                    ps.executeUpdate();
                    out.print("{\"success\": true}");
                }
            }
            else if ("stats".equals(action)) {
                LocalDate empJoiningDate = today; 
                try (PreparedStatement psDoj = con.prepareStatement("SELECT date_of_joining FROM users WHERE email=?")) {
                    psDoj.setString(1, targetEmail);
                    try (ResultSet rsDoj = psDoj.executeQuery()) {
                        if (rsDoj.next() && rsDoj.getDate("date_of_joining") != null) {
                            empJoiningDate = rsDoj.getDate("date_of_joining").toLocalDate();
                        }
                    }
                }

                int presentThisMonth = 0;
                String sqlMonth = "SELECT COUNT(*) FROM attendance WHERE user_email=? AND EXTRACT(MONTH FROM work_date)=? AND EXTRACT(YEAR FROM work_date)=?";
                try (PreparedStatement ps = con.prepareStatement(sqlMonth)) {
                    ps.setString(1, targetEmail);
                    ps.setInt(2, today.getMonthValue());
                    ps.setInt(3, today.getYear());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) presentThisMonth = rs.getInt(1);
                    }
                }

                int workingDaysThisMonthSoFar = 0;
                LocalDate tempMonth = today.withDayOfMonth(1);
                while (!tempMonth.isAfter(today)) {
                    if (!tempMonth.isBefore(empJoiningDate) && tempMonth.getDayOfWeek().getValue() != 7 && !holidayDaysList.contains(tempMonth.getDayOfMonth())) {
                        workingDaysThisMonthSoFar++;
                    }
                    tempMonth = tempMonth.plusDays(1);
                }

                int totalPresent = 0;
                try (PreparedStatement psTotal = con.prepareStatement("SELECT COUNT(*) FROM attendance WHERE user_email=?")) {
                    psTotal.setString(1, targetEmail);
                    try (ResultSet rsTotal = psTotal.executeQuery()) {
                        if (rsTotal.next()) totalPresent = rsTotal.getInt(1);
                    }
                }

                int totalPassedWorkingDays = 0;
                Set<LocalDate> allHolidays = new HashSet<>();
                try (Statement st = con.createStatement(); ResultSet hrsAll = st.executeQuery("SELECT holiday_date FROM holidays")) {
                    while(hrsAll.next()) {
                        Date d = hrsAll.getDate("holiday_date");
                        if (d != null) allHolidays.add(d.toLocalDate());
                    }
                }

                LocalDate lifeIter = empJoiningDate; 
                while(!lifeIter.isAfter(today)) {
                    if (lifeIter.getDayOfWeek().getValue() != 7 && !allHolidays.contains(lifeIter)) {
                        totalPassedWorkingDays++;
                    }
                    lifeIter = lifeIter.plusDays(1);
                }

                int totalAbsences = totalPassedWorkingDays - totalPresent;
                out.print("{\"workingDays\": " + workingDaysThisMonthSoFar + ", \"present\": " + presentThisMonth + ", \"leaves\": " + (totalAbsences < 0 ? 0 : totalAbsences) + "}");
            }
            else if ("monthly_status".equals(action)) {
                StringBuilder json = new StringBuilder("{\"attendance\": {");
                String sql = "SELECT EXTRACT(DAY FROM work_date)::int FROM attendance WHERE user_email=? AND EXTRACT(MONTH FROM work_date)=? AND EXTRACT(YEAR FROM work_date)=?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, targetEmail);
                    ps.setInt(2, reqMonth);
                    ps.setInt(3, reqYear);
                    try (ResultSet rs = ps.executeQuery()) {
                        boolean first = true;
                        while(rs.next()) {
                            if (!first) json.append(",");
                            json.append("\"").append(rs.getInt(1)).append("\": 1");
                            first = false;
                        }
                    }
                }
                json.append("}, \"holidays\": [");
                boolean first = true;
                for(Integer d : holidayDaysList) {
                    if(!first) json.append(",");
                    json.append(d);
                    first = false;
                }
                json.append("], \"holidayDetails\": {");
                first = true;
                for (Map.Entry<Integer, String> entry : holidayDetails.entrySet()) {
                    if (!first) json.append(",");
                    json.append("\"").append(entry.getKey()).append("\": \"").append(entry.getValue().replace("\"", "\\\"")).append("\"");
                    first = false;
                }
                json.append("}}");
                out.print(json.toString());
            }
            else if ("toggle_holiday".equals(action)) {
                String hDate = request.getParameter("date");
                String reason = request.getParameter("reason");

                if (reason == null || reason.trim().isEmpty()) {
                    try (PreparedStatement del = con.prepareStatement("DELETE FROM holidays WHERE holiday_date=CAST(? AS DATE)")) {
                        del.setString(1, hDate);
                        del.executeUpdate();
                    }
                } else {
                    String sql = "INSERT INTO holidays (holiday_date, reason) VALUES (CAST(? AS DATE), ?) " +
                                 "ON CONFLICT (holiday_date) DO UPDATE SET reason=EXCLUDED.reason";
                    try (PreparedStatement upsert = con.prepareStatement(sql)) {
                        upsert.setString(1, hDate);
                        upsert.setString(2, reason);
                        upsert.executeUpdate();
                    }
                }
                out.print("{\"success\": true}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            out.print("{\"success\": false, \"error\": \"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }
}
