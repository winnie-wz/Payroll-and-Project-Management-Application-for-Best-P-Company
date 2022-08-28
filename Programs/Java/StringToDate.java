package PayrollMgt;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;  
import java.util.*;
import java.sql.Date;

public class StringToDate {
	//convert a string to sql date
	SimpleDateFormat datef = new SimpleDateFormat("yyyy-MM-dd");
	String date;
	private java.sql.Date sqlDate;
	private java.util.Date utilDate;
	public StringToDate(String date) {
		this.date=date;
	
	}
	
	private Date stringToDate(String date) throws Exception {
        java.sql.Date sqlDate = null;
        if( !date.isEmpty()) {

            try {
                java.util.Date normalDate = datef.parse(date);
                sqlDate = new java.sql.Date(normalDate.getTime());
            } catch (ParseException e) {
                throw new Exception("Not able to Parse the date", e);
            }
        }
        return sqlDate;
    }

	private String dateToString (java.util.Date utilDate) {
		String stringDate = datef.format(utilDate);
		return stringDate;
	}
	public void setSqlDate(java.sql.Date sqlDate) {
		this.sqlDate = sqlDate;
	}
	
	public java.sql.Date getSqlDate( ) {
		try {
			sqlDate=stringToDate(date);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sqlDate;
	}
}
