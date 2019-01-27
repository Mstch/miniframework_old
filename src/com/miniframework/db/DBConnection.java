package com.miniframework.db;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
	//TODO:2019/1/27有空的时候会改成配置文件的...
		String driver = "com.mysql.cj.jdbc.Driver";
	    String url= "jdbc:mysql://localhost:3306/LLL?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8";
	    String user = "root";
	    String password = "Zhang123@";
	    
	    public Connection conn;

	    public DBConnection() {

	        try {
	            Class.forName(driver);
	            conn = (Connection) DriverManager.getConnection(url, user, password);
	            if(!conn.isClosed())
	                System.out.println("Succeeded connecting to the Database!"); 
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	    
	    public void close() {
	        try {
	            this.conn.close();
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }

}
