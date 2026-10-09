package com.proyecto1.crud;

import com.proyecto1.crud.controller.MainController;
import com.proyecto1.crud.service.DatabaseService;
import com.proyecto1.crud.service.impl.PostgresDatabaseService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        
        DatabaseService service = new PostgresDatabaseService();

        MainController controller = new MainController(service);

        Scene scene = new Scene(controller.createView(), 1150, 680);
        URL css = getClass().getResource("/styles.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setTitle("CRUD Dinamico - PostgreSQL");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.show();

        try {
            service.connect();
            controller.loadTables();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
