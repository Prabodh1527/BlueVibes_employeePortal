import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.WebListener;
import java.sql.*;
import java.time.LocalDate;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@WebListener
public class BirthdayScheduler implements ServletContextListener {
    private static final Logger LOGGER = Logger.getLogger(BirthdayScheduler.class.getName());
    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("=== AUTOMATED PERSONALIZED BIRTHDAY SCHEDULER INITIALIZED ===");
        scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // Executes automatically once every 24 hours
        scheduler.scheduleAtFixedRate(this::checkAndSendBirthdays, 0, 1, TimeUnit.DAYS);
    }

    private void checkAndSendBirthdays() {
        LOGGER.info("Executing automated database query for today's birthdays...");
        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();
        int currentDay = today.getDayOfMonth();

        Connection con = null;
        try {
            con = DBConnection.getConnection();
            
            // Targets 'email', 'fullname', and 'date_of_birth' exactly as they exist in your database
            String sql = "SELECT email, fullname FROM users WHERE EXTRACT(MONTH FROM date_of_birth) = ? AND EXTRACT(DAY FROM date_of_birth) = ?";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.setInt(1, currentMonth);
            pst.setInt(2, currentDay);
            
            ResultSet rs = pst.executeQuery();
            int matchCount = 0;
            
            while (rs.next()) {
                String employeeEmail = rs.getString("email");
                String employeeName = rs.getString("fullname");
                
                if (employeeName == null || employeeName.trim().isEmpty()) {
                    employeeName = "Valued Team Member";
                }
                
                LOGGER.info("Birthday match found! Preparing personalized card for: " + employeeName + " (" + employeeEmail + ")");
                
                // Dispatches to the mailer engine
                BirthdayEmailSender.sendBirthdayEmail(employeeEmail, employeeName);
                matchCount++;
            }
            LOGGER.info("Daily birthday run complete. Total greetings dispatched today: " + matchCount);
            
        } catch (Exception e) {
            LOGGER.severe("CRITICAL ERROR in automated birthday run: " + e.getMessage());
        } finally {
            if (con != null) { 
                try { con.close(); } catch (SQLException e) { LOGGER.severe("Failed to release DB handle: " + e.getMessage()); } 
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (scheduler != null) {
            scheduler.shutdownNow();
            LOGGER.info("Birthday Scheduler Service successfully stopped.");
        }
    }
}
