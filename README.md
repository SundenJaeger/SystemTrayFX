# SystemTrayFX

SystemTrayFX is a JavaFX library that provides a native system tray implementation by integrating with the Standard
Widget Toolkit (SWT). It allows JavaFX applications to have a fully functional system tray icon with context menus,
notifications, and minimize-to-tray behavior without blocking the main JavaFX application thread.

# Features

- **Native Integration:** Uses SWT to provide a truly native system tray experience across different operating systems.
- **Non-Blocking Architecture:** The system tray operates on a dedicated SWT thread, ensuring the JavaFX application
  thread remains responsive.
- **JavaFX Property Binding:** Many tray components support JavaFX properties (like `StringProperty` and `BooleanProperty`),
  allowing for seamless synchronization between your application state and the tray UI.
- **Rich Menu Support:**
  - **Standard Items:** Regular clickable menu items. 
  - **Check Items:** Menu items with a toggleable checkbox state. 
  - **Submenus:** Nested menu structures for complex tray applications. 
  - **Icons:** Support for adding images to the tray icon and individual menu items.
- **JavaFX Wrapper:** Includes `FXMenuItemWrapper`, which allows you to reuse existing `javafx.scene.control.MenuItem` objects directly in the system tray.
- **System Notifications:** A dedicated `Notification` utility for displaying Information, Warning, and Error messages via the system tray.
- **Minimize-to-Tray:** Built-in support for capturing window close events and hiding the application to the tray instead of exiting.

# Usage
> Check the Wiki Page

# Platform Support

| Platform | Supported | Notes                                                    |
|----------|-----------|----------------------------------------------------------|
| Windows  | YES       | Full support for native icons, menus, and notifications. |
| macOS    | NO        | Not currently supported.                                 |
| Linux    | NO        | Not currently supported.                                 |

# Building Locally
> More Information can be found in [Contributing Page](https://github.com/SundenJaeger/SystemTrayFX/blob/master/CONTRIBUTING.md)

# Third-party libraries used in this project

- [**SWT**](https://github.com/eclipse-platform/eclipse.platform.swt) by The Eclipse Foundation under [**EPL-2.0 License**](https://github.com/eclipse-platform/eclipse.platform.swt/blob/master/LICENSE)