package com.basas.blackholesim.app;

import com.basas.blackholesim.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class BlackHoleSimApp extends Application {

    @Override
    public void start(Stage stage) {
        MainView view = new MainView();

        Scene scene = new Scene(view.getRoot(), 1180, 720);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
stage.setTitle("Black Hole Simulator (JavaFX)");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setMinWidth(980);
        stage.setMinHeight(620);
        stage.show();

        
        view.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
