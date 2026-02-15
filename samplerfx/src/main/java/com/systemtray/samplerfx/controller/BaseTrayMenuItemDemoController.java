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
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public abstract class BaseTrayMenuItemDemoController extends BaseDemoController {
    private TrayMenuItem trayMenuItem;

    @FXML
    protected CheckBox isDisabledCheckBox;

    public BaseTrayMenuItemDemoController(SystemTrayFX systemTrayFX) {
        super(systemTrayFX);
    }

    protected abstract TrayMenuItem createMenuItem();

    @FXML
    @Override
    protected void initialize() {
        textPropertyTextField.disableProperty().bind(enableTextPropertyCheckBox.selectedProperty().not());
        chooseImageButton.disableProperty().bind(enableImagePropertyCheckBox.selectedProperty().not());

        addTrayMenuItem();
    }

    @FXML
    @Override
    protected void chooseImage() {
        Stage stage = (Stage) chooseImageButton.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image files", "*.jpg;*.jpeg;*.png"));

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            trayMenuItem.setImage(new Image(file.toURI().toString()));
        }
    }

    private void addTrayMenuItem() {
        trayMenuItem = createMenuItem();
        trayMenuItem.disableProperty().bind(isDisabledCheckBox.selectedProperty());
        trayMenuItem.textProperty().bind(textPropertyTextField.textProperty());

        systemTrayFX.addEntry(trayMenuItem);
    }
}
