package com.proyecto1.crud.ui;

import com.proyecto1.crud.service.model.ColumnInfo;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DynamicFormView extends VBox {

    private final Label titleLabel = new Label("Nuevo registro");
    private final GridPane grid = new GridPane();
    private final Button saveButton = new Button("Guardar");
    private final Button clearButton = new Button("Limpiar");

    private final List<ColumnInfo> columns = new ArrayList<>();
    private final Map<String, Control> controls = new LinkedHashMap<>();

    private Runnable onSave = () -> { };
    private Runnable onClear = () -> { };

    public DynamicFormView() {
        getStyleClass().add("form-panel");
        setPadding(new Insets(15));
        setSpacing(12);
        setMinWidth(330);
        setPrefWidth(370);

        titleLabel.getStyleClass().add("form-title");
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(5, 0, 5, 0));

        saveButton.setDefaultButton(true);
        saveButton.getStyleClass().add("primary-button");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(saveButton, Priority.ALWAYS);
        clearButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(clearButton, Priority.ALWAYS);

        saveButton.setOnAction(e -> onSave.run());
        clearButton.setOnAction(e -> onClear.run());

        HBox buttons = new HBox(10, saveButton, clearButton);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(titleLabel, grid, buttons);
    }

    public void setColumns(List<ColumnInfo> cols) {
        columns.clear();
        controls.clear();
        grid.getChildren().clear();
        int row = 0;
        for (ColumnInfo column : cols) {
            columns.add(column);
            Label label = new Label(column.name() + (column.isRequired() ? " *" : ""));
            label.setTooltip(new javafx.scene.control.Tooltip(
                    column.typeName() + (column.primaryKey() ? " (PK)" : "")));
            Control control = createControl(column);
            control.setMaxWidth(Double.MAX_VALUE);
            controls.put(column.name(), control);
            grid.add(label, 0, row);
            grid.add(control, 1, row);
            GridPane.setHgrow(control, Priority.ALWAYS);
            row++;
        }
    }

    private Control createControl(ColumnInfo column) {
        if (column.isBoolean()) {
            ComboBox<Boolean> combo = new ComboBox<>();
            combo.getItems().addAll(Boolean.TRUE, Boolean.FALSE);
            combo.setButtonCell(booleanCell());
            combo.setCellFactory(list -> booleanCell());
            combo.setPromptText("(vacio)");
            return combo;
        }
        if (column.sqlType() == Types.DATE) {
            return new DatePicker();
        }
        if (column.sqlType() == Types.TIME || column.sqlType() == Types.TIME_WITH_TIMEZONE) {
            TextField field = new TextField();
            field.setPromptText("HH:mm:ss");
            return field;
        }
        if (column.sqlType() == Types.TIMESTAMP || column.sqlType() == Types.TIMESTAMP_WITH_TIMEZONE) {
            TextField field = new TextField();
            field.setPromptText("yyyy-MM-dd HH:mm:ss");
            return field;
        }
        if (column.sqlType() == Types.LONGVARCHAR || column.sqlType() == Types.LONGNVARCHAR
                || column.sqlType() == Types.CLOB || column.sqlType() == Types.NCLOB
                || column.size() > 255) {
            TextArea area = new TextArea();
            area.setPrefRowCount(3);
            area.setWrapText(true);
            return area;
        }
        TextField field = new TextField();
        if (column.isInteger()) {
            field.setPromptText("numero entero");
        } else if (column.isNumeric()) {
            field.setPromptText("numero decimal");
        }
        return field;
    }

    private ListCell<Boolean> booleanCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item ? "true" : "false"));
            }
        };
    }

    public Map<String, Object> getValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        for (ColumnInfo column : columns) {
            Control control = controls.get(column.name());
            boolean empty = isEmpty(control);
            if (empty && column.autoIncrement()) {
                continue;
            }
            values.put(column.name(), empty ? null : readControl(column, control));
        }
        return values;
    }

    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        for (ColumnInfo column : columns) {
            Control control = controls.get(column.name());
            if (column.isRequired() && isEmpty(control)) {
                errors.add("El campo '" + column.name() + "' es obligatorio.");
                continue;
            }
            if (!isEmpty(control)) {
                try {
                    readControl(column, control);
                } catch (RuntimeException ex) {
                    errors.add(ex.getMessage());
                }
            }
        }
        return errors;
    }

    public void setValues(Map<String, Object> row) {
        for (ColumnInfo column : columns) {
            writeControl(column, controls.get(column.name()), row.get(column.name()));
        }
    }

    public void clear() {
        for (int i = 0; i < columns.size(); i++) {
            ColumnInfo column = columns.get(i);
            Control control = controls.get(column.name());
            if (control instanceof TextInputControl) {
                ((TextInputControl) control).clear();
            } else if (control instanceof DatePicker) {
                ((DatePicker) control).setValue(null);
            } else if (control instanceof ComboBox) {
                ((ComboBox<?>) control).setValue(null);
            }
        }
    }

    public void setEditing(boolean editing) {
        titleLabel.setText(editing ? "Editar registro" : "Nuevo registro");
        saveButton.setText(editing ? "Actualizar" : "Guardar");
        for (ColumnInfo column : columns) {
            if (column.primaryKey() || column.autoIncrement()) {
                controls.get(column.name()).setDisable(editing);
            }
        }
    }

    public void setOnSave(Runnable handler) {
        this.onSave = handler;
    }

    public void setOnClear(Runnable handler) {
        this.onClear = handler;
    }

    private boolean isEmpty(Control control) {
        if (control instanceof TextInputControl) {
            return ((TextInputControl) control).getText() == null
                    || ((TextInputControl) control).getText().trim().isEmpty();
        }
        if (control instanceof DatePicker) {
            return ((DatePicker) control).getValue() == null;
        }
        if (control instanceof ComboBox) {
            return ((ComboBox<?>) control).getValue() == null;
        }
        return false;
    }

    private Object readControl(ColumnInfo column, Control control) {
        if (control instanceof ComboBox) {
            return ((ComboBox<?>) control).getValue();
        }
        if (control instanceof DatePicker) {
            LocalDate date = ((DatePicker) control).getValue();
            return date == null ? null : java.sql.Date.valueOf(date);
        }
        String text = ((TextInputControl) control).getText().trim();
        return parse(column, text);
    }

    private Object parse(ColumnInfo column, String text) {
        try {
            switch (column.sqlType()) {
                case Types.TINYINT:
                case Types.SMALLINT:
                case Types.INTEGER:
                    return Integer.valueOf(text);
                case Types.BIGINT:
                    return Long.valueOf(text);
                case Types.REAL:
                case Types.FLOAT:
                    return Float.valueOf(text);
                case Types.DOUBLE:
                    return Double.valueOf(text);
                case Types.NUMERIC:
                case Types.DECIMAL:
                    return new BigDecimal(text);
                case Types.BOOLEAN:
                case Types.BIT:
                    return Boolean.valueOf(text);
                case Types.DATE:
                    return java.sql.Date.valueOf(text.replace('/', '-'));
                case Types.TIME:
                case Types.TIME_WITH_TIMEZONE:
                    return java.sql.Time.valueOf(normalizeTime(text));
                case Types.TIMESTAMP:
                case Types.TIMESTAMP_WITH_TIMEZONE:
                    return Timestamp.valueOf(normalizeTimestamp(text));
                default:
                    return text;
            }
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(
                    "Valor invalido para '" + column.name() + "': " + text);
        }
    }

    private String normalizeTime(String text) {
        return text.length() == 5 ? text + ":00" : text;
    }

    private String normalizeTimestamp(String text) {
        String value = text.replace('/', '-').trim();
        if (value.length() <= 10) {
            value = value + " 00:00:00";
        } else if (value.length() == 16) {
            value = value + ":00";
        }
        return value;
    }

    private void writeControl(ColumnInfo column, Control control, Object value) {
        if (control instanceof ComboBox) {
            if (value == null) {
                ((ComboBox<?>) control).setValue(null);
            } else if (value instanceof Boolean) {
                ((ComboBox<Boolean>) control).setValue((Boolean) value);
            } else {
                ((ComboBox<Boolean>) control).setValue(Boolean.valueOf(String.valueOf(value)));
            }
            return;
        }
        if (control instanceof DatePicker) {
            ((DatePicker) control).setValue(toLocalDate(value));
            return;
        }
        ((TextInputControl) control).setText(value == null ? "" : String.valueOf(value));
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }
        if (value instanceof java.util.Date) {
            return new java.sql.Date(((java.util.Date) value).getTime()).toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }
}
