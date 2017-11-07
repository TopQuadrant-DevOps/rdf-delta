/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.seaborne.delta.server.local.patchlog;

import java.nio.file.Path ;
import java.util.List;
import java.util.Map ;
import java.util.concurrent.ConcurrentHashMap ;

import org.apache.jena.atlas.logging.FmtLog ;
import org.seaborne.delta.DataSourceDescription;
import org.seaborne.delta.DeltaException ;
import org.seaborne.delta.Id ;
import org.seaborne.delta.lib.IOX ;
import org.seaborne.delta.server.local.DataSource;
import org.seaborne.delta.server.local.LocalServerConfig;
import org.slf4j.Logger ;
import org.slf4j.LoggerFactory ;

/**
 * A {@code PatchStore} is manager for a number of {@link PatchLog}s. One {@link PatchLog}
 * is the log for one data source; a {@code PatchStore} is a collection of such logs and
 * it is responsible for initialization, restoring {@code PatchLog}s on external storage
 * (files etc.)
 * <p>
 * There is normally one {@code PatchStore} for each patch storage technology but this is
 * not a requirement.
 */
public abstract class PatchStore {
    protected static Logger LOG = LoggerFactory.getLogger(PatchStore.class); 
    
    // ---- PatchStore.Provider

    // -------- Global
    // XXX Split out PatchStore.Provider management.
    // DataRegistry?
    private static Map<Id, PatchLog> logs = new ConcurrentHashMap<>();

    /** Return the {@link PatchLog}, which must already exist. */ 
    public static PatchLog getLog(Id dsRef) { 
        return logs.get(dsRef);
    }
    
    protected static boolean logExists(Id dsRef) {
        return logs.containsKey(dsRef);
    }
    
    /**
     * Release ("delete") the {@link PatchLog}. 
     * @param patchLog
     */
    public static void release(PatchLog patchLog) {
        Id dsRef = patchLog.getDescription().getDataSourceId();
        if ( ! logExists(dsRef) ) {
            FmtLog.warn(LOG, "PatchLog not known to PatchStore: dsRef=%s", dsRef);
            return;
        }
        logs.remove(dsRef);
        patchLog.release();
    }
    
    public static void clearPatchLogs() {
        logs.clear() ;
    }
    
    // ---- Global
    
    // -------- Instance
    private final String providerName ;
    
    protected PatchStore(String providerName) {
        this.providerName = providerName ;
    }
    
    /** Return the name of the provider implementation. */ 
    public String getProviderName() { 
        return providerName;
    }
    
    /** Does the PAtchStore have state across restarts? */ 
    public boolean isEphemeral() {
        return false;
    }
    
    /** All the {@link DataSource} on external persistent storage. */
    public abstract List<DataSource> listPersistent(LocalServerConfig config);
    
    /** All the {@link DataSource} currently managed by the {@code PatchStore}. */
    // Somewhat related to the DataRegistry.
    // Could scan than looking for this PatchStore.
    public abstract List<DataSourceDescription> listDataSources();

    /**
     * Return a new {@link PatchLog}. Checking that there is no registered
     * {@link PatchLog} for this {@code dsRef} has already been done.
     * 
     * @param dsRef
     * @param dsName
     * @param path : Path to the Logs/ directory. Contents are PatchStore-impl specific.
     * @return PatchLog
     */
    protected abstract PatchLog create(DataSourceDescription dsd, Path path);

    /** Return a new {@link PatchLog}, which must not already exist. */ 
    public PatchLog createLog(DataSourceDescription dsd, Path path) {
        // Path to "Log/" area workspace
        Id dsRef = dsd.getId();
        if ( logExists(dsRef) )
            throw new DeltaException("Can't create - PatchLog exists");
        IOX.ensureDirectory(path);
        PatchLog patchLog = create(dsd, path);
        logs.put(dsRef, patchLog);
        return patchLog;
    }
}
