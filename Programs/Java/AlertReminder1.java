package PayrollMgt;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

public class AlertReminder1  {
String warnMsg;

	public AlertReminder1(String warnMsg) {
		this.warnMsg=warnMsg;
	}

	public void popAlert1 (HttpServletResponse response) throws IOException {
		// @servlet writer to get results posted
		PrintWriter out = response.getWriter();
		out.println( "<script>alert(\""+warnMsg+"\")</script>");
	}
	
	public void popAlert1 (PrintWriter out) throws IOException {
		// @servlet writer to get results posted
		
		out.println( "<script>alert(\""+warnMsg+"\")</script>");
	}
	
}
