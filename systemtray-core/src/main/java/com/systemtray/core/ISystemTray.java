package com.systemtray.core;

interface ISystemTray {
    void addEntry(TrayMenuItem... items);

    void dispose();
}
