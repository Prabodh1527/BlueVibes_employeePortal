import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@WebListener
public class CelebrationScheduler
implements ServletContextListener {

private ScheduledExecutorService scheduler;

@Override
public void contextInitialized(
        ServletContextEvent sce) {

    scheduler =
    Executors.newSingleThreadScheduledExecutor();

    long initialDelay =
    calculateDelayTo7AM();

    scheduler.scheduleAtFixedRate(

        () -> {

            try{

                checkAndSendCelebrations();

            }catch(Exception e){

                e.printStackTrace();
            }

        },

        initialDelay,

        24,

        TimeUnit.HOURS
    );

    try{

        checkAndSendCelebrations();

    }catch(Exception e){

        e.printStackTrace();
    }
}

private void checkAndSendCelebrations() {

    LocalDate today =
    LocalDate.now(
            java.time.ZoneId.of("Asia/Kolkata")
            );

    int month =
    today.getMonthValue();

    int day =
    today.getDayOfMonth();

    try(
        Connection con =
        DBConnection.getConnection()
    ){

        String birthdaySql =

        "SELECT fullname," +
        "communication_email," +
        "email " +
        "FROM users " +
        "WHERE EXTRACT(MONTH FROM date_of_birth)=? " +
        "AND EXTRACT(DAY FROM date_of_birth)=?";

        PreparedStatement birthdayPs =
        con.prepareStatement(
        birthdaySql);

        birthdayPs.setInt(1,month);
        birthdayPs.setInt(2,day);

        ResultSet birthdayRs =
        birthdayPs.executeQuery();

        while(birthdayRs.next()){

            String employeeName =
            birthdayRs.getString(
            "fullname");

            String email =
            birthdayRs.getString(
            "communication_email");

            if(email==null ||
               email.trim().isEmpty()){

                email =
                birthdayRs.getString(
                "email");
            }

            PreparedStatement allUsersPs =
                con.prepareStatement(
                "SELECT email, communication_email FROM users");
                
                ResultSet allUsersRs =
                allUsersPs.executeQuery();
                
                while(allUsersRs.next()){
                
                    String targetEmail =
                    allUsersRs.getString(
                    "communication_email");
                
                    if(targetEmail == null ||
                       targetEmail.trim().isEmpty()){
                
                        targetEmail =
                        allUsersRs.getString(
                        "email");
                    }
                
                    if(targetEmail == null ||
                       targetEmail.trim().isEmpty()){
                        continue;
                    }
                
                    BirthdayWorkAnniversaryMailer
                    .sendBirthdayMail(
                        targetEmail,
                        employeeName
                    );
                }

            System.out.println(
            "Birthday Mail Sent To "
            + employeeName);
        }

        String anniversarySql =

        "SELECT fullname," +
        "communication_email," +
        "email " +
        "FROM users " +
        "WHERE EXTRACT(MONTH FROM date_of_joining)=? " +
        "AND EXTRACT(DAY FROM date_of_joining)=?";

        PreparedStatement anniversaryPs =
        con.prepareStatement(
        anniversarySql);

        anniversaryPs.setInt(1,month);
        anniversaryPs.setInt(2,day);

        ResultSet anniversaryRs =
        anniversaryPs.executeQuery();

        while(anniversaryRs.next()){

            String employeeName =
            anniversaryRs.getString(
            "fullname");

            String email =
            anniversaryRs.getString(
            "communication_email");

            if(email==null ||
               email.trim().isEmpty()){

                email =
                anniversaryRs.getString(
                "email");
            }

            PreparedStatement allUsersPs =
                con.prepareStatement(
                "SELECT email, communication_email FROM users");
                
                ResultSet allUsersRs =
                allUsersPs.executeQuery();
                
                while(allUsersRs.next()){
                
                    String targetEmail =
                    allUsersRs.getString(
                    "communication_email");
                
                    if(targetEmail == null ||
                       targetEmail.trim().isEmpty()){
                
                        targetEmail =
                        allUsersRs.getString(
                        "email");
                    }
                
                    if(targetEmail == null ||
                       targetEmail.trim().isEmpty()){
                        continue;
                    }
                
                    BirthdayWorkAnniversaryMailer
                    .sendAnniversaryMail(
                        targetEmail,
                        employeeName
                    );
                }

            System.out.println(
            "Anniversary Mail Sent To "
            + employeeName);
        }

    }catch(Exception e){

        e.printStackTrace();
    }
}

private long calculateDelayTo7AM() {

    LocalDateTime now =
    LocalDateTime.now();

    LocalDateTime nextRun =
    now.withHour(7)
       .withMinute(0)
       .withSecond(0)
       .withNano(0);

    if(now.isAfter(nextRun)){

        nextRun =
        nextRun.plusDays(1);
    }

    return Duration
           .between(
                now,
                nextRun)
           .getSeconds();
}

@Override
public void contextDestroyed(
        ServletContextEvent sce) {

    if(scheduler!=null){

        scheduler.shutdown();
    }
}

}
