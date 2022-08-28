package PayrollMgt;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation login
 */
@WebServlet("/login")
public class login extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public login() {
		super();
		// TODO Auto-generated constructor stub
	}

	// the login user info. for validation
	static String useridPay, pwdPay, emailPay;
	static String useridProj, pwdProj, emailProj;

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// read login forms
		String warningMsg="No permission to sensitive database!";
			if (request.getParameter("loginPay")!=null) {
				useridPay = request.getParameter("useridPay");
				pwdPay = request.getParameter("pwdPay");
				emailPay=request.getParameter("emailPay");
				if (pwdPay.equals("root") && (useridPay.equals("001")||useridPay.equals("002")||useridPay.equals("003")||useridPay.equals("004")))
					response.sendRedirect("MainPayroll.html");	
				else 
				new AlertReminder().popAlert(warningMsg);
			}
			if (request.getParameter("loginProj")!=null) {
				useridProj = request.getParameter("useridProj");
				pwdProj = request.getParameter("pwdProj");
				emailProj=request.getParameter("emailProj");
				if (pwdProj.equals("root") && (useridProj.equals("101")||useridPay.equals("102")||useridPay.equals("103")||useridPay.equals("104")))
					response.sendRedirect("MainProject.html");	
				else 
					new AlertReminder().popAlert(warningMsg);
			}
			

 	/*
		// get response writer
		PrintWriter writer = response.getWriter();
		
		// build HTML code
		String htmlRespone = "<html>"+warningMsg+"</html>";
			
		// return response
		writer.println(htmlRespone);*/
		
	}

}
