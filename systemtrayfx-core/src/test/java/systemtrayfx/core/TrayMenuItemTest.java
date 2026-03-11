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

package systemtrayfx.core;

import javafx.application.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TrayMenuItemTest {

    private static Display display;

    @BeforeAll
    static void startPlatforms() throws InterruptedException {
        FXPlatformSupport.start();

        CountDownLatch swtReady = new CountDownLatch(1);
        Thread swtThread = new Thread(() -> {
            display = new Display();
            swtReady.countDown();
            while (!display.isDisposed()) {
                if (!display.readAndDispatch()) display.sleep();
            }
        }, "SWT-Test-Thread");
        swtThread.setDaemon(true);
        swtThread.start();
        assertTrue(swtReady.await(5, TimeUnit.SECONDS), "SWT Display did not initialise in time");
    }

    @AfterAll
    static void stopPlatforms() {
        if (display != null && !display.isDisposed()) {
            display.asyncExec(display::dispose);
        }
    }

    private Shell shell;
    private org.eclipse.swt.widgets.Menu swtMenu;

    @BeforeEach
    void createFixtures() throws InterruptedException {
        runOnSWT(() -> {
            shell = new Shell(display);
            swtMenu = new org.eclipse.swt.widgets.Menu(shell, SWT.POP_UP);
        });
    }

    @AfterEach
    void disposeFixtures() throws InterruptedException {
        runOnSWT(() -> {
            if (swtMenu != null && !swtMenu.isDisposed()) swtMenu.dispose();
            if (shell != null && !shell.isDisposed()) shell.dispose();
        });
    }

    private void runOnSWT(Runnable task) throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();
        display.asyncExec(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                err.set(t);
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(3, TimeUnit.SECONDS), "SWT task timed out");
        if (err.get() != null) throw new RuntimeException(err.get());
    }

    private TrayMenuItem build(String text) {
        TrayMenuItem item = new TrayMenuItem(text);
        item.create(display, swtMenu, null);
        return item;
    }

    private org.eclipse.swt.widgets.MenuItem firstSwtItem() {
        org.eclipse.swt.widgets.MenuItem[] items = swtMenu.getItems();
        assertNotEquals(0, items.length, "swtMenu has no items");
        return items[0];
    }

    // =========================================================================
    // 1. Default text / fallback
    // =========================================================================

    @Nested
    @DisplayName("Text — defaults and fallbacks")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class TextDefaults {

        @Test
        @Order(1)
        @DisplayName("No-arg constructor uses default text 'Item'")
        void noArgConstructorUsesDefaultText() throws InterruptedException {
            runOnSWT(() -> new TrayMenuItem().create(display, swtMenu, null));
            runOnSWT(() -> assertEquals("Item", firstSwtItem().getText(),
                    "No-arg constructor should produce 'Item' as the label"));
        }

        @Test
        @Order(2)
        @DisplayName("Supplied text is applied to the SWT item")
        void suppliedTextApplied() throws InterruptedException {
            runOnSWT(() -> build("Open"));
            runOnSWT(() -> assertEquals("Open", firstSwtItem().getText()));
        }

        @Test
        @Order(3)
        @DisplayName("Null text falls back to 'Item'")
        void nullTextFallsBackToDefault() throws InterruptedException {
            runOnSWT(() -> build(null));
            runOnSWT(() -> assertEquals("Item", firstSwtItem().getText(),
                    "Null text should fall back to the default label"));
        }

        @Test
        @Order(4)
        @DisplayName("Blank text falls back to 'Item'")
        void blankTextFallsBackToDefault() throws InterruptedException {
            runOnSWT(() -> build("   "));
            runOnSWT(() -> assertEquals("Item", firstSwtItem().getText(),
                    "Blank-only text should fall back to the default label"));
        }
    }

    // =========================================================================
    // 2. Text property synchronisation
    // =========================================================================

    @Nested
    @DisplayName("Text property synchronisation")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class TextSync {

        @Test
        @Order(1)
        @DisplayName("Changing text after creation updates the SWT label")
        void textChangeReflectsInSWT() throws InterruptedException {
            AtomicReference<TrayMenuItem> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("Open")));

            ref.get().setText("Close");
            Thread.sleep(200);

            runOnSWT(() -> assertEquals("Close", firstSwtItem().getText()));
        }

        @Test
        @Order(2)
        @DisplayName("Setting text to blank after creation falls back to 'Item'")
        void settingBlankAfterCreationFallsBack() throws InterruptedException {
            AtomicReference<TrayMenuItem> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("Open")));

            ref.get().setText("");
            Thread.sleep(200);

            runOnSWT(() -> assertEquals("Item", firstSwtItem().getText(),
                    "Clearing the label should fall back to the default"));
        }

        @Test
        @Order(3)
        @DisplayName("textProperty().bind() drives the SWT label")
        void textPropertyBindDrivesSWT() throws InterruptedException {
            javafx.beans.property.SimpleStringProperty src = new javafx.beans.property.SimpleStringProperty("Alpha");
            AtomicReference<TrayMenuItem> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("Alpha")));
            ref.get().textProperty().bind(src);

            src.set("Beta");
            Thread.sleep(200);

            runOnSWT(() -> assertEquals("Beta", firstSwtItem().getText()));
        }
    }

    // =========================================================================
    // 3. Disable property synchronisation
    // =========================================================================

    @Nested
    @DisplayName("Disable property synchronisation")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class DisableSync {

        @Test
        @Order(1)
        @DisplayName("Item is enabled by default")
        void enabledByDefault() throws InterruptedException {
            runOnSWT(() -> build("Open"));
            runOnSWT(() -> assertTrue(firstSwtItem().isEnabled(),
                    "SWT item should be enabled when disable is not set"));
        }

        @Test
        @Order(2)
        @DisplayName("setDisable(true) disables the SWT item")
        void setDisableTrueDisablesSWT() throws InterruptedException {
            AtomicReference<TrayMenuItem> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("Open")));

            ref.get().setDisable(true);
            Thread.sleep(200);

            runOnSWT(() -> assertFalse(firstSwtItem().isEnabled()));
        }

        @Test
        @Order(3)
        @DisplayName("Re-enabling after disable re-enables the SWT item")
        void reEnablingReEnablesSWT() throws InterruptedException {
            AtomicReference<TrayMenuItem> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("Open")));

            ref.get().setDisable(true);
            Thread.sleep(100);
            ref.get().setDisable(false);
            Thread.sleep(200);

            runOnSWT(() -> assertTrue(firstSwtItem().isEnabled()));
        }

        @Test
        @Order(4)
        @DisplayName("disableProperty().bind() drives the SWT enabled state")
        void disablePropertyBindDrivesSWT() throws InterruptedException {
            javafx.beans.property.SimpleBooleanProperty src = new javafx.beans.property.SimpleBooleanProperty(false);
            AtomicReference<TrayMenuItem> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("Open")));
            ref.get().disableProperty().bind(src);

            src.set(true);
            Thread.sleep(200);

            runOnSWT(() -> assertFalse(firstSwtItem().isEnabled()));
        }
    }

    // =========================================================================
    // 4. onAction synchronisation
    // =========================================================================

    @Nested
    @DisplayName("onAction synchronisation")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class OnActionSync {

        @Test
        @Order(1)
        @DisplayName("SWT selection fires the registered onAction handler")
        void swtSelectionFiresOnAction() throws InterruptedException {
            AtomicBoolean fired = new AtomicBoolean(false);
            AtomicReference<TrayMenuItem> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("Open")));
            ref.get().setOnAction(e -> fired.set(true));

            runOnSWT(() -> firstSwtItem().notifyListeners(SWT.Selection, new org.eclipse.swt.widgets.Event()));
            Thread.sleep(300);

            assertTrue(fired.get(), "onAction handler must be called on SWT selection");
        }

        @Test
        @Order(2)
        @DisplayName("Replacing onAction fires only the new handler")
        void replacingOnActionUsesNewHandler() throws InterruptedException {
            AtomicBoolean first = new AtomicBoolean(false);
            AtomicBoolean second = new AtomicBoolean(false);
            AtomicReference<TrayMenuItem> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("Open")));
            ref.get().setOnAction(e -> first.set(true));

            ref.get().setOnAction(e -> second.set(true));
            Thread.sleep(200);

            runOnSWT(() -> firstSwtItem().notifyListeners(SWT.Selection, new org.eclipse.swt.widgets.Event()));
            Thread.sleep(300);

            assertFalse(first.get(), "Old handler must not fire");
            assertTrue(second.get(), "New handler must fire");
        }

        @Test
        @Order(3)
        @DisplayName("No NullPointerException when onAction is null and SWT item is selected")
        void noNpeWhenOnActionIsNull() throws InterruptedException {
            runOnSWT(() -> build("Open")); // no action set
            assertDoesNotThrow(() ->
                    runOnSWT(() -> firstSwtItem().notifyListeners(SWT.Selection, new org.eclipse.swt.widgets.Event()))
            );
        }

        @Test
        @Order(4)
        @DisplayName("Setting onAction to null after it was set does not fire on selection")
        void settingOnActionToNullClearsHandler() throws InterruptedException {
            AtomicBoolean fired = new AtomicBoolean(false);
            AtomicReference<TrayMenuItem> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("Open")));
            ref.get().setOnAction(e -> fired.set(true));
            ref.get().setOnAction(null);
            Thread.sleep(200);

            runOnSWT(() -> firstSwtItem().notifyListeners(SWT.Selection, new org.eclipse.swt.widgets.Event()));
            Thread.sleep(300);

            assertFalse(fired.get(), "Cleared handler must not fire");
        }
    }

    // =========================================================================
    // 5. SWT style
    // =========================================================================

    @Nested
    @DisplayName("SWT style")
    class SwtStyle {

        @Test
        @DisplayName("Default getSWTStyle() returns SWT.PUSH")
        void defaultStyleIsPush() throws InterruptedException {
            runOnSWT(() -> build("Open"));
            runOnSWT(() -> assertEquals(SWT.PUSH, firstSwtItem().getStyle() & SWT.PUSH,
                    "TrayMenuItem must use the PUSH style"));
        }
    }

    // =========================================================================
    // 6. Disposal
    // =========================================================================

    @Nested
    @DisplayName("Disposal")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Disposal {

        @Test
        @Order(1)
        @DisplayName("dispose() marks the SWT item as disposed")
        void disposeMarksSwtItemDisposed() throws InterruptedException {
            AtomicReference<TrayMenuItem> ref = new AtomicReference<>();
            AtomicReference<org.eclipse.swt.widgets.MenuItem> swtRef = new AtomicReference<>();

            runOnSWT(() -> {
                ref.set(build("Open"));
                swtRef.set(firstSwtItem());
            });

            runOnSWT(() -> ref.get().dispose());

            runOnSWT(() -> assertTrue(swtRef.get().isDisposed(),
                    "SWT item must be disposed after TrayMenuItem.dispose()"));
        }

        @Test
        @Order(2)
        @DisplayName("Property changes after dispose() do not throw")
        void propertyChangesAfterDisposeDoNotThrow() throws InterruptedException {
            AtomicReference<TrayMenuItem> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("Open")));
            runOnSWT(() -> ref.get().dispose());

            assertDoesNotThrow(() -> {
                ref.get().setText("Anything");
                ref.get().setDisable(true);
                Thread.sleep(200);
            }, "No exception should be thrown when mutating properties after disposal");
        }

        @Test
        @Order(3)
        @DisplayName("Calling dispose() twice does not throw")
        void doubleDisposeDoesNotThrow() throws InterruptedException {
            AtomicReference<TrayMenuItem> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("Open")));

            assertDoesNotThrow(() -> {
                runOnSWT(() -> ref.get().dispose());
                runOnSWT(() -> ref.get().dispose());
            }, "Calling dispose() twice must not throw");
        }
    }
}