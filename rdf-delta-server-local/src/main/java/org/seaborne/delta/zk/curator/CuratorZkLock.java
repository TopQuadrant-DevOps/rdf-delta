/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  See the NOTICE file distributed with this work for additional
 *  information regarding copyright ownership.
 */

package org.seaborne.delta.zk.curator;

import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.seaborne.delta.zk.ZkLock;

/**
 * A lock handle to an Apache Curator distributed lock.
 */
public final class CuratorZkLock implements ZkLock {
    /**
     * The lock represented by this handle.
     */
    private final InterProcessLock lock;

    /**
     * Instantiates a new {@link CuratorZkLock} with the supplied {@link InterProcessLock}.
     * @param lock The lock represented by this handle.
     */
    CuratorZkLock(final InterProcessLock lock) {
        this.lock = lock;
    }

    @Override
    public void close() throws Exception {
        this.lock.release();
    }
}
