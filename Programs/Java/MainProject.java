package PayrollMgt;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;  

@WebServlet("/MainProject")
public class MainProject extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {
	static final long serialVersionUID = 1L;

	//this set of employee basic information is used by all applications, thus enlisted here for efficiency	
	String empID, name, address, city, state, zip, SSN, email, office_code, title, dept_code, div_code, projID,
	office_name, phone, classification, proj_name, proj_manager, location, start1, end1, status, mgr_name;
	Date start_date, start, end, proj_startdate, proj_enddate;
	Integer budget, salary;
	BigInteger totalEmp;
	Double hourly_pay,totalCharge,totalworkhour, prog_milestone;
	
	// @ initial messages
	String errorMsgCon="SQL connection failed! ";
	String errorMsgData = "Wrong Data Entry or No Data. Please try again!";
	String errorMsgMiss = "Missing input from HTML forms. Please check again!";
	String errorMsgTake = "Already in database. Please try again!";
	String errorMsg1 = "Project exists! Please try again!";
	String goodMsg1 = "Your request is accepted! New project is available now!";
	String errorMsg2 = "Not available, manager occupied already!!";
	String goodMsg2 = "Your request is accepted! Database assigned manager to project!";
	String errorMsg3 = "Not available, employee occupied already!";
	String goodMsg3 = "Your request is accepted! Database assigned employee to project!";
	String errorMsg4 = "Please enter correct project info!";
	String goodMsg4 = "Your request is accepted! Get project statistics!";	
	String errorMsg5 = "too many employees assigned! Budget overflow!";	
	// @ initial database for web page
	String sqlUpdateProj="SELECT * FROM (SELECT projID, proj_name FROM project WHERE projID>0) as p ORDER BY p.projID ASC";
	String sqlUpdateMgr="SELECT * FROM (SELECT ea.empID, ep.name FROM employeeassign ea, employeepayroll ep "
			+ "WHERE ea.empID=ep.empID AND ea.projID is null AND ea.title = 'project manager') "
			+ "as e ORDER BY e.name ASC";
	String sqlUpdateEmp="SELECT * FROM (SELECT ea.empID, ep.name FROM employeeassign ea, employeepayroll ep "
			+ "WHERE ea.empID=ep.empID AND ea.projID is null AND ea.title != 'project manager') "
			+ "as e ORDER BY e.name ASC";
	

	
	public MainProject() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			// @ JDBC connection to SQL - create database and table
			ConnectionReset con=new ConnectionReset();
			prepRefreshPage(con, response);
			
		} catch (Exception ex) {
			System.out.println(errorMsgCon + " sql error is "+ex);
			// @servlet response
			response.sendRedirect("MainProject.html");
		}
	
	}
	
	private void prepRefreshPage(ConnectionReset con, HttpServletResponse response) throws IOException {
		// @servlet writer to get newly updated lists and page refreshed
		PrintWriter out = response.getWriter();
		
		try {
		PreparedStatement pstmtProj = con.connection.prepareStatement(sqlUpdateProj);
		PreparedStatement pstmtMgr = con.connection.prepareStatement(sqlUpdateMgr);
		PreparedStatement pstmtEmp = con.connection.prepareStatement(sqlUpdateEmp);
		
		ResultSet updateEmp=pstmtEmp.executeQuery();					
		ResultSet updateMgr=pstmtMgr.executeQuery();
		ResultSet updateProj=pstmtProj.executeQuery();
		//refresh the main page
		refreshPage(out, updateEmp, updateMgr, updateProj);
		
		} catch (Exception ex) {
			System.out.println(errorMsgCon + " sql error is "+ex);
			// @servlet response
			response.sendRedirect("MainProject.html");
		}
		
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/html");
		// @servlet writer to get results posted
		PrintWriter out = response.getWriter();
		// sql query result set
		ResultSet rs;
		// initial html checkbox  - myCheckBox is true if checked, false if not
		boolean checkNewProj = request.getParameter("checkNewProj") != null;
		boolean checkAssignProj = request.getParameter("checkAssignProj") != null;
		String checkProjSta;

		// @ create queries prepared statement.
			
		String sqlInsertProj = "INSERT INTO project VALUES (?, ?, null, ?, ?, null, null)";
		String sqlAssignMgr = "UPDATE project SET proj_manager = ?, start =?, end = ? WHERE projID =?";
		String sqlAssignEmp = "UPDATE employeeassign SET projID = ?, proj_startdate=?, proj_enddate=? WHERE empID = ?";
		String sqlProjStatus = "SELECT * FROM projectProgress WHERE projID=?";
		String sqlAllProj = "SELECT * FROM projectProgress";
		
	
		try {
			// @ JDBC connection to SQL - create database and table
			ConnectionReset con=new ConnectionReset();
			//	check out the four major functions, insert & assign resources to new project, quote all & each project status	
			if (request.getParameter("restart")!=null) {
				//initialize database 
				//initialize SQL employee view and project view for entire company, remove wrong trials
				con.resetDB();
				con.iniProject();
				response.sendRedirect("MainProject.html");
			}
			// Submit button to insert a new proj is true if checked, false if not checked
			if (request.getParameter("projNew") != null) {
				// myCheckBox is true if checked, false if not checked
				if (checkNewProj == true) {
					//recalculate empview2 per most updated entity tables
					con.iniProject();
					PreparedStatement pstmt = con.connection.prepareStatement(sqlInsertProj);
					//sqlInsertProj = "INSERT INTO project VALUES (?, ?, null, ?, ?, null, null)";
					
					try {
						projID = request.getParameter("projNewID");
						proj_name = request.getParameter("projNewName");
						location = request.getParameter("projNewLoc");
						budget = Integer.parseInt(request.getParameter("projNewbgt"));

						// run sql query to insert new project ID  
						pstmt.setString(1, projID);
						pstmt.setString(2, proj_name);
						pstmt.setString(3, location);
						pstmt.setInt(4, budget);

						Integer i= pstmt.executeUpdate();
						//check if the project is inserted to database
						if (i==1) new AlertReminder1(goodMsg1).popAlert1(response);
						else if (i==0) {
							new AlertReminder1(errorMsg1).popAlert1(response); 
							throw new Exception ("errorMsg1");
						}
						
					} catch (NumberFormatException e) {
						System.out.println(errorMsgMiss);
						System.out.println(e.getMessage());
					} catch (Exception e) {
						System.out.println(errorMsgTake);
						System.out.println(e.getMessage());
					}
				pstmt.close();
				} else {
					System.out.println(errorMsgMiss);
				}
				//refresh the main page
				prepRefreshPage(con, response);
				
			} else if (request.getParameter("projAssign")!=null) { 
					// Submit button to assign resources to proj is true if checked, false if not checked
					if (checkAssignProj==true) {
						//recalculate empview2 per most updated entity tables
						con.iniProject();
								
					PreparedStatement pstmt1 = con.connection.prepareStatement(sqlAssignMgr);
					//sqlAssignMgr = "UPDATE project SET proj_manager = ?, start =?, end = ? WHERE projID =?";
					
					String projIDr = request.getParameter("projNewIDr");
					proj_manager = request.getParameter("availMgr");
					start1 = request.getParameter("projStart")+"-01-01";
					end1 = request.getParameter("projEnd")+"-12-31";
					
					start = new StringToDate(start1).getSqlDate();
					end = new StringToDate(end1).getSqlDate();
					
					pstmt1.setString(1, proj_manager);
					pstmt1.setDate(2, start);
					pstmt1.setDate(3, end);
					pstmt1.setString(4, projIDr);
					//insert manager
					Integer j=pstmt1.executeUpdate();
					//check if the project manager is inserted to database
					if (j==1) System.out.println(goodMsg2);
					else if (j==0) {
						new AlertReminder1(errorMsg2).popAlert1(response);
					}
					
					//insert employees
					PreparedStatement pstmt2 = con.connection.prepareStatement(sqlAssignEmp);	
					//sqlAssignEmp = "UPDATE employeeassign SET projID = ?, proj_startdate=?, proj_enddate=? WHERE empID = ?";
					//insert employee as a manager is inserted to database
					pstmt2.setString(1, projIDr);
					pstmt2.setDate(2, start);
					pstmt2.setDate(3, end);
					pstmt2.setString(4, proj_manager);
					Integer i=pstmt2.executeUpdate();
					//check if the employee as a manager is inserted to database
					if (i==1) System.out.println(goodMsg3);
					else if (i==0) {
						System.out.println(errorMsg3);
					}
	
					
					String[] availEmp=request.getParameterValues("availEmp");
				//insert all empIDs newly assigned to identified project
					if (availEmp!=null) {
						if (availEmp.length>3) new AlertReminder1(errorMsg5).popAlert1(response);
						else {
							for (String emp:availEmp) {
								pstmt2.setString(1, projIDr);
								pstmt2.setDate(2, start);
								pstmt2.setDate(3, end);
								pstmt2.setString(4, emp);
								Integer k=pstmt2.executeUpdate();
								//check if the employee as employee is inserted to database
								if (k==1) System.out.println(goodMsg3);
								else if (k==0) {
									new AlertReminder1(errorMsg3).popAlert1(response); 
								}
								
							}
							
						}
					}
					
					//refresh the main page
					prepRefreshPage(con, response);
					pstmt1.close();
					pstmt2.close();
					
					}else  {
						System.out.println(errorMsgMiss);
					}
					
				} else if (request.getParameter("allProj")!=null) { 
					// Submit button to get status of all proj is true if checked, false if not checked
					//recalculate status of all projects per most updated entity tables
					con.staProject();
					if(con.stmt.execute(sqlAllProj)) {
						// sql has results
							rs=con.stmt.executeQuery(sqlAllProj);
							printAllProj(out, rs);
					} else System.out.println(errorMsgMiss);
					 
					//refresh the main page
					//prepRefreshPage(con, response);
					
				} else if (request.getParameter("projSta")!=null) { 
					// Submit button to get status of one proj is true if checked, false if not checked
						//recalculate status of all projects per most updated entity tables
						con.staProject();
						//sqlProjStatus = "SELECT * FROM projectProgress WHERE projID=?";
						PreparedStatement pstmt3=con.connection.prepareStatement(sqlProjStatus);
				        if (request.getParameter("checkProjSta")!=null) {
				        	if(request.getParameter("checkProjSta").equals("1")) {
							projID=request.getParameter("projID");
							} else if (request.getParameter("checkProjSta").equals("2")) {
							projID=request.getParameter("projName");
							}  
				        	 
				        	pstmt3.setString(1, projID);
				        	rs=pstmt3.executeQuery();
				        	printAllProj(out, rs);
							pstmt3.close();		
				        } else System.out.println(errorMsg4);
						//refresh the main page
						//prepRefreshPage(con, response);
				
				}
				}catch (Exception ex) {
					System.out.println(errorMsgCon + " sql error is "+ex);

					// @servlet response
					response.sendRedirect("MainProject.html");
				}
		out.close();
	}

	public void printAllProj (PrintWriter out, ResultSet rs)  {
		
		// query has output not null
		
		out.println("<html><head><link rel=\"stylesheet\" href=\"./CS631.css\">");
		out.println("<script type=\"text/javascript\"</script><script src=\"https://code.jquery.com/jquery-1.12.4.min.js\"></script>");
		out.println("<script src='js/autoresize.jquery.min.js'></script></head><body>");
		out.println("<h4>"+"YuQing Ding, Wei Zhang, CS631 Term Project   Date: "+LocalDate.now());
		out.println("</h4><h1><tab3><tab3> The Best-P Company Project Progress Reports <tab2></h1><br></head><body>");
		//start of a table
		out.println("<table> <tr><th>Project ID</th><th>Project Name</th><th>Location</th><th>Budget</th>");
		out.println("<th>Start Date</th><th>End Date</th><th>Project Manager</th><th>Total # Employees</th>");
		out.println("<th>Total Charge</th><th>Total Work-Hours</th><th>Milestone/Progress/</th></tr>");

		
		try {
			while (rs.next()) {
				projID = rs.getString(1);
				proj_name = rs.getString(2);
//			proj_manager = rs.getString(3);
				location= rs.getString(4);
				budget=rs.getInt(5);
				proj_startdate = rs.getDate(6);
				proj_enddate = rs.getDate(7);
//			span = rs.getString(8);
				status = rs.getString(9);
				mgr_name = rs.getString(10);
				totalEmp = BigInteger.valueOf(rs.getLong(11));
				totalCharge = rs.getDouble(12);
				totalworkhour = rs.getDouble(13);
				prog_milestone = rs.getDouble(14);
				int mstone=(int)Math.round(prog_milestone*100.00);
				
				out.println("<tr><td>"+projID+"</td>");
				out.println("<td>"+proj_name+"</td>");	
				out.println("<td>"+location+"</td>");
				out.println("<td>"+budget+"</td>");	
				
				out.println("<td>"+proj_startdate+"</td>");
				out.println("<td>"+proj_enddate+"</td>");	
				out.println("<td>"+mgr_name+"</td>");
				out.println("<td>"+totalEmp+"</td>");	
				
				out.println("<td>"+String.format("%.2f", totalCharge)+"</td>");
				out.println("<td>"+String.format("%.2f", totalworkhour)+"</td>");	
				out.println("<td>"+status+"<progress id=\"progressBar\" max=\"100\" value=\""+mstone+"\" > </progress>");
				out.println("</td></tr>");
		
			}
		out.println("</table><tab3><tab3><button onclick=\"load()\">Return to Main Page </button>");
		out.println("<script>    function load() {");
		out.println("location.assign(\"http://localhost:8080/cs631db/MainProject.html\");");
		out.println("} </script>	</div></body></html>");
		} catch (SQLException e) {
			System.out.println(errorMsgData);
			e.printStackTrace();
		}
	}

