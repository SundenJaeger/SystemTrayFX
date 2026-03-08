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

import javafx.event.ActionEvent;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link Notification}.
 *
 * <p>Because {@link Notification} is a static utility class we reset its
 * internal state via reflection between tests so that each test starts clean.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotificationTest {

    // -------------------------------------------------------------------------
    // Reflection helpers to reach the package-private / private internals
    // -------------------------------------------------------------------------

    private static void setTray(TrayNotification tray) throws Exception {
        Field f = Notification.class.getDeclaredField("tray");
        f.setAccessible(true);
        f.set(null, tray);
    }

    @SuppressWarnings("unchecked")
    private static Queue<Runnable> getPending() throws Exception {
        Field f = Notification.class.getDeclaredField("pending");
        f.setAccessible(true);
        return (Queue<Runnable>) f.get(null);
    }

    /** Wipes internal state before / after each test. */
    @BeforeEach
    @AfterEach
    void resetState() throws Exception {
        setTray(null);
        getPending().clear();
    }

    private TrayNotification mockTray() {
        return Mockito.mock(TrayNotification.class);
    }

    // =========================================================================
    // 1. Instantiation guard
    // =========================================================================

    @Nested
    @DisplayName("Instantiation guard")
    class InstantiationGuard {

        @Test
        @DisplayName("Constructor throws UnsupportedOperationException via reflection")
        void constructorThrowsViaReflection() throws Exception {
            Constructor<Notification> ctor = Notification.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            assertThrows(Exception.class, ctor::newInstance,
                    "Constructing Notification must throw");
        }
    }

    // =========================================================================
    // 2. register()
    // =========================================================================

    @Nested
    @DisplayName("register()")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Register {

        @Test
        @Order(1)
        @DisplayName("register(null) throws NullPointerException")
        void registerNullThrows() {
            assertThrows(NullPointerException.class, () -> Notification.register(null),
                    "register(null) must throw NullPointerException");
        }

        @Test
        @Order(2)
        @DisplayName("Pending notifications are flushed when register() is called")
        void pendingFlushedOnRegister() throws Exception {
            // Queue a notification before the tray is ready
            Notification.info("Title", "Message");
            assertEquals(1, getPending().size(), "Pending queue should have 1 task before registration");

            TrayNotification tray = mockTray();
            Notification.register(tray);

            assertEquals(0, getPending().size(), "Pending queue must be empty after register()");
            verify(tray, times(1)).show(eq("Title"), eq("Message"), eq(NotificationType.INFORMATION), isNull());
        }

        @Test
        @Order(3)
        @DisplayName("Multiple pending notifications are all flushed")
        void multiplePendingFlushedOnRegister() throws Exception {
            Notification.info("T1", "M1");
            Notification.warn("T2", "M2");
            Notification.error("T3", "M3");
            assertEquals(3, getPending().size());

            TrayNotification tray = mockTray();
            Notification.register(tray);

            assertEquals(0, getPending().size());
            verify(tray, times(3)).show(any(), any(), any(), any());
        }
    }

    // =========================================================================
    // 3. info() / warn() / error()
    // =========================================================================

    @Nested
    @DisplayName("info() / warn() / error() — delegate to TrayNotification.show()")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ShowMethods {

        @Test
        @Order(1)
        @DisplayName("info() calls show() with INFORMATION type")
        void infoCallsShowWithInformationType() throws Exception {
            TrayNotification tray = mockTray();
            Notification.register(tray);

            Notification.info("Hello", "World");

            verify(tray).show("Hello", "World", NotificationType.INFORMATION, null);
        }

        @Test
        @Order(2)
        @DisplayName("warn() calls show() with WARNING type")
        void warnCallsShowWithWarningType() throws Exception {
            TrayNotification tray = mockTray();
            Notification.register(tray);

            Notification.warn("Careful", "Something is off");

            verify(tray).show("Careful", "Something is off", NotificationType.WARNING, null);
        }

        @Test
        @Order(3)
        @DisplayName("error() calls show() with ERROR type")
        void errorCallsShowWithErrorType() throws Exception {
            TrayNotification tray = mockTray();
            Notification.register(tray);

            Notification.error("Boom", "It broke");

            verify(tray).show("Boom", "It broke", NotificationType.ERROR, null);
        }

        @Test
        @Order(4)
        @DisplayName("onAction callback is forwarded to show()")
        void onActionCallbackForwarded() throws Exception {
            TrayNotification tray = mockTray();
            Notification.register(tray);

            AtomicReference<javafx.event.EventHandler<ActionEvent>> handler = new AtomicReference<>();
            javafx.event.EventHandler<ActionEvent> cb = e -> {};
            handler.set(cb);

            Notification.info("T", "M", cb);

            verify(tray).show("T", "M", NotificationType.INFORMATION, cb);
        }
    }

    // =========================================================================
    // 4. Failure modes — null arguments
    // =========================================================================

    @Nested
    @DisplayName("Failure modes — null arguments")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class NullArguments {

        @Test
        @Order(1)
        @DisplayName("info(null, message) throws NullPointerException")
        void infoNullTitleThrows() {
            assertThrows(NullPointerException.class, () -> Notification.info(null, "msg"),
                    "Null title must throw NullPointerException");
        }

        @Test
        @Order(2)
        @DisplayName("info(title, null) throws NullPointerException")
        void infoNullMessageThrows() {
            assertThrows(NullPointerException.class, () -> Notification.info("title", null),
                    "Null message must throw NullPointerException");
        }

        @Test
        @Order(3)
        @DisplayName("warn(null, message) throws NullPointerException")
        void warnNullTitleThrows() {
            assertThrows(NullPointerException.class, () -> Notification.warn(null, "msg"));
        }

        @Test
        @Order(4)
        @DisplayName("error(null, message) throws NullPointerException")
        void errorNullTitleThrows() {
            assertThrows(NullPointerException.class, () -> Notification.error(null, "msg"));
        }
    }

    // =========================================================================
    // 5. Queuing behaviour — tray not yet registered
    // =========================================================================

    @Nested
    @DisplayName("Queuing behaviour — tray not yet registered")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Queuing {

        @Test
        @Order(1)
        @DisplayName("Calling info() before register() adds one item to the pending queue")
        void infoBeforeRegisterQueuesPending() throws Exception {
            Notification.info("T", "M");
            assertEquals(1, getPending().size(),
                    "One notification must be queued when the tray is not yet registered");
        }

        @Test
        @Order(2)
        @DisplayName("Calling info() after register() does NOT add to pending queue")
        void infoAfterRegisterDoesNotQueue() throws Exception {
            Notification.register(mockTray());

            Notification.info("T", "M");

            assertEquals(0, getPending().size(),
                    "No items should be queued when the tray is already registered");
        }

        @Test
        @Order(3)
        @DisplayName("Queue is cleared on dispose(), not triggered again by subsequent register()")
        void queueClearedOnDispose() throws Exception {
            Notification.info("T", "M"); // queued
            Notification.dispose();

            assertEquals(0, getPending().size(), "dispose() must clear the pending queue");

            // register a new tray — it must not receive the cleared notification
            TrayNotification freshTray = mockTray();
            Notification.register(freshTray);

            verify(freshTray, never()).show(any(), any(), any(), any());
        }
    }

    // =========================================================================
    // 6. dispose()
    // =========================================================================

    @Nested
    @DisplayName("dispose()")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class DisposeTests {

        @Test
        @Order(1)
        @DisplayName("dispose() calls tray.dispose() when tray is registered")
        void disposeCallsTrayDispose() throws Exception {
            TrayNotification tray = mockTray();
            Notification.register(tray);

            Notification.dispose();

            verify(tray, times(1)).dispose();
        }

        @Test
        @Order(2)
        @DisplayName("dispose() when no tray is registered does not throw")
        void disposeWithNoTrayDoesNotThrow() {
            assertDoesNotThrow(Notification::dispose,
                    "dispose() must not throw when no tray has been registered");
        }

        @Test
        @Order(3)
        @DisplayName("Calling dispose() twice does not throw")
        void doubleDisposeDoesNotThrow() throws Exception {
            Notification.register(mockTray());
            assertDoesNotThrow(() -> {
                Notification.dispose();
                Notification.dispose();
            }, "Calling dispose() twice must not throw");
        }

        @Test
        @Order(4)
        @DisplayName("After dispose(), new notifications are queued (tray is null again)")
        void afterDisposeNotificationsAreQueued() throws Exception {
            Notification.register(mockTray());
            Notification.dispose();

            Notification.info("T", "M");

            assertEquals(1, getPending().size(),
                    "After dispose(), notifications must be queued until a new tray is registered");
        }
    }
}