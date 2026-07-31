package it.polimi.mae.musiclabeling.utils;

import jakarta.servlet.ServletContext;
import jakarta.servlet.UnavailableException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionHandler {
    public static Connection getConnection(ServletContext context) throws UnavailableException {
        Connection connection;
        try {
            // Environment variables take priority over web.xml (used in production/Railway)
            String driver = System.getenv("DB_DRIVER") != null ? System.getenv("DB_DRIVER") : context.getInitParameter("dbDriver");
            String url = System.getenv("DB_URL") != null ? System.getenv("DB_URL") : context.getInitParameter("dbUrl");
            String user = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : context.getInitParameter("dbUser");
            String password = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : context.getInitParameter("dbPassword");
            Class.forName(driver);
            connection = DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new UnavailableException("Can't load database driver");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new UnavailableException("Couldn't get db connection");
        }
        return connection;
    }

    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
