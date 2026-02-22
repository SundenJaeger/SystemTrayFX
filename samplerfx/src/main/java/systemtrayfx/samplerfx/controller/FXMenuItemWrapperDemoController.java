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

import systemtrayfx.core.FXMenuItemWrapper;
import systemtrayfx.core.SystemTrayFX;
import systemtrayfx.core.TrayMenuItem;
import javafx.fxml.FXML;
import javafx.scene.control.MenuBar;

public class FXMenuItemWrapperDemoController extends BaseTrayMenuItemDemoController {
    @FXML
    private MenuBar menuBar;

    public FXMenuItemWrapperDemoController(SystemTrayFX systemTrayFX) {
        super(systemTrayFX);
    }

    @FXML
    @Override
    protected void initialize() {
        menuBar.getMenus().forEach(menu -> systemTrayFX.addEntry(new FXMenuItemWrapper(menu)));
    }

    @Override
    protected TrayMenuItem createMenuItem() {
        return null;
    }
}
