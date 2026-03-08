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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SeparatorTest {

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

    private Separator build() {
        Separator sep = new Separator();
        sep.create(display, swtMenu, null);
        return sep;
    }

    private org.eclipse.swt.widgets.MenuItem firstSwtItem() {
        org.eclipse.swt.widgets.MenuItem[] items = swtMenu.getItems();
        assertNotEquals(0, items.length, "swtMenu has no items");
        return items[0];
    }

    // =========================================================================
    // 1. SWT style
    // =========================================================================

    @Nested
    @DisplayName("SWT style")
    class SwtStyle {

        @Test
        @DisplayName("Created SWT item has the SEPARATOR style")
        void swtItemHasSeparatorStyle() throws InterruptedException {
            runOnSWT(() -> build());
            runOnSWT(() -> assertEquals(SWT.SEPARATOR, firstSwtItem().getStyle() & SWT.SEPARATOR,
                    "Separator must produce a SEPARATOR-styled SWT item"));
        }

        @Test
        @DisplayName("getSWTStyle() returns SWT.SEPARATOR")
        void getSwtStyleReturnsSeparator() {
            assertEquals(SWT.SEPARATOR, new Separator().getSWTStyle(),
                    "getSWTStyle() must return SWT.SEPARATOR");
        }
    }

    // =========================================================================
    // 2. Visual placement
    // =========================================================================

    @Nested
    @DisplayName("Visual placement")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class VisualPlacement {

        @Test
        @Order(1)
        @DisplayName("Separator between two items is at index 1")
        void separatorIsAtCorrectIndex() throws InterruptedException {
            runOnSWT(() -> {
                new TrayMenuItem("A").create(display, swtMenu, null);
                new Separator().create(display, swtMenu, null);
                new TrayMenuItem("B").create(display, swtMenu, null);
            });

            runOnSWT(() -> {
                org.eclipse.swt.widgets.MenuItem[] items = swtMenu.getItems();
                assertEquals(3, items.length, "Menu must have 3 items");
                assertEquals(SWT.SEPARATOR, items[1].getStyle() & SWT.SEPARATOR,
                        "The middle item must be the separator");
            });
        }

        @Test
        @Order(2)
        @DisplayName("Two consecutive separators both have the SEPARATOR style")
        void twoConsecutiveSeparators() throws InterruptedException {
            runOnSWT(() -> {
                new Separator().create(display, swtMenu, null);
                new Separator().create(display, swtMenu, null);
            });

            runOnSWT(() -> {
                org.eclipse.swt.widgets.MenuItem[] items = swtMenu.getItems();
                assertEquals(2, items.length);
                for (org.eclipse.swt.widgets.MenuItem item : items) {
                    assertEquals(SWT.SEPARATOR, item.getStyle() & SWT.SEPARATOR,
                            "Both items must have the SEPARATOR style");
                }
            });
        }
    }

    // =========================================================================
    // 3. Immutability — a separator's text/action cannot meaningfully change
    // =========================================================================

    @Nested
    @DisplayName("Immutability — text and action are inert")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Immutability {

        @Test
        @Order(1)
        @DisplayName("setText() after creation does not throw")
        void setTextDoesNotThrow() throws InterruptedException {
            AtomicReference<Separator> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build()));

            assertDoesNotThrow(() -> {
                ref.get().setText("Ignored");
                Thread.sleep(100);
            }, "Setting text on a separator must not throw even though it is visual-only");
        }

        @Test
        @Order(2)
        @DisplayName("setOnAction() with a handler does not throw, selection is a no-op")
        void setOnActionDoesNotThrow() throws InterruptedException {
            AtomicReference<Separator> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build()));

            assertDoesNotThrow(() -> {
                ref.get().setOnAction(e -> { /* intentionally empty */ });
                Thread.sleep(100);
            }, "setOnAction() on a separator must not throw");
        }
    }

    // =========================================================================
    // 4. Disposal
    // =========================================================================

    @Nested
    @DisplayName("Disposal")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Disposal {

        @Test
        @Order(1)
        @DisplayName("dispose() marks the SWT separator item as disposed")
        void disposeMarksSwtItemDisposed() throws InterruptedException {
            AtomicReference<Separator> ref = new AtomicReference<>();
            AtomicReference<org.eclipse.swt.widgets.MenuItem> swtRef = new AtomicReference<>();

            runOnSWT(() -> {
                ref.set(build());
                swtRef.set(firstSwtItem());
            });

            runOnSWT(() -> ref.get().dispose());

            runOnSWT(() -> assertTrue(swtRef.get().isDisposed(),
                    "SWT item must be disposed after Separator.dispose()"));
        }

        @Test
        @Order(2)
        @DisplayName("Calling dispose() twice does not throw")
        void doubleDisposeDoesNotThrow() throws InterruptedException {
            AtomicReference<Separator> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build()));

            assertDoesNotThrow(() -> {
                runOnSWT(() -> ref.get().dispose());
                runOnSWT(() -> ref.get().dispose());
            }, "Calling dispose() twice must not throw");
        }
    }

    // =========================================================================
    // 5. Failure modes
    // =========================================================================

    @Nested
    @DisplayName("Failure modes")
    class FailureModes {

        @Test
        @DisplayName("Mutations after dispose() do not throw")
        void mutationsAfterDisposeDoNotThrow() throws InterruptedException {
            AtomicReference<Separator> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build()));
            runOnSWT(() -> ref.get().dispose());

            assertDoesNotThrow(() -> {
                ref.get().setText("Ghost");
                ref.get().setDisable(true);
                Thread.sleep(200);
            }, "Mutating a disposed separator must not throw");
        }
    }
}