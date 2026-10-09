package com.proyecto1.crud.ui;

import com.proyecto1.crud.service.model.ColumnInfo;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Vista de datos dinamica: selector de tabla, busqueda, paginacion y una
 * {@link TableView} cuyas columnas se construyen a partir de la lista de
 * {@link ColumnInfo}. No conoce la implementacion de la base de datos.
 */
public class DynamicTableView extends BorderPane {

    private final ComboBox<String> tableSelector = new ComboBox<>();
    private final TextField searchField = new TextField();
    private final Button refreshButton = new Button("Actualizar");
    private final Button addButton = new Button("Nuevo");
    private final Button deleteButton = new Button("Eliminar");
    private final Button prevButton = new Button("\u25C0 Anterior");
    private final Button nextButton = new Button("Siguiente \u25B6");
    private final Label pageLabel = new Label();

    private final TableView<Map<String, Object>> table = new TableView<>();
    private final ObservableList<Map<String, Object>> masterData = FXCollections.observableArrayList();
    private final FilteredList<Map<String, Object>> filteredData = new FilteredList<>(masterData, r -> true);

    private Consumer<String> onTableSelected = t -> { };
    private Runnable onRefresh = () -> { };
    private Runnable onAdd = () -> { };
    private Runnable onDelete = () -> { };
    private Runnable onPrev = () -> { };
    private Runnable onNext = () -> { };
    private Consumer<Map<String, Object>> onRowSelected = r -> { };

    public DynamicTableView() {
        getStyleClass().add("table-panel");
        setPadding(new Insets(12));
        buildToolbar();
        buildTable();
    }

    private void buildToolbar() {
        Label tableLabel = new Label("Tabla:");
        tableSelector.setPrefWidth(180);
        tableSelector.setPromptText("Selecciona una tabla");
        tableSelector.setOnAction(e -> {
            String value = tableSelector.getValue();
            if (value != null) {
                onTableSelected.accept(value);
            }
        });

        searchField.setPromptText("Buscar en la pagina...");
        searchField.setPrefWidth(200);
        searchField.textProperty().addListener((obs, old, text) -> applyFilter(text));

        refreshButton.setOnAction(e -> onRefresh.run());
        addButton.setOnAction(e -> onAdd.run());
        deleteButton.setOnAction(e -> onDelete.run());
        deleteButton.disableProperty().bind(
                table.getSelectionModel().selectedItemProperty().isNull());

        prevButton.setOnAction(e -> onPrev.run());
        nextButton.setOnAction(e -> onNext.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(8, tableLabel, tableSelector, searchField,
                refreshButton, addButton, deleteButton, spacer,
                prevButton, pageLabel, nextButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 10, 0));
        toolbar.getStyleClass().add("table-toolbar");
        setTop(toolbar);
    }

    private void buildTable() {
        table.setItems(filteredData);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, row) -> onRowSelected.accept(row));
        Label placeholder = new Label("Selecciona una tabla para ver sus datos");
        table.setPlaceholder(placeholder);
        setCenter(table);
    }

    /** Reconstruye las columnas de la tabla a partir de los metadatos. */
    public void setColumns(List<ColumnInfo> columns) {
        table.getColumns().clear();
        table.getSortOrder().clear();
        for (ColumnInfo column : columns) {
            TableColumn<Map<String, Object>, Object> tableColumn = new TableColumn<>(column.name());
            tableColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().get(column.name())));
            tableColumn.setCellFactory(c -> new ValueCell());
            tableColumn.setPrefWidth(preferredWidth(column));
            if (column.primaryKey()) {
                tableColumn.setStyle("-fx-font-weight: bold;");
            }
            table.getColumns().add(tableColumn);
        }
    }

    private double preferredWidth(ColumnInfo column) {
        switch (column.sqlType()) {
            case java.sql.Types.BOOLEAN:
            case java.sql.Types.BIT:
                return 80;
            case java.sql.Types.DATE:
            case java.sql.Types.TIME:
            case java.sql.Types.TIMESTAMP:
                return 140;
            default:
                return Math.max(100, Math.min(220, column.name().length() * 12 + 60));
        }
    }

    public void setTables(List<String> tables) {
        tableSelector.setItems(FXCollections.observableArrayList(tables));
    }

    public void selectFirstTable() {
        if (!tableSelector.getItems().isEmpty()) {
            tableSelector.getSelectionModel().selectFirst();
        }
    }

    public String getSelectedTable() {
        return tableSelector.getValue();
    }

    public void setItems(List<Map<String, Object>> rows) {
        masterData.setAll(rows);
        applyFilter(searchField.getText());
    }

    public Map<String, Object> getSelectedRow() {
        return table.getSelectionModel().getSelectedItem();
    }

    public void clearSelection() {
        table.getSelectionModel().clearSelection();
    }

    public void setPageInfo(int currentPage, int totalPages, long totalRows) {
        pageLabel.setText(String.format("Pagina %d de %d  (%d registros)", currentPage, totalPages, totalRows));
        prevButton.setDisable(currentPage <= 1);
        nextButton.setDisable(currentPage >= totalPages);
    }

    private void applyFilter(String text) {
        if (text == null || text.isBlank()) {
            filteredData.setPredicate(r -> true);
            return;
        }
        String needle = text.toLowerCase(Locale.ROOT).trim();
        filteredData.setPredicate(row -> row.values().stream()
                .filter(v -> v != null)
                .anyMatch(v -> String.valueOf(v).toLowerCase(Locale.ROOT).contains(needle)));
    }

    public void setOnTableSelected(Consumer<String> handler) {
        this.onTableSelected = handler;
    }

    public void setOnRefresh(Runnable handler) {
        this.onRefresh = handler;
    }

    public void setOnAdd(Runnable handler) {
        this.onAdd = handler;
    }

    public void setOnDelete(Runnable handler) {
        this.onDelete = handler;
    }

    public void setOnPrev(Runnable handler) {
        this.onPrev = handler;
    }

    public void setOnNext(Runnable handler) {
        this.onNext = handler;
    }

    public void setOnRowSelected(Consumer<Map<String, Object>> handler) {
        this.onRowSelected = handler;
    }

    /** Celda que muestra los valores nulos de forma diferenciada. */
    private static final class ValueCell extends TableCell<Map<String, Object>, Object> {
        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            getStyleClass().remove("null-cell");
            if (empty) {
                setText(null);
                return;
            }
            if (item == null) {
                setText("NULL");
                getStyleClass().add("null-cell");
            } else if (item instanceof byte[]) {
                setText("(" + ((byte[]) item).length + " bytes)");
            } else {
                setText(String.valueOf(item));
            }
        }
    }
}
