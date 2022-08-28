package PayrollMgt;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/MainPayroll")
public class MainPayroll extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {
	static final long serialVersionUID = 1L;


	//this set of employee basic information is used by all applications, thus enlisted here for efficiency	
	String empID, name, address, city, state, zip, SSN, email, office_code, title, dept_code, div_code, projID,
			office_name, phone, classification, proj_name;
	Date start_date,start, end;
	Integer salary;
	Double hourly_pay;
	
	String errorMessage="SQL connection failed! ";
	String errorMsg1 = "Please enter correct employee -missing employee start at the date";
	String errorMsg2 = "Please enter correct Department ID!";
	String errorMsg3 = "Please enter correct Division ID!";
	String errorMsg4 = "Please enter correct employee ID!";
	String errorMsg5 = "Please enter correct IRS year";
	String errorMsgMiss = "Missing input from HTML forms. Please check again!";

	String fromGmail="eugina.ding@gmail.com";
	String toGmail="eugina.ding@gmail.com";
	static Integer reportID = 101;
	static Integer reportSeries = 1001;

	public MainPayroll() {
		super();
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();

		// @ initial messages
		ResultSet rs;
		// initial html checkbox parameters - myCheckBox is true if checked, false if
		// not checked
		boolean checkPayEmp = request.getParameter("checkPayEmp") != null;
		boolean checkPayDept = request.getParameter("checkPayDept") != null;
		boolean checkPayDiv = request.getParameter("checkPayDiv") != null;
		boolean checkComIRS = request.getParameter("checkComIRS") != null;
		boolean checkEmpIRS = request.getParameter("checkEmpIRS") != null;
		// @ create queries prepared statement for monthly paychecks.
		// note that hourly employees are contractors who has limited duration and no benefit plans!
		// note that in the following calculations use this view as a master for new start and end times for contractors only

		String sqlMonthSal = "SELECT * FROM empview WHERE empID = ? AND start_date<=? AND start<=? AND ?<=end";
		
		String sqlDeptPerm="SELECT d.dept_code,d.dept_name, e.name as manager, e.phone, d.email, count(*) as permEmp, sum(view.hourly_pay) as permSal " + 
		"FROM cs631db.empview as view, department as d, empview as e "+
		"WHERE view.dept_code= d.dept_code AND view.classification='permanent' AND d.dept_head=e.empID AND "+
		"view.dept_code=? AND view.start_date<=? AND view.start<=? AND ?<=view.end ";

		String sqlDeptHour="SELECT d.dept_code,d.dept_name, e.name as manager, e.phone, d.email, count(*) as hourEmp, sum(view.hourly_pay) as hourSal "+ 
		"FROM cs631db.empview as view, department as d, empview as e "+
		"WHERE view.dept_code= d.dept_code AND view.classification='hourly' AND d.dept_head=e.empID AND "+
		"view.dept_code=? AND view.start_date<=? AND view.start<=? AND ?<=view.end ";

		String sqlDivPerm="SELECT d.div_code,d.div_name, e.name as manager, e.phone, d.email, count(*) as permEmp, sum(view.hourly_pay) as permSal "+ 
		"FROM cs631db.empview as view, division as d, empview as e "+
		"WHERE view.div_code= d.div_code AND view.classification='permanent' AND d.div_head=e.empID AND "+
		"view.div_code=? AND view.start_date<=? AND view.start<=? AND ?<=view.end ";

		String sqlDivHour="SELECT d.div_code,d.div_name, e.name as manager, e.phone, d.email,count(*) as hourEmp, sum(view.hourly_pay) as hourSal  "+ 
		"FROM cs631db.empview as view, division as d, empview as e "+
		"WHERE view.div_code= d.div_code AND view.classification='hourly' AND d.div_head=e.empID AND "+
		"view.div_code=? AND view.start_date<=? AND view.start<=? AND ?<=view.end ";

		// @ create queries prepared statement for annual IRS, entire year, first half or second half of the year.
		// note that 3 cases depending on date assigned to project (project is 10 yrs, one can start late and leave early) 

		String sqlEmpWholeYear = "SELECT * FROM empview WHERE empID = ? AND start_date<=? AND start<=? AND ?<=end";	
		//late start, so yearend-start, start_date<= yearend '2020-12-31' AND start>=yearstart'2020-01-01' AND start<= yearend '2020-12-31'
		String sqlEmpPartYear1 = "SELECT *, hourly_pay*DATEDIFF(?, start)/7*40 as income FROM empview WHERE empID = ? AND start_date<=? AND start>=? AND start<=?";	
		//early leave, so end-yearstart, start_date<=yearend'2020-12-31' AND end>=yearstart'2020-01-01' AND end<=yearend'2020-12-31'
		String sqlEmpPartYear2 = "SELECT *, hourly_pay*DATEDIFF(end, ?)/7*40 as income FROM empview WHERE empID = ? AND start_date<=? AND end>=? AND end<=?";	

		
		String sqlComIRS = "SELECT * FROM companyirs ORDER BY IRSyear, empID ASC";	
		String sqlComIRS1 = "SELECT * FROM companyirs WHERE IRSyear>=? AND IRSyear<=? ORDER BY IRSyear, empID ASC";	
		
		// @ JDBC connection to SQL - create database and table
		try {

			ConnectionReset con=new ConnectionReset();
			// this is done in project			con.resetDB();
			//initialize employee view for entire company
			con.iniPayroll();
			//sqlMonthSal = "SELECT * FROM empview WHERE empID = ? AND start_date<=? AND start<=? AND ?<=end";

			// @servlet writer to get results posted

			// mySubmit button is true if checked, false if not checked
			if (request.getParameter("payEmp") != null) {
				// myCheckBox is true if checked, false if not checked
				if (checkPayEmp == true) {
					PreparedStatement pstmt = con.connection.prepareStatement(sqlMonthSal);
					empID = request.getParameter("empID");
					String monthEmp = request.getParameter("monthEmp");
					String yearEmp = request.getParameter("yearEmp");
					// run sql query to get employee ID and print paycheck within the project limit
					// the charge to a project shall be between the live span of the project
					pstmt.setString(1, empID);
					pstmt.setString(2, yearEmp+"-"+monthEmp+"-01");
					pstmt.setString(3, yearEmp+"-"+monthEmp+"-01");
					pstmt.setString(4, yearEmp+"-"+monthEmp+"-01");
					
					rs = pstmt.executeQuery();
					//generate monthly pay check for employee
					paycheckEmp(out, rs, monthEmp, yearEmp);
					pstmt.close();
					} else {
						System.out.println(errorMsgMiss);
					}

			} else if (request.getParameter("payDept") != null ) {
				if (checkPayDept == true) {
					dept_code = request.getParameter("deptID");
					String monthDept = request.getParameter("monthDept");
					String yearDept = request.getParameter("yearDept");

					PreparedStatement pstmt1 = con.connection.prepareStatement(sqlDeptPerm);
					PreparedStatement pstmt2 = con.connection.prepareStatement(sqlDeptHour);					
					pstmt1.setString(1, dept_code);
					pstmt1.setString(2, yearDept+"-"+monthDept+"-01");
					pstmt1.setString(3, yearDept+"-"+monthDept+"-01");
					pstmt1.setString(4, yearDept+"-"+monthDept+"-01");
					ResultSet rs1=pstmt1.executeQuery();
					
					pstmt2.setString(1, dept_code);
					pstmt2.setString(2, yearDept+"-"+monthDept+"-01");
					pstmt2.setString(3, yearDept+"-"+monthDept+"-01");
					pstmt2.setString(4, yearDept+"-"+monthDept+"-01");
					ResultSet rs2=pstmt2.executeQuery();					
					
					costDeptDiv(out, dept_code, "Department", monthDept, yearDept, rs1, rs2);
					pstmt1.close();
					}else {
					System.out.println(errorMsgMiss);
				}
			
			} else if (request.getParameter("payDiv") != null) {
				if (checkPayDiv == true) {
					div_code = request.getParameter("divID");
					String monthDiv = request.getParameter("monthDiv");
					String yearDiv = request.getParameter("yearDiv");
								
					PreparedStatement pstmt1 = con.connection.prepareStatement(sqlDivPerm);
					PreparedStatement pstmt2 = con.connection.prepareStatement(sqlDivHour);					
					pstmt1.setString(1, div_code);
					pstmt1.setString(2, yearDiv+"-"+monthDiv+"-01");
					pstmt1.setString(3, yearDiv+"-"+monthDiv+"-01");
					pstmt1.setString(4, yearDiv+"-"+monthDiv+"-01");
					ResultSet rs1=pstmt1.executeQuery();
					
					pstmt2.setString(1, div_code);
					pstmt2.setString(2, yearDiv+"-"+monthDiv+"-01");
					pstmt2.setString(3, yearDiv+"-"+monthDiv+"-01");
					pstmt2.setString(4, yearDiv+"-"+monthDiv+"-01");
					ResultSet rs2=pstmt2.executeQuery();					
					
					costDeptDiv(out, div_code, "Division", monthDiv, yearDiv, rs1, rs2);
					pstmt1.close();
					pstmt2.close();
					
				}else {
					System.out.println(errorMsgMiss);
				}
			  
			} else if (request.getParameter("comIRS") != null) {
				if (checkComIRS == true) {
					
					//create the most updated IRS report for the entire company
					con.companyIRS();
					//generate the most updated IRS report for the entire company
					
/*					Statement stmt=con.connection.createStatement();
					rs=stmt.executeQuery(sqlComIRS);*/
					
					//sqlComIRS1 = "SELECT * FROM companyirs WHERE IRSyear>=? AND IRSyear<=? ORDER BY IRSyear, empID ASC"
					if(request.getParameter("startYr")!=null&&request.getParameter("endYr")!=null) {
					Integer startYr = Integer.parseInt(request.getParameter("startYr"));
					Integer endYr = Integer.parseInt(request.getParameter("endYr"));
					
					//send email out to user
					//new SendEmails(	fromGmail, toGmail,"IRS Reports Ready to download").send();
					PreparedStatement pstmt=con.connection.prepareStatement(sqlComIRS1);
					pstmt.setInt(1, startYr);
					pstmt.setInt(2, endYr);
					rs=pstmt.executeQuery();
					printIRSreport(out, rs);
					}
				
					//sent IRS report out to user
					
					
				} else {
					System.out.println(errorMsgMiss);
				}
			} else if (request.getParameter("empIRS") != null) {
				if (checkEmpIRS == true) {
					String empIDIRS=request.getParameter("empIDIRS");
					String yearIRS=request.getParameter("yearEmpIRS");

					// sqlEmpWholeYear = "SELECT * FROM empview WHERE empID = ? AND start_date<=? AND start<=? AND ?<=end";	

					//late start, so yearend-start, start_date<= yearend '2020-12-31' AND start>=yearstart'2020-01-01' AND start<= yearend '2020-12-31'
					// sqlEmpPartYear1 = "SELECT *, hourly_pay*DATEDIFF(?, start)/7*40 as income FROM empview WHERE empID = ? AND start_date<=? AND start>=? AND start<=?";	

					//early leave, so end-yearstart, start_date<=yearend'2020-12-31' AND end>=yearstart'2020-01-01' AND end<=yearend'2020-12-31'
					// sqlEmpPartYear2 = "SELECT *, hourly_pay*DATEDIFF(end, ?)/7*40 as income FROM empview WHERE empID = ? AND start_date<=? AND end>=? AND end<=?";	

					//check if whole year				
					PreparedStatement pstmtEmpIRS=con.connection.prepareStatement(sqlEmpWholeYear);
					
					pstmtEmpIRS.setString(1, empIDIRS);
					pstmtEmpIRS.setString(2, yearIRS+"-12-31");
					pstmtEmpIRS.setString(3, yearIRS+"-01-01");
					pstmtEmpIRS.setString(4, yearIRS+"-12-31");

					//check if late start 
					PreparedStatement pstmtEmpIRS1=con.connection.prepareStatement(sqlEmpPartYear1);
					
					pstmtEmpIRS1.setString(1, empIDIRS);
					pstmtEmpIRS1.setString(2, yearIRS+"-12-31");
					pstmtEmpIRS1.setString(3, yearIRS+"-01-01");
					pstmtEmpIRS1.setString(4, yearIRS+"-12-31");

					//check if early leave
					PreparedStatement pstmtEmpIRS2=con.connection.prepareStatement(sqlEmpPartYear2);
					
					pstmtEmpIRS2.setString(1, empIDIRS);
					pstmtEmpIRS2.setString(2, yearIRS+"-12-31");
					pstmtEmpIRS2.setString(3, yearIRS+"-01-01");
					pstmtEmpIRS2.setString(4, yearIRS+"-12-31");
					
					//check if whole year
					if(pstmtEmpIRS.execute()) {
						rs=pstmtEmpIRS.executeQuery();
					//type=0: whole year to generate yearly IRS report for employee
						empIRSreport(out, yearIRS, rs, 0);	
					} else if (pstmtEmpIRS1.execute()) {
						//check if late start
						rs=pstmtEmpIRS1.executeQuery();
					//type=1: late start to generate yearly IRS report for employee
					empIRSreport(out, yearIRS, rs, 1);
					
					} else if (pstmtEmpIRS2.execute()) {
						//check if early leave
						rs=pstmtEmpIRS1.executeQuery();
						//type=2: early leave to generate yearly IRS report for employee
						empIRSreport(out, yearIRS, rs, 2);
						
					} else {
						System.out.println(errorMsg4);
					}
					pstmtEmpIRS.close();
					pstmtEmpIRS2.close();
					
				}else {
					System.out.println(errorMsgMiss);
				}
			}

			/*
			 * stmt.executeUpdate(s1); stmt.executeUpdate(s2); stmt.executeUpdate(s3);
			 * stmt.executeUpdate(s4); rs = stmt.executeQuery(sqlQuery);
			 */
		} catch (Exception ex) {
			System.out.println(errorMessage + ex);
			System.exit(0);

			// @servlet response

		}
		out.close();
	}

