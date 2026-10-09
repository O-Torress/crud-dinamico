package com.proyecto1.crud.service.model;

import java.sql.Types;

/**
 * Metadatos de una columna de una tabla.
 *
 * <p>Es el contrato de datos que el backend debe construir (por ejemplo a partir
 * de {@link java.sql.DatabaseMetaData}) y que la interfaz usa para generar
 * dinamicamente tanto la tabla como el formulario.</p>
 *
 * @param name          nombre de la columna
 * @param sqlType       tipo JDBC, uno de los valores de {@link java.sql.Types}
 * @param typeName      nombre del tipo tal como lo reporta la base de datos (ej. "varchar")
 * @param size          tamano/ancho declarado (0 si no aplica)
 * @param nullable      true si la columna admite NULL
 * @param primaryKey    true si la columna forma parte de la llave primaria
 * @param autoIncrement true si la columna se autogenera (serial / identity)
 */
public record ColumnInfo(
        String name,
        int sqlType,
        String typeName,
        int size,
        boolean nullable,
        boolean primaryKey,
        boolean autoIncrement) {

    public boolean isBoolean() {
        return sqlType == Types.BOOLEAN || sqlType == Types.BIT;
    }

    public boolean isInteger() {
        switch (sqlType) {
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
                return true;
            default:
                return false;
        }
    }

    public boolean isDecimal() {
        return sqlType == Types.NUMERIC || sqlType == Types.DECIMAL;
    }

    public boolean isNumeric() {
        return isInteger() || isDecimal()
                || sqlType == Types.FLOAT || sqlType == Types.REAL || sqlType == Types.DOUBLE;
    }

    public boolean isTemporal() {
        switch (sqlType) {
            case Types.DATE:
            case Types.TIME:
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
            case Types.TIME_WITH_TIMEZONE:
                return true;
            default:
                return false;
        }
    }

    /** Indica si la columna debe capturarse obligatoriamente en el formulario. */
    public boolean isRequired() {
        return !nullable && !autoIncrement;
    }

    /** True si la columna puede usarse como parte de la llave para UPDATE/DELETE. */
    public boolean isKey() {
        return primaryKey;
    }
}
