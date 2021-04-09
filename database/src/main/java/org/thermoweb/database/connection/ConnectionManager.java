package org.thermoweb.database.connection;

import org.thermoweb.core.config.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static org.thermoweb.database.conf.DatabaseConfiguration.PASSWORD;
import static org.thermoweb.database.conf.DatabaseConfiguration.URL;
import static org.thermoweb.database.conf.DatabaseConfiguration.USER;

public class ConnectionManager {

    public static Connection getConnection() throws SQLException {
        Properties bddProperties = new Properties();
        bddProperties.put("user", Configuration.getProperty(USER));
        bddProperties.put("password", Configuration.getProperty(PASSWORD));
        return DriverManager.getConnection(Configuration.getProperty(URL), bddProperties);
    }
}