public void refreshPage(PrintWriter out, ResultSet newEmp, ResultSet newMgr, ResultSet newProj) throws SQLException {
	
	//title
	out.println("<html><head><link rel=\"stylesheet\" href=\"./CS631.css\">");
	out.println("<script type=\"text/javascript\"</script><script src=\"https://code.jquery.com/jquery-1.12.4.min.js\"></script> ");
	out.println("<script src='js/autoresize.jquery.min.js'></script></head><body><img src=\"adminLogo.jpg\" class=\"image1\"> ");
	
	out.println("<h1><br> <form method=\"post\" action=\"MainProject\"><tab2>The Best-P Company Project Management (UPDATED)");
	out.println("<tab2><img src=\"warning.jpg\" style=\"width: 25px; height: 20px;\">");
	out.println("<input type=\"submit\" value=\"Restart Database\" name=\"restart\" class=\"button2\"></tab1></h1> </form>");
//	out.println("");
	out.println("<h2><tab2><tab3> <a href=\"https://www.njit.edu\">https://www.njit.edu</a>");
	out.println("<tab1>Edge in Technology </h2>");
	out.println("<div id=\"navbar\"><p>");
	out.println("<a href=\"www.njit.edu\">Home</a> <a href=\"www.google.com\">About</a> ");
	out.println("<a href=\"#contact\">Contact</a> <a href=\"www.google.com\">Search</a> ");
	out.println("<a href=\"#\"><script>document.write(new Date().toLocaleDateString());</script></a></p></div>");
	out.println("<form id=\"vform\" method=\"post\" action=\"MainProject\"><! insert a new project query/>");
	// insert a new project
	out.println("<h3>&#160; <div class=\"container\">");
	out.println(" <img src=\"insert.jpg\" style=\"width: 35px; height: 30px;\" class=\"image\">");	
	out.println(" <div class=\"overlay\"> <tab1>Please insert a new project </div></div></h3> ");

	out.println("<h4><tab1><input type=\"checkbox\" name=\"checkNewProj\"> New Project ID:");
	out.println("&#160; <input type=\"text\" maxlength=\"5\" size=\"5\" name=\"projNewID\"placeholder=\"112\"> ");
	//newProj
	out.println("&#160; New Project Name: &#160;<select name=\"projNewName\">");
	out.println("<option value=\"Disney Spring Resort Development\" selected>Disney Winter Resort Development</option> ");	
	out.println("<option value=\"Cape May Summer Resort Development\">Cape May Summer Resort Development</option> ");	
	out.println("<option value=\"LA Autumn Resort Development\">LA Autumn Resort Development</option> ");	
	out.println("<option value=\"DC Winter Musium Development\">DC Autumn Resort Development</option></select> ");
	
	out.println("&#160; New Project Location: &#160;<select name=\"projNewLoc\">");
	out.println("<option value=\"FL ORLANDO\" selected>FL ORLANDO</option>");
	out.println("<option value=\"NEW YORK CITY\">NEW YORK CITY</option>");
	out.println("<option value=\"CA LOS ANGELES\">CA LOS ANGELES</option>");
	out.println("<option value=\"DC WASHINGTON\">DC WASHINGTON</option>");
	out.println("<option value=\"NJ KRAYS LANDING\">NJ KRAYS LANDING</option>");
	out.println("<option value=\"IN EAST CHICAGO\">IN EAST CHICAGO</option></select>");
	
	out.println("&#160; New Project Budget: &#160;<input type=\"text\" maxlength=\"15\" size=\"15\" name=\"projNewbgt\" placeholder=\"1000000\">");
	out.println("<tab1> <br><br><tab3> <input type=\"submit\" value=\"Insert New Project\" name=\"projNew\" class=\"button\">");
	// assign resources to a new project
	out.println("<br> <hr> <! assign new resources query/>");
	// static
	out.println("<h3>&#160; <div class=\"container\">");
	out.println("<img src=\"aboutinfo.jpg\" style=\"width: 35px; height: 30px;\" class=\"image\">");	
	out.println("<div class=\"overlay\"> <tab1>Please assign resources to the new project </div></div></h3> ");

	out.println("<h4> <tab1> <input type=\"checkbox\" name=\"checkAssignProj\"> New Project ID:");
	out.println("&#160; <input type=\"text\" maxlength=\"5\" size=\"5\" name=\"projNewIDr\" placeholder=\"112\">");
	out.println("&#160; Choose Employees (Multiple)");
	out.println("<select name=\"availEmp\" multiple=\"multiple\">");
	//refresh and update the available employees ResultSet newEmp 
	while (newEmp.next()) {
		out.println("<option value=\""+newEmp.getString(1)+"\">"+newEmp.getString(2)+"</option>");
	}
/*	out.println("<option value=\"7\">Bolton, David</option>");
	out.println("<option value=\"9\">Bomboy, James</option>");
	out.println("<option value=\"19\">Bonhom, Terrell</option>");
	out.println("<option value=\"27\">Booker, Petrine</option>");
	out.println("<option value=\"40\">Ghee, Jamie</option>");
	out.println("<option value=\"63\">Harrison, Valencia</option>");
	out.println("<option value=\"64\">Harrison, Villie</option>");
	out.println("<option value=\"68\">Harski, Richard</option>");
	out.println("<option value=\"70\">Hartiens, Benjamin</option>");
	out.println("<option value=\"76\">Lin, Qinglu</option>");
	out.println("<option value=\"96\">Lincoln, Jonh</option>");
	out.println("<option value=\"105\">Ling, Alice</option>");
	out.println("<option value=\"109\">Lionberger, Brian</option>  ");*/
	
	//refresh and update the available managers ResultSet newMgr
	out.println("</select>&#160;&#160; Choose Managers: <select name=\"availMgr\">");
	while (newMgr.next()) {
		out.println("<option value=\""+newMgr.getString(1)+"\">"+newMgr.getString(2)+"</option>");
	}	
	
	/*out.println("<option value=\"105\">Ling, Alice</option>");
	out.println("<option value=\"116\">Joseph Paul</option>");
	out.println("<option value=\"117\">Maria Swabota</option>");
	out.println("<option value=\"118\">Michael Anthony</option>");
	out.println("<option value=\"119\">Pitbull Perez</option>");
	out.println("<option value=\"120\">Richard Steven</option>");
	out.println("<option value=\"121\">Robert Daniel</option>");
	out.println("<option value=\"122\">Thomas Andrew</option>");
	out.println("<option value=\"123\">William Mark</option>");*/
	//static
	out.println(" </select>&#160; New Project Start Time: &#160;<select name=\"projStart\">");
	out.println("<option value=\"2023\" selected>2023</option>");
	out.println("<option value=\"2024\">2024</option>");
	out.println("<option value=\"2025\">2025</option> </select>");
	
	out.println("&#160;New Project End Time: &#160;<select name=\"projEnd\">");
	out.println("<option value=\"2023\" selected>2023</option>");
	out.println("<option value=\"2024\">2024</option>");
	out.println("<option value=\"2025\">2025</option> </select> <tab1> <br> <br>");
	out.println("<tab3> <input type=\"submit\" value=\"Assign to New Project\"  name=\"projAssign\" class=\"button\">");
	//download all project statistics
	out.println("<br>  <hr> <! start project query/> <h3> &#160; <img src=\"projstatus.jpg\" style=\"width: 27px; height: 28px;\">Please choose Project Report:");
//	out.println("<tab2> (or Click and Download All)");
	out.println("<input type=\"submit\" name=\"allProj\"  value=\"Download All Project Reports include History Projects\"");
	
	//choose one project to quote status
	out.println("class=\"button2\"> </h3> <hr> <! start each project query/> <h5>");
	out.println("<tab1>  <input type=\"radio\" name=\"checkProjSta\" value=\"1\" checked> Enter Project ID:");
	out.println("<tab1> <input type=\"text\" maxlength=\"5\" size=\"5\" name=\"projID\"> <tab1> or <br><br>");
	out.println("<tab1> <input type=\"radio\" name=\"checkProjSta\" value=\"2\">  Choose Project Name: <select name=\"projName\">");
	
	//refresh and update the available project names ResultSet newProj
	while (newProj.next()) {
		out.println("<option value=\""+newProj.getString(1)+"\">"+newProj.getString(2)+"</option>");
	}
	
/*	out.println("<option value=\"101\">Dagwood Development</option>");
	out.println("<option value=\"102\">Cyclone Development</option>");
	out.println("<option value=\"103\">Yellow Moose Development</option>");
	out.println("<option value=\"104\">Boomrenge Development</option>");
	out.println("<option value=\"105\">Bongo Development</option>");
	out.println("<option value=\"106\">Hashtag Development</option>");
	out.println("<option value=\"107\">Silverstar Development</option>");
	out.println("<option value=\"108\">Hidden Hook Development</option>");
	out.println("<option value=\"109\">Early First Development</option>");
	out.println("<option value=\"110\">Bull Winky Development</option>");
	out.println("<option value=\"111\">Lonely Fox Development</option>");
	out.println("<option value=\"112\">Disney Winter Development</option>");*/
	
	out.println("</select> <tab1> <input type=\"submit\" value=\" Track Individual Project Status \" name=\"projSta\" class=\"button2\"><hr>");
	
//	out.println("&#160;<img src=\"warning.jpg\" style=\"width: 25px; height: 20px;\">");
//	out.println("<input type=\"submit\" value=\"Restart Database\" name=\"restart\" class=\"button2\"></tab1>");
	out.println("</form></body></html>");
//	out.println("<br> <tab1> Project Progress Status is: ");

}

}
