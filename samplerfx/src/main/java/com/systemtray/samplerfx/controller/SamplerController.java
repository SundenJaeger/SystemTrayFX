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
import com.systemtray.samplerfx.enums.View;
import com.systemtray.samplerfx.model.Category;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SamplerController {
    private static final Map<View, TreeItem<Category>> TREE_ITEM = new EnumMap<>(View.class);
    private static final Map<View, Node> NODE_CACHE = new EnumMap<>(View.class);

    private final SystemTrayFX systemTrayFX;

    @FXML
    private BorderPane borderPane;
    @FXML
    private TreeView<Category> treeView;

    public SamplerController(SystemTrayFX systemTrayFX) {
        this.systemTrayFX = systemTrayFX;
    }

    @FXML
    private void initialize() {
        initCategories();
    }

    public void navigateTo(View view) {
        treeView.getSelectionModel().select(TREE_ITEM.get(view));
    }

    private void initCategories() {
        TreeItem<Category> hidden = new TreeItem<>(new Category("Hidden", null));

        TreeItem<Category> home = createTreeItem("Home", View.HOME);
        TreeItem<Category> systemTray = createTreeItem("System tray", View.SYSTEM_TRAY_DEMO);

        TreeItem<Category> menuItems = createTreeItem("Menu Items", View.MENU_ITEMS);
        menuItems.getChildren().addAll(List.of(
                createTreeItem("TrayMenuItem", View.TRAY_MENU_ITEM_DEMO),
                createTreeItem("TrayExitMenuItem", View.TRAY_EXIT_MENU_ITEM_DEMO)
        ));

        hidden.getChildren().addAll(List.of(
                home,
                systemTray,
                menuItems
        ));

        treeView.setRoot(hidden);
        treeView.setShowRoot(false);

        treeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.getValue().view() != null) {
                borderPane.setCenter(loadNode2(newValue.getValue().view()));
            }
        });

        treeView.getSelectionModel().select(home);
    }

    private Node loadNode2(View view) {
        return NODE_CACHE.computeIfAbsent(view, v -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(v.getFxml()));
                loader.setControllerFactory(param -> {
                    if (param == SystemTrayDemoController.class) {
                        return new SystemTrayDemoController(systemTrayFX);
                    } else if (param == MenuItemsController.class) {
                        return new MenuItemsController(this);
                    } else if (param == TrayMenuItemDemoController.class) {
                        return new TrayMenuItemDemoController(systemTrayFX);
                    } else if (param == TrayExitMenuItemDemoController.class) {
                        return new TrayExitMenuItemDemoController(systemTrayFX);
                    } else {
                        try {
                            return param.getConstructor().newInstance();
                        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException |
                                 IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                return loader.load();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private TreeItem<Category> createTreeItem(String title, View view) {
        return TREE_ITEM.computeIfAbsent(view, v -> new TreeItem<>(new Category(title, v)));
    }
}
