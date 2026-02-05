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

import javafx.application.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.swt.widgets.TrayItem;

/**
 * Internal handler for displaying notifications from the system tray.
 *
 * <p>This class manages SWT balloon tooltips that appear near the system tray icon.
 * It handles notification lifecycle, icon types, click actions, and auto-hide timeouts.
 *
 * <p>The notification system optimizes tooltip reuse by only recreating when the
 * icon type changes, improving performance for frequent notifications.
 *
 * @see NotificationType
 * @see Notification
 * @see SystemTrayFX
 */
final class TrayNotification {

    /* ---------------- SWT Components ---------------- */

    private final Display display;
    private final Shell shell;
    private final TrayItem trayItem;

    /* ---------------- Notification Components ---------------- */

    private ToolTip toolTip;
    private NotificationType currentIcon = NotificationType.NONE;
    private SelectionAdapter selectionAdapter;

    /* ---------------- Notification Components ---------------- */

    /**
     * Creates a new tray notification handler.
     *
     * @param display the SWT display
     * @param shell the parent shell for tooltips
     * @param trayItem the tray icon to attach notifications to
     */
    TrayNotification(Display display, Shell shell, TrayItem trayItem) {
        this.display = display;
        this.shell = shell;
        this.trayItem = trayItem;
    }

    /* ---------------- Internals ---------------- */

    /**
     * Displays a notification with the specified parameters.
     *
     * <p>This method executes asynchronously on the SWT thread. The tooltip is reused
     * if the icon type matches the previous notification, otherwise it's recreated.
     *
     * <p>If an action is provided, the notification becomes clickable. Clicking it
     * will execute the action on the JavaFX thread via {@link Platform#runLater}.
     *
     * @param title the notification title
     * @param message the notification message text
     * @param timeout auto-hide timeout in milliseconds (0 or negative = no auto-hide)
     * @param icon the notification icon type
     * @param action optional action to run when notification is clicked (may be null)
     */
    void show(String title, String message, int timeout, NotificationType icon, Runnable action) {
        if (display == null || display.isDisposed()) return;

        display.asyncExec(() -> {
            // Remove old listener regardless of icon change
            if (toolTip != null && !toolTip.isDisposed() && selectionAdapter != null) {
                toolTip.removeSelectionListener(selectionAdapter);
                selectionAdapter = null;
            }

            // Recreate tooltip if icon type changed
            if (toolTip != null && !toolTip.isDisposed() && icon != currentIcon) {
                toolTip.dispose();
                toolTip = null;
            }

            if (toolTip == null || toolTip.isDisposed()) {
                int swtIcon = switch (icon) {
                    case INFORMATION -> SWT.ICON_INFORMATION;
                    case WARNING -> SWT.ICON_WARNING;
                    case ERROR -> SWT.ICON_ERROR;
                    case NONE -> SWT.NONE;
                };
                toolTip = new ToolTip(shell, SWT.BALLOON | swtIcon);
                trayItem.setToolTip(toolTip);
                currentIcon = icon;
            }

            if (toolTip.isDisposed()) return;

            toolTip.setText(title);
            toolTip.setMessage(message);
            toolTip.setVisible(true);
            selectionAdapter = new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (action != null) {
                        Platform.runLater(action);
                    }
                }
            };
            toolTip.addSelectionListener(selectionAdapter);

            if (timeout > 0) {
                display.timerExec(timeout, () -> {
                    if (toolTip != null && !toolTip.isDisposed()) {
                        toolTip.setVisible(false);
                    }
                });
            }
        });
    }

//    public boolean isDisposed() {
//        return toolTip == null || toolTip.isDisposed();
//    }

    /**
     * Disposes of all notification resources.
     *
     * <p>This method cleans up the tooltip, removes listeners, and resets state.
     * It executes asynchronously on the SWT thread and is safe to call multiple times.
     */
    void dispose() {
        if (display != null && !display.isDisposed()) {
            display.asyncExec(() -> {
                if (selectionAdapter != null && toolTip != null && !toolTip.isDisposed()) {
                    toolTip.removeSelectionListener(selectionAdapter);
                }
                selectionAdapter = null;

                if (toolTip != null && !toolTip.isDisposed()) {
                    toolTip.dispose();
                }
                toolTip = null;
                currentIcon = NotificationType.NONE;
            });
        }
    }
}
