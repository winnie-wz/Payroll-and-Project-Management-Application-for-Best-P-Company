/**
 * 
 */
package PayrollMgt;

/**
 * @author tinca
 *
 */
import java.sql.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;

public class ConnectionReset {

	static final String SQL_DRIVER = "com.mysql.cj.jdbc.Driver";
	static final String DB_URL = "jdbc:mysql://localhost/cs631db";
	static final String USER = "root";
	static final String PASS = "root";

	public Connection connection;
	public Statement stmt;
	public PreparedStatement pstmt;
	public ResultSet rs;

	String resetDB1 = "UPDATE employeeassign SET projID=null WHERE projID>='112'";
	String resetDB2 = "DELETE FROM project WHERE projID>='112'";

	String sqlEmpViewPay1 = "DROP view if exists empview";
	String sqlEmpViewPay2 = "CREATE VIEW empview AS " + 
			"SELECT e.empID, e.name, ea.title, e.start_date, e.address, e.city, e.state, " + 
			"e.zip, e.SSN, e.email, e.office_code, office.phone, office.office_name, ea.dept_code, ea.div_code, ea.projID, " + 
			"ea.proj_startdate as start, ea.proj_enddate as end, salary.salary, salary.classification, salary.hourly_pay, " + 
			" project.proj_name " + 
			"FROM employeepayroll as e " + 
			"JOIN employeeassign as ea ON e.empID=ea.empID " + 
			"JOIN office ON e.office_code =office.office_code " + 
			"JOIN project ON ea.projID =project.projID " + 
			"JOIN salary ON ea.title=salary.title";

	String sqlEmpViewProj1 = "DROP view if exists empview2";
	String sqlEmpViewProj2 = "CREATE VIEW empview2 AS " + 
			"SELECT e.empID, e.name, ea.title, e.start_date, e.address, e.city, e.state, " + 
			"e.zip, e.SSN, e.email, e.office_code, office.phone, office.office_name, ea.dept_code, ea.div_code, ea.projID, " + 
			"ea.proj_startdate as start, ea.proj_enddate as end, salary.salary, salary.classification, salary.hourly_pay, " + 
			" project.proj_name " + 
			"FROM employeepayroll as e " + 
			"JOIN employeeassign as ea ON e.empID=ea.empID " + 
			"JOIN office ON e.office_code =office.office_code " + 
			"JOIN project ON ea.projID =project.projID " + 
			"JOIN salary ON ea.title=salary.title";

	String sqlProjectSta1 = "DROP view if exists projectSta";
	String sqlProjectSta2 = "CREATE VIEW  projectSta AS "
			+ "(SELECT projID, year(end)-year(start) AS span, 'COMPLETED' as status, '100' as milestone FROM project WHERE (CURDATE()>=end AND projID != '0') " + 
			"UNION  " + 
			"SELECT projID, '0' AS span, 'FUTURE' as status, '0' as milestone FROM project WHERE (CURDATE()<=start AND projID != '0') " + 
			"UNION  " + 
			"SELECT projID, year(CURDATE())-year(start) AS span, 'ONGOING' as status, null milestone FROM project WHERE (start<=CURDATE() AND CURDATE()<=end AND projID != '0') " + 
			")";

	String sqlProjectEmpHr1 = "DROP view if exists projectEmpHr";
	String sqlProjectEmpHr2 = " CREATE VIEW  projectEmpHr AS "
			+ "(SELECT *, DATEDIFF(end, start)/7*40 AS workhour, DATEDIFF(end, start)/7*40*hourly_pay as charge FROM empview2 WHERE (CURDATE()>=end AND projID != '0') " + 
			"UNION  " + 
			"SELECT *, '0' AS workhour, '0' as charge FROM empview2 WHERE (CURDATE()<=start AND projID != '0') " + 
			"UNION  " + 
			"SELECT *, DATEDIFF(end, start)/7*40 AS workhour, DATEDIFF(end, start)/7*40*hourly_pay as charge FROM empview2 WHERE (start<=CURDATE() AND CURDATE()<=end AND projID != '0') " + 
			")";

