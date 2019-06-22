package io.esoma.odbr;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Main {

	public static void main(String[] args) throws Exception {

		// Logger setup
		Logger logger = Logger.getLogger("ORCL");
		logger.setLevel(Level.ALL);
		Handler handler = new ConsoleHandler();
		handler.setFormatter(new SimpleFormatter());
		handler.setLevel(Level.ALL);
		logger.addHandler(handler);

		// Initialize Oracle driver
		logger.fine(bannerFormat("Oracle Database Reader"));
		Class.forName("oracle.jdbc.driver.OracleDriver");

		// DB connection
		Connection conn = null;

		try {
			logger.fine("Loading configuration properties...");
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			InputStream in = cl.getResourceAsStream("config.properties");

			Properties props = new Properties();
			props.load(in);

			// Get connection
			logger.fine("Connecting to datasource...");
			conn = DriverManager.getConnection(props.getProperty("url"), props.getProperty("username"),
					props.getProperty("password"));
		} catch (Exception e) {
			logger.severe("Error in initialization. Printing stack trace...");
			throw e;
		}

		// Check connection
		if (conn != null) {
			logger.fine("Connected to datasource at " + conn.toString());
		} else {
			logger.severe("Unable to establish connection. Abort");
			return;
		}

		// Read SQL script
		String sqlStmt = null;

		try {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			InputStream in = cl.getResourceAsStream("custom.sql");
			InputStreamReader reader = new InputStreamReader(in);

			StringBuilder sb = new StringBuilder();
			int ch = 0;
			while ((ch = reader.read()) != -1) {
				sb.append((char) ch);
			}
			sqlStmt = sb.toString();
		} catch (Exception e) {
			logger.severe("Error in reading SQL script. Printing stack trace...");
			throw e;
		}

		// Check statement
		if (sqlStmt == null || sqlStmt.length() == 0) {
			logger.severe("Failed to read SQL or SQL file has no content");
			return;
		}

		conn.setAutoCommit(false);
		conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

		// Execute SQL script
		ResultSet rs = null;
		Statement stmt = conn.createStatement();

		try {
			rs = stmt.executeQuery(sqlStmt);
		} catch (Exception e) {
			logger.severe("Error in executing SQL script. Printing stack trace...");
			throw e;
		}

		System.out.println(rs.getMetaData().getColumnCount());

		// Create JSON object with result set
	}

	public static String bannerFormat(String msg) {
		final String BANNER = "==========";
		return BANNER + " " + msg + " " + BANNER;
	}

}
