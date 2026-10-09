package com.proyecto1.crud.controller;

import com.proyecto1.crud.service.DatabaseService;
import com.proyecto1.crud.service.model.ColumnInfo;
import com.proyecto1.crud.ui.DynamicFormView;
import com.proyecto1.crud.ui.DynamicTableView;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainController {

    private static final int PAGE_SIZE = 25;

    private final DatabaseService service;
    private final DynamicTableView tableView = new DynamicTableView();
    private final DynamicFormView formView = new DynamicFormView();

    private List<ColumnInfo> columns = new ArrayList<>();
    private List<String> primaryKeyColumns = new ArrayList<>();
    private String currentTable;
    private int currentPage = 0;
    private Map<String, Object> editingRow;

    public MainController(DatabaseService service) {
        this.service = service;
        wireEvents();
    }

    public Parent createView() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setCenter(tableView);
        root.setRight(formView);
        return root;
    }

    private void wireEvents() {
        tableView.setOnTableSelected(this::onTableSelected);
        tableView.setOnRefresh(this::reloadPage);
        tableView.setOnAdd(this::startInsert);
        tableView.setOnDelete(this::deleteSelected);
        tableView.setOnPrev(() -> changePage(-1));
        tableView.setOnNext(() -> changePage(1));
        tableView.setOnRowSelected(this::onRowSelected);
        formView.setOnSave(this::save);
        formView.setOnClear(this::startInsert);
    }

    public void loadTables() {
        try {
            List<String> tables = service.getTableNames();
            tableView.setTables(tables);
            if (!tables.isEmpty()) {
                tableView.selectFirstTable();
            }
        } catch (Exception e) {
            showError("No se pudieron cargar las tablas", e);
        }
    }

    private void onTableSelected(String table) {
        this.currentTable = table;
        try {
            columns = service.getColumns(table);
            primaryKeyColumns = columns.stream()
                    .filter(ColumnInfo::primaryKey)
                    .map(ColumnInfo::name)
                    .collect(Collectors.toList());
            tableView.setColumns(columns);
            formView.setColumns(columns);
            startInsert();
            currentPage = 0;
            reloadPage();
        } catch (Exception e) {
            showError("No se pudo cargar la tabla " + table, e);
        }
    }

    private void reloadPage() {
        if (currentTable == null) {
            return;
        }
        try {
            long totalRows = service.countRows(currentTable);
            List<Map<String, Object>> rows = service.fetchRows(currentTable, PAGE_SIZE, currentPage * PAGE_SIZE);
            tableView.setItems(rows);
            int totalPages = Math.max(1, (int) Math.ceil(totalRows / (double) PAGE_SIZE));
            if (currentPage >= totalPages) {
                currentPage = totalPages - 1;
                reloadPage();
                return;
            }
            tableView.setPageInfo(currentPage + 1, totalPages, totalRows);
        } catch (Exception e) {
            showError("No se pudieron cargar los datos", e);
        }
    }

    private void changePage(int delta) {
        int target = currentPage + delta;
        if (target < 0) {
            return;
        }
        currentPage = target;
        reloadPage();
    }

    private void onRowSelected(Map<String, Object> row) {
        if (row == null) {
            return;
        }
        editingRow = row;
        formView.setValues(row);
        formView.setEditing(true);
    }

    private void startInsert() {
        editingRow = null;
        tableView.clearSelection();
        formView.clear();
        formView.setEditing(false);
    }

    private void save() {
        if (currentTable == null) {
            return;
        }
        List<String> errors = formView.validate();
        if (!errors.isEmpty()) {
            showWarning("Datos invalidos", String.join("\n", errors));
            return;
        }
        Map<String, Object> values;
        try {
            values = formView.getValues();
        } catch (RuntimeException ex) {
            showWarning("Datos invalidos", ex.getMessage());
            return;
        }
        boolean insert = editingRow == null;
        try {
            if (insert) {
                service.insert(currentTable, values);
            } else {
                service.update(currentTable, values, extractKey(editingRow));
            }
            startInsert();
            reloadPage();
        } catch (Exception e) {
            showError(insert ? "No se pudo insertar el registro" : "No se pudo actualizar el registro", e);
        }
    }

    private void deleteSelected() {
        Map<String, Object> row = tableView.getSelectedRow();
        if (row == null || currentTable == null) {
            showWarning("Sin seleccion", "Selecciona una fila para eliminar.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Deseas eliminar el registro seleccionado?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.setTitle("Confirmar eliminacion");
        confirm.showAndWait()
                .filter(button -> button == ButtonType.YES)
                .ifPresent(button -> {
                    try {
                        service.delete(currentTable, extractKey(row));
                        startInsert();
                        reloadPage();
                    } catch (Exception e) {
                        showError("No se pudo eliminar el registro", e);
                    }
                });
    }

    private Map<String, Object> extractKey(Map<String, Object> row) {
        List<String> keys = primaryKeyColumns.isEmpty() ? columns.stream()
                .map(ColumnInfo::name).collect(Collectors.toList()) : primaryKeyColumns;
        Map<String, Object> key = new LinkedHashMap<>();
        for (String name : keys) {
            key.put(name, row.get(name));
        }
        return key;
    }

    private void showError(String message, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(message);
        alert.setContentText(e.getMessage() == null ? e.toString() : e.getMessage());
        alert.showAndWait();
    }

    private void showWarning(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Aviso");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
