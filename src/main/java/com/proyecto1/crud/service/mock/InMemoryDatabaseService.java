package com.proyecto1.crud.service.mock;

import com.proyecto1.crud.service.DatabaseService;
import com.proyecto1.crud.service.model.ColumnInfo;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementacion en memoria del contrato {@link DatabaseService}.
 *
 * <p>Sirve para desarrollar y ejecutar la interfaz sin una base de datos real.
 * Cuando el backend de PostgreSQL este listo, basta con crear otra
 * implementacion de {@code DatabaseService} y usarla en {@code App}.</p>
 */
public class InMemoryDatabaseService implements DatabaseService {

    private final Map<String, List<ColumnInfo>> schema = new LinkedHashMap<>();
    private final Map<String, List<Map<String, Object>>> tables = new LinkedHashMap<>();
    private boolean connected;

    public InMemoryDatabaseService() {
        seedSchema();
        seedData();
    }

    private void seedSchema() {
        schema.put("clientes", List.of(
                new ColumnInfo("id", Types.INTEGER, "serial", 0, false, true, true),
                new ColumnInfo("nombre", Types.VARCHAR, "varchar", 100, false, false, false),
                new ColumnInfo("email", Types.VARCHAR, "varchar", 150, true, false, false),
                new ColumnInfo("telefono", Types.VARCHAR, "varchar", 20, true, false, false),
                new ColumnInfo("activo", Types.BOOLEAN, "boolean", 0, true, false, false),
                new ColumnInfo("fecha_alta", Types.DATE, "date", 0, true, false, false)));

        schema.put("productos", List.of(
                new ColumnInfo("id", Types.INTEGER, "serial", 0, false, true, true),
                new ColumnInfo("nombre", Types.VARCHAR, "varchar", 100, false, false, false),
                new ColumnInfo("precio", Types.NUMERIC, "numeric", 10, true, false, false),
                new ColumnInfo("stock", Types.INTEGER, "integer", 0, true, false, false),
                new ColumnInfo("disponible", Types.BOOLEAN, "boolean", 0, true, false, false)));
    }

    private void seedData() {
        List<Map<String, Object>> clientes = new ArrayList<>();
        clientes.add(row("id", 1, "nombre", "Ana Torres", "email", "ana@mail.com",
                "telefono", "099123456", "activo", Boolean.TRUE, "fecha_alta", Date.valueOf("2024-01-15")));
        clientes.add(row("id", 2, "nombre", "Bruno Diaz", "email", "bruno@mail.com",
                "telefono", "098765432", "activo", Boolean.FALSE, "fecha_alta", Date.valueOf("2024-03-02")));
        clientes.add(row("id", 3, "nombre", "Carla Ruiz", "email", "carla@mail.com",
                "telefono", "097111222", "activo", Boolean.TRUE, "fecha_alta", Date.valueOf("2024-05-20")));
        tables.put("clientes", clientes);

        List<Map<String, Object>> productos = new ArrayList<>();
        productos.add(row("id", 1, "nombre", "Cafe molido", "precio", new BigDecimal("250.50"),
                "stock", 40, "disponible", Boolean.TRUE));
        productos.add(row("id", 2, "nombre", "Te verde", "precio", new BigDecimal("180.00"),
                "stock", 0, "disponible", Boolean.FALSE));
        productos.add(row("id", 3, "nombre", "Azucar", "precio", new BigDecimal("90.75"),
                "stock", 120, "disponible", Boolean.TRUE));
        tables.put("productos", productos);
    }

    private static Map<String, Object> row(Object... pairs) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            row.put((String) pairs[i], pairs[i + 1]);
        }
        return row;
    }

    @Override
    public void connect() {
        connected = true;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public List<String> getTableNames() {
        return new ArrayList<>(schema.keySet());
    }

    @Override
    public List<ColumnInfo> getColumns(String table) {
        return new ArrayList<>(schema.getOrDefault(table, List.of()));
    }

    @Override
    public List<Map<String, Object>> fetchRows(String table, int limit, int offset) {
        List<Map<String, Object>> all = tables.getOrDefault(table, List.of());
        int from = Math.min(Math.max(0, offset), all.size());
        int to = Math.min(all.size(), from + Math.max(0, limit));
        return new ArrayList<>(all.subList(from, to));
    }

    @Override
    public long countRows(String table) {
        return tables.getOrDefault(table, List.of()).size();
    }

    @Override
    public void insert(String table, Map<String, Object> values) {
        List<ColumnInfo> columns = schema.getOrDefault(table, List.of());
        Map<String, Object> row = new LinkedHashMap<>();
        for (ColumnInfo column : columns) {
            Object value = values.get(column.name());
            if (column.autoIncrement() && value == null) {
                value = nextId(table, column.name());
            }
            row.put(column.name(), value);
        }
        tables.computeIfAbsent(table, k -> new ArrayList<>()).add(row);
    }

    @Override
    public void update(String table, Map<String, Object> values, Map<String, Object> primaryKey) {
        for (Map<String, Object> row : tables.getOrDefault(table, List.of())) {
            if (matches(row, primaryKey)) {
                row.putAll(values);
                return;
            }
        }
    }

    @Override
    public void delete(String table, Map<String, Object> primaryKey) {
        tables.getOrDefault(table, List.of()).removeIf(row -> matches(row, primaryKey));
    }

    @Override
    public void close() {
        connected = false;
    }

    private int nextId(String table, String column) {
        int max = 0;
        for (Map<String, Object> row : tables.getOrDefault(table, List.of())) {
            Object value = row.get(column);
            if (value instanceof Number) {
                max = Math.max(max, ((Number) value).intValue());
            }
        }
        return max + 1;
    }

    private boolean matches(Map<String, Object> row, Map<String, Object> key) {
        for (Map.Entry<String, Object> entry : key.entrySet()) {
            if (!valueEquals(row.get(entry.getKey()), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private boolean valueEquals(Object a, Object b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a instanceof Number && b instanceof Number) {
            return new BigDecimal(a.toString()).compareTo(new BigDecimal(b.toString())) == 0;
        }
        return a.equals(b);
    }
}