	public void paycheckEmp(PrintWriter out, ResultSet rs, String monthEmp, String yearEmp) throws SQLException {
		
		// query has output not null
		if (rs.next()) {
			// empID = rs.getString(1);
			name = rs.getString(2);
			title = rs.getString(3);
			start_date = rs.getDate(4);
			address = rs.getString(5);
			city = rs.getString(6);
			state = rs.getString(7);
			zip = rs.getString(8);
			SSN = rs.getString(9);
			email = rs.getString(10);
			office_code = rs.getString(11);
			phone = rs.getString(12);
			office_name = rs.getString(13);
			dept_code = rs.getString(14);
			div_code = rs.getString(15);
			projID = rs.getString(16);
			salary = rs.getInt(19);
			classification = rs.getString(20);
			hourly_pay = rs.getDouble(21);
			proj_name = rs.getString(22);
		}else System.out.println(errorMsgMiss);
		
		out.println("<html>");
		out.println("<head> <meta charset=\"utf-8\" />");
		out.println("<title>The Best-P Company Payroll Pay Check</title>");
		out.println("<link rel=\"stylesheet\" href=\"report.css\">");
		out.println("<script type=\"text/javascript\"</script>");
		out.println("<script src=\"https://code.jquery.com/jquery-1.12.4.min.js\"></script>");
		out.println("</head> <body><div class=\"invoice-box\">");
		out.println("<table cellpadding=\"0\" cellspacing=\"0\">");
		out.println("<tr class=\"top\"><td colspan=\"2\"><table>");
		out.println("<tr><td class=\"title\"><img src=\"adminLogo.jpg\" class=\"image1\" />");
		out.println("<td>Pay Check #: " + reportID++);
		out.println("<br/>Created: <a href=\"#\"><script>document.write(new Date().toLocaleDateString()); </script>");
		out.println("</a></p><br/>Time Period: " + MonthNumber.matchName(monthEmp) + " " + yearEmp);
		out.println("</td></tr></table></td></tr>");
		out.println("<tr class=\"information\"><td colspan=\"2\"><table> <tr><td>");
		out.println("The Best-P Company<br/>7545 MLK J. Blvd<br/>Newark, NJ 07102<br/>USA Headquarter</td><td>");
		out.println("Employee ID-0" + empID);
		out.println("<br/>Name: " + name);
		out.println("<br/>Office Phone: " + phone);
		out.println("<br/>Email: " + email);
		out.println("</td></tr></table></td></tr>");
		out.println("<tr class=\"heading\"><td>Payment Method</td><td>Check Series #</td></tr>");
		out.println("<tr class=\"details\"><td>Check</td><td>");
		out.println("" + reportSeries++ + " </td></tr>");
		out.println("<tr class=\"heading\"><td>Earning/Tax Deduction</td><td>Amount</td>");
		out.println("</tr><tr class=\"item\"><td>Gross Pay</td><td>");
		// calculate the salary
		Double monthSal = hourly_pay * 160;
		Double taxFed = monthSal * 0.1;
		Double taxSta = monthSal * 0.05;
		Double benefit = monthSal * 0.03;
		if (classification.equalsIgnoreCase("hourly"))
			benefit = 0.00;
		Double netPay = monthSal - taxFed - taxSta - benefit;

		out.println("$" + String.format("%.2f", monthSal) + "</td></tr>");
		out.println("<tr class=\"item\"><td>Federal Tax (10%)</td><td>");
		out.println("$" + String.format("%.2f", taxFed) + "</td></tr>");
		out.println("<tr class=\"item\"><td>State Tax (5%)</td><td>");
		out.println("$" + String.format("%.2f", taxSta) + "</td></tr>");
		out.println("<tr class=\"item last\"><td>Benefit Plan (3%)</td><td>");
		if (classification.equalsIgnoreCase("hourly")) {
			out.println("No benefit Plan for Hourly");
		} else {
			out.println("$" + String.format("%.2f", benefit) + "</td></tr>");
		}
		out.println("<tr class=\"heading\"><td>Net Pay</td><td>");
		out.println("$" + String.format("%.2f", netPay) + "</td></tr>");

		out.println("<tr class=\"heading\"><td>");
		out.println("<tab3><tab3><button onclick=\"load()\">Return to Main Page </button>");
		out.println("<script>    function load() {");
		out.println("location.assign(\"http://localhost:8080/cs631db/MainPayroll.html\");");
		out.println("} </script>	</td></tr></table></div></body></html>");
		
	}

