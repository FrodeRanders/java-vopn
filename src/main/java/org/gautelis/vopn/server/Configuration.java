/*
 * Copyright (C) 2025-2026 Frode Randers
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

public interface Configuration {

    @Configurable(property = "request-processor-classname", value="org.gautelis.vopn.server.RequestProcessor")
    String requestProcessorClassname();

    @Configurable(property = "num-request-threads", value = "8")
    int numRequestThreads();

    @Configurable(property = "shutdown-grace-period", value = "30")
    int shutdownGracePeriod();

    @Configurable(property = "remote-host")
    String remoteHost();

    @Configurable(property = "remote-port")
    int remotePort();

    @Configurable(property = "local-host", value = "localhost")
    String localHost();

    @Configurable(property = "local-port", value = "4100")
    int localPort();

    @Configurable(property = "username")
    String username();

    @Configurable(property = "password")
    String password();

    @Configurable(property = "debug", value = "false")
    boolean doDebug();
}
