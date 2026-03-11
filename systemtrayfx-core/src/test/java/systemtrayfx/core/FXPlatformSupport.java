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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Shared JavaFX platform lifecycle helper for unit tests.
 *
 * <p>JavaFX's {@link Platform} is a JVM-wide singleton: once
 * {@link Platform#exit()} is called it cannot be restarted in the same JVM.
 * When Maven Surefire runs all test classes in a single forked JVM the first
 * test class that shuts down the platform kills it for every class that runs
 * afterwards.
 *
 * <p>This helper starts the platform exactly once (guarded by a static flag)
 * and never calls {@link Platform#exit()} directly. Shutdown is deferred to a
 * JVM shutdown hook so the platform stays alive for the full test run.
 *
 * <p><strong>Usage in each test class:</strong>
 * <pre>{@code
 * @BeforeAll
 * static void startFX() throws InterruptedException {
 *     FXPlatformSupport.start();
 * }
 *
 * // No @AfterAll Platform.exit() call needed — the shutdown hook handles it.
 * }</pre>
 */
final class FXPlatformSupport {

    private static final AtomicBoolean started = new AtomicBoolean(false);

    static {
        // Shut the platform down cleanly when the JVM exits, regardless of
        // which test class happens to be last.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (started.get()) {
                Platform.exit();
            }
        }, "FX-Shutdown-Hook"));
    }

    private FXPlatformSupport() {}

    /**
     * Starts the JavaFX platform if it has not already been started.
     * Safe to call from multiple {@code @BeforeAll} methods across different
     * test classes — subsequent calls are a no-op.
     *
     * @throws InterruptedException if the calling thread is interrupted while
     *                              waiting for the platform to become ready
     * @throws AssertionError       if the platform does not become ready within
     *                              5 seconds
     */
    static void start() throws InterruptedException {
        if (started.compareAndSet(false, true)) {
            CountDownLatch ready = new CountDownLatch(1);
            Platform.startup(ready::countDown);
            if (!ready.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("JavaFX Platform did not initialise in time");
            }
        }
    }
}