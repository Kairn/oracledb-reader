# Oracle Database Reader

Oracle Database Reader is a tool that can run any DQL query on a Oracle DB connection and output the result in a beautified JSON format. It is a convenient way of exporting tabular data from a database server when you only have access to low level connection methods.

***WARNING: Do NOT use this tool to run any DML or DDL statements as it will fail 99.99% of the cases. In the cases it does not fail it could lead to serious consequences if you are connected to an important database. Also, it has not been tested to work in all environments and scenarios.***

## Build
### Prerequisites
* [JDK 8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Apache Maven](https://maven.apache.org/download.cgi) version 3.5 or above
* [OJDBC 12.1.0.2](https://www.oracle.com/technetwork/database/features/jdbc/default-2280470.html)

### Execution
1. Download OJDBC jar file and place it in your local maven repository (`~/.m2/repository/com/oracle/ojdbc/ojdbc/12.1.0.2/`). You also need to make sure the name of this jar matches the artifact name declared in `pom.xml` (`ojdbc-12.1.0.2.jar` should be used by default).
2. Clone or download this repository and change into the project's root directory (where `pom.xml` is located).
3. Setup basic configuration by creating a file named `config.properties` and place it in `src/main/resources/`. Then, copy the content from `config.template` into the config file and fill out values required for database connection as well as input and output directories (where to locate SQL script and generate JSON dump).
4. Run `mvn clean package` to build an executable jar (`orcl-reader.jar` is the default name).
5. Create a file named `custom.sql` and place it in your input directory specified in your config file and write your DQL statement (do NOT add a semicolon at the end). Only one statement is allowed.
6. Run `java -jar target/orcl-reader.jar` to execute the jar.
7. Locate the JSON dump (.json file) in the output directory (name of the file will be the timestamp in Epoch seconds).
8. To execute more queries, simply edit the SQL script in `custom.sql` and re-run the jar.

## Remarks
***Use this tool at your own RISK. I won't be responsible for negative consequences caused by user's carelessness. TEST it before use.**

Please contact me directly if you have suggestions or would like to report an issue.