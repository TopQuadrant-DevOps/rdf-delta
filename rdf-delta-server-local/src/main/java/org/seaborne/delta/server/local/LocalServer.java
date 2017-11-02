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

package org.seaborne.delta.server.local;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.lib.FileOps;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.tdb.base.file.Location;
import org.seaborne.delta.DataSourceDescription;
import org.seaborne.delta.DeltaBadRequestException;
import org.seaborne.delta.DeltaConfigException;
import org.seaborne.delta.DeltaConst;
import org.seaborne.delta.DeltaException;
import org.seaborne.delta.Id;
import org.seaborne.delta.PatchLogInfo;
import org.seaborne.delta.lib.IOX;
import org.seaborne.delta.lib.LibX;
import org.seaborne.delta.link.DeltaLink;
import org.seaborne.delta.server.local.patchlog.PatchStore ;
import org.seaborne.delta.server.local.patchlog.PatchStoreFile ;
import org.seaborne.delta.server.system.DeltaSystem ;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A local server.
 *  <p>
 *  This provides for several {@link DataSource} areas (one per managed patch set - i.e. one per dataset).
 *  {@code LocalServer} is responsible for server wide configuration and for the 
 *  {@link DataSource} lifecycle of create and delete.  
 *    
 * @see DeltaLinkLocal 
 * @see DataSource 
 *
 */
public class LocalServer {
    private static Logger LOG = LoggerFactory.getLogger(LocalServer.class);

    static { 
        DeltaSystem.init();
        initSystem();
    }
    
    /** After Delta has initialized, make sure some sort of PatchStore provision is set. */ 
    static private void initSystem() {
        // Ensure the file-based PatchStore provider is available
        if ( ! PatchStore.isRegistered(DPS.PatchStoreFileProvider) ) {
            FmtLog.warn(LOG, "PatchStoreFile provider not registered");
            PatchStore ps = new PatchStoreFile();
            if ( ! DPS.PatchStoreFileProvider.equals(ps.getProviderName())) {
                FmtLog.error(LOG, "PatchStoreFile provider name is wrong (expected=%s, got=%s)", DPS.PatchStoreFileProvider, ps.getProviderName());
                throw new DeltaConfigException();
            }
            PatchStore.register(ps);
        }
        
        // Default the log provider to "file"
        if ( PatchStore.getDefault() == null ) {
            //FmtLog.warn(LOG, "PatchStore default not set.");
            PatchStore.setDefault(DPS.PatchStoreFileProvider);
        }
    }
    
    /* File system layout:
     *   Server Root
     *      delta.cfg
     *      /NAME ... per DataSource.
     *          autogenerated assembler?
     *          /patches
     *          /data -- TDB database (optional)
     *          disabled -- if this file is present, then the datasource is not accessible.  
     *          
     *  But also external databases 
     *  
     *  Need to stop two LocalServers on one location.
     */
    
    
    private static Map<Location, LocalServer> servers = new ConcurrentHashMap<>();
    
    private final DataRegistry dataRegistry;
    private final Location serverRoot;
    private final LocalServerConfig serverConfig;
    private static AtomicInteger counter = new AtomicInteger(0);
    // Cache of known disabled data sources. (Not set at startup).
    private Set<Id> disabledDatasources = new HashSet<>();
    private Object lock = new Object();
    
    /** Attach to the runtime area for the server. Use "delta.cfg" as the configuration file name.  
     * @param serverRoot
     * @return LocalServer
     */
    public static LocalServer attach(Location serverRoot) {
        return create(serverRoot, DeltaConst.SERVER_CONFIG); 
    }
    
    /** Attach to the runtime area for the server. 
     * Use "delta.cfg" as the configuration file name.
     * Use the directory fo the configuration file as the location.
     * @param confFile  Filename
     * @return LocalServer
     */
    public static LocalServer create(String confFile) {
        LocalServerConfig conf = LocalServerConfig.create()
            .parse(confFile)
            .build();
        return create(conf);
    }
    

    /** Attach to the runtime area for the server.
     * @param serverRoot
     * @param confFile  Filename: absolute filename, or relative to the server process.
     * @return LocalServer
     */
    public static LocalServer create(Location serverRoot, String confFile) {
        confFile = LibX.resolve(serverRoot, confFile);
        LocalServerConfig conf = LocalServerConfig.create()
            .setLocation(serverRoot)
            .parse(confFile)
            .build();
        return create(conf);
    }
    
