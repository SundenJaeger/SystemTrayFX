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

package com.systemtray.samplerfx;

import com.systemtray.core.*;
import com.systemtray.samplerfx.controller.SamplerController;
import com.systemtray.samplerfx.enums.View;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        Image image = new Image(Objects.requireNonNull(App.class.getResource("media/default-icon.png")).toExternalForm());

        SystemTrayFX systemTrayFX = new SystemTrayFX(stage, "SystemTrayFX Sampler", image);

        FXMLLoader loader = new FXMLLoader(App.class.getResource(View.SAMPLER.getFxml()));
        loader.setControllerFactory(param -> {
            if (param == SamplerController.class) {
                return new SamplerController(systemTrayFX);
            } else {
                try {
                    return param.getConstructor().newInstance();
                } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                         NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        Scene scene = new Scene(loader.load(), 900, 480);

        stage.setTitle("SystemTrayFX Sampler");
        stage.setScene(scene);
        stage.show();
    }


    static void main(String[] args) {
        launch(args);
    }
}