	public void costDeptDiv(PrintWriter out, String code, String type, String month, String year, ResultSet rsPerm, ResultSet rsHr) throws SQLException {

		String deptdivName = null;
		String headName = null;
		String phone = null; 
		String email = null; 
		Integer permEmp = null; 
		Integer hrEmp = null; 
		Double permSal = 0.0; 
		Double hrSal=0.0;
		
		if (rsPerm.next() && rsHr.next()) {
			
		deptdivName= rsPerm.getString(2);
		headName=rsPerm.getString(3);
		phone=rsPerm.getString(4); 
		email=rsPerm.getString(5); 
		permEmp=rsPerm.getInt(6); 
		hrEmp =rsHr.getInt(6);
		permSal=rsPerm.getDouble(7); 
		hrSal=rsHr.getDouble(7);
		
		} else System.out.println(errorMsgMiss);
		
		out.println("<html><head><meta charset=\"utf-8\" />");
		out.println("<title>The Best-P Company Payroll Division/Department Cost Report</title>");
		out.println("<link rel=\"stylesheet\" href=\"report.css\">");
		out.println("<script type=\"text/javascript\"</script><script src=\"https://code.jquery.com/jquery-1.12.4.min.js\"></script>");
		out.println("</head><body>	<div class=\"invoice-box\">");
		out.println("<table cellpadding=\"0\" cellspacing=\"0\">	<tr class=\"top\"><td colspan=\"2\">");
		out.println("<table>	<tr><td class=\"title\"><img src=\"adminLogo.jpg\" class=\"image1\" /></td>");
		out.println("<td>Cost Report #: "+ reportSeries++ +"<br />");
		out.println("Created: <a href=\"#\"><script>document.write(new Date().toLocaleDateString()); </script></a></p><br />");
		out.println("Time Period: " + MonthNumber.matchName(month) + "  " + year);
		out.println("</td></tr></table></td></tr><tr class=\"information\"><td colspan=\"2\"><table><tr><td>");
		out.println("The Best-P Company<br/>7545 MLK J. Blvd<br/>Newark, NJ 07102<br/>USA Headquarter</td><td>");
		out.println(type+" ID: "+code+"<br/>");
		out.println("Name: "+ deptdivName+"<br/>");
		out.println("Head: "+headName+"<br/>");
		out.println("Head's Email: "+email+"</td></tr>");
		out.println("<tr class=\"heading\"><td>Type of Employees</td><td> No. of Employees </td>");
		out.println("</tr><tr class=\"details\"><td>Permanent / Hourly</td>");
		out.println("<td>"+permEmp+" / "+hrEmp+"</td></tr>");
		
		//calculate operating cost for each department or division
		Integer totalSal=(int) (permSal+hrSal) * 160;
		Integer totalSal1=(int) (permSal * 160);
		Integer totalSal2=(int) (hrSal * 160);
		Integer taxFed = (int) (totalSal * 0.1);
		Integer taxSta = (int) (totalSal * 0.05);
		Integer benefit = (int) (permSal * 160 * 0.03);
		Integer netCost=totalSal-taxFed-taxSta-benefit;
		
		out.println("<tr class=\"heading\"><td>Cost Item</td><td>Amount</td></tr><tr class=\"item\">");
		out.println("<td>Gross Pay</td><td>"+totalSal+"<br/>("+totalSal1+" / "+totalSal2+")</td>");
		out.println("</tr><tr class=\"item\"><td>Federal Tax (10%)</td>");
		out.println("<td>" + taxFed +"</td></tr>");
		out.println("<tr class=\"item\"><td>State Tax (5%)</td>");
		out.println("<td>"+ taxSta +"</td></tr>");
		out.println("<tr class=\"item last\"><td>Benefit Plan (3%)</td>");
		out.println("<td>"+ benefit +"</td></tr>");
		out.println("<tr class=\"heading\"><td>Total Net Cost (Salary Only)</td>");
		out.println("<td>"+netCost+"</td></tr></table>");
		out.println("<tab3><tab3><button onclick=\"load()\">Return to Main Page </button>");
		out.println("<script>    function load() {");
		out.println("location.assign(\"http://localhost:8080/cs631db/MainPayroll.html\");");
		out.println("} </script>	</td></tr></table></div></body></html>");
		
	}
	
