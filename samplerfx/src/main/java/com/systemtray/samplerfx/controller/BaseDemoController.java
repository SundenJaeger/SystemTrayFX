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
import com.systemtray.core.TrayMenuItem;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public abstract class BaseDemoController {
    protected final SystemTrayFX systemTrayFX;

    @FXML
    protected CheckBox isMinimizeToTrayCheckBox;
    @FXML
    protected CheckBox enableTextPropertyCheckBox;
    @FXML
    protected TextField textPropertyTextField;
    @FXML
    protected CheckBox enableImagePropertyCheckBox;
    @FXML
    protected Button chooseImageButton;

    public BaseDemoController(SystemTrayFX systemTrayFX) {
        this.systemTrayFX = systemTrayFX;
    }

    @FXML
    protected void initialize() {
        systemTrayFX.minimizeToTrayProperty().bind(isMinimizeToTrayCheckBox.selectedProperty());
        systemTrayFX.titleProperty().bind(textPropertyTextField.textProperty());

        textPropertyTextField.disableProperty().bind(enableTextPropertyCheckBox.selectedProperty().not());
        chooseImageButton.disableProperty().bind(enableImagePropertyCheckBox.selectedProperty().not());
    }

    @FXML
    protected void chooseImage() {
        Stage stage = (Stage) chooseImageButton.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image files", "*.jpg;*.jpeg;*.png"));

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            systemTrayFX.setImage(new Image(file.toURI().toString()));
        }
    }
}
