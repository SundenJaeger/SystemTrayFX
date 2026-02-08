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

package com.systemtray.samplerfx.controller;

import com.systemtray.core.SystemTrayFX;
import com.systemtray.samplerfx.enums.View;
import com.systemtray.samplerfx.model.Category;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;

public class SamplerController {
    private final SystemTrayFX systemTrayFX;

    @FXML
    private BorderPane borderPane;
    @FXML
    private TreeView<Category> treeView;

    public SamplerController(SystemTrayFX systemTrayFX) {
        this.systemTrayFX = systemTrayFX;
    }

    @FXML
    private void initialize() {
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

        treeView.setRoot(hidden);
        treeView.setShowRoot(false);

        treeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.getValue().view() != null) {
                borderPane.setCenter(loadNode2(newValue.getValue().view().getFxml()));
            }
        });

        treeView.getSelectionModel().select(home);
    }

    private Node loadNode2(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            loader.setControllerFactory(param -> {
                if (param == SystemTrayDemoController.class) {
                    return new SystemTrayDemoController(systemTrayFX);
                } else {
                    try {
                        return param.getConstructor().newInstance();
                    } catch (InvocationTargetException | NoSuchMethodException | InstantiationException |
                             IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            return loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
