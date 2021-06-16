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

package org.seaborne.delta.zk;

import java.util.List;
import java.util.function.Supplier;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;

/**
 * Retries method calls using an exponential wait between attempts. This is not
 * Exponential Backoff which is a more complex algorithm concerned with avoiding collisions.
 * The failures anticipated with these method invocations are not collisions, so that
 * complexity is not needed.
 */
public final class ExponentialRetryZkConnection implements ZkConnection {
    private final ZkConnection connection;
    private final int retries;

    public ExponentialRetryZkConnection(final ZkConnection connection, final int retries) {
        this.connection = connection;
        this.retries = retries;
    }

    @FunctionalInterface
    private interface Method<T> {
        T evaluate() throws Exception;
    }

    @FunctionalInterface
    private interface Proc {
        void evaluate() throws Exception;
    }

    /**
     * Retry with an exponentially increasing wait time between attempts.
     *
     * <p>
     *     Try to evaluate the method. If it fails, if we've exhausted the number of retries,
     *     rethrow the exception, but if we haven't, sleep for 2^n milliseconds where n
     *     is the number of the current attempt. If it succeeds, stop trying and return the
     *     result.
     * </p>
     * @param method The method to try
     * @param <T> The type returned by the method
     * @return The result of the method invocation
     * @throws Exception If the method never succeeds
     */
    private <T> T retry(final Method<T> method) throws Exception {
        T result = null;
        var counter = 1;
        while (result == null) {
            try {
                result = method.evaluate();
            } catch (final KeeperException e) {
                if (counter == this.retries) {
                    throw e;
                } else {
                    Thread.sleep((long) Math.pow(2, counter));
                    counter++;
                }
            }
        }
        return result;
    }

    private void retry(final Proc method) throws Exception {
        retry(
            () -> {
                method.evaluate();
                return 0;
            }
        );
    }

    @Override
    public boolean pathExists(final String path) throws Exception {
        return retry(() -> connection.pathExists(path));
    }

    @Override
    public String ensurePathExists(final String path) throws Exception {
        return retry(() -> connection.ensurePathExists(path));
    }

    @Override
    public byte[] fetch(final String path) throws Exception {
        return retry(() -> connection.fetch(path));
    }

    @Override
    public byte[] fetch(final Watcher watcher, final String path) throws Exception {
        return retry(() -> connection.fetch(watcher, path));
    }

    @Override
    public JsonObject fetchJson(final String path) throws Exception {
        return retry(() -> connection.fetchJson(path));
    }

    @Override
    public JsonObject fetchJson(final Watcher watcher, final String path) throws Exception {
        return retry(() -> connection.fetchJson(watcher, path));
    }

    @Override
    public List<String> fetchChildren(final String path) throws Exception {
        return retry(() -> connection.fetchChildren(path));
    }

    @Override
    public List<String> fetchChildren(final Watcher watcher, final String path) throws Exception {
        return retry(() -> connection.fetchChildren(watcher, path));
    }

    @Override
    public String createZNode(final String path) throws Exception {
        return retry(() -> connection.createZNode(path));
    }

    @Override
    public String createZNode(final String path, final CreateMode mode) throws Exception {
        return retry(() -> connection.createZNode(path, mode));
    }

    @Override
    public String createAndSetZNode(final String path, final JsonObject object) throws Exception {
        return retry(() -> connection.createAndSetZNode(path, object));
    }

    @Override
    public String createAndSetZNode(final String path, final byte[] bytes) throws Exception {
        return retry(() -> connection.createAndSetZNode(path, bytes));
    }

    @Override
    public void setZNode(final String path, final JsonObject object) throws Exception {
        retry(() -> connection.setZNode(path, object));
    }

    @Override
    public void setZNode(final String path, final byte[] bytes) throws Exception {
        retry(() -> connection.setZNode(path, bytes));
    }

    @Override
    public void deleteZNodeAndChildren(final String path) throws Exception {
        retry(() -> connection.deleteZNodeAndChildren(path));
    }

    @Override
    public void runWithLock(final String path, final Runnable action) {
        connection.runWithLock(path, action);
    }

    @Override
    public <X> X runWithLock(final String path, final Supplier<X> action) {
        return connection.runWithLock(path, action);
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }
}
