package com.proyecto1.crud.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;

public class DynamicQueryService {

    public static void selectAll(String tableName) throws SQLException {

        if (!tableName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Nombre de tabla no válido");
        }

        String sql = "SELECT * FROM " + tableName;

        try (
            Connection connection = DatabaseConnection.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql)
        ) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (resultSet.next()) {

                for (int i = 1; i <= columnCount; i++) {

                    String columnName = metaData.getColumnName(i);
                    Object value = resultSet.getObject(i);

                    System.out.print(
                        columnName + ": " + value + " | "
                    );
                }

                System.out.println();
            }
        }
    }

    public static void insert(
            String tableName,
            String[] columns,
            Object[] values
    ) throws SQLException {

        if (columns.length != values.length) {
            throw new IllegalArgumentException(
                "La cantidad de columnas y valores debe coincidir"
            );
        }

        if (!tableName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException(
                "Nombre de tabla no válido"
            );
        }

        for (String column : columns) {

            if (!column.matches("[A-Za-z0-9_]+")) {
                throw new IllegalArgumentException(
                    "Nombre de columna no válido: " + column
                );
            }
        }

        String columnNames =
                String.join(", ", columns);

        String placeholders =
                String.join(
                    ", ",
                    java.util.Collections.nCopies(
                        columns.length,
                        "?"
                    )
                );

        String sql =
                "INSERT INTO " + tableName +
                " (" + columnNames + ")" +
                " VALUES (" + placeholders + ")";

        try (
            Connection connection =
                    DatabaseConnection.getConnection();

            PreparedStatement preparedStatement =
                    connection.prepareStatement(sql)
        ) {

            for (int i = 0; i < values.length; i++) {
                preparedStatement.setObject(
                    i + 1,
                    values[i]
                );
            }

            preparedStatement.executeUpdate();
        }
    }

    public static void update(
            String tableName,
            String[] columns,
            Object[] values,
            Object primaryKeyValue
    ) throws SQLException {

        if (columns.length != values.length) {
            throw new IllegalArgumentException(
                "La cantidad de columnas y valores debe coincidir"
            );
        }

        if (!tableName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException(
                "Nombre de tabla no válido"
            );
        }

        for (String column : columns) {

            if (!column.matches("[A-Za-z0-9_]+")) {
                throw new IllegalArgumentException(
                    "Nombre de columna no válido: " + column
                );
            }
        }

        String primaryKeyColumn =
                DatabaseMetadataService
                    .getPrimaryKey(tableName);

        if (primaryKeyColumn == null) {
            throw new IllegalArgumentException(
                "La tabla no tiene una clave primaria definida"
            );
        }

        StringBuilder setClause =
                new StringBuilder();

        for (int i = 0; i < columns.length; i++) {

            setClause
                .append(columns[i])
                .append(" = ?");

            if (i < columns.length - 1) {
                setClause.append(", ");
            }
        }

        String sql =
                "UPDATE " + tableName +
                " SET " + setClause +
                " WHERE " + primaryKeyColumn +
                " = ?";

        try (
            Connection connection =
                    DatabaseConnection.getConnection();

            PreparedStatement preparedStatement =
                    connection.prepareStatement(sql)
        ) {

            for (int i = 0; i < values.length; i++) {

                preparedStatement.setObject(
                    i + 1,
                    values[i]
                );
            }

            preparedStatement.setObject(
                values.length + 1,
                primaryKeyValue
            );

            preparedStatement.executeUpdate();
        }
    }

    public static void delete(
            String tableName,
            Object primaryKeyValue
    ) throws SQLException {

        if (!tableName.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException(
                "Nombre de tabla no válido"
            );
        }

        String primaryKeyColumn =
                DatabaseMetadataService
                    .getPrimaryKey(tableName);

        if (primaryKeyColumn == null) {
            throw new IllegalArgumentException(
                "La tabla no tiene una clave primaria definida"
            );
        }

        String sql =
                "DELETE FROM " + tableName +
                " WHERE " + primaryKeyColumn +
                " = ?";

        try (
            Connection connection =
                    DatabaseConnection.getConnection();

            PreparedStatement preparedStatement =
                    connection.prepareStatement(sql)
        ) {

            preparedStatement.setObject(
                1,
                primaryKeyValue
            );

            preparedStatement.executeUpdate();
        }
    }
}