	String sqlProgress1 = "DROP view if exists projectProgress";
	String sqlProgress2 = "CREATE VIEW projectProgress AS "+"SELECT * FROM "
			+"(SELECT p.projID, p.proj_name, p.proj_manager, p.location, p.budget, p.start, p.end, s.span, s.status, e.name, " + 
			"COUNT(*) as totalEmp, SUM(eh.charge) as totalCharge, SUM(eh.workhour) as totalworkhour, SUM(eh.charge)/p.budget as prog_milestone " + 
			"FROM project p, projectSta s, projectEmpHr eh, empview2 e " + 
			"WHERE p.projID=s.projID AND p.proj_manager=e.empID AND p.projID=eh.projID " + 
			"GROUP BY p.projID" + ") as results " + "ORDER BY results.projID ASC";

	
	String sqlComIRS1 = "DROP view if exists companyirshr";
	String sqlComIRS2 = "CREATE VIEW companyirshr AS \r\n" + 
			"SELECT e.empID, e.name, e.title, e.start_date, e.address, e.city, e.state, \r\n" + 
			"e.zip, e.SSN, e.email, e.office_code, e.dept_code, e.div_code, e.classification, e.hourly_pay as unit_pay, '0' as span, CAST(year(e.start) as signed integer) as IRSyear, (DATEDIFF(e.end,e.start))/7*40*e.hourly_pay as SSincome\r\n" + 
			"FROM empview as e \r\n" + 
			"where year(e.start)=year(e.end)\r\n" + 
			"UNION\r\n" + 
			"/* generate IRS for all employees work for only two IRS years */\r\n" + 
			"SELECT e.empID, e.name, e.title, e.start_date, e.address, e.city, e.state, \r\n" + 
			"e.zip, e.SSN, e.email, e.office_code, e.dept_code, e.div_code, e.classification, e.hourly_pay as unit_pay, '0' as span, CAST(year(e.start)as signed integer) as IRSyear, (DATEDIFF(CONCAT(year(e.start),'-12-31'),start))/7*40*e.hourly_pay  as SSincome\r\n" + 
			"FROM empview as e\r\n" + 
			"where year(e.end)-year(e.start)=1\r\n" + 
			"UNION\r\n" + 
			"SELECT e.empID, e.name, e.title, e.start_date, e.address, e.city, e.state, \r\n" + 
			"e.zip, e.SSN, e.email, e.office_code, e.dept_code, e.div_code, e.classification, e.hourly_pay as unit_pay, '0' as span, CAST(year(e.end)as signed integer) as IRSyear, (DATEDIFF(end,CONCAT(year(e.end),'-1-1')))/7*40*e.hourly_pay  as SSincome\r\n" + 
			"FROM empview as e\r\n" + 
			"where year(e.end)-year(e.start)=1\r\n" + 
			"UNION\r\n" + 
			"/* generate IRS for all employees work for >three IRS years */\r\n" + 
			"SELECT e.empID, e.name, e.title, e.start_date, e.address, e.city, e.state, \r\n" + 
			"e.zip, e.SSN, e.email, e.office_code, e.dept_code, e.div_code, e.classification, e.hourly_pay as unit_pay, '0' as span, CAST(year(e.start) as signed integer) as IRSyear, (DATEDIFF(CONCAT(year(e.start),'-12-31'),start))/7*40*e.hourly_pay as SSincome\r\n" + 
			"FROM empview as e\r\n" + 
			"where year(e.end)-year(e.start)>1\r\n" + 
			"UNION\r\n" + 
			"SELECT e.empID, e.name, e.title, e.start_date, e.address, e.city, e.state, \r\n" + 
			"e.zip, e.SSN, e.email, e.office_code, e.dept_code, e.div_code, e.classification, e.hourly_pay as unit_pay, '0' as span, CAST(year(e.end) as signed integer)as IRSyear, (DATEDIFF(end,CONCAT(year(e.end),'-1-1')))/7*40*e.hourly_pay as SSincome\r\n" + 
			"FROM empview as e\r\n" + 
			"where year(e.end)-year(e.start)>1\r\n" + 
			"UNION\r\n" + 
			"SELECT e.empID, e.name, e.title, e.start_date, e.address, e.city, e.state, \r\n" + 
			"e.zip, e.SSN, e.email, e.office_code, e.dept_code, e.div_code, e.classification, e.salary as unit_pay, (year(e.end)-year(e.start)-1) as span, CAST((year(e.start)+1)as signed integer) as IRSyear, e.salary as SSincome\r\n" + 
			"FROM empview as e\r\n" + 
			"where year(e.end)-year(e.start)>1";

	String sqlComIRS3 = "DROP table if exists companyirs";
	String sqlComIRS4 = "CREATE table companyirs AS \r\n" + 
			"SELECT c.empID, c.name, c.address, c.city, c.state, c.zip, c.SSN, c.office_code, c.dept_code, c.div_code, c.classification,\r\n" + 
			"c.span, c.IRSyear, c.SSincome, SSincome*0.1 as FederalTax, SSincome*0.05 as StateTax, SSincome*0.062 as SSTax, SSincome*0.0145 as MedicalTax, SSincome*(1-0.1-0.05-0.062-0.0145) as StateWage\r\n" + 
			"FROM companyirshr as c\r\n" + 
			"WHERE c.start_date<=CURDATE() " ; 
	
	//these tupples are multiple years
//	String sqlComIRS5 ="DROP view if exists allirs"; 
	String sqlComIRS6 ="SELECT * FROM companyirs WHERE span>1"; 
		
	
	String sqlPrepIRS = "INSERT INTO companyirs VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?, ?, ?,?)";

	
	String errorMessage1 = "SQL connection failed! ";
	String errorMessage2 = "Wrong Reset, Missing Project Data!";
	String goodMessage2 = "Database reset, good to go!";
	String errorMessage3 = "Wrong Reset, Missing Employee Data!";
	String goodMessage3 = "Employee reset, good to go!";
	String errorMessage4 = "Wrong Reset, Missing Emp-Project Data!";
	String goodMessage4 = "Emp-Project reset, good to go!";
	String errorMessage5 = "Wrong Reset, Missing Status-Project Data!";
	String goodMessage5 = "Status-Project reset, good to go!";

	
	public ConnectionReset() {
		try {
			Class.forName(SQL_DRIVER).newInstance();
			connection = DriverManager.getConnection(DB_URL, USER, PASS);
			stmt = connection.createStatement();

			// pstmt = connection.prepareStatement(sqlQuerySal);

		} catch (Exception e) {
			System.out.println(errorMessage1);
			System.out.print(e.getMessage());
		}
	}

