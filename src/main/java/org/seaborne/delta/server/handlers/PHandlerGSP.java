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

package org.seaborne.delta.server.handlers;

import java.util.ArrayList ;
import java.util.List ;

import org.apache.jena.atlas.io.IndentedLineBuffer ;
import org.apache.jena.query.ARQ ;
import org.apache.jena.riot.WebContent ;
import org.apache.jena.riot.web.HttpOp ;
import org.seaborne.delta.server.PatchHandler ;
import org.seaborne.patch.RDFChanges ;
import org.seaborne.patch.RDFChangesWriteUpdate ;

public class PHandlerGSP implements PatchHandler {
    
    // SPARQL Update services to poke
    private List<String> updateEndpoints = new ArrayList<>() ;
    public PHandlerGSP addEndpoint(String url) {
        updateEndpoints.add(url) ;
        return this ;
    }
    
    public PHandlerGSP() { }
    
    private Object dft = ARQ.getContext().get(ARQ.constantBNodeLabels) ;
    
    @Override
    public RDFChanges handler() {
        IndentedLineBuffer x = new IndentedLineBuffer() ;
        RDFChanges scData = new RDFChangesWriteUpdate(x) {
            @Override
            public void finish() {
                // This has bnode nodes as <_:....> 
                String reqStr = x.asString() ;
                updateEndpoints.forEach((ep)->{
                    HttpOp.execHttpPost(ep, WebContent.contentTypeSPARQLUpdate, reqStr) ;                    
                }) ;
                super.finish() ;
            }
        } ;
        return scData ;
    }
}

