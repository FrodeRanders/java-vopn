/*
 * Copyright (C) 2025 Frode Randers
 * All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.gautelis.vopn.server;

/**
 * Server lifecycle contract used by the request processors.
 */
public interface Server {

    /**
     * Requests a graceful shutdown of the server.
     *
     * @param reason human-readable shutdown reason
     */
    void requestShutdown(String reason);

    /**
     * Starts the server event loop.
     */
    void start();

    /**
     * Hook invoked when the server is stopping.
     */
    void stopping();

    /**
     * Hook invoked after the server has stopped.
     */
    void stopped();
}
