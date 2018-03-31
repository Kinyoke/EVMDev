package application.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/*****************************************************
 *	@author : Faisal burhan Abdu.                    *
 *	@version : v1.0.1.            					 *
 *	@date : 2017-01-14. 							 *
 ****************************************************/

public class dataHandler {
	private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	private static final String DB_URL = "jdbc:mysql://localhost/EVM";
	private static String USER = "root";
	private static String PASS = "";
	private static Connection conn = null;
	private static Statement stmt = null;
	private static String sql;
	
	
	public static void setUser(String user){
		USER = user;
	}
	
	public static void setPass(String pass){
		PASS = pass;
	}
	
	public static void init(){
		try{
			//STEP 2: Register JDBC driver
			Class.forName(JDBC_DRIVER);
			//STEP 3: Open a connection
			System.out.println("Connecting to database...");
			conn = DriverManager.getConnection(DB_URL,USER,PASS);
		}catch(SQLException | ClassNotFoundException se){
			se.printStackTrace();
		}
	}
	
	
	public static void setData(String data){
		try {
			sql = "INSERT INTO Voters(FirstName,MiddleName,LastName,BirthDate,Gender,Residence,FingerPrint,faceID) VALUES ("+data+")";
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			//System.out.println(rows);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//finally block used to close resources
		try{
		if(stmt!=null)
		stmt.close();
		}catch(SQLException se2){
		}// nothing we can do
		
	}
	public static void retrData(String query){

		try{
		//STEP 4: Execute a query
		System.out.println("Creating statement...");
		stmt = conn.createStatement();
//		+-------------+--------------+------+-----+---------+----------------+
//		| Field       | Type         | Null | Key | Default | Extra          |
//		+-------------+--------------+------+-----+---------+----------------+
//		| ID          | int(11)      | NO   | PRI | NULL    | auto_increment |
//		| FirstName   | varchar(16)  | YES  |     | NULL    |                |
//		| MiddleName  | varchar(16)  | YES  |     | NULL    |                |
//		| LastName    | varchar(16)  | YES  |     | NULL    |                |
//		| BirthDate   | date         | YES  |     | NULL    |                |
//		| Gender      | varchar(8)   | YES  |     | NULL    |                |
//		| Residence   | varchar(32)  | YES  |     | NULL    |                |
//		| FingerPrint | varchar(124) | YES  |     | NULL    |                |
//		| faceID      | blob         | YES  |     | NULL    |                |
//		+-------------+--------------+------+-----+---------+----------------+

		sql = query;
		ResultSet results = stmt.executeQuery(sql);
		//STEP 5: Extract data from result set
		while(results.next()){
		//Retrieve by column name
		int id = results.getInt("ID");
		String fname = results.getString("FirstName");
		String mname = results.getString("MiddleName");
		String lname = results.getString("LastName");
	///	String bdate = rs.getString("BirthDate");
		String gender = results.getString("Gender");
		String residence = results.getString("Residence");
		String fprint = results.getString("FingerPrint");
		String fscan = results.getString("faceID");
		//Display values
		System.out.print("ID: " + id);
		System.out.print(", FirstName: " + fname);
		System.out.print(", MiddleName: " + mname);
		System.out.print(", LastName: " + lname);
	//	System.out.print(", BirthDate: " + bdate);
		System.out.print(", Gender: " + gender);
		System.out.print(", Residence: " + residence);
		System.out.print(", FirstName: " + fname);
		System.out.print(", FingerPrint: " + fprint);
		System.out.println(", FaceScan: " + fscan);
		}
		//STEP 6: Clean-up environment
		results.close();
		stmt.close();
		}catch(SQLException se){
		//Handle errors for JDBC
		se.printStackTrace();
		}catch(Exception e){
		//Handle errors for Class.forName
		e.printStackTrace();
		}finally{
		//finally block used to close resources
		try{
		if(stmt!=null)
		stmt.close();
		}catch(SQLException se2){
		}// nothing we can do
		}
		//return query;
	}
	
	public static void exit(){
		try{
			if(conn!=null){
				conn.close();
				System.out.println("terminating con...!");
			}
			}catch(SQLException se){
				se.printStackTrace();
			}
	}
	
	/**
	 * init conn
	 * retr data
	 * insert data
	 * close conn
	 * */
	
	public static void main(String[] args){
		dataHandler.init();
		sql = "'Ibrahim','Said','Yakoot','2013-10-12','Male','Dar-es-salaam,Temeke,Kigamboni','',''";
		dataHandler.setData(sql);
		//dataHandler.retrData("SELECT ID, FirstName, MiddleName, LastName, BirthDate, Gender, Residence, FingerPrint, faceID FROM Voters");
		dataHandler.exit();
	}

}
