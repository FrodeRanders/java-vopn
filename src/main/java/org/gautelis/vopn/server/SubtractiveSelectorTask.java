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
 * Represents a wish (task) for removing interest.
 * Issued by a request processor thread, put on the SelectorQueue
 * and picked up by the main server thread.
 */class SubtractiveSelectorTask extends SelectorTask
{
    /**
     * Creates a subtractive selector task.
     * @param request in question (just served)
     * @param interest to be removed
     */
    SubtractiveSelectorTask(Request request, int interest) {
        super(request, interest);
    }

    /**
     * Queries whether selector task adds interest.
     * @return true if additive, false if subtractive
     */
    @Override
    boolean isAdditive() {
        return false;
    }
}
