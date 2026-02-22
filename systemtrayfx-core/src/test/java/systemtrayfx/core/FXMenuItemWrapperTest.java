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
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FXMenuItemWrapperTest {
    private static Display display;

    @BeforeAll
    static void startSWT() throws InterruptedException {
        // Boot the JavaFX platform (no stage needed)
        CountDownLatch fxReady = new CountDownLatch(1);
        Platform.startup(fxReady::countDown);
        assertTrue(fxReady.await(5, TimeUnit.SECONDS), "JavaFX Platform did not initialise in time");

        // Then start SWT as before
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
    static void stopSWT() {
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

    private FXMenuItemWrapper wrap(MenuItem fxItem) {
        FXMenuItemWrapper w = new FXMenuItemWrapper(fxItem);
        w.create(display, swtMenu, null);
        return w;
    }

    private org.eclipse.swt.widgets.MenuItem firstSwtItem() {
        org.eclipse.swt.widgets.MenuItem[] items = swtMenu.getItems();
        assertNotEquals(0, items.length, "swtMenu has no items");
        return items[0];
    }

    private int swtSubMenuItemCount() {
        for (org.eclipse.swt.widgets.MenuItem item : swtMenu.getItems()) {
            org.eclipse.swt.widgets.Menu sub = item.getMenu();
            if (sub != null && !sub.isDisposed()) {
                return (int) Arrays.stream(sub.getItems())
                        .filter(i -> !i.isDisposed())
                        .count();
            }
        }
        return 0;
    }

    // =========================================================================
    // 1. Property synchronisation — text
    // =========================================================================

    @Nested
    @DisplayName("Text property synchronisation")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class TextSync {

        @Test
        @Order(1)
        @DisplayName("Initial text is applied to the SWT item")
        void initialTextApplied() throws InterruptedException {
            MenuItem fxItem = new MenuItem("Open");
            runOnSWT(() -> wrap(fxItem));

            runOnSWT(() ->
                    assertEquals("Open", firstSwtItem().getText(),
                            "SWT item should display the initial FX text")
            );
        }

        @Test
        @Order(2)
        @DisplayName("Changing FX text updates SWT item text")
        void textChangeReflectsInSWT() throws InterruptedException {
            MenuItem fxItem = new MenuItem("Open");
            runOnSWT(() -> wrap(fxItem));

            fxItem.setText("Close");
            Thread.sleep(200);

            runOnSWT(() ->
                    assertEquals("Close", firstSwtItem().getText(),
                            "SWT item text should update when FX text changes")
            );
        }

        @Test
        @Order(3)
        @DisplayName("Null text falls back to the default 'Item' label")
        void nullTextFallsBackToDefault() throws InterruptedException {
            MenuItem fxItem = new MenuItem(null);
            runOnSWT(() -> wrap(fxItem));

            runOnSWT(() ->
                    assertEquals("Item", firstSwtItem().getText(),
                            "Null text should fall back to the default label")
            );
        }

        @Test
        @Order(4)
        @DisplayName("Blank text falls back to the default 'Item' label")
        void blankTextFallsBackToDefault() throws InterruptedException {
            MenuItem fxItem = new MenuItem("   ");
            runOnSWT(() -> wrap(fxItem));

            runOnSWT(() ->
                    assertEquals("Item", firstSwtItem().getText(),
                            "Blank text should fall back to the default label")
            );
        }

        @Test
        @Order(5)
        @DisplayName("Changing FX text to blank falls back to default on SWT side")
        void changingToBlankFallsBackToDefault() throws InterruptedException {
            MenuItem fxItem = new MenuItem("Open");
            runOnSWT(() -> wrap(fxItem));

            fxItem.setText("");
            Thread.sleep(200);

            runOnSWT(() ->
                    assertEquals("Item", firstSwtItem().getText(),
                            "SWT item should fall back to default when text is cleared")
            );
        }
    }

    // =========================================================================
    // 2. Property synchronisation — disable
    // =========================================================================

    @Nested
    @DisplayName("Disable property synchronisation")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class DisableSync {

        @Test
        @Order(1)
        @DisplayName("Item is enabled by default")
        void enabledByDefault() throws InterruptedException {
            MenuItem fxItem = new MenuItem("Open");
            runOnSWT(() -> wrap(fxItem));

            runOnSWT(() ->
                    assertTrue(firstSwtItem().isEnabled(),
                            "SWT item should be enabled when FX item is not disabled")
            );
        }

        @Test
        @Order(2)
        @DisplayName("Initially disabled FX item creates a disabled SWT item")
        void initiallyDisabledApplied() throws InterruptedException {
            MenuItem fxItem = new MenuItem("Open");
            fxItem.setDisable(true);
            runOnSWT(() -> wrap(fxItem));

            runOnSWT(() ->
                    assertFalse(firstSwtItem().isEnabled(),
                            "SWT item should be disabled when FX item starts disabled")
            );
        }

        @Test
        @Order(3)
        @DisplayName("Disabling FX item disables SWT item")
        void disablingFxDisablesSWT() throws InterruptedException {
            MenuItem fxItem = new MenuItem("Open");
            runOnSWT(() -> wrap(fxItem));

            fxItem.setDisable(true);
            Thread.sleep(200);

            runOnSWT(() ->
                    assertFalse(firstSwtItem().isEnabled(),
                            "SWT item should become disabled after FX item is disabled")
            );
        }

        @Test
        @Order(4)
        @DisplayName("Re-enabling FX item re-enables SWT item")
        void reEnablingFxReEnablesSWT() throws InterruptedException {
            MenuItem fxItem = new MenuItem("Open");
            fxItem.setDisable(true);
            runOnSWT(() -> wrap(fxItem));

            fxItem.setDisable(false);
            Thread.sleep(200);

            runOnSWT(() ->
                    assertTrue(firstSwtItem().isEnabled(),
                            "SWT item should become enabled after FX item is re-enabled")
            );
        }
    }

    // =========================================================================
    // 3. Property synchronisation — onAction
    // =========================================================================

    @Nested
    @DisplayName("onAction synchronisation")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class OnActionSync {

        @Test
        @Order(1)
        @DisplayName("Selecting the SWT item fires the FX onAction handler")
        void swtSelectionFiresFxAction() throws InterruptedException {
            AtomicBoolean fired = new AtomicBoolean(false);
            MenuItem fxItem = new MenuItem("Open");
            fxItem.setOnAction(e -> fired.set(true));

            runOnSWT(() -> wrap(fxItem));

            // Simulate SWT selection
            runOnSWT(() -> {
                org.eclipse.swt.widgets.MenuItem swtItem = firstSwtItem();
                swtItem.notifyListeners(SWT.Selection, new org.eclipse.swt.widgets.Event());
            });

            // Platform.runLater is async — give it a moment
            Thread.sleep(300);
            assertTrue(fired.get(), "onAction handler should have been called");
        }

        @Test
        @Order(2)
        @DisplayName("Replacing onAction after creation uses the new handler")
        void replacingOnActionUsesNewHandler() throws InterruptedException {
            AtomicBoolean firstFired = new AtomicBoolean(false);
            AtomicBoolean secondFired = new AtomicBoolean(false);

            MenuItem fxItem = new MenuItem("Open");
            fxItem.setOnAction(e -> firstFired.set(true));
            runOnSWT(() -> wrap(fxItem));

            // Replace the handler
            fxItem.setOnAction(e -> secondFired.set(true));
            Thread.sleep(200);

            runOnSWT(() ->
                    firstSwtItem().notifyListeners(SWT.Selection, new org.eclipse.swt.widgets.Event())
            );
            Thread.sleep(300);

            assertFalse(firstFired.get(), "Old handler must not fire");
            assertTrue(secondFired.get(), "New handler must fire");
        }

        @Test
        @Order(3)
        @DisplayName("No NullPointerException when onAction is null and SWT item is selected")
        void noNpeWhenOnActionIsNull() throws InterruptedException {
            MenuItem fxItem = new MenuItem("Open");
            // no onAction set
            runOnSWT(() -> wrap(fxItem));

            assertDoesNotThrow(() ->
                    runOnSWT(() ->
                            firstSwtItem().notifyListeners(SWT.Selection, new org.eclipse.swt.widgets.Event())
                    )
            );
        }
    }

    // =========================================================================
    // 4. CheckMenuItem behaviour
    // =========================================================================

    @Nested
    @DisplayName("CheckMenuItem behaviour")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class CheckMenuItemTests {

        @Test
        @Order(1)
        @DisplayName("SWT style is SWT.CHECK for a CheckMenuItem")
        void swtStyleIsCheck() throws InterruptedException {
            CheckMenuItem fxCheck = new CheckMenuItem("Dark Mode");
            runOnSWT(() -> wrap(fxCheck));

            runOnSWT(() ->
                    assertEquals(SWT.CHECK, firstSwtItem().getStyle() & SWT.CHECK,
                            "SWT item should have the CHECK style")
            );
        }

        @Test
        @Order(2)
        @DisplayName("Initial selected=false is mirrored to SWT")
        void initialUnselectedMirrored() throws InterruptedException {
            CheckMenuItem fxCheck = new CheckMenuItem("Dark Mode");
            fxCheck.setSelected(false);
            runOnSWT(() -> wrap(fxCheck));

            runOnSWT(() ->
                    assertFalse(firstSwtItem().getSelection(),
                            "SWT item should be unselected initially")
            );
        }

        @Test
        @Order(3)
        @DisplayName("Initial selected=true is mirrored to SWT")
        void initialSelectedMirrored() throws InterruptedException {
            CheckMenuItem fxCheck = new CheckMenuItem("Dark Mode");
            fxCheck.setSelected(true);
            runOnSWT(() -> wrap(fxCheck));

            runOnSWT(() ->
                    assertTrue(firstSwtItem().getSelection(),
                            "SWT item should be selected initially")
            );
        }

        @Test
        @Order(4)
        @DisplayName("Changing FX selectedProperty to true updates SWT selection")
        void fxSelectedTrueUpdatesSWT() throws InterruptedException {
            CheckMenuItem fxCheck = new CheckMenuItem("Dark Mode");
            runOnSWT(() -> wrap(fxCheck));

            fxCheck.setSelected(true);
            Thread.sleep(200);

            runOnSWT(() ->
                    assertTrue(firstSwtItem().getSelection(),
                            "SWT item should become selected when FX selectedProperty is set true")
            );
        }

        @Test
        @Order(5)
        @DisplayName("Changing FX selectedProperty to false updates SWT selection")
        void fxSelectedFalseUpdatesSWT() throws InterruptedException {
            CheckMenuItem fxCheck = new CheckMenuItem("Dark Mode");
            fxCheck.setSelected(true);
            runOnSWT(() -> wrap(fxCheck));

            fxCheck.setSelected(false);
            Thread.sleep(200);

            runOnSWT(() ->
                    assertFalse(firstSwtItem().getSelection(),
                            "SWT item should become unselected when FX selectedProperty is set false")
            );
        }

        @Test
        @Order(6)
        @DisplayName("Selecting SWT CHECK item feeds back to FX selectedProperty")
        void swtSelectionFeedsBackToFx() throws InterruptedException {
            CheckMenuItem fxCheck = new CheckMenuItem("Dark Mode");
            runOnSWT(() -> wrap(fxCheck));

            // Simulate the user clicking the SWT check item
            runOnSWT(() -> {
                org.eclipse.swt.widgets.MenuItem swtItem = firstSwtItem();
                swtItem.setSelection(true);
                swtItem.notifyListeners(SWT.Selection, new org.eclipse.swt.widgets.Event());
            });

            Thread.sleep(300);
            assertTrue(fxCheck.isSelected(),
                    "FX selectedProperty should update when the SWT check item is toggled");
        }

        @Test
        @Order(7)
        @DisplayName("SWT does not overwrite a bound FX selectedProperty")
        void swtDoesNotOverwriteBoundFxProperty() throws InterruptedException {
            CheckMenuItem fxCheck = new CheckMenuItem("Dark Mode");
            SimpleBooleanProperty boundProp = new SimpleBooleanProperty(false);
            fxCheck.selectedProperty().bind(boundProp);

            runOnSWT(() -> wrap(fxCheck));

            // Simulate SWT click — the bound property is read-only from FX perspective
            runOnSWT(() -> {
                org.eclipse.swt.widgets.MenuItem swtItem = firstSwtItem();
                swtItem.setSelection(true);
                swtItem.notifyListeners(SWT.Selection, new org.eclipse.swt.widgets.Event());
            });

            Thread.sleep(300);
            // The bound property itself controls the value; SWT feedback must not throw
            // and the FX property must still equal the bound source
            assertFalse(fxCheck.isSelected(),
                    "FX selectedProperty must not be written when it is bound");
            assertFalse(boundProp.get(),
                    "The bound source property must remain unchanged");
        }
    }

    // =========================================================================
    // 5. Submenu add / remove
    // =========================================================================

    @Nested
    @DisplayName("Submenu add / remove")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class SubMenuAddRemove {

        @Test
        @Order(1)
        @DisplayName("Initial children are created in the SWT submenu")
        void initialChildrenCreated() throws InterruptedException {
            Menu fxMenu = new Menu("File");
            fxMenu.getItems().addAll(new MenuItem("New"), new MenuItem("Open"), new MenuItem("Save"));

            runOnSWT(() -> wrap(fxMenu));

            runOnSWT(() ->
                    assertEquals(3, swtSubMenuItemCount(),
                            "SWT submenu should have 3 items after initial wrapping")
            );
        }

        @Test
        @Order(2)
        @DisplayName("Adding a FX item reflects in the SWT submenu")
        void addingFxItemReflectsInSWT() throws InterruptedException {
            Menu fxMenu = new Menu("File");
            fxMenu.getItems().addAll(new MenuItem("New"), new MenuItem("Open"));
            runOnSWT(() -> wrap(fxMenu));

            fxMenu.getItems().add(new MenuItem("Save"));
            Thread.sleep(200);

            runOnSWT(() ->
                    assertEquals(3, swtSubMenuItemCount(),
                            "SWT submenu should grow to 3 after adding a FX item")
            );
        }

        @Test
        @Order(3)
        @DisplayName("Removing a FX item reduces the SWT submenu count by one")
        void removingFxItemReducesSwtCount() throws InterruptedException {
            MenuItem fxNew = new MenuItem("New");
            MenuItem fxOpen = new MenuItem("Open");
            MenuItem fxSave = new MenuItem("Save");

            Menu fxMenu = new Menu("File");
            fxMenu.getItems().addAll(fxNew, fxOpen, fxSave);
            runOnSWT(() -> wrap(fxMenu));

            fxMenu.getItems().remove(fxOpen);
            Thread.sleep(200);

            runOnSWT(() ->
                    assertEquals(2, swtSubMenuItemCount(),
                            "SWT submenu should shrink to 2 after removing one FX item")
            );
        }

        @Test
        @Order(4)
        @DisplayName("Removing the first item does not dispose the parent CASCADE item")
        void removingFirstItemKeepsParentAlive() throws InterruptedException {
            Menu fxMenu = new Menu("File");
            fxMenu.getItems().addAll(new MenuItem("New"), new MenuItem("Open"), new MenuItem("Save"));
            runOnSWT(() -> wrap(fxMenu));

            fxMenu.getItems().removeFirst();
            Thread.sleep(200);

            runOnSWT(() -> {
                org.eclipse.swt.widgets.MenuItem[] items = swtMenu.getItems();
                assertTrue(items.length > 0 && !items[0].isDisposed(),
                        "Parent CASCADE item must not be disposed when a child is removed");
                assertEquals(2, swtSubMenuItemCount());
            });
        }

        @Test
        @Order(5)
        @DisplayName("Removing all items leaves an empty SWT submenu with the parent alive")
        void removingAllItemsLeavesEmptySubMenu() throws InterruptedException {
            MenuItem fxNew = new MenuItem("New");
            MenuItem fxOpen = new MenuItem("Open");
            MenuItem fxSave = new MenuItem("Save");

            Menu fxMenu = new Menu("File");
            fxMenu.getItems().addAll(fxNew, fxOpen, fxSave);
            runOnSWT(() -> wrap(fxMenu));

            fxMenu.getItems().remove(fxNew);
            Thread.sleep(100);
            fxMenu.getItems().remove(fxOpen);
            Thread.sleep(100);
            fxMenu.getItems().remove(fxSave);
            Thread.sleep(200);

            runOnSWT(() -> {
                assertEquals(0, swtSubMenuItemCount(),
                        "SWT submenu should be empty after all FX items are removed");
                org.eclipse.swt.widgets.MenuItem[] items = swtMenu.getItems();
                assertTrue(items.length > 0 && !items[0].isDisposed(),
                        "Parent CASCADE item must survive even after all children are removed");
            });
        }

        @Test
        @Order(6)
        @DisplayName("Remove then add lands on the correct count")
        void removeAndAddLandsOnCorrectCount() throws InterruptedException {
            MenuItem fxNew = new MenuItem("New");
            MenuItem fxOpen = new MenuItem("Open");

            Menu fxMenu = new Menu("File");
            fxMenu.getItems().addAll(fxNew, fxOpen);
            runOnSWT(() -> wrap(fxMenu));

            fxMenu.getItems().remove(fxOpen);
            Thread.sleep(150);
            fxMenu.getItems().add(new MenuItem("Save As"));
            Thread.sleep(200);

            runOnSWT(() ->
                    assertEquals(2, swtSubMenuItemCount(),
                            "After remove + add, SWT submenu should have 2 items")
            );
        }
    }

    // =========================================================================
    // 6. Edge cases
    // =========================================================================

    @Nested
    @DisplayName("Edge cases")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class EdgeCases {

        @Test
        @Order(1)
        @DisplayName("SWT style is SWT.SEPARATOR for a SeparatorMenuItem")
        void separatorHasSeparatorStyle() throws InterruptedException {
            SeparatorMenuItem fxSep = new SeparatorMenuItem();
            runOnSWT(() -> wrap(fxSep));

            runOnSWT(() ->
                    assertEquals(SWT.SEPARATOR, firstSwtItem().getStyle() & SWT.SEPARATOR,
                            "SWT item for a SeparatorMenuItem must have the SEPARATOR style")
            );
        }

        @Test
        @Order(2)
        @DisplayName("SWT style is SWT.CASCADE for a javafx.scene.control.Menu")
        void menuHasCascadeStyle() throws InterruptedException {
            Menu fxMenu = new Menu("File");
            runOnSWT(() -> wrap(fxMenu));

            runOnSWT(() ->
                    assertEquals(SWT.CASCADE, firstSwtItem().getStyle() & SWT.CASCADE,
                            "SWT item for a JavaFX Menu must have the CASCADE style")
            );
        }

        @Test
        @Order(3)
        @DisplayName("SWT style is SWT.PUSH for a regular MenuItem")
        void regularItemHasPushStyle() throws InterruptedException {
            MenuItem fxItem = new MenuItem("Open");
            runOnSWT(() -> wrap(fxItem));

            runOnSWT(() ->
                    assertEquals(SWT.PUSH, firstSwtItem().getStyle() & SWT.PUSH,
                            "SWT item for a regular MenuItem must have the PUSH style")
            );
        }

        @Test
        @Order(4)
        @DisplayName("Disposing the wrapper disposes the SWT item")
        void disposingWrapperDisposesSwt() throws InterruptedException {
            MenuItem fxItem = new MenuItem("Open");
            AtomicReference<FXMenuItemWrapper> wrapperRef = new AtomicReference<>();
            runOnSWT(() -> wrapperRef.set(wrap(fxItem)));

            AtomicReference<org.eclipse.swt.widgets.MenuItem> swtRef = new AtomicReference<>();
            runOnSWT(() -> swtRef.set(firstSwtItem()));

            runOnSWT(() -> wrapperRef.get().dispose());

            runOnSWT(() ->
                    assertTrue(swtRef.get().isDisposed(),
                            "SWT item must be disposed after wrapper.dispose() is called")
            );
        }

        @Test
        @Order(5)
        @DisplayName("FX property changes after dispose do not throw")
        void propertyChangesAfterDisposeDoNotThrow() throws InterruptedException {
            MenuItem fxItem = new MenuItem("Open");
            AtomicReference<FXMenuItemWrapper> wrapperRef = new AtomicReference<>();
            runOnSWT(() -> wrapperRef.set(wrap(fxItem)));
            runOnSWT(() -> wrapperRef.get().dispose());

            // Mutate FX properties after the SWT item is gone
            assertDoesNotThrow(() -> {
                fxItem.setText("Closed");
                fxItem.setDisable(true);
                Thread.sleep(200); // let asyncExec guard clauses fire
            });
        }

        @Test
        @Order(6)
        @DisplayName("Nested submenus — grandchild items are created correctly")
        void nestedSubMenusCreateGrandchildren() throws InterruptedException {
            Menu fxChild = new Menu("Recent");
            fxChild.getItems().addAll(new MenuItem("file1.txt"), new MenuItem("file2.txt"));

            Menu fxRoot = new Menu("File");
            fxRoot.getItems().add(fxChild);

            runOnSWT(() -> wrap(fxRoot));

            runOnSWT(() -> {
                // Root CASCADE
                org.eclipse.swt.widgets.MenuItem rootSwtItem = firstSwtItem();
                assertNotNull(rootSwtItem.getMenu(), "Root CASCADE must have a submenu");

                // Child CASCADE
                org.eclipse.swt.widgets.MenuItem[] rootChildren = rootSwtItem.getMenu().getItems();
                assertEquals(1, rootChildren.length, "Root submenu should have 1 child (Recent)");
                assertEquals(SWT.CASCADE, rootChildren[0].getStyle() & SWT.CASCADE,
                        "The 'Recent' child should be a CASCADE item");

                // Grandchildren
                org.eclipse.swt.widgets.Menu grandMenu = rootChildren[0].getMenu();
                assertNotNull(grandMenu, "The 'Recent' CASCADE must have its own submenu");
                assertEquals(2, grandMenu.getItems().length,
                        "The 'Recent' submenu should contain 2 grandchild items");
            });
        }

        @Test
        @Order(7)
        @DisplayName("Adding a nested FX item reflects in the grandchild SWT menu")
        void addingNestedFxItemReflectsInGrandchildSwtMenu() throws InterruptedException {
            Menu fxChild = new Menu("Recent");
            fxChild.getItems().add(new MenuItem("file1.txt"));

            Menu fxRoot = new Menu("File");
            fxRoot.getItems().add(fxChild);

            runOnSWT(() -> wrap(fxRoot));

            fxChild.getItems().add(new MenuItem("file2.txt"));
            Thread.sleep(200);

            runOnSWT(() -> {
                org.eclipse.swt.widgets.MenuItem rootSwtItem = firstSwtItem();
                org.eclipse.swt.widgets.Menu grandMenu =
                        rootSwtItem.getMenu().getItems()[0].getMenu();
                assertNotNull(grandMenu);
                assertEquals(2, grandMenu.getItems().length,
                        "Grandchild SWT menu should have 2 items after adding to nested FX menu");
            });
        }
    }
}
