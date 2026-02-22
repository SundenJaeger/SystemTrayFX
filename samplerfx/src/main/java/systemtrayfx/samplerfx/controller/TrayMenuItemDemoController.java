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

package systemtrayfx.samplerfx.controller;

import systemtrayfx.core.SystemTrayFX;
import systemtrayfx.core.TrayMenuItem;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class TrayMenuItemDemoController extends BaseTrayMenuItemDemoController {
    private final IntegerProperty numberOfClicks = new SimpleIntegerProperty(0);

    @FXML
    private Label numberOfClicksLabel;

    public TrayMenuItemDemoController(SystemTrayFX systemTrayFX) {
        super(systemTrayFX);
    }

    @FXML
    protected void initialize() {
        super.initialize();
        numberOfClicksLabel.textProperty().bind(numberOfClicks.asString());
    }

    @Override
    protected TrayMenuItem createMenuItem() {
        TrayMenuItem trayMenuItem = new TrayMenuItem("Sample TrayMenuItem");
        trayMenuItem.setOnAction(event -> numberOfClicks.set(numberOfClicks.get() + 1));

        return trayMenuItem;
    }
}
