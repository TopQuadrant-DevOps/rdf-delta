package org.seaborne.delta.zk;

import org.apache.jena.atlas.json.JsonObject;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;

import java.util.List;
import java.util.function.Supplier;

public final class ExponentialBackoffZkConnection implements ZkConnection {
    private final ZkConnection connection;
    private final int retries;

    public ExponentialBackoffZkConnection(final ZkConnection connection, final int retries) {
        this.connection = connection;
        this.retries = retries;
    }

    @FunctionalInterface
    private interface ZkMethod<T> {
        T evaluate() throws Exception;
    }

    @FunctionalInterface
    private interface ZkProc {
        void evaluate() throws Exception;
    }

    private <T> T retry(final ZkMethod<T> method) throws Exception {
        T result = null;
        var i = 0;
        while (result == null) {
            try {
                result = method.evaluate();
            } catch (final KeeperException e) {
                if (i == this.retries) {
                    throw e;
                } else {
                    Thread.sleep((long) Math.pow(2, i));
                }
            }
            i++;
        }
        return result;
    }

    private void retry(final ZkProc method) throws Exception {
        retry(() -> { method.evaluate(); return 0; });
    }

    private void uncheckedRetry(final Runnable method) {
        uncheckedRetry(() -> { method.run(); return 0; });
    }

    private <T> T uncheckedRetry(final Supplier<T> method) {
        try {
            return retry(method::get);
        } catch(final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            // None expected, however, if one is received:
            throw new RuntimeException(e);
        }
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
        uncheckedRetry(() -> connection.runWithLock(path, action));
    }

    @Override
    public <X> X runWithLock(final String path, final Supplier<X> action) {
        return uncheckedRetry(() -> connection.runWithLock(path, action));
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }
}
