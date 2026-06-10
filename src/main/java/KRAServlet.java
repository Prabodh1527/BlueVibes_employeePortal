import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

            default:
                response.getWriter().print("Invalid Action");
        }
    }

    private void getEmployees(HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");

        response.getWriter().print("[]");
    }

    private void saveDraft(HttpServletRequest request,
                           HttpServletResponse response)
            throws IOException {

        response.getWriter().print("Draft Saved");
    }

    private void publish(HttpServletRequest request,
                         HttpServletResponse response)
            throws IOException {

        response.getWriter().print("Published");
    }

    private void getReviewData(HttpServletRequest request,
                               HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");

        response.getWriter().print("[]");
    }
}
