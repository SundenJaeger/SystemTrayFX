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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

/**
 * A visual separator line for organizing menu items into groups.
 *
 * <p><strong>Usage Examples:</strong>
 * <pre>{@code
 * // Basic usage - separating menu sections
 * tray.addEntry(
 *     new TrayMenuItem("Open"),
 *     new TrayMenuItem("Save"),
 *     new Separator(),
 *     new TrayMenuItem("Settings"),
 *     new Separator(),
 *     new TrayExitMenuItem("Exit")
 * );
 *
 * // Using getItems() approach
 * tray.getItems().addAll(
 *     new TrayMenuItem("File Operations"),
 *     new TrayMenuItem("Edit"),
 *     new Separator(),
 *     new TrayMenuItem("View"),
 *     new Separator(),
 *     new TrayExitMenuItem()
 * );
 *
 * // In a submenu
 * TrayMenu fileMenu = new TrayMenu("File");
 * fileMenu.getItems().addAll(
 *     new TrayMenuItem("New"),
 *     new TrayMenuItem("Open"),
 *     new Separator(),
 *     new TrayMenuItem("Save"),
 *     new TrayMenuItem("Save As"),
 *     new Separator(),
 *     new TrayMenuItem("Exit")
 * );
 * }</pre>
 *
 * @see TrayMenuItem
 * @see TrayCheckMenuItem
 * @see TrayExitMenuItem
 * @see FXMenuItemWrapper
 * @see TrayMenu
 */
public class Separator extends TrayMenuItem {

    /**
     * Creates a separator menu item.
     *
     * <p>The separator displays as a horizontal line and has no text or icon.
     */
    public Separator() {
        super("");
    }

    /**
     * Returns the SWT style flags for a separator menu item.
     *
     * @return SWT.SEPARATOR style constant
     */
    @Override
    protected int getSWTStyle() {
        return SWT.SEPARATOR;
    }
}
