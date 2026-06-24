import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/DashboardCelebrationServlet")
public class DashboardCelebrationServlet extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");

        JSONObject result =
        new JSONObject();

        JSONArray birthdays =
        new JSONArray();

        JSONArray anniversaries =
        new JSONArray();

        try(
            Connection con =
            DBConnection.getConnection()
        ){

            LocalDate today =
            LocalDate.now();

            int month =
            today.getMonthValue();

            int day =
            today.getDayOfMonth();

            String birthdaySql =

            "SELECT fullname " +
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

                birthdays.put(
                birthdayRs.getString(
                "fullname"));
            }

            String anniversarySql =

            "SELECT fullname " +
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

                anniversaries.put(
                anniversaryRs.getString(
                "fullname"));
            }

            result.put(
            "birthdays",
            birthdays);

            result.put(
            "anniversaries",
            anniversaries);

        }catch(Exception e){

            e.printStackTrace();
        }

        PrintWriter out =
        response.getWriter();

        out.print(
        result.toString());
    }
}
