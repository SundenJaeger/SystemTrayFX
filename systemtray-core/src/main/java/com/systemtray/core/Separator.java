package com.systemtray.core;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

public class Separator extends TrayMenuItem {
    public Separator() {
        super("");
    }

    @Override
    protected void create(Display display, Menu menu, SystemTrayFX ctx) {
        new org.eclipse.swt.widgets.MenuItem(menu, SWT.SEPARATOR);
    }
}
