package io.esoma.odbr;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.json.JSONArray;
import org.json.JSONObject;

public class Main {

	public static void main(String[] args) throws Exception {

		// Logger setup
		Logger logger = Logger.getLogger("ORCL");
		logger.setLevel(Level.ALL);
		Handler handler = new ConsoleHandler();
		handler.setFormatter(new SimpleFormatter());
		handler.setLevel(Level.ALL);
		logger.addHandler(handler);

		// I/O base path
		String inputPath = null;
		String outputPath = null;

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
			if (in != null) {
				in.close();
			}

			inputPath = props.getProperty("input");
			outputPath = props.getProperty("output");
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

		// Check path
		if (inputPath == null) {
			inputPath = "";
		}
		if (outputPath == null) {
			outputPath = "";
		}

		// Read SQL script
		String sqlStmt = null;

		try {
			FileReader reader = new FileReader(inputPath);

			StringBuilder sb = new StringBuilder();
			int ch = 0;
			while ((ch = reader.read()) != -1) {
				sb.append((char) ch);
			}
			sqlStmt = sb.toString();

			if (reader != null) {
				reader.close();
			}
		} catch (Exception e) {
			logger.severe("Error in reading SQL script. Printing stack trace...");
			throw e;
		}

		// Check statement
		if (sqlStmt == null || sqlStmt.length() == 0) {
			logger.severe("Failed to read SQL or SQL file has no content");
			return;
		}

//		System.out.println(sqlStmt);

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

		// Create JSON object with result set
		JSONArray jarr = new JSONArray();
		ResultSetMetaData rsmt = rs.getMetaData();
		int cols = rsmt.getColumnCount();

		try {
			while (rs.next()) {
				JSONObject jo = new JSONObject();

				// Write JSON data based on SQL type
				for (int i = 1; i <= cols; ++i) {
					String colName = rsmt.getColumnName(i);
					int dt = rsmt.getColumnType(i);

					// If type switch
					if (dt == Types.VARCHAR || dt == Types.CHAR || dt == Types.LONGVARCHAR) {
						jo.put(colName, rs.getString(i));
					} else if (dt == Types.INTEGER || dt == Types.SMALLINT || dt == Types.TINYINT
							|| dt == Types.BIGINT) {
						jo.put(colName, rs.getInt(i));
					} else if (dt == Types.NUMERIC || dt == Types.DECIMAL || dt == Types.DOUBLE) {
						jo.put(colName, rs.getBigDecimal(i));
					} else if (dt == Types.DATE) {
						jo.put(colName, rs.getDate(i));
					} else if (dt == Types.TIMESTAMP || dt == Types.TIMESTAMP_WITH_TIMEZONE) {
						jo.put(colName, rs.getTimestamp(i));
					} else if (dt == Types.TIME || dt == Types.TIME_WITH_TIMEZONE) {
						jo.put(colName, rs.getTime(i));
					} else if (dt == Types.NCHAR || dt == Types.NVARCHAR || dt == Types.LONGNVARCHAR) {
						jo.put(colName, rs.getNString(i));
					} else if (dt == Types.CLOB) {
						Reader re = rs.getCharacterStream(i);
						jo.put(colName, readClob(re));
					} else if (dt == Types.NCLOB) {
						Reader re = rs.getNCharacterStream(i);
						jo.put(colName, readClob(re));
					} else if (dt == Types.BLOB || dt == Types.BINARY) {
						// Use placeholder rather than writing BLOB
						jo.put(colName, "<BLOB>");
					} else if (dt == Types.NULL) {
						jo.put(colName, (Object) null);
					} else {
						jo.put(colName, "<" + rsmt.getColumnTypeName(i) + ">");
					}
				}

				jarr.put(jo);
			}
		} catch (Exception e) {
			logger.severe("Error in processing query result. Printing stack trace...");
			throw e;
		}

		// Output JSON data to file
		String fileName = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) + ".json";
		logger.fine("Creating output file " + fileName + "...");
		File file = new File(outputPath + fileName);
		file.createNewFile();
		Writer wr = new FileWriter(file);

		try {
			jarr.write(wr, 2, 0);
		} catch (Exception e) {
			logger.severe("Error in writing output file. Printing stack trace...");
			throw e;
		} finally {
			if (wr != null) {
				wr.close();
			}
		}

		logger.fine("Output complete. Closing resources...");
		closeResources(conn, stmt, rs);
		logger.fine("Finished cleaning up");

		logger.fine(bannerFormat("End of Operation"));
	}

	public static String bannerFormat(String msg) {
		final String BANNER = "==========";
		return BANNER + " " + msg + " " + BANNER;
	}

	public static String readClob(Reader re) throws Exception {
		if (re == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		int ch = 0;
		while ((ch = re.read()) != -1) {
			sb.append((char) ch);
		}

		return sb.toString();
	}

	public static void closeResources(Connection conn, Statement stmt, ResultSet rs) throws Exception {
		if (rs != null) {
			rs.close();
		}
		if (stmt != null) {
			stmt.close();
		}
		if (conn != null) {
			conn.close();
		}
	}

}
