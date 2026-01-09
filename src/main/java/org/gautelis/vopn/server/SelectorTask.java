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

/**
 * Base class for selector interest update tasks.
 */
public abstract class SelectorTask {
    private final Request request;
    private final int interest;

    /**
     * Creates a selector task for a request and interest mask.
     *
     * @param request request owning the selection key
     * @param interest interest ops to add or remove
     */
    SelectorTask(Request request, int interest) {
        this.request = request;
        this.interest = interest;
    }

    /**
     * Returns the request tied to this task.
     *
     * @return request instance
     */
    Request getRequest() {
        return request;
    }

    /**
     * Returns the selection key interest mask.
     *
     * @return interest ops
     */
    int getInterest() {
        return interest;
    }

    /**
     * Returns whether this task adds or removes interest.
     *
     * @return {@code true} if additive
     */
    abstract boolean isAdditive();
}