	public void empIRSreport(PrintWriter out, String yearIRS, ResultSet rs, int type) throws SQLException {
	    
		int empIRSIncome = 0; 
		if(rs.next()) {
			
		empID=rs.getString(1); 
		name=rs.getString(2);
		address=rs.getString(5);
		city=rs.getString(6);
		state = rs.getString(7);
		zip = rs.getString(8);
		SSN = rs.getString(9);
		start=rs.getDate(17);
		end=rs.getDate(18);
		salary = rs.getInt(19);
		classification = rs.getString(20);
		hourly_pay = rs.getDouble(21);
		} else System.out.println(errorMsgMiss);
	//compare the days for partial year
		try {
			SimpleDateFormat datef = new SimpleDateFormat("yyyy-MM-dd");
	       java.util.Date javaStartDate = datef.parse(yearIRS+"-01-01");
	       java.util.Date javaEndDate = datef.parse(yearIRS+"-12-31");	       
	       java.util.Date sqlStartDate = new java.util.Date(start.getTime());
	       java.util.Date sqlEndDate = new java.util.Date(end.getTime());

	       long effectiveDays;
		switch (type) { 
		case 0:
			//type=0: whole year
			empIRSIncome=salary;
			break;
		case 1: 
			//type=1: late start
			effectiveDays=((javaEndDate.getTime()-sqlStartDate.getTime())/ (1000 * 60 * 60 * 24));
			 //convert to income
			 empIRSIncome=(int) (hourly_pay* effectiveDays/7*40);
			break;
		case 2: 
			//type=2: early leave
			effectiveDays=((sqlEndDate.getTime()-javaStartDate.getTime())/ (1000 * 60 * 60 * 24));
			 //convert to income
			 empIRSIncome=(int) (hourly_pay* effectiveDays/7*40);
	
			break;
		}
		} catch (ParseException ex) {
			System.out.println(errorMsg5 + ex.getMessage());
		} catch (Exception e) {
			System.out.println(errorMsg5 + e);
		}
		// Federal=15%, State = 5%, 3% benefit, plus 6.2% for Social Security Tax and 1.45% for Medical
		Integer taxFed = (int) (empIRSIncome * 0.1);
		Integer taxSta = (int) (empIRSIncome * 0.05);
//		Integer benefit = (int) (empIRSIncome * 0.03);
		Integer taxSS = (int) (empIRSIncome * 0.062);
		Integer medWage=empIRSIncome-taxFed-taxSS;
		Integer taxMedi = (int) (empIRSIncome * 0.0145);
		Integer netWage=empIRSIncome-taxFed-taxSS-taxMedi;
		
		out.println("<html><head><meta charset=\"utf-8\" />");
		out.println("<title>The Best-P Company IRS Report</title>");
		out.println("<link rel=\"stylesheet\" href=\"report.css\">");
		out.println("<script type=\"text/javascript\"</script><script src=\"https://code.jquery.com/jquery-1.12.4.min.js\"></script>");
		out.println("</head><body>	<div class=\"invoice-box\">");
		out.println("<table border=\"1\" cellpadding=\"0\" cellspacing=\"0\"><tr>");
		out.println("<td><font color=RED> Copy B-To Be Filed with Employee's<br>");
		out.println("FEDERAL TAX Return</font></td>");
		out.println("<td>OMB #: 1234-5678<br> Created: <a href=\"#\">");
		out.println("<script>document.write(new Date().toLocaleDateString());</script></a>");
		out.println("</p><h1>"+yearIRS+"</h1></td></tr>");
		out.println("<tr class=\"heading\">");
		out.println("<td>a. Employee's Soc. Sec. Number<br> <tab1>");
		out.println( ""+SSN+"</td>");
		out.println("<td>b. Employer's ID Number<br> <tab1> 36-01234"+empID+"</td></tr>");
		out.println("<tr class=\"details\">");
		out.println("<td>1. Wages, tips, other comp.<br> <tab1>$"+netWage+"</td>");
		out.println("<td>2. Federal income tax. withheld<br> <tab1>$"+ taxFed+"</td>");
		out.println("</tr>");
		out.println("<tr class=\"details\">");
		out.println("<td>3. Social security wages<br> <tab1>$"+empIRSIncome+"</td>");
		out.println("<td>4. Social security tax. withheld<br> <tab1>");
		out.println("$"+taxSS+"</td>");
		out.println("</tr>");
		out.println("<tr class=\"details\">");
		out.println("<td>5. Medical wages and tips<br> <tab1>$"+ medWage +"</td>");
		out.println("<td>6. Medical tax. withheld<br> <tab1>$"+taxMedi+"</td>");
		out.println("</tr>");
		out.println("</table>");
		out.println("<table border=\"1\" cellpadding=\"0\" cellspacing=\"0\">");
		out.println("<tr class=\"information\">");
		out.println("<td colspan=\"0\">c. Employer's name, address, and ZIP code<br />");
		out.println("<tab2> The Best-P Company<br />");
		out.println("<tab2> 7545 MLK J. Blvd<br />");
		out.println("<tab2> Newark, NJ 07102 </td>");
		out.println("</tr>");
		out.println("<tr class=\"information\">");
		out.println("<td colspan=\"0\">d. Employee's name, address, and ZIP code<br />");
		out.println("<tab2>"+name+"<br />");
		out.println("<tab2>"+address+"<br />");
		out.println("<tab2>"+city+","+state+","+zip+"</td>");
		out.println("</tr>");
		out.println("</table>");
		out.println("<table border=\"1\" cellpadding=\"0\" cellspacing=\"0\">");
		out.println("<tr class=\"information\">");
		out.println("<td colspan=\"4\">"+state+"<br> State");
		out.println("<td>360-982-12345 <br> Employer's state ID");
		out.println("</td>");
		out.println("<td>$ "+netWage +"<br> State wages, tips, etc.");
		out.println("</td>");
		out.println("<td>$ "+taxSS+"<br> State income tax</td>");
		out.println("</tr></table>");
		out.println("<tab3><tab3><button onclick=\"load()\">Return to Main Page </button>");
		out.println("<script>    function load() {");
		out.println("location.assign(\"http://localhost:8080/cs631db/MainPayroll.html\");");
		out.println("} </script></div></body></html>");
		
	}
	
