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

import com.systemtray.core.Notification;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class NotificationsController {
    @FXML
    private void showInfoNotification() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText("This is an information type notification.");

        Notification.info("Information", "This is an information type notification.", event -> alert.show());
    }

    @FXML
    private void showWarnNotification() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText("This is a warning type notification.");

        Notification.warn("Warning", "This is a warning type notification.", event -> alert.show());
    }

    @FXML
    private void showErrorNotification() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText("This is an error type notification.");

        Notification.error("Error", "This is an error type notification.", event -> alert.show());
    }
}
