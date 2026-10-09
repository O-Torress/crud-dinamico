package com.proyecto1.crud.service;

import com.proyecto1.crud.service.model.ColumnInfo;

import java.util.List;
import java.util.Map;

/**
 * Contrato desacoplado entre la interfaz (JavaFX) y la capa de acceso a datos.
 *
 * <p>El backend de PostgreSQL debe implementar esta interfaz. La interfaz de
 * usuario solo conoce este contrato (y {@link ColumnInfo}), por lo que puede
 * conectarse a cualquier base de datos PostgreSQL sin cambiar su codigo.</p>
 *
 * <p>Convenciones:</p>
 * <ul>
 *   <li>{@code table} es el nombre de la tabla, opcionalmente "esquema.tabla".</li>
 *   <li>Cada fila es un {@code Map<columna, valor>} con los tipos originales del
 *       {@link java.sql.ResultSet} (usar {@code getObject}).</li>
 *   <li>Las llaves para UPDATE/DELETE son las columnas marcadas como
 *       {@link ColumnInfo#primaryKey()}.</li>
 * </ul>
 */
public interface DatabaseService extends AutoCloseable {

    /** Abre la conexion con la base de datos. */
    void connect() throws Exception;

    /** Devuelve true si la conexion esta activa. */
    boolean isConnected();

    /** Nombres de todas las tablas disponibles. */
    List<String> getTableNames() throws Exception;

    /** Metadatos de las columnas de una tabla. */
    List<ColumnInfo> getColumns(String table) throws Exception;

    /**
     * Obtiene una pagina de filas.
     *
     * @param table  nombre de la tabla
     * @param limit  maximo de filas a devolver
     * @param offset desplazamiento inicial
     */
    List<Map<String, Object>> fetchRows(String table, int limit, int offset) throws Exception;

    /** Cantidad total de filas de la tabla. */
    long countRows(String table) throws Exception;

    /** Inserta una fila con los valores indicados. */
    void insert(String table, Map<String, Object> values) throws Exception;

    /**
     * Actualiza la fila identificada por {@code primaryKey}.
     *
     * @param values valores nuevos (puede incluir o no las columnas llave)
     */
    void update(String table, Map<String, Object> values, Map<String, Object> primaryKey) throws Exception;

    /** Elimina la fila identificada por {@code primaryKey}. */
    void delete(String table, Map<String, Object> primaryKey) throws Exception;

    @Override
    void close() throws Exception;
}
