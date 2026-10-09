package com.proyecto1.crud.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DatabaseConnection {
  private static String url;
  private static String username;
  private static String password;


private static void loadProperties() {
  Properties properties = new Properties();
  try (InputStream input = DatabaseConnection.class
      .getClassLoader()
      .getResourceAsStream("config.properties")) {
    if (input == null) {
      throw new RuntimeException("No se encontro config.properties en el classpath");
    }
    properties.load(input);
  } catch (IOException e) {
    throw new RuntimeException("No se pudo cargar config.properties", e);
  }

  url = properties.getProperty("db.url");
  username = properties.getProperty("db.user");
  password = properties.getProperty("db.password");

  if (url == null || url.isBlank()) {
    throw new RuntimeException("La propiedad db.url esta vacia en config.properties");
  }
}

public static Connection getConnection() throws SQLException {
  loadProperties();
  return DriverManager.getConnection(url, username, password);
}

}