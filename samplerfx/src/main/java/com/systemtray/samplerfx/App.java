/*
 * Copyright (C) 2026 Rentoki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.systemtray.samplerfx;

import com.systemtray.core.SystemTrayFX;
import com.systemtray.samplerfx.controller.SamplerController;
import com.systemtray.samplerfx.enums.View;
import com.systemtray.samplerfx.model.Category;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class App extends Application {
    private SamplerController controller;

    @Override
    public void start(Stage stage) throws IOException {
        Image image = new Image(Objects.requireNonNull(App.class.getResource("media/default-icon.png")).toExternalForm());
        SystemTrayFX systemTrayFX = new SystemTrayFX(stage, "SystemTrayFX Sampler", image);

        FXMLLoader loader = new FXMLLoader(App.class.getResource(View.SAMPLER.getFxml()));
        Scene scene = new Scene(loader.load(), 900, 480);
        controller = loader.getController();

        stage.setTitle("SystemTrayFX Sampler");
        stage.setScene(scene);
        stage.show();

        initCategories();

    }

    private void initCategories() {
        TreeItem<Category> hidden = new TreeItem<>(new Category("Hidden", null));

        TreeItem<Category> home = new TreeItem<>(new Category("Home", View.HOME));

        TreeItem<Category> systemTray = new TreeItem<>(new Category("System tray", View.SYSTEM_TRAY_DEMO));

        hidden.getChildren().addAll(List.of(
                home,
                systemTray
        ));

        controller.getTreeView().setRoot(hidden);
        controller.getTreeView().setShowRoot(false);

        controller.getTreeView().getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.getValue().view() != null) {
                controller.getBorderPane().setCenter(loadNode2(newValue.getValue().view().getFxml()));
            }
        });
    }

    private Node loadNode2(String fxml) {
        try {
            return FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxml)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void main(String[] args) {
        launch(args);
    }
}