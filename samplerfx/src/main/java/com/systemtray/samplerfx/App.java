package com.systemtray.samplerfx;

import com.systemtray.core.SystemTrayFX;
import com.systemtray.core.TrayExitMenuItem;
import com.systemtray.core.TrayMenuItem;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("primary.fxml"));
        Scene scene = new Scene(loader.load(), 640, 480);

        PrimaryController controller = loader.getController();

        stage.setScene(scene);
        stage.show();

        Image image = new Image(Objects.requireNonNull(App.class.getResource("aa.png")).toExternalForm());

        TrayMenuItem menuItem = new TrayMenuItem("Hello World");
        menuItem.setImage(image);
        menuItem.setOnAction(event -> System.out.println("Bomba"));
        menuItem.textProperty().bind(controller.textField.textProperty());

        SystemTrayFX systemTrayFX = new SystemTrayFX(stage, "Hello World", image, true);
        systemTrayFX.addEntry(menuItem, new TrayExitMenuItem());
        systemTrayFX.show();
    }

    static void main(String[] args) {
        launch(args);
    }
}