    /** Create a {@code LocalServer} based on a configuration file.
     * 
     * @param conf
     * @return LocalServer
     */
    public static LocalServer create(LocalServerConfig conf) {
        Objects.requireNonNull(conf, "Null for configuation");
        if ( conf.location == null )
            throw new DeltaConfigException("No location");
        if ( servers.containsKey(conf.location) ) {
            LocalServer server = servers.get(conf.location);
            if ( ! conf.equals(server.getConfig()) )
                throw new DeltaConfigException("Attempt to have two servers, with different configurations for the same file area"); 
            return server;
        }
        
        // Choose default PatchStore.
        
        DataRegistry dataRegistry = new DataRegistry("Server"+counter.incrementAndGet());
        fillDataRegistry(dataRegistry, conf);
        return attachServer(conf, dataRegistry);
    }
    
    // Scan the location, looking for DataSources.
    private static void fillDataRegistry(DataRegistry dataRegistry, LocalServerConfig config) {
        PatchStore.registered().stream().forEach(ps-> {
            List<DataSource> x = ps.listPersistent(config);
            x.forEach(ds->dataRegistry.put(ds.getId(), ds));
            FmtLog.info(LOG, "PatchStore: %s", ps.getProviderName());   
            FmtLog.info(LOG, "PatchStore:    %s", ps.listDataSources());
        });
    }

    public static void release(LocalServer localServer) {
        Location key = localServer.serverConfig.location;
        if ( key != null ) {
            servers.remove(key);
            localServer.shutdown$();
        }
        PatchStore.clearPatchLogs();
    }

    private static LocalServer attachServer(LocalServerConfig config, DataRegistry dataRegistry) {
        Location loc = config.location;
        if ( ! loc.isMemUnique() ) {
            if ( servers.containsKey(loc) ) {
                LocalServer lServer = servers.get(loc);
                LocalServerConfig config2 = lServer.getConfig();
                if ( Objects.equals(config, config2) ) {
                    return lServer; 
                } else {
                    throw new DeltaException("Attempt to attach to existing location with different configuration: "+loc);
                }
            }
        }
        LocalServer lServer = new LocalServer(config, dataRegistry);
        if ( ! loc.isMemUnique() ) 
            servers.put(loc, lServer);
        lServer.init();
        return lServer ;
    }

    private LocalServer(LocalServerConfig config, DataRegistry dataRegistry) {
        this.serverConfig = config;
        this.dataRegistry = dataRegistry;
        this.serverRoot = config.location;
    }

    private void init() {
        if ( serverConfig.logProvider != null && PatchStore.getDefault() == null )
            PatchStore.setDefault(serverConfig.logProvider);  
    }
    
    public void shutdown() {
        LocalServer.release(this);
    }

    private void shutdown$() {
        dataRegistry.clear();
    }

    public DataRegistry getDataRegistry() {
        return dataRegistry;
    }
    
    public DataSource getDataSource(Id dsRef) {
        DataSource ds = dataRegistry.get(dsRef);
        return dataSource(ds);
    }

    public DataSource getDataSourceByName(String name) {
        DataSource ds = dataRegistry.getByName(name);
        return dataSource(ds);
    }
    
    public DataSource getDataSourceByURI(String uri) {
        DataSource ds = dataRegistry.getByURI(uri);
        return dataSource(ds);
    }

    private DataSource dataSource(DataSource ds) {
        if ( ds == null )
            return null;
        if ( disabledDatasources.contains(ds.getId()) )
            return null;
        return ds;
    }
    
    /** Get the LocalServerConfig use for this server */
    public LocalServerConfig getConfig() {
        return serverConfig;
    }

    public List<Id> listDataSourcesIds() {
        return new ArrayList<>(dataRegistry.keys());
    }
    
    public List<DataSource> listDataSources() {
      List<DataSource> x = new ArrayList<>();
      dataRegistry.forEach((id, ds)-> x.add(ds));
      return x;
    }

    public List<PatchLogInfo> listPatchLogInfo() {
        // Important enough to have it's own cut-through method. 
        List<PatchLogInfo> x = new ArrayList<>();
        dataRegistry.forEach((id, ds)-> x.add(ds.getPatchLog().getDescription()));
        return x;
      }

    public DataSourceDescription getDescriptor(Id dsRef) {
        DataSource dataSource = dataRegistry.get(dsRef);
        return descriptor(dataSource);
    }
    
    private DataSourceDescription descriptor(DataSource dataSource) {
        DataSourceDescription descr = new DataSourceDescription
            (dataSource.getId(),
             dataSource.getURI(),
             dataSource.getName());
        return descr;
    }
    
    private static Location dataSourceArea(Location serverRoot, String name) {
        return serverRoot.getSubLocation(name);
    }
    
//    /** Inital data : a file is some RDF format (the file extension determines the syntax)
//     *  or a directory, which is a TDB database.
//     *  null means no initial data.
//     */
//    /*package*/ static Path initialData(Location dataSourceArea) {
//        if ( dataSourceArea.isMem() )
//            return null;
//        return Paths.get(dataSourceArea.getPath("data.ttl"));
//    }

    // static DataSource.createDataSource.
    
    /** Create a new data source.
     * This can not be one that has been removed (i.e disabled) whose files must be cleaned up manually.
     */
    public Id createDataSource(boolean inMemory, String name, String baseURI/*, details*/) {
        synchronized(lock) { 
            Location sourceArea = dataSourceArea(serverRoot, name);
            Path sourcePath = IOX.asPath(sourceArea);

            // Checking.
            // The area can exist, but it must not be formatted for a DataSource 
            //        if ( sourceArea.exists() )
            //            throw new DeltaException("Area already exists");

            if ( Cfg.isMinimalDataSource(LOG, sourcePath) )
                throw new DeltaBadRequestException("DataSource area already exists at: "+sourceArea);
            if ( ! Cfg.isEnabled(sourcePath) )
                throw new DeltaBadRequestException("DataSource area disabled: "+sourceArea);

            String patchesDirName = sourceArea.getPath(DeltaConst.LOG);
            if ( FileOps.exists(patchesDirName) )
                throw new DeltaBadRequestException("DataSource area does not have a configuration but does have a patches area.");

            String dataDirName = sourceArea.getPath(DeltaConst.DATA);
            if ( FileOps.exists(dataDirName) )
                throw new DeltaBadRequestException("DataSource area has a likely looking database already");

            //Location db = sourceArea.getSubLocation(DPConst.DATA);

            Id dsRef = Id.create();
            DataSourceDescription descr = new DataSourceDescription(dsRef, baseURI, name);

            // Create source.cfg.
            if ( ! inMemory ) {
                JsonObject obj = descr.asJson();
                LOG.info(JSON.toStringFlat(obj));
                try (OutputStream out = Files.newOutputStream(sourcePath.resolve(DeltaConst.DS_CONFIG))) {
                    JSON.write(out, obj);
                } catch (IOException ex)  { throw IOX.exception(ex); }
            }
            DataSource newDataSource = DataSource.connect(dsRef, baseURI, name, sourcePath);
            // Atomic.
            dataRegistry.put(dsRef, newDataSource);
            return dsRef ;
        }
    }
    
    public void removeDataSource(Id dsRef) {
        DataSource datasource1 = getDataSource(dsRef);
        if ( datasource1 == null )
            return;
        // Lock with create.
        synchronized(lock) {
            DataSource datasource = getDataSource(dsRef);
            if ( datasource == null )
                return;
            // Make inaccessible to getDataSource (for create and remove). 
            dataRegistry.remove(dsRef);
            disabledDatasources.add(dsRef);
            datasource.release();
        }
    }

//    /** JsonObject -> SourceDescriptor */
//    // XXX SCOPE? --> SourceDescriptor
//    public static SourceDescriptor fromJsonObject(JsonObject sourceObj) {
//        String idStr = JSONX.getStrOrNull(sourceObj, F_ID);
//        SourceDescriptor descr = new SourceDescriptor
//            (Id.fromString(idStr), 
//             JSONX.getStrOrNull(sourceObj, F_URI),
//             JSONX.getStrOrNull(sourceObj, F_BASE));
//        return descr;
//    }
//    
//    /** SourceDescriptor -> JsonObject */
//    private static JsonObject toJsonObj(SourceDescriptor descr) {
//        return
//            JSONX.buildObject(builder->{
//                set(builder, F_ID, descr.id.asPlainString());
//                set(builder, F_URI, descr.uri);
//                set(builder, F_BASE, descr.base);
//            });
//    }
//
//    private static void set(JsonBuilder builder, String field, String value) {
//        if ( value != null )
//            builder.key(field).value(value);
//    }
//
    public void resetEngine(DeltaLink link) {}
}
