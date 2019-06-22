package io.esoma.odbr;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class Main {

	public static void main(String[] args) throws ClassNotFoundException {

		// Initialize Oracle driver
		Class.forName("oracle.jdbc.driver.OracleDriver");

		// DB connection
		Connection conn = null;

		try {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			InputStream in = cl.getResourceAsStream("config.properties");

			Properties props = new Properties();
			props.load(in);

			// Get connection
			conn = DriverManager.getConnection(props.getProperty("url"), props.getProperty("username"),
					props.getProperty("password"));
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		if (conn != null) {
			System.out.println(conn.toString());
		} else {
			System.out.println("Connection failed");
		}

	}

}
