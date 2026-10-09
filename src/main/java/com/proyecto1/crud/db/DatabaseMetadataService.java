package com.proyecto1.crud.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseMetadataService {
public static void listTables() throws SQLException {
      try (Connection connection = DatabaseConnection.getConnection()) {
                DatabaseMetaData metadata = connection.getMetaData();
                        try (ResultSet tables = metadata.getTables(null, "public", "%", new String[]{"TABLE"})) {
                                      while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                System.out.println(tableName);
            }

      }
    }
  }

  public static void listColumns(String tableName) throws SQLException {
    try (Connection connection = DatabaseConnection.getConnection()) {
          DatabaseMetaData metadata = connection.getMetaData();
              try (ResultSet columns = metadata.getColumns(null, "public", tableName, "%")) {
                        while (columns.next()) {
            String columnName = columns.getString("COLUMN_NAME");
            String dataType = columns.getString("TYPE_NAME");

            System.out.println(columnName + " - " + dataType);
        }
      }
    }
  }

  public static String getPrimaryKey(String tableName) throws SQLException {
    try (Connection connection = DatabaseConnection.getConnection()) {

        DatabaseMetaData metadata =
                connection.getMetaData();

        try (ResultSet primaryKeys =
                metadata.getPrimaryKeys(
                    null,
                    "public",
                    tableName
                )) {

            if (primaryKeys.next()) {
                return primaryKeys.getString(
                    "COLUMN_NAME"
                );
            }
        }
    }

    return null;
}

public static void listPrimaryKeys(String tableName) throws SQLException {
  try (Connection connection = DatabaseConnection.getConnection()) {
        DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet primaryKeys = metadata.getPrimaryKeys(null, "public", tableName)) {
                      while (primaryKeys.next()) {
            String columnName = primaryKeys.getString("COLUMN_NAME");
            System.out.println("Primary Key: " + columnName);       
      }
    }
  }
}
}
