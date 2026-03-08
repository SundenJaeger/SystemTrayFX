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
import javafx.beans.property.SimpleBooleanProperty;
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
class TrayCheckMenuItemTest {

    private static Display display;

    @BeforeAll
    static void startPlatforms() throws InterruptedException {
        CountDownLatch fxReady = new CountDownLatch(1);
        Platform.startup(fxReady::countDown);
        assertTrue(fxReady.await(5, TimeUnit.SECONDS), "JavaFX Platform did not initialise in time");

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
        Platform.exit();
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

    private TrayCheckMenuItem build(String text) {
        TrayCheckMenuItem item = new TrayCheckMenuItem(text);
        item.create(display, swtMenu, null);
        return item;
    }

    private org.eclipse.swt.widgets.MenuItem firstSwtItem() {
        org.eclipse.swt.widgets.MenuItem[] items = swtMenu.getItems();
        assertNotEquals(0, items.length, "swtMenu has no items");
        return items[0];
    }

    // =========================================================================
    // 1. Constructors and default text
    // =========================================================================

    @Nested
    @DisplayName("Constructors and default text")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Constructors {

        @Test
        @Order(1)
        @DisplayName("No-arg constructor uses default text 'Check Item'")
        void noArgConstructorUsesDefaultText() throws InterruptedException {
            runOnSWT(() -> new TrayCheckMenuItem().create(display, swtMenu, null));
            runOnSWT(() -> assertEquals("Check Item", firstSwtItem().getText(),
                    "No-arg constructor should produce 'Check Item' as the label"));
        }

        @Test
        @Order(2)
        @DisplayName("Supplied text is applied")
        void suppliedTextApplied() throws InterruptedException {
            runOnSWT(() -> build("Dark Mode"));
            runOnSWT(() -> assertEquals("Dark Mode", firstSwtItem().getText()));
        }

        @Test
        @Order(3)
        @DisplayName("Null text falls back to 'Check Item'")
        void nullTextFallsBackToDefault() throws InterruptedException {
            runOnSWT(() -> build(null));
            runOnSWT(() -> assertEquals("Check Item", firstSwtItem().getText(),
                    "Null text must fall back to the default label"));
        }

        @Test
        @Order(4)
        @DisplayName("Blank text falls back to 'Check Item'")
        void blankTextFallsBackToDefault() throws InterruptedException {
            runOnSWT(() -> build("   "));
            runOnSWT(() -> assertEquals("Check Item", firstSwtItem().getText(),
                    "Blank text must fall back to the default label"));
        }

        @Test
        @Order(5)
        @DisplayName("setText() with blank resets to 'Check Item' even after creation")
        void setTextBlankAfterCreationResetsToDefault() throws InterruptedException {
            AtomicReference<TrayCheckMenuItem> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("Dark Mode")));

            ref.get().setText("");
            Thread.sleep(200);

            runOnSWT(() -> assertEquals("Check Item", firstSwtItem().getText(),
                    "Clearing the label post-creation should reset to the default"));
        }
    }

    // =========================================================================
    // 2. SWT style
    // =========================================================================

    @Nested
    @DisplayName("SWT style")
    class SwtStyle {

        @Test
        @DisplayName("SWT item has the CHECK style")
        void swtItemHasCheckStyle() throws InterruptedException {
            runOnSWT(() -> build("Dark Mode"));
            runOnSWT(() -> assertEquals(SWT.CHECK, firstSwtItem().getStyle() & SWT.CHECK,
                    "TrayCheckMenuItem must use the CHECK style"));
        }
    }

    // =========================================================================
    // 3. Selected state — initial values
    // =========================================================================

    @Nested
    @DisplayName("Selected state — initial values")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class InitialSelectedState {

        @Test
        @Order(1)
        @DisplayName("Default selection state is false")
        void defaultSelectionIsFalse() throws InterruptedException {
            runOnSWT(() -> build("Dark Mode"));
            runOnSWT(() -> assertFalse(firstSwtItem().getSelection(),
                    "SWT item should start unselected by default"));
        }

        @Test
        @Order(2)
        @DisplayName("setSelected(true) before creation mirrors to SWT as selected")
        void preCreationSelectedTrueMirrored() throws InterruptedException {
            TrayCheckMenuItem item = new TrayCheckMenuItem("Dark Mode");
            item.setSelected(true);
            runOnSWT(() -> item.create(display, swtMenu, null));
            runOnSWT(() -> assertTrue(firstSwtItem().getSelection(),
                    "SWT item should be selected when pre-creation selected is true"));
        }

        @Test
        @Order(3)
        @DisplayName("setSelected(false) before creation mirrors to SWT as unselected")
        void preCreationSelectedFalseMirrored() throws InterruptedException {
            TrayCheckMenuItem item = new TrayCheckMenuItem("Dark Mode");
            item.setSelected(false);
            runOnSWT(() -> item.create(display, swtMenu, null));
            runOnSWT(() -> assertFalse(firstSwtItem().getSelection(),
                    "SWT item should be unselected when pre-creation selected is false"));
        }
    }

    // =========================================================================
    // 4. Selected state — bidirectional sync
    // =========================================================================

    @Nested
    @DisplayName("Selected state — bidirectional synchronisation")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class SelectedStateSync {

        @Test
        @Order(1)
        @DisplayName("setSelected(true) after creation updates the SWT selection")
        void setSelectedTrueUpdatesSWT() throws InterruptedException {
            AtomicReference<TrayCheckMenuItem> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("Dark Mode")));

            ref.get().setSelected(true);
            Thread.sleep(200);

            runOnSWT(() -> assertTrue(firstSwtItem().getSelection()));
        }

        @Test
        @Order(2)
        @DisplayName("setSelected(false) after creation updates the SWT selection")
        void setSelectedFalseUpdatesSWT() throws InterruptedException {
            AtomicReference<TrayCheckMenuItem> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("Dark Mode")));
            ref.get().setSelected(true);
            Thread.sleep(100);

            ref.get().setSelected(false);
            Thread.sleep(200);

            runOnSWT(() -> assertFalse(firstSwtItem().getSelection()));
        }

        @Test
        @Order(3)
        @DisplayName("SWT click feeds back to selectedProperty()")
        void swtClickFeedsBackToFxProperty() throws InterruptedException {
            AtomicReference<TrayCheckMenuItem> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("Dark Mode")));

            runOnSWT(() -> {
                org.eclipse.swt.widgets.MenuItem swtItem = firstSwtItem();
                swtItem.setSelection(true);
                swtItem.notifyListeners(SWT.Selection, new org.eclipse.swt.widgets.Event());
            });
            Thread.sleep(300);

            assertTrue(ref.get().isSelected(),
                    "FX selectedProperty must update when the SWT item is toggled");
        }

        @Test
        @Order(4)
        @DisplayName("SWT click does NOT overwrite a bound selectedProperty()")
        void swtClickDoesNotOverwriteBoundProperty() throws InterruptedException {
            SimpleBooleanProperty bound = new SimpleBooleanProperty(false);
            TrayCheckMenuItem item = new TrayCheckMenuItem("Dark Mode");
            item.selectedProperty().bind(bound);
            runOnSWT(() -> item.create(display, swtMenu, null));

            runOnSWT(() -> {
                org.eclipse.swt.widgets.MenuItem swtItem = firstSwtItem();
                swtItem.setSelection(true);
                swtItem.notifyListeners(SWT.Selection, new org.eclipse.swt.widgets.Event());
            });
            Thread.sleep(300);

            assertFalse(item.isSelected(),
                    "A bound selectedProperty must not be written by an SWT click");
            assertFalse(bound.get(),
                    "The bound source property must remain unchanged");
        }

        @Test
        @Order(5)
        @DisplayName("selectedProperty().bindBidirectional() round-trips correctly")
        void bidirectionalBindRoundTrips() throws InterruptedException {
            SimpleBooleanProperty external = new SimpleBooleanProperty(false);
            AtomicReference<TrayCheckMenuItem> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("Dark Mode")));
            ref.get().selectedProperty().bindBidirectional(external);

            // Drive from external → FX → SWT
            external.set(true);
            Thread.sleep(200);
            runOnSWT(() -> assertTrue(firstSwtItem().getSelection(),
                    "SWT should reflect the bidirectionally-bound external property"));
        }
    }

    // =========================================================================
    // 5. Failure modes
    // =========================================================================

    @Nested
    @DisplayName("Failure modes")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class FailureModes {

        @Test
        @Order(1)
        @DisplayName("Mutating selected after dispose() does not throw")
        void mutatingSelectedAfterDisposeDoesNotThrow() throws InterruptedException {
            AtomicReference<TrayCheckMenuItem> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("Dark Mode")));
            runOnSWT(() -> ref.get().dispose());

            assertDoesNotThrow(() -> {
                ref.get().setSelected(true);
                Thread.sleep(200);
            }, "Setting selected after dispose must not throw");
        }

        @Test
        @Order(2)
        @DisplayName("Rapidly toggling selected does not corrupt the final SWT state")
        void rapidToggleDoesNotCorruptFinalState() throws InterruptedException {
            AtomicReference<TrayCheckMenuItem> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("Dark Mode")));

            // Flip 10 times; final state should be 'true' (even number of extra flips)
            for (int i = 0; i < 10; i++) {
                ref.get().setSelected(i % 2 == 0);
            }
            // Final call leaves it true (index 9 → false, so last even → 8 → true)
            ref.get().setSelected(true);
            Thread.sleep(300);

            runOnSWT(() -> assertTrue(firstSwtItem().getSelection(),
                    "After rapid toggling, the final state must be reflected in SWT"));
        }

        @Test
        @Order(3)
        @DisplayName("Calling dispose() twice does not throw")
        void doubleDisposeDoesNotThrow() throws InterruptedException {
            AtomicReference<TrayCheckMenuItem> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("Dark Mode")));

            assertDoesNotThrow(() -> {
                runOnSWT(() -> ref.get().dispose());
                runOnSWT(() -> ref.get().dispose());
            }, "Calling dispose() twice must not throw");
        }

        @Test
        @Order(4)
        @DisplayName("Action handler fires even when selected is true — verifies no short-circuit")
        void actionHandlerFiresWhenSelected() throws InterruptedException {
            AtomicBoolean fired = new AtomicBoolean(false);
            AtomicReference<TrayCheckMenuItem> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("Dark Mode")));
            ref.get().setSelected(true);
            ref.get().setOnAction(e -> fired.set(true));

            runOnSWT(() -> firstSwtItem().notifyListeners(SWT.Selection, new org.eclipse.swt.widgets.Event()));
            Thread.sleep(300);

            assertTrue(fired.get(), "onAction handler must fire regardless of selected state");
        }
    }
}