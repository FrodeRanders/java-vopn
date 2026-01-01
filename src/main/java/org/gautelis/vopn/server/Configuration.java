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

import org.gautelis.vopn.lang.Configurable;

/**
 * Configuration contract for server instances.
 */
public interface Configuration {

    /**
     * Returns the fully qualified request processor class name.
     *
     * @return processor class name
     */
    @Configurable(property = "request-processor-classname", value="org.gautelis.vopn.server.RequestProcessor")
    String requestProcessorClassname();

    /**
     * Returns the number of request processor threads.
     *
     * @return thread count
     */
    @Configurable(property = "num-request-threads", value = "8")
    int numRequestThreads();

    /**
     * Returns the graceful shutdown grace period in seconds.
     *
     * @return grace period in seconds
     */
    @Configurable(property = "shutdown-grace-period", value = "30")
    int shutdownGracePeriod();

    /**
     * Returns the configured remote host.
     *
     * @return remote host
     */
    @Configurable(property = "remote-host")
    String remoteHost();

    /**
     * Returns the configured remote port.
     *
     * @return remote port
     */
    @Configurable(property = "remote-port")
    int remotePort();

    /**
     * Returns the local host bind name.
     *
     * @return local host
     */
    @Configurable(property = "local-host", value = "localhost")
    String localHost();

    /**
     * Returns the local port to bind the server to.
     *
     * @return local port
     */
    @Configurable(property = "local-port", value = "4100")
    int localPort();

    /**
     * Returns the configured username.
     *
     * @return username
     */
    @Configurable(property = "username")
    String username();

    /**
     * Returns the configured password.
     *
     * @return password
     */
    @Configurable(property = "password")
    String password();

    /**
     * Returns whether debug mode is enabled.
     *
     * @return {@code true} if debug is enabled
     */
    @Configurable(property = "debug", value = "false")
    boolean doDebug();
}
