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

package org.seaborne.delta.zk.direct;

import org.apache.zookeeper.*;
import org.seaborne.delta.zk.ZkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Supplier;

public final class ValidZooKeeperSupplier implements Supplier<ZooKeeper>, Watcher {
    private static final Logger LOG = LoggerFactory.getLogger(ValidZooKeeperSupplier.class);
    private final Object token = new Object();
    private final int retries;
    private String connectString;
    private ZooKeeper zooKeeper;
    private boolean isValid = false;

    public ValidZooKeeperSupplier(final String connectString) throws IOException, KeeperException, InterruptedException {
        this(connectString, 5);
    }

    public ValidZooKeeperSupplier(final String connectString, final int retries) throws IOException, KeeperException, InterruptedException {
        this.connectString = connectString;
        this.retries = retries;
        this.connect(connectString);
    }

    private void connect(final String connectString) throws IOException, KeeperException, InterruptedException {
        this.zooKeeper = new ZooKeeper(
                connectString,
            10_000,
            watchedEvent -> {
                synchronized (this.token) {
                    switch (watchedEvent.getState()) {
                        case SyncConnected:
                        case SaslAuthenticated:
                        case ConnectedReadOnly:
                            this.isValid = true;
                            break;
                        case Closed:
                        case Expired:
                        case AuthFailed:
                        case Disconnected:
                            this.isValid = false;
                    }
                    this.token.notifyAll();
                }
            }
        );
        this.updateConfig();
    }

    @Override
    public ZooKeeper get() {
        try {
            int tries = 1;
            while (!this.isValid) {
                synchronized (this.token) {
                    switch (this.zooKeeper.getState()) {
                        case CONNECTING:
                            this.token.wait();
                            break;
                        case AUTH_FAILED:
                            throw new ZkException("Authentication failed.");
                        case CLOSED:
                        case NOT_CONNECTED:
                            try {
                                this.connect(this.connectString);
                            } catch (final IOException | KeeperException e) {
                                LOG.error("Unable to connect to the ZooKeeper Ensemble.", e);
                            }
                    }
                }
                if (tries == this.retries) {
                    throw new ZkException(
                        String.format(
                            "Failed after %d attempts to connect to the ZooKeeper Ensemble.",
                            this.retries
                        )
                    );
                } else {
                    tries++;
                }
            }
            return this.zooKeeper;
        } catch (final InterruptedException e) {
            throw new ZkException("Interrupted while attempting to connect to the ZooKeeper Ensemble.", e);
        }
    }

    private void updateConfig() throws KeeperException, InterruptedException, IOException {
        final byte[] newConfig = this.get().getConfig(
            this,
            this.get().exists(
                ZooDefs.CONFIG_NODE,
                false
            )
        );
        if (newConfig.length > 0) {
            this.get().updateServerList(new String(newConfig));
        }
    }

    @Override
    public void process(final WatchedEvent event) {
        if (event.getType() == Watcher.Event.EventType.NodeDataChanged) {
            try {
                this.updateConfig();
            } catch (final KeeperException | InterruptedException e) {
                LOG.error("Failure retrieving the updated ZooKeeper config.", e);
            } catch (IOException e) {
                throw new ZkException("Failure updating the ZooKeeper config.", e);
            }
        }
    }
}
