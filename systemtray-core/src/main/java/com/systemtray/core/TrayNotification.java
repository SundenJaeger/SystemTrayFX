package com.systemtray.core;

import javafx.application.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.swt.widgets.TrayItem;

final class TrayNotification {
    private final Display display;
    private final Shell shell;
    private final TrayItem trayItem;

    private ToolTip toolTip;
    private NotificationType currentIcon = NotificationType.NONE;
    private SelectionAdapter selectionAdapter;

    TrayNotification(Display display, Shell shell, TrayItem trayItem) {
        this.display = display;
        this.shell = shell;
        this.trayItem = trayItem;
    }

    public void show(String title, String message, int timeout, NotificationType icon, Runnable action) {
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

    public void dispose() {
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
