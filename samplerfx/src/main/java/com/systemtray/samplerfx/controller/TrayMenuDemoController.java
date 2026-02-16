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

import com.systemtray.core.*;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;

public class TrayMenuDemoController extends BaseTrayMenuItemDemoController {
    private TrayMenu trayMenu;

    @FXML
    private CheckBox enableAddMenuItemsCheckBox;
    @FXML
    private HBox addMenuItemsContainer;

    public TrayMenuDemoController(SystemTrayFX systemTrayFX) {
        super(systemTrayFX);
    }

    @FXML
    @Override
    protected void initialize() {
        super.initialize();

        addMenuItemsContainer.disableProperty().bind(enableAddMenuItemsCheckBox.selectedProperty().not());
    }

    @Override
    protected TrayMenuItem createMenuItem() {
        return trayMenu = new TrayMenu("Sample TrayMenu");
    }

    @FXML
    private void onTrayMenuItem() {
        if (trayMenu != null) {
            trayMenu.getItems().add(new TrayMenuItem("TrayMenuItem"));
        }
    }

    @FXML
    private void onTrayCheckMenuItem() {
        if (trayMenu != null) {
            trayMenu.getItems().add(new TrayCheckMenuItem("TrayCheckMenuItem"));
        }
    }

    @FXML
    private void onTrayMenu() {
        if (trayMenu != null) {
            trayMenu.getItems().add(new TrayMenu("TrayMenu"));
        }
    }

    @FXML
    private void onSeparator() {
        if (trayMenu != null) {
            trayMenu.getItems().add(new Separator());
        }
    }

    @FXML
    private void removeAllItems() {
        if (trayMenu != null) {
            trayMenu.getItems().clear();
        }
    }
}
