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

            case "getKRADefinition":
                getKRADefinition(request,response);
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
                            .getAttribute("userEmail");
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

                String responseSql =
                    
                    "SELECT self_appraisal,self_rating " +
                    "FROM kra_response " +
                    "WHERE kra_id=? " +
                    "AND employee_email=? " +
                    "ORDER BY id DESC LIMIT 1";
                    
                    PreparedStatement responsePs =
                    con.prepareStatement(responseSql);
                    
                    responsePs.setInt(
                        1,
                        rs.getInt("id"));
                    
                    responsePs.setString(
                        2,
                        email);
                    
                    ResultSet responseRs =
                    responsePs.executeQuery();
                    
                    if(responseRs.next()){
                    
                        row.put(
                            "selfAppraisal",
                            responseRs.getString(
                            "self_appraisal"));
                    
                        row.put(
                            "selfRating",
                            responseRs.getInt(
                            "self_rating"));
                    }
                    else{
                    
                        row.put(
                            "selfAppraisal",
                            "");
                    
                        row.put(
                            "selfRating",
                            0);
                    }
    
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

    JSONArray arr = new JSONArray();

        try{
    
            Connection con =
            DBConnection.getConnection();
    
            String sql =
    
            "SELECT " +
            "k.id as kra_id," +
            "k.employee_email," +
            "k.employee_id," +
            "k.designation," +
            "k.assessment_year," +
            "k.objective," +
            "k.measurement_criteria," +
            "k.sub_activity," +
            "k.weightage," +
            "r.self_appraisal," +
            "r.self_rating," +
            "r.response_status " +
            "FROM kra_master k " +
            "LEFT JOIN kra_response r " +
            "ON k.id=r.kra_id " +
            "WHERE r.response_status='SUBMITTED' "+
            "AND k.employee_email=? " +
            "AND k.assessment_year=?";
    
            PreparedStatement ps =
            con.prepareStatement(sql);

            ps.setString(
                1,
                request.getParameter("email"));
            
            ps.setString(
                2,
                request.getParameter("year"));
    
            ResultSet rs =
            ps.executeQuery();
    
            while(rs.next()){
    
                JSONObject obj =
                new JSONObject();
    
                obj.put(
                    "kraId",
                    rs.getInt("kra_id"));
    
                obj.put(
                    "employeeEmail",
                    rs.getString("employee_email"));
    
                obj.put(
                    "employeeId",
                    rs.getString("employee_id"));
    
                obj.put(
                    "designation",
                    rs.getString("designation"));
    
                obj.put(
                    "assessmentYear",
                    rs.getString("assessment_year"));
    
                obj.put(
                    "objective",
                    rs.getString("objective"));
    
                obj.put(
                    "measurementCriteria",
                    rs.getString("measurement_criteria"));
    
                obj.put(
                    "subActivity",
                    rs.getString("sub_activity"));
    
                obj.put(
                    "weightage",
                    rs.getInt("weightage"));
    
                obj.put(
                    "selfAppraisal",
                    rs.getString("self_appraisal"));
    
                obj.put(
                    "selfRating",
                    rs.getInt("self_rating"));
    
                obj.put(
                    "status",
                    rs.getString("response_status"));
    
                arr.put(obj);
            }
    
        }catch(Exception e){
    
            e.printStackTrace();
        }
    
        response.getWriter().print(
        arr.toString());
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

        try{
    
            String email =
            (String)request.getSession()
                           .getAttribute("userEmail");
    
            String body =
            getRequestBody(request);
    
            JSONObject json =
            new JSONObject(body);
    
            JSONArray appraisals =
            json.getJSONArray("appraisals");
    
            Connection con =
            DBConnection.getConnection();
    
            String sql =
    
            "INSERT INTO kra_response(" +
            "kra_id," +
            "employee_email," +
            "self_appraisal," +
            "self_rating," +
            "response_status," +
            "submitted_on" +
            ") VALUES(?,?,?,?,?,CURRENT_TIMESTAMP)";
    
            PreparedStatement ps =
            con.prepareStatement(sql);

            String kraSql =

                "SELECT id FROM kra_master " +
                "WHERE employee_email=? " +
                "AND assessment_year=? " +
                "AND status='PUBLISHED'";
                
                PreparedStatement kraPs =
                con.prepareStatement(kraSql);
                
                kraPs.setString(1,email);
                
                kraPs.setString(
                    2,
                    json.getString("assessmentYear"));
                
                ResultSet kraRs =
                kraPs.executeQuery();
                
                int kraId = 0;
                
                if(kraRs.next()){
                
                    kraId = kraRs.getInt("id");
                }
    
            for(int i=0;i<appraisals.length();i++){
    
                JSONObject row =
                appraisals.getJSONObject(i);
    
                ps.setInt(1,kraId);
    
                ps.setString(2,email);
    
                ps.setString(
                    3,
                    row.getString(
                    "selfAppraisal"));
    
                ps.setInt(
                    4,
                    Integer.parseInt(
                    row.getString(
                    "selfRating")));
    
                ps.setString(
                    5,
                    "DRAFT");
    
                ps.executeUpdate();
            }
    
            response.getWriter()
                    .print(
                    "Employee Draft Saved");
    
        }catch(Exception e){
    
            e.printStackTrace();
    
            response.getWriter()
                    .print("Error");
        }
    }

    private void submitEmployeeAppraisal(
        HttpServletRequest request,
        HttpServletResponse response)
        throws IOException {

        try{
    
            String email =
            (String)request.getSession()
                           .getAttribute("userEmail");
    
            String body =
            getRequestBody(request);
    
            JSONObject json =
            new JSONObject(body);
    
            String assessmentYear =
            json.getString("assessmentYear");
    
            Connection con =
            DBConnection.getConnection();
    
            String sql =
    
            "UPDATE kra_response " +
            "SET response_status='SUBMITTED' " +
            "WHERE employee_email=? " +
            "AND kra_id IN (" +
            "SELECT id FROM kra_master " +
            "WHERE employee_email=? " +
            "AND assessment_year=? " +
            "AND status='PUBLISHED'" +
            ")";
    
            PreparedStatement ps =
            con.prepareStatement(sql);
    
            ps.setString(1,email);
            ps.setString(2,email);
            ps.setString(3,assessmentYear);
    
            int rowsUpdated =
            ps.executeUpdate();
    
            System.out.println(
                "ROWS UPDATED = " +
                rowsUpdated);
    
            response.getWriter()
                    .print(
                    "Employee Appraisal Submitted");
    
        }catch(Exception e){
    
            e.printStackTrace();
    
            response.getWriter()
                    .print("Error");
        }
    }

    private void getKRADefinition(
    HttpServletRequest request,
    HttpServletResponse response)
    throws IOException {

    response.setContentType("application/json");

    JSONArray arr = new JSONArray();

    try{

        String email =
        request.getParameter("email");

        Connection con =
        DBConnection.getConnection();

        String sql =

        "SELECT * FROM kra_master " +
        "WHERE employee_email=?";

        PreparedStatement ps =
        con.prepareStatement(sql);

        ps.setString(1,email);

        ResultSet rs =
        ps.executeQuery();

        while(rs.next()){

            JSONObject obj =
            new JSONObject();

            obj.put(
                "objective",
                rs.getString("objective"));

            obj.put(
                "measurementCriteria",
                rs.getString("measurement_criteria"));

            obj.put(
                "subActivity",
                rs.getString("sub_activity"));

            obj.put(
                "weightage",
                rs.getInt("weightage"));

            arr.put(obj);
        }

    }catch(Exception e){

        e.printStackTrace();
    }

    response.getWriter().print(arr.toString());
}

}
