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

package org.seaborne.delta.server.patchstores;

import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Before;
import org.seaborne.delta.lib.LogX;
import org.seaborne.delta.server.ZkT;
import org.seaborne.delta.server.local.patchstores.PatchStorage;
import org.seaborne.delta.server.local.patchstores.zk.PatchStorageZk;
import org.seaborne.delta.zk.ExponentialBackoffZkConnection;
import org.seaborne.delta.zk.UncheckedZkConnection;
import org.seaborne.delta.zk.WrappedUncheckedZkConnection;
import org.seaborne.delta.zk.direct.DirectZkConnection;

public class TestPatchStorageZk extends AbstractTestPatchStorage {
    static { LogX.setJavaLogging(); }

     private static int counter = 0 ;
     private TestingServer server = null;
     private UncheckedZkConnection client = null;
     private String patches = "/patches-"+(counter++);

     @Before public void before() {
        try {
            server = ZkT.localServer();
            server.start();
            String connectionString = "localhost:"+server.getPort();
            client = new WrappedUncheckedZkConnection(
                new ExponentialBackoffZkConnection(
                    DirectZkConnection.connect(connectionString),
                    5
                )
            );
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    @After public void after() {
        try {
            client.close();
            server.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    @Override
    protected PatchStorage patchStorage() {
        return new PatchStorageZk(client, "0", patches);
    }
}
