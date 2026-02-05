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

package com.systemtray.core;

/**
 * Interface defining the core contract for system tray implementations.
 */
interface ISystemTray {

    /**
     * Adds one or more menu items to the system tray context menu.
     *
     * <p>This method supports adding multiple items at once using varargs.
     * Items are displayed in the order they are added to the menu.
     *
     * <p><strong>Example - Adding single item:</strong>
     * <pre>{@code
     * tray.addEntry(new TrayMenuItem("Open", e -> stage.show()));
     * }</pre>
     *
     * <p><strong>Example - Adding multiple items:</strong>
     * <pre>{@code
     * tray.addEntry(
     *     new TrayMenuItem("Open"),
     *     new TrayMenuItem("Settings"),
     *     new TraySeparatorMenuItem(),
     *     new TrayExitMenuItem()
     * );
     * }</pre>
     *
     * <p><strong>Example - Adding items with icons:</strong>
     * <pre>{@code
     * Image openIcon = new Image("/icons/open.png");
     * Image settingsIcon = new Image("/icons/settings.png");
     *
     * tray.addEntry(
     *     new TrayMenuItem("Open", openIcon, e -> stage.show()),
     *     new TrayMenuItem("Settings", settingsIcon, e -> openSettings())
     * );
     * }</pre>
     *
     * @param items the menu items to add (varargs - can pass one or more items)
     * @throws NullPointerException if items array is null
     * @see TrayMenuItem
     * @see TrayExitMenuItem
     * @see Separator
     * @see TrayCheckMenuItem
     * @see TrayMenu
     * @see FXMenuItemWrapper
     */
    void addEntry(TrayMenuItem... items);

    /**
     * Disposes of all resources associated with the system tray.
     *
     *
     * <p><strong>When to Call:</strong> This method should be called when:
     * <ul>
     *   <li>The application is closing or shutting down</li>
     *   <li>The system tray is no longer needed</li>
     *   <li>You need to recreate the tray with different settings</li>
     * </ul>
     *
     * <p><strong>Example - Normal cleanup:</strong>
     * <pre>{@code
     * // When application exits normally
     * SystemTrayFX tray = new SystemTrayFX(stage, "My App", icon, false);
     * // ... use tray ...
     * tray.dispose(); // Called automatically on window close
     * }</pre>
     *
     * <p><strong>Example - Manual cleanup with minimize to tray:</strong>
     * <pre>{@code
     * SystemTrayFX tray = new SystemTrayFX(stage, "My App", icon, true);
     * tray.addEntry(new TrayExitMenuItem()); // Calls dispose() internally
     * }</pre>
     *
     * <p><strong>Example - Shutdown hook:</strong>
     * <pre>{@code
     * SystemTrayFX tray = new SystemTrayFX(stage, "My App", icon, true);
     * Runtime.getRuntime().addShutdownHook(new Thread(() -> {
     *     tray.dispose();
     * }));
     * }</pre>
     *
     * <p><strong>Important:</strong> After calling dispose, the system tray instance
     * cannot be reused. Create a new instance if you need to show the tray again.
     *
     * @see TrayExitMenuItem
     */
    void dispose();
}
