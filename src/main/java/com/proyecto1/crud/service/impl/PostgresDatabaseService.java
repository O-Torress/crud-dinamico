package com.proyecto1.crud.service.impl;

import com.proyecto1.crud.db.DatabaseConnection;
import com.proyecto1.crud.db.DatabaseMetadataService;
import com.proyecto1.crud.db.DynamicQueryService;
import com.proyecto1.crud.service.DatabaseService;
import com.proyecto1.crud.service.model.ColumnInfo;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adapta el backend de PostgreSQL (clases estaticas en {@code com.proyecto1.crud.db})
 * al contrato {@link DatabaseService} que consume la interfaz JavaFX.
 *
 * <p>Las escrituras se delegan en {@link DynamicQueryService} (que usa
 * {@link PreparedStatement}); las lecturas y metadatos usan {@link DatabaseMetaData}.</p>
 */
public class PostgresDatabaseService implements DatabaseService {

    private static final String SCHEMA = "public";

    private boolean connected;

    @Override
    public void connect() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connected = connection != null && !connection.isClosed();
        }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public List<String> getTableNames() throws SQLException {
        List<String> names = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             ResultSet tables = connection.getMetaData()
                     .getTables(null, SCHEMA, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                names.add(tables.getString("TABLE_NAME"));
            }
        }
        return names;
    }

    @Override
    public List<ColumnInfo> getColumns(String table) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            Set<String> primaryKeys = new HashSet<>();
            try (ResultSet keys = connection.getMetaData().getPrimaryKeys(null, SCHEMA, table)) {
                while (keys.next()) {
                    primaryKeys.add(keys.getString("COLUMN_NAME"));
                }
            }

            List<ColumnInfo> columns = new ArrayList<>();
            try (ResultSet rs = connection.getMetaData().getColumns(null, SCHEMA, table, "%")) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");
                    columns.add(new ColumnInfo(
                            name,
                            rs.getInt("DATA_TYPE"),
                            rs.getString("TYPE_NAME"),
                            rs.getInt("COLUMN_SIZE"),
                            rs.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls,
                            primaryKeys.contains(name),
                            "YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT"))));
                }
            }
            return columns;
        }
    }

    @Override
    public List<Map<String, Object>> fetchRows(String table, int limit, int offset) throws SQLException {
        validateIdentifier(table);
        String sql = "SELECT * FROM " + table + " LIMIT ? OFFSET ?";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            statement.setInt(2, offset);
            try (ResultSet rs = statement.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(metaData.getColumnLabel(i), rs.getObject(i));
                    }
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    @Override
    public long countRows(String table) throws SQLException {
        validateIdentifier(table);
        String sql = "SELECT COUNT(*) FROM " + table;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    @Override
    public void insert(String table, Map<String, Object> values) throws SQLException {
        validateIdentifier(table);

        List<String> columns = new ArrayList<>();
        List<Object> columnValues = new ArrayList<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getValue() != null) {
                validateIdentifier(entry.getKey());
                columns.add(entry.getKey());
                columnValues.add(entry.getValue());
            }
        }
        if (columns.isEmpty()) {
            throw new SQLException("No hay valores para insertar en " + table);
        }

        DynamicQueryService.insert(table, columns.toArray(new String[0]), columnValues.toArray());
    }

    @Override
    public void update(String table, Map<String, Object> values, Map<String, Object> primaryKey) throws SQLException {
        validateIdentifier(table);

        String pkColumn = DatabaseMetadataService.getPrimaryKey(table);
        if (pkColumn == null) {
            throw new SQLException("La tabla " + table + " no tiene clave primaria definida");
        }
        Object pkValue = primaryKey.get(pkColumn);
        if (pkValue == null) {
            throw new SQLException("Falta el valor de la clave primaria '" + pkColumn + "'");
        }

        List<String> columns = new ArrayList<>();
        List<Object> columnValues = new ArrayList<>();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(pkColumn)) {
                continue;
            }
            validateIdentifier(entry.getKey());
            columns.add(entry.getKey());
            columnValues.add(entry.getValue());
        }
        if (columns.isEmpty()) {
            throw new SQLException("No hay columnas para actualizar en " + table);
        }

        DynamicQueryService.update(table, columns.toArray(new String[0]), columnValues.toArray(), pkValue);
    }

    @Override
    public void delete(String table, Map<String, Object> primaryKey) throws SQLException {
        validateIdentifier(table);

        String pkColumn = DatabaseMetadataService.getPrimaryKey(table);
        if (pkColumn == null) {
            throw new SQLException("La tabla " + table + " no tiene clave primaria definida");
        }
        Object pkValue = primaryKey.get(pkColumn);
        if (pkValue == null) {
            throw new SQLException("Falta el valor de la clave primaria '" + pkColumn + "'");
        }

        DynamicQueryService.delete(table, pkValue);
    }

    @Override
    public void close() {
        connected = false;
    }

    private void validateIdentifier(String identifier) throws SQLException {
        if (identifier == null || !identifier.matches("[A-Za-z0-9_]+")) {
            throw new SQLException("Identificador no valido: " + identifier);
        }
    }
}
