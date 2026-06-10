import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;

@WebServlet("/KRAServlet")
public class KRAServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");

        if(action == null){
            response.getWriter().print("No Action");
            return;
        }

        switch(action){

            case "getEmployees":
                getEmployees(response);
                break;

            case "getReviewData":
                getReviewData(request,response);
                break;

            case "getEmployeeKRA":
                getEmployeeKRA(request,response);
                break;

            default:
                response.getWriter().print("Invalid Action");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");

        if(action == null){
            response.getWriter().print("No Action");
            return;
        }

        switch(action){

            case "saveDraft":
                saveDraft(request,response);
                break;

            case "publish":
                publish(request,response);
                break;
                
            case "saveEmployeeDraft":
                saveEmployeeDraft(request,response);
                break;
            
            case "submitAppraisal":
                submitEmployeeAppraisal(request,response);
                break;

            default:
                response.getWriter().print("Invalid Action");
        }
    }

    /*private void getEmployees(HttpServletResponse response)
        throws IOException {

        response.setContentType("application/json");
    
        JSONArray arr = new JSONArray();
    
        try(Connection con = DBConnection.getConnection()){
    
            String sql =
            "SELECT fullname,email,employee_id,designation " +
            "FROM users " +
            "WHERE role <> 'Admin' " +
            "ORDER BY fullname";
    
            PreparedStatement ps =
            con.prepareStatement(sql);
    
            ResultSet rs =
            ps.executeQuery();
    
            while(rs.next()){
    
                JSONObject obj =
                new JSONObject();
    
                obj.put("fullname",
                        rs.getString("fullname"));
    
                obj.put("email",
                        rs.getString("email"));
    
                obj.put("employeeId",
                        rs.getString("employee_id"));
    
                obj.put("designation",
                        rs.getString("designation"));
    
                arr.put(obj);
            }
    
        }catch(Exception e){
            e.printStackTrace();
        }
    
        response.getWriter().print(arr.toString());
    }*/
    private void getEmployees(HttpServletResponse response)
        throws IOException {

        response.setContentType("application/json");
    
        JSONArray arr = new JSONArray();
    
        try(Connection con = DBConnection.getConnection()){
    
            System.out.println("DB Connected = " + (con != null));
    
            String sql =
            "SELECT fullname,email,employee_id,designation,role " +
            "FROM users " +
            "ORDER BY fullname";
    
            PreparedStatement ps =
            con.prepareStatement(sql);
    
            ResultSet rs =
            ps.executeQuery();
    
            while(rs.next()){
    
                System.out.println(
                    "Employee Found: "
                    + rs.getString("fullname")
                );
    
                JSONObject obj =
                new JSONObject();
    
                obj.put("fullname",
                        rs.getString("fullname"));
    
                obj.put("email",
                        rs.getString("email"));
    
                obj.put("employeeId",
                        rs.getString("employee_id"));
    
                obj.put("designation",
                        rs.getString("designation"));
    
                arr.put(obj);
            }
    
        }catch(Exception e){
            e.printStackTrace();
        }
    
        System.out.println(arr.toString());
    
        response.getWriter().print(arr.toString());
    }

    private void getEmployeeKRA(
        HttpServletRequest request,
        HttpServletResponse response)
        throws IOException {

        response.setContentType("application/json");
    
        JSONObject result =
        new JSONObject();
    
        JSONArray rows =
        new JSONArray();
    
        try{
    
            String email =
            (String) request.getSession()
                            .getAttribute("email");
            System.out.println("SESSION EMAIL = " + email);
            
            System.out.println(
                "QUERY = SELECT * FROM kra_master " +
                "WHERE employee_email='" + email +
                "' AND status='PUBLISHED'"
            );
    
            Connection con =
            DBConnection.getConnection();
    
            String sql =
    
            "SELECT * " +
            "FROM kra_master " +
            "WHERE employee_email=? " +
            "AND status='PUBLISHED'";
    
            PreparedStatement ps =
            con.prepareStatement(sql);
    
            ps.setString(1,email);
    
            ResultSet rs =
            ps.executeQuery();
    
            boolean first=true;
    
            while(rs.next()){
    
                if(first){
    
                    result.put(
                        "assessmentYear",
                        rs.getString(
                        "assessment_year"));
    
                    result.put(
                        "employeeId",
                        rs.getString(
                        "employee_id"));
    
                    result.put(
                        "designation",
                        rs.getString(
                        "designation"));
    
                    first=false;
                }
    
                JSONObject row =
                new JSONObject();
    
                row.put(
                    "objective",
                    rs.getString("objective"));
    
                row.put(
                    "measurementCriteria",
                    rs.getString(
                    "measurement_criteria"));
    
                row.put(
                    "subActivity",
                    rs.getString(
                    "sub_activity"));
    
                row.put(
                    "weightage",
                    rs.getInt("weightage"));
    
                rows.put(row);
            }
    
            result.put("rows",rows);
    
        }catch(Exception e){
            e.printStackTrace();
        }
    
        response.getWriter().print(
        result.toString());
    }

    private void saveDraft(HttpServletRequest request,
                       HttpServletResponse response)
        throws IOException {

        try{
    
            String body = getRequestBody(request);
    
            JSONObject json =
            new JSONObject(body);
    
            String assessmentYear =
            json.getString("assessmentYear");
    
            String employeeEmail =
            json.getString("employeeEmail");
    
            String employeeId =
            json.getString("employeeId");
    
            String designation =
            json.getString("designation");
    
            JSONArray rows =
            json.getJSONArray("rows");
    
            Connection con =
            DBConnection.getConnection();
    
            String deleteSql =
            "DELETE FROM kra_master " +
            "WHERE employee_email=? " +
            "AND assessment_year=?";
    
            PreparedStatement deletePs =
            con.prepareStatement(deleteSql);
    
            deletePs.setString(1,employeeEmail);
            deletePs.setString(2,assessmentYear);
    
            deletePs.executeUpdate();
    
            String insertSql =
    
            "INSERT INTO kra_master(" +
            "employee_email," +
            "employee_id," +
            "designation," +
            "assessment_year," +
            "objective," +
            "measurement_criteria," +
            "sub_activity," +
            "weightage," +
            "status," +
            "created_on" +
            ") VALUES(?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)";
    
            PreparedStatement ps =
            con.prepareStatement(insertSql);
    
            for(int i=0;i<rows.length();i++){
    
                JSONObject row =
                rows.getJSONObject(i);
    
                ps.setString(1,employeeEmail);
                ps.setString(2,employeeId);
                ps.setString(3,designation);
                ps.setString(4,assessmentYear);
    
                ps.setString(5,
                row.getString("objective"));
    
                ps.setString(6,
                row.getString("measurementCriteria"));
    
                ps.setString(7,
                row.getString("subActivity"));
    
                ps.setInt(8,
                Integer.parseInt(
                row.getString("weightage")));
    
                ps.setString(9,"DRAFT");
    
                ps.executeUpdate();
            }
    
            response.getWriter()
                    .print("Draft Saved");
    
        }catch(Exception e){
    
            e.printStackTrace();
    
            response.getWriter()
                    .print("Error");
        }
    }
    
    private void publish(HttpServletRequest request,
                     HttpServletResponse response)
        throws IOException {

        try{
    
            String body =
            getRequestBody(request);
    
            JSONObject json =
            new JSONObject(body);
    
            String assessmentYear =
            json.getString("assessmentYear");
    
            String employeeEmail =
            json.getString("employeeEmail");
    
            Connection con =
            DBConnection.getConnection();
    
            String sql =
    
            "UPDATE kra_master " +
            "SET status='PUBLISHED' " +
            "WHERE employee_email=? " +
            "AND assessment_year=?";
    
            PreparedStatement ps =
            con.prepareStatement(sql);
    
            ps.setString(1,employeeEmail);
            ps.setString(2,assessmentYear);
    
            ps.executeUpdate();
    
            response.getWriter()
                    .print("Published");
    
        }catch(Exception e){
    
            e.printStackTrace();
    
            response.getWriter()
                    .print("Error");
        }
    }

    private void getReviewData(HttpServletRequest request,
                               HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");

        response.getWriter().print("[]");
    }

    private String getRequestBody(HttpServletRequest request)
        throws IOException {

        StringBuilder sb = new StringBuilder();
    
        BufferedReader reader = request.getReader();
    
        String line;
    
        while((line = reader.readLine()) != null){
            sb.append(line);
        }
    
        return sb.toString();
    }

    private void saveEmployeeDraft(
        HttpServletRequest request,
        HttpServletResponse response)
        throws IOException {

        response.getWriter().print(
            "Employee Draft Saved");
    }
    
    private void submitEmployeeAppraisal(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {
    
        response.getWriter().print(
            "Employee Appraisal Submitted");
    }
    

}
