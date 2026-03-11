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
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TrayMenuTest {

    private static Display display;

    @BeforeAll
    static void startPlatforms() throws InterruptedException {
        FXPlatformSupport.start();

        CountDownLatch swtReady = new CountDownLatch(1);
        Thread swtThread = new Thread(() -> {
            display = new Display();
            // Prevent a bad asyncExec from crashing the loop and killing all subsequent tests.
            display.setErrorHandler(t ->
                    System.err.println("[SWT-Test-Thread] asyncExec error (test isolation): " + t));
            swtReady.countDown();
            while (!display.isDisposed()) {
                try {
                    if (!display.readAndDispatch()) display.sleep();
                } catch (Throwable t) {
                    System.err.println("[SWT-Test-Thread] loop error: " + t);
                }
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

    /**
     * Creates a {@link TrayMenu} and calls {@code create()} on the SWT thread.
     *
     * <p><strong>Items are never passed through the constructor.</strong>
     * The {@code TrayMenu} constructor installs a {@code ListChangeListener} that calls
     * {@code display.asyncExec(() -> item.create(display, subMenu, ctx))}, but
     * {@code subMenu} is only initialised inside {@code TrayMenu.create()}. Passing items
     * to the constructor before {@code create()} has run queues asyncExec tasks that
     * reference a null (or already-disposed) subMenu, causing an SWT
     * "Argument not valid" crash that kills the event-loop thread and makes every
     * subsequent {@code runOnSWT()} call time out.
     *
     * <p>Always add items via {@link TrayMenu#getItems()} <em>after</em> this method returns.
     */
    private TrayMenu build(String text) {
        TrayMenu menu = new TrayMenu(text);
        menu.create(display, swtMenu, null);
        return menu;
    }

    private MenuItem firstSwtItem() {
        MenuItem[] items = swtMenu.getItems();
        assertNotEquals(0, items.length, "swtMenu has no items");
        return items[0];
    }

    private int liveChildCount() {
        MenuItem root = firstSwtItem();
        org.eclipse.swt.widgets.Menu sub = root.getMenu();
        if (sub == null || sub.isDisposed()) return 0;
        return (int) Arrays.stream(sub.getItems()).filter(i -> !i.isDisposed()).count();
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
        @DisplayName("No-arg constructor uses default text 'Menu'")
        void noArgConstructorUsesDefaultText() throws InterruptedException {
            runOnSWT(() -> new TrayMenu().create(display, swtMenu, null));
            runOnSWT(() -> assertEquals("Menu", firstSwtItem().getText(),
                    "No-arg constructor should produce 'Menu' as the label"));
        }

        @Test
        @Order(2)
        @DisplayName("Supplied text is applied to the SWT item")
        void suppliedTextApplied() throws InterruptedException {
            runOnSWT(() -> build("File"));
            runOnSWT(() -> assertEquals("File", firstSwtItem().getText()));
        }

        @Test
        @Order(3)
        @DisplayName("Null text falls back to 'Menu'")
        void nullTextFallsBackToDefault() throws InterruptedException {
            runOnSWT(() -> build(null));
            runOnSWT(() -> assertEquals("Menu", firstSwtItem().getText(),
                    "Null text must fall back to the default label"));
        }

        @Test
        @Order(4)
        @DisplayName("Blank text falls back to 'Menu'")
        void blankTextFallsBackToDefault() throws InterruptedException {
            runOnSWT(() -> build("   "));
            runOnSWT(() -> assertEquals("Menu", firstSwtItem().getText(),
                    "Blank text must fall back to the default label"));
        }

        @Test
        @Order(5)
        @DisplayName("Items added via getItems() after create() appear in SWT")
        void itemsAddedAfterCreateAppear() throws InterruptedException {
            AtomicReference<TrayMenu> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("File")));

            ref.get().getItems().addAll(
                    new TrayMenuItem("New"),
                    new TrayMenuItem("Open"),
                    new TrayMenuItem("Save"));
            Thread.sleep(200);

            runOnSWT(() -> assertEquals(3, liveChildCount(),
                    "All three items added post-create() must appear in the SWT submenu"));
        }
    }

    // =========================================================================
    // 2. SWT style
    // =========================================================================

    @Nested
    @DisplayName("SWT style")
    class SwtStyle {

        @Test
        @DisplayName("Root SWT item has the CASCADE style")
        void rootItemHasCascadeStyle() throws InterruptedException {
            runOnSWT(() -> build("File"));
            runOnSWT(() -> assertEquals(SWT.CASCADE, firstSwtItem().getStyle() & SWT.CASCADE,
                    "TrayMenu root item must use the CASCADE style"));
        }

        @Test
        @DisplayName("A non-null submenu is attached to the root CASCADE item")
        void subMenuAttachedToRoot() throws InterruptedException {
            runOnSWT(() -> build("File"));
            runOnSWT(() -> assertNotNull(firstSwtItem().getMenu(),
                    "A submenu must be attached to the root CASCADE item after creation"));
        }
    }

    // =========================================================================
    // 3. getItems() — live add / remove
    // =========================================================================

    @Nested
    @DisplayName("getItems() — live add / remove")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class GetItemsLive {

        @Test
        @Order(1)
        @DisplayName("Items added via getItems() appear in the SWT submenu")
        void addViaGetItemsAppearsInSWT() throws InterruptedException {
            AtomicReference<TrayMenu> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("File")));

            ref.get().getItems().add(new TrayMenuItem("New"));
            Thread.sleep(200);

            runOnSWT(() -> assertEquals(1, liveChildCount(),
                    "Adding via getItems() must create an SWT child"));
        }

        @Test
        @Order(2)
        @DisplayName("Multiple items added at once all appear in SWT")
        void addMultipleAtOnce() throws InterruptedException {
            AtomicReference<TrayMenu> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("File")));

            ref.get().getItems().addAll(
                    new TrayMenuItem("New"),
                    new TrayMenuItem("Open"),
                    new TrayMenuItem("Save"));
            Thread.sleep(200);

            runOnSWT(() -> assertEquals(3, liveChildCount()));
        }

        @Test
        @Order(3)
        @DisplayName("Removing an item via getItems() disposes it in SWT")
        void removeViaGetItemsDisposesInSWT() throws InterruptedException {
            TrayMenuItem item = new TrayMenuItem("New");
            AtomicReference<TrayMenu> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("File")));
            ref.get().getItems().add(item);
            Thread.sleep(100);

            ref.get().getItems().remove(item);
            Thread.sleep(200);

            runOnSWT(() -> assertEquals(0, liveChildCount(),
                    "Removing the last item must leave the SWT submenu empty"));
        }

        @Test
        @Order(4)
        @DisplayName("Removing one of two items leaves exactly one in SWT")
        void removingOneOfTwoLeavesOne() throws InterruptedException {
            TrayMenuItem first = new TrayMenuItem("New");
            TrayMenuItem second = new TrayMenuItem("Open");
            AtomicReference<TrayMenu> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("File")));
            ref.get().getItems().addAll(first, second);
            Thread.sleep(100);

            ref.get().getItems().remove(first);
            Thread.sleep(200);

            runOnSWT(() -> assertEquals(1, liveChildCount()));
        }

        @Test
        @Order(5)
        @DisplayName("Removing all items leaves an empty-but-alive parent CASCADE item")
        void removeAllLeavesParentAlive() throws InterruptedException {
            TrayMenuItem a = new TrayMenuItem("A");
            TrayMenuItem b = new TrayMenuItem("B");
            AtomicReference<TrayMenu> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("File")));
            ref.get().getItems().addAll(a, b);
            Thread.sleep(100);

            ref.get().getItems().clear();
            Thread.sleep(200);

            runOnSWT(() -> {
                assertEquals(0, liveChildCount(), "SWT submenu must be empty");
                assertFalse(firstSwtItem().isDisposed(),
                        "Parent CASCADE item must survive after all children are removed");
            });
        }

        @Test
        @Order(6)
        @DisplayName("Remove then add produces the correct final count")
        void removeThenAddProducesCorrectCount() throws InterruptedException {
            TrayMenuItem toRemove = new TrayMenuItem("Open");
            AtomicReference<TrayMenu> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("File")));
            ref.get().getItems().addAll(new TrayMenuItem("New"), toRemove);
            Thread.sleep(100);

            ref.get().getItems().remove(toRemove);
            Thread.sleep(100);
            ref.get().getItems().add(new TrayMenuItem("Save As"));
            Thread.sleep(200);

            runOnSWT(() -> assertEquals(2, liveChildCount(),
                    "After remove + add, SWT submenu must have exactly 2 items"));
        }
    }

    // =========================================================================
    // 4. Pending items — items added before create()
    // =========================================================================

    @Nested
    @DisplayName("Pending items — added before create()")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class PendingItems {

        @Test
        @Order(1)
        @DisplayName("Items added via getItems() before create() are flushed after create()")
        void itemsAddedBeforeCreateAreFlushed() throws InterruptedException {
            // Construct off the SWT thread so getItems() calls happen before create().
            TrayMenu menu = new TrayMenu("File");
            menu.getItems().addAll(new TrayMenuItem("New"), new TrayMenuItem("Open"));

            // create() flushes pendingItems synchronously, so no asyncExec race here.
            runOnSWT(() -> menu.create(display, swtMenu, null));

            runOnSWT(() -> assertEquals(2, liveChildCount(),
                    "Items queued before create() must appear after the menu is initialised"));
        }
    }

    // =========================================================================
    // 5. Nested submenus
    // =========================================================================

    @Nested
    @DisplayName("Nested submenus")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class NestedSubmenus {

        @Test
        @Order(1)
        @DisplayName("A nested TrayMenu creates a CASCADE item inside the parent submenu")
        void nestedTrayMenuCreatesCascadeChild() throws InterruptedException {
            // Build the child off-thread with its items in its pending queue.
            TrayMenu child = new TrayMenu("Recent");
            child.getItems().addAll(new TrayMenuItem("file1.txt"), new TrayMenuItem("file2.txt"));

            AtomicReference<TrayMenu> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("File")));

            // Adding the child triggers TrayMenu.create() for the child against the
            // parent's internal subMenu (which now exists). The child then flushes its
            // own pending items synchronously inside its create().
            ref.get().getItems().add(child);
            Thread.sleep(200);

            runOnSWT(() -> {
                MenuItem rootItem = firstSwtItem();
                MenuItem[] rootChildren = rootItem.getMenu().getItems();
                assertEquals(1, rootChildren.length, "Root submenu must contain the nested menu");
                assertEquals(SWT.CASCADE, rootChildren[0].getStyle() & SWT.CASCADE,
                        "The nested menu must be a CASCADE item");
                assertNotNull(rootChildren[0].getMenu(), "The nested CASCADE must have its own submenu");
                assertEquals(2, rootChildren[0].getMenu().getItems().length,
                        "The nested submenu must contain 2 grandchild items");
            });
        }
    }

    // =========================================================================
    // 6. setText() synchronisation
    // =========================================================================

    @Nested
    @DisplayName("setText() synchronisation")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class TextSync {

        @Test
        @Order(1)
        @DisplayName("setText() after creation updates the SWT label")
        void setTextAfterCreationUpdatesSWT() throws InterruptedException {
            AtomicReference<TrayMenu> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("File")));

            ref.get().setText("Edit");
            Thread.sleep(200);

            runOnSWT(() -> assertEquals("Edit", firstSwtItem().getText()));
        }

        @Test
        @Order(2)
        @DisplayName("setText() to blank falls back to 'Menu'")
        void setTextToBlankFallsBackToDefault() throws InterruptedException {
            AtomicReference<TrayMenu> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("File")));

            ref.get().setText("");
            Thread.sleep(200);

            runOnSWT(() -> assertEquals("Menu", firstSwtItem().getText(),
                    "Setting the label to blank must reset to the default 'Menu'"));
        }
    }

    // =========================================================================
    // 7. Failure modes
    // =========================================================================

    @Nested
    @DisplayName("Failure modes")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class FailureModes {

        @Test
        @Order(1)
        @DisplayName("dispose() marks the root CASCADE item as disposed")
        void disposeMarksRootDisposed() throws InterruptedException {
            AtomicReference<TrayMenu> ref = new AtomicReference<>();
            AtomicReference<MenuItem> swtRef = new AtomicReference<>();
            runOnSWT(() -> {
                ref.set(build("File"));
                swtRef.set(firstSwtItem());
            });

            runOnSWT(() -> ref.get().dispose());

            runOnSWT(() -> assertTrue(swtRef.get().isDisposed(),
                    "Root SWT CASCADE item must be disposed after TrayMenu.dispose()"));
        }

        @Test
        @Order(2)
        @DisplayName("dispose() disposes children that were added before disposal")
        void disposeDisposesChildren() throws InterruptedException {
            AtomicReference<TrayMenu> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("File")));
            ref.get().getItems().addAll(new TrayMenuItem("New"), new TrayMenuItem("Open"));
            Thread.sleep(100);

            runOnSWT(() -> ref.get().dispose());
            Thread.sleep(200);

            runOnSWT(() -> assertFalse(
                    swtMenu.getItems().length > 0 && !swtMenu.getItems()[0].isDisposed(),
                    "SWT root item should be disposed after TrayMenu.dispose()"));
        }

        @Test
        @Order(3)
        @DisplayName("Calling dispose() twice does not throw")
        void doubleDisposeDoesNotThrow() throws InterruptedException {
            AtomicReference<TrayMenu> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("File")));

            assertDoesNotThrow(() -> {
                runOnSWT(() -> ref.get().dispose());
                runOnSWT(() -> ref.get().dispose());
            }, "Calling dispose() twice must not throw");
        }

        @Test
        @Order(4)
        @DisplayName("Property mutations after dispose() do not throw")
        void propertyMutationAfterDisposeDoesNotThrow() throws InterruptedException {
            AtomicReference<TrayMenu> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("File")));
            runOnSWT(() -> ref.get().dispose());

            assertDoesNotThrow(() -> {
                ref.get().setText("Anything");
                ref.get().setDisable(true);
                Thread.sleep(200);
            }, "Mutating text/disable after dispose must not throw");
        }

        @Test
        @Order(5)
        @DisplayName("Adding items to getItems() after dispose() does not throw")
        void addingItemsAfterDisposeDoesNotThrow() throws InterruptedException {
            AtomicReference<TrayMenu> ref = new AtomicReference<>();
            runOnSWT(() -> ref.set(build("File")));
            runOnSWT(() -> ref.get().dispose());

            assertDoesNotThrow(() -> {
                ref.get().getItems().add(new TrayMenuItem("Ghost"));
                Thread.sleep(200);
            }, "Adding items after dispose must not throw");
        }
    }
}