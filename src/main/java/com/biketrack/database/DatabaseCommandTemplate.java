package com.biketrack.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.io.InputStream;
import java.util.Properties;

/**
 * DatabaseCommandTemplate is a utility class that provides a template for creating,
 * executing, and handling SQL commands in the Bike-Track application.
 */
public class DatabaseCommandTemplate {

    private final Connection connection;
    private Session sshSession;

    /**
     * Constructor to initialize the DatabaseCommandTemplate with a database connection.
     *
     * @param connection The SQL database connection to be used for executing commands.
     */
    public DatabaseCommandTemplate(Connection connection) {
        this.connection = connection;
    }

    /**
     * Closes the SSH connection if it is established.
     */
    public void closeSSHConnection() {
        if (sshSession != null && sshSession.isConnected()) {
            sshSession.disconnect();
        }
    }

    /**
     * Executes a SQL command on the remote database via SSH.
     *
     * @param sqlCommand The SQL command to be executed.
     * @return The output of the command execution.
     * @throws Exception If an error occurs during command execution.
     */
    public String executeRemoteCommand(String sqlCommand) throws Exception {
        if (sshSession == null || !sshSession.isConnected()) {
            throw new IllegalStateException("SSH session is not established.");
        }

        try (InputStream inputStream = sshSession.openChannel("exec").getInputStream()) {
            var channel = sshSession.openChannel("exec");
            ((com.jcraft.jsch.ChannelExec) channel).setCommand(sqlCommand);
            channel.connect();

            StringBuilder output = new StringBuilder();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output.append(new String(buffer, 0, bytesRead));
            }

            channel.disconnect();
            return output.toString();
        }
    }

    /**
     * Establishes an SSH connection to the remote server.
     *
     * @param host The remote host address.
     * @param username The username for the SSH connection.
     * @param privateKeyPath The path to the private key file.
     * @throws Exception If an error occurs while establishing the SSH connection.
     */
    public void establishSSHConnection(String host, String username, String privateKeyPath) throws Exception {
        JSch jsch = new JSch();
        jsch.addIdentity(privateKeyPath);

        sshSession = jsch.getSession(username, host, 22);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        sshSession.setConfig(config);

        sshSession.connect();
    }

    /**
     * Executes a SQL query and processes the result using the provided ResultHandler.
     *
     * @param sqlQuery       The SQL query to be executed.
     * @param parameterSetter A lambda or functional interface to set parameters in the PreparedStatement.
     * @param resultHandler  A lambda or functional interface to handle the ResultSet.
     * @throws SQLException If an error occurs while executing the query or processing the result.
     */
    public void executeQuery(String sqlQuery, ParameterSetter parameterSetter, ResultHandler resultHandler) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery)) {
            if (parameterSetter != null) {
                parameterSetter.setParameters(preparedStatement);
            }
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultHandler != null) {
                    resultHandler.handle(resultSet);
                }
            }
        }
    }

    /**
     * Executes a SQL update (INSERT, UPDATE, DELETE) and returns the number of affected rows.
     *
     * @param sqlUpdate       The SQL update statement to be executed.
     * @param parameterSetter A lambda or functional interface to set parameters in the PreparedStatement.
     * @return The number of rows affected by the update.
     * @throws SQLException If an error occurs while executing the update.
     */
    public int executeUpdate(String sqlUpdate, ParameterSetter parameterSetter) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlUpdate)) {
            if (parameterSetter != null) {
                parameterSetter.setParameters(preparedStatement);
            }
            return preparedStatement.executeUpdate();
        }
    }

    /**
     * Functional interface for setting parameters in a PreparedStatement.
     */
    @FunctionalInterface
    public interface ParameterSetter {
        void setParameters(PreparedStatement preparedStatement) throws SQLException;
    }

    /**
     * Functional interface for handling the ResultSet of a query.
     */
    @FunctionalInterface
    public interface ResultHandler {
        void handle(ResultSet resultSet) throws SQLException;
    }
}
```

### Explanation of the Implementation:
1. **Purpose of the Class**:
   - The `DatabaseCommandTemplate` class is designed to provide a reusable template for interacting with an SQL database. It simplifies the process of setting up SQL queries, executing them, and handling results.

2. **Constructor**:
   - The constructor accepts a `Connection` object, which is used to interact with the database.

3. **Methods**:
   - `executeQuery`: Executes a SQL query (e.g., SELECT) and processes the result using a `ResultHandler`.
   - `executeUpdate`: Executes a SQL update (e.g., INSERT, UPDATE, DELETE) and returns the number of affected rows.

4. **Functional Interfaces**:
   - `ParameterSetter`: Allows the caller to set parameters in a `PreparedStatement`.
   - `ResultHandler`: Allows the caller to process the `ResultSet` returned by a query.

5. **Error Handling**:
   - The `try-with-resources` statement is used to ensure that `PreparedStatement` and `ResultSet` are automatically closed, preventing resource leaks.

6. **Flexibility**:
   - The use of functional interfaces (`ParameterSetter` and `ResultHandler`) makes the class highly flexible and reusable for various SQL operations.

### Example Usage:
Here’s how this class can be used in practice:
```java
DatabaseCommandTemplate template = new DatabaseCommandTemplate(databaseConnection);

// Example: Executing a SELECT query
template.executeQuery(
    "SELECT * FROM users WHERE id = ?",
    preparedStatement -> preparedStatement.setInt(1, 123),
    resultSet -> {
        while (resultSet.next()) {
            System.out.println("User ID: " + resultSet.getInt("id"));
            System.out.println("User Name: " + resultSet.getString("name"));
        }
    }
);

// Example: Executing an INSERT query
int rowsAffected = template.executeUpdate(
    "INSERT INTO users (name, email) VALUES (?, ?)",
    preparedStatement -> {
        preparedStatement.setString(1, "John Doe");
        preparedStatement.setString(2, "john.doe@example.com");
    }
);
System.out.println("Rows inserted: " + rowsAffected);