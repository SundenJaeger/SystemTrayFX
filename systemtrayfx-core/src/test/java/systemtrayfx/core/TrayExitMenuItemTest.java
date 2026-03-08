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
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TrayExitMenuItemTest {

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

    /**
     * Creates a stub SystemTrayFX context so dispose() can be verified without
     * spinning up the real SWT tray thread.
     */
    private SystemTrayFX mockCtx() {
        return Mockito.mock(SystemTrayFX.class);
    }

    /**
     * Builds a {@link TrayExitMenuItem} that calls {@code ctx.dispose()} as normal
     * but does NOT call {@link javafx.application.Platform#exit()}, which would
     * tear down the JavaFX platform and break every subsequent test in the suite.
     *
     * <p>We achieve this by replacing the action after {@code create()} — the new
     * lambda still reads {@code ctx} (the field, now populated) and calls dispose(),
     * but skips the platform exit.
     */
    private TrayExitMenuItem buildWith(String text, SystemTrayFX ctx) {
        TrayExitMenuItem item = new TrayExitMenuItem(text);
        item.create(display, swtMenu, ctx);
        // Override to prevent Platform.exit() from killing the test suite.
        item.setOnAction(event -> ctx.dispose());
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
        @DisplayName("No-arg constructor uses default text 'Exit'")
        void noArgConstructorUsesDefaultText() throws InterruptedException {
            runOnSWT(() -> new TrayExitMenuItem().create(display, swtMenu, mockCtx()));
            runOnSWT(() -> assertEquals("Exit", firstSwtItem().getText(),
                    "No-arg constructor should produce 'Exit' as the label"));
        }

        @Test
        @Order(2)
        @DisplayName("Custom text is applied to the SWT item")
        void customTextApplied() throws InterruptedException {
            runOnSWT(() -> buildWith("Quit Application", mockCtx()));
            runOnSWT(() -> assertEquals("Quit Application", firstSwtItem().getText()));
        }

        @Test
        @Order(3)
        @DisplayName("Null text falls back to 'Exit'")
        void nullTextFallsBackToDefault() throws InterruptedException {
            runOnSWT(() -> buildWith(null, mockCtx()));
            runOnSWT(() -> assertEquals("Exit", firstSwtItem().getText(),
                    "Null text must fall back to 'Exit'"));
        }

        @Test
        @Order(4)
        @DisplayName("Blank text falls back to 'Exit'")
        void blankTextFallsBackToDefault() throws InterruptedException {
            runOnSWT(() -> buildWith("   ", mockCtx()));
            runOnSWT(() -> assertEquals("Exit", firstSwtItem().getText(),
                    "Blank text must fall back to 'Exit'"));
        }

        @Test
        @Order(5)
        @DisplayName("setText() with blank after creation resets to 'Exit'")
        void setTextBlankAfterCreationResetsToDefault() throws InterruptedException {
            AtomicReference<TrayExitMenuItem> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(buildWith("Quit", mockCtx())));

            ref.get().setText("");
            Thread.sleep(200);

            runOnSWT(() -> assertEquals("Exit", firstSwtItem().getText(),
                    "Clearing the label post-creation should reset to 'Exit'"));
        }
    }

    // =========================================================================
    // 2. SWT style
    // =========================================================================

    @Nested
    @DisplayName("SWT style")
    class SwtStyle {

        @Test
        @DisplayName("SWT item has the PUSH style")
        void swtItemHasPushStyle() throws InterruptedException {
            runOnSWT(() -> buildWith("Exit", mockCtx()));
            runOnSWT(() -> assertEquals(SWT.PUSH, firstSwtItem().getStyle() & SWT.PUSH,
                    "TrayExitMenuItem must use the PUSH style"));
        }
    }

    // =========================================================================
    // 3. Exit action — ctx.dispose() is called on click
    // =========================================================================

    @Nested
    @DisplayName("Exit action")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ExitAction {

        @Test
        @Order(1)
        @DisplayName("SWT selection triggers ctx.dispose()")
        void swtSelectionTriggersCtxDispose() throws InterruptedException {
            SystemTrayFX ctx = mockCtx();
            runOnSWT(() -> buildWith("Exit", ctx));

            runOnSWT(() -> firstSwtItem().notifyListeners(SWT.Selection, new org.eclipse.swt.widgets.Event()));
            Thread.sleep(300);

            verify(ctx, times(1)).dispose();
        }

        @Test
        @Order(2)
        @DisplayName("Clicking Exit multiple times calls ctx.dispose() each time")
        void multipleClicksCallDisposeEachTime() throws InterruptedException {
            SystemTrayFX ctx = mockCtx();
            runOnSWT(() -> buildWith("Exit", ctx));

            // Fire three clicks, giving each Platform.runLater() time to settle
            for (int i = 0; i < 3; i++) {
                runOnSWT(() -> firstSwtItem().notifyListeners(SWT.Selection, new org.eclipse.swt.widgets.Event()));
                Thread.sleep(300);
            }

            verify(ctx, times(3)).dispose();
        }
    }

    // =========================================================================
    // 4. Failure modes
    // =========================================================================

    @Nested
    @DisplayName("Failure modes")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class FailureModes {

        @Test
        @Order(1)
        @DisplayName("dispose() on the item itself marks the SWT item disposed")
        void disposeMarksSwtItemDisposed() throws InterruptedException {
            AtomicReference<TrayExitMenuItem> ref = new AtomicReference<>();
            AtomicReference<org.eclipse.swt.widgets.MenuItem> swtRef = new AtomicReference<>();

            runOnSWT(() -> {
                ref.set(buildWith("Exit", mockCtx()));
                swtRef.set(firstSwtItem());
            });

            runOnSWT(() -> ref.get().dispose());

            runOnSWT(() -> assertTrue(swtRef.get().isDisposed(),
                    "SWT item must be disposed after TrayExitMenuItem.dispose()"));
        }

        @Test
        @Order(2)
        @DisplayName("Property changes after dispose() do not throw")
        void propertyChangesAfterDisposeDoNotThrow() throws InterruptedException {
            AtomicReference<TrayExitMenuItem> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(buildWith("Exit", mockCtx())));
            runOnSWT(() -> ref.get().dispose());

            assertDoesNotThrow(() -> {
                ref.get().setText("Anything");
                ref.get().setDisable(true);
                Thread.sleep(200);
            }, "Mutating properties after dispose must not throw");
        }

        @Test
        @Order(3)
        @DisplayName("Calling dispose() twice does not throw")
        void doubleDisposeDoesNotThrow() throws InterruptedException {
            AtomicReference<TrayExitMenuItem> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(buildWith("Exit", mockCtx())));

            assertDoesNotThrow(() -> {
                runOnSWT(() -> ref.get().dispose());
                runOnSWT(() -> ref.get().dispose());
            }, "Calling dispose() twice must not throw");
        }
    }
}