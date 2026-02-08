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

import com.systemtray.core.TrayMenuItem;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class TrayMenuItemDemoController {
    private final IntegerProperty numberOfClicks = new SimpleIntegerProperty(0);

    private final TrayMenuItem trayMenuItem;

    @FXML
    private CheckBox isDisabledCheckBox;
    @FXML
    private CheckBox enableTextPropertyCheckBox;
    @FXML
    private TextField textPropertyTextField;
    @FXML
    private CheckBox enableImagePropertyCheckBox;
    @FXML
    private Label numberOfClicksLabel;
    @FXML
    private Button chooseImageButton;

    public TrayMenuItemDemoController(TrayMenuItem trayMenuItem) {
        this.trayMenuItem = trayMenuItem;
    }

    @FXML
    private void initialize() {
        trayMenuItem.setOnAction(event -> numberOfClicks.set(numberOfClicks.get() + 1));
        numberOfClicksLabel.textProperty().bind(numberOfClicks.asString());
        trayMenuItem.disableProperty().bind(isDisabledCheckBox.selectedProperty());
        textPropertyTextField.disableProperty().bind(enableTextPropertyCheckBox.selectedProperty().not());
        trayMenuItem.textProperty().bind(textPropertyTextField.textProperty());
        chooseImageButton.disableProperty().bind(enableImagePropertyCheckBox.selectedProperty().not());
    }

    @FXML
    private void chooseImage() {
        Stage stage = (Stage) chooseImageButton.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image files", "*.jpg;*.jpeg;*.png"));

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            trayMenuItem.setImage(new Image(file.toURI().toString()));
        }
    }
}
