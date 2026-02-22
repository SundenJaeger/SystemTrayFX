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
import systemtrayfx.core.TrayCheckMenuItem;
import systemtrayfx.core.TrayMenuItem;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;

public class TrayCheckMenuItemDemoController extends BaseTrayMenuItemDemoController {
    @FXML
    private ToggleButton clickMeButton;

    public TrayCheckMenuItemDemoController(SystemTrayFX systemTrayFX) {
        super(systemTrayFX);
    }

    @Override
    protected TrayMenuItem createMenuItem() {
        TrayCheckMenuItem trayCheckMenuItem = new TrayCheckMenuItem("Sample TrayCheckMenuItem");
        trayCheckMenuItem.selectedProperty().bindBidirectional(clickMeButton.selectedProperty());

        return trayCheckMenuItem;
    }
}