	public void printIRSreport (PrintWriter out, ResultSet rs) throws SQLException  {
		
		// query has output not null  >
		
		out.println("<html><head><link rel=\"stylesheet\" href=\"./CS631.css\">");
		out.println("<script type=\"text/javascript\"</script><script src=\"https://code.jquery.com/jquery-1.12.4.min.js\"></script>");
		out.println("<script src='js/autoresize.jquery.min.js'></script></head><body>");
		out.println("<h4>"+"YuQing Ding, Wei Zhang, CS631 Term Project   Date: "+LocalDate.now());
		out.println("</h4><h1><tab3><tab3> The Best-P Company IRS Records<tab2></h1><br></head><body>");
		//start of a table
		out.println("<table border = \"1\" bordercolor = \"black\" bgcolor = \"lightgray\"> <tr><th>Year</th><th>Emp ID</th><th>Emp Name</th>");
		out.println("<th>address</th><th>city</th><th>state</th><th>zip</th><th>SSN</th><th>Social.S.Wage</th><th>Federal Tax</th>");
		out.println("<th>State Tax</th><th>Social.S.Tax</th><th>Medical Tax</th><th>State_Wage</th></tr>");

		while (rs.next()) {
			
			empID = rs.getString(1);
			name = rs.getString(2);	
			address = rs.getString(3);					
			city = rs.getString(4);
			state = rs.getString(5);	
			zip = rs.getString(6);							
			SSN = rs.getString(7);
			office_code = rs.getString(8);	
			dept_code = rs.getString(9);					
			div_code = rs.getString(10);
			classification = rs.getString(11);
			Integer span = rs.getInt(12);  
			Integer IRSyear = rs.getInt(13);
			//post all sorts of taxes and salaries 
			Integer SSincome = (int) rs.getDouble(14);
			Integer FederalTax = (int)rs.getDouble(15);						
			Integer StateTax = (int)rs.getDouble(16);						
			Integer SSTax = (int)rs.getDouble(17);
			Integer MedicalTax = (int)rs.getDouble(18);
			Integer StateWage = (int) rs.getDouble(19);
			
			out.println("<tr><th>"+IRSyear+"</th><th>"+empID+"</th><th>"+name+"</th>");
			out.println("<th>"+ address +"</th><th>"+city+"</th><th>"+state+"</th><th>"+zip+"</th><th>"+SSN+"</th><th>"+SSincome+"</th>");
			out.println("<th>"+FederalTax+"</th><th>"+StateTax+"</th><th>"+SSTax+"</th><th>"+MedicalTax+"</th><th>"+StateWage+"</th></tr>");
		}
/*		out.println("");
		out.println("");
		out.println("");
		out.println("");
		out.println("");
		out.println("");
		out.println("");
		out.println("");
		out.println("");
		out.println("");*/
		
	}
}