	public void resetDB() {
		try {
			stmt.executeUpdate(resetDB1);
			stmt.executeUpdate(resetDB2);
			System.out.println(goodMessage2);
		} catch (Exception e) {
			System.out.println(errorMessage2);
			System.out.print(e.getMessage());
		}
	}

	public void iniPayroll() {	
		//generate empview with most updated entity tables for payroll analysis
		try {
			stmt.executeUpdate(sqlEmpViewPay1);
			stmt.execute(sqlEmpViewPay2);
			System.out.println(goodMessage3);
		} catch (Exception e) {
			System.out.println(errorMessage3);
			System.out.print(e.getMessage());
		}
	}

	public void iniProject() {
		//generate empview2 with most updated entity tables for project analysis
		try {
			stmt.executeUpdate(sqlEmpViewProj1);
			stmt.execute(sqlEmpViewProj2);
			System.out.println(goodMessage4);
		} catch (Exception e) {
			System.out.println(errorMessage4);
			System.out.print(e.getMessage());
		}

	}

	public void staProject() {
		//initiate and generate project status with most updated entity tables for project progress
		try {
			iniProject();
			stmt.executeUpdate(sqlProjectSta1);
			stmt.execute(sqlProjectSta2);
			stmt.executeUpdate(sqlProjectEmpHr1);
			stmt.execute(sqlProjectEmpHr2);
			stmt.executeUpdate(sqlProgress1);
			stmt.execute(sqlProgress2);
			System.out.println(goodMessage5);
		} catch (Exception e) {
			System.out.println(errorMessage5);
			System.out.print(e.getMessage());
		}
	}
		public void companyIRS() {
			//initiate empview with most updated entity tables and generate the IRS report for entire company
			Statement dupStmt = null;
			ResultSet rs = null;

			try {
				iniPayroll();
				stmt.executeUpdate(sqlComIRS1);
				stmt.execute(sqlComIRS2);
				stmt.executeUpdate(sqlComIRS3);
				stmt.execute(sqlComIRS4);
//				stmt.executeUpdate(sqlComIRS5);
				
			// regenerate the duplicated years for employees working for more than 2 year span
			//sqlPrepIRS = "INSERT INTO companyirs VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?, ?, ?,?)";	
				pstmt=connection.prepareStatement(sqlPrepIRS);
				
				
				dupStmt = connection.createStatement();
				rs=dupStmt.executeQuery(sqlComIRS6);
				
			/*  empID varchar(10) 
				name varchar(45) 
				address varchar(45) 
				city varchar(45) 
				state varchar(45) 
				zip varchar(45) 
				SSN varchar(45) 
				office_code varchar(45) 
				dept_code varchar(45) 
				div_code varchar(45) 
				classification varchar(45) 
				span bigint 
				IRSyear bigint 
				SSincome double 
				FederalTax double 
				StateTax double 
				SSTax double 
				MedicalTax double 
				StateWage double	*/
				while (rs.next()) {
					int counter=rs.getInt(12);
					int year=rs.getInt(13);
					while (counter>1) {
				
						counter--;
						year++;
						pstmt.setString(1, rs.getString(1));
						pstmt.setString(2, rs.getString(2));	
						pstmt.setString(3, rs.getString(3));					
						pstmt.setString(4, rs.getString(4));
						pstmt.setString(5, rs.getString(5));	
						pstmt.setString(6, rs.getString(6));							
						pstmt.setString(7, rs.getString(7));
						pstmt.setString(8, rs.getString(8));	
						pstmt.setString(9, rs.getString(9));					
						pstmt.setString(10, rs.getString(10));
						pstmt.setString(11, rs.getString(11));	
						//set missing IRS year
						pstmt.setInt(12, counter);							
						pstmt.setInt(13, year);
						//set salaries in IRS form
						pstmt.setDouble(14, rs.getDouble(14));
						pstmt.setDouble(15, rs.getDouble(15));						
						pstmt.setDouble(16, rs.getDouble(16));						
						pstmt.setDouble(17, rs.getDouble(17));
						pstmt.setDouble(18, rs.getDouble(18));
						pstmt.setDouble(19, rs.getDouble(19));
						pstmt.execute();
					} 
				}
				rs.close();
				dupStmt.close();
				
				System.out.println(goodMessage5);
			} catch (Exception e) {
				System.out.println(errorMessage5);
				System.out.print(e.getMessage());
			}
	}
}
