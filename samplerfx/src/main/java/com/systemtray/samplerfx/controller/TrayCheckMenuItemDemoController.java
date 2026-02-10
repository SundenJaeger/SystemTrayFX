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
import com.systemtray.core.TrayCheckMenuItem;
import com.systemtray.core.TrayMenuItem;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ToggleButton;

public class TrayCheckMenuItemDemoController extends BaseTrayMenuItemDemoController {
    @FXML
    private CheckBox enableSelectedPropertyCheckBox;
    @FXML
    private ToggleButton clickMeButton;

    public TrayCheckMenuItemDemoController(SystemTrayFX systemTrayFX) {
        super(systemTrayFX);
    }

    @FXML
    @Override
    protected void initialize() {
        super.initialize();

        clickMeButton.disableProperty().bind(enableSelectedPropertyCheckBox.selectedProperty().not());
    }

    @Override
    protected TrayMenuItem createMenuItem() {
        TrayCheckMenuItem trayCheckMenuItem = new TrayCheckMenuItem("Sample TrayCheckMenuItem");
        trayCheckMenuItem.selectedProperty().bindBidirectional(clickMeButton.selectedProperty());

        return trayCheckMenuItem;
    }
}
