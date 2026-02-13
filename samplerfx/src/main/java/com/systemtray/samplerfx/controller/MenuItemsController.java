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

import com.systemtray.samplerfx.enums.View;
import javafx.fxml.FXML;

public class MenuItemsController {
    private final SamplerController samplerController;

    public MenuItemsController(SamplerController samplerController) {
        this.samplerController = samplerController;
    }

    @FXML
    private void toTrayMenuItem() {
        samplerController.navigateTo(View.TRAY_MENU_ITEM_DEMO);
    }

    @FXML
    private void toTrayExitMenuItem() {
        samplerController.navigateTo(View.TRAY_EXIT_MENU_ITEM_DEMO);
    }

    @FXML
    private void toTrayCheckMenuItem() {
        samplerController.navigateTo(View.TRAY_CHECK_MENU_ITEM_DEMO);
    }

    @FXML
    private void toTrayMenu() {
        samplerController.navigateTo(View.TRAY_MENU_DEMO);
    }

    @FXML
    private void toFXMenuItemWrapper() {

    }

    @FXML
    private void toSeparator() {

    }
}
