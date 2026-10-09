package com.proyecto1.crud.db;

import java.sql.SQLException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        try (Scanner scanner = new Scanner(System.in)) {

            System.out.println("Tablas disponibles:");
            DatabaseMetadataService.listTables();

            System.out.print("\nEscribe el nombre de la tabla que deseas consultar: ");
            String tableName = scanner.nextLine().trim();

            System.out.println("\nColumnas:");
            DatabaseMetadataService.listColumns(tableName);

            System.out.println("\nClave primaria:");
            DatabaseMetadataService.listPrimaryKeys(tableName);

            System.out.println("\nRegistros:");
            DynamicQueryService.selectAll(tableName);

        } catch (SQLException e) {
            System.out.println("Error al consultar la base de datos.");
            e.printStackTrace();
        }
    }
}