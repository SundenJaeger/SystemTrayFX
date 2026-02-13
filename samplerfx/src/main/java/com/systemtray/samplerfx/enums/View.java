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

package com.systemtray.samplerfx.enums;

public enum View {
    SAMPLER("sampler-view"),
    HOME("home-view"),
    SYSTEM_TRAY_DEMO("system-tray-demo-view"),
    MENU_ITEMS("menu-items-view"),
    TRAY_MENU_ITEM_DEMO("tray-menu-item-demo-view"),
    TRAY_EXIT_MENU_ITEM_DEMO("tray-exit-menu-item-demo-view"),
    TRAY_CHECK_MENU_ITEM_DEMO("tray-check-menu-item-demo-view"),
    TRAY_MENU_DEMO("tray-menu-demo-view");

    private static final String BASE_PATH = "/com/systemtray/samplerfx/views/";
    private final String fxml;

    View(String fxml) {
        this.fxml = fxml;
    }

    public String getFxml() {
        return BASE_PATH + fxml + ".fxml";
    }
}
