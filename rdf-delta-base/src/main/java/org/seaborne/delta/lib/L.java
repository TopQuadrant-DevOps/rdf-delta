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

package org.seaborne.delta.lib;

import java.io.ByteArrayInputStream ;
import java.io.InputStream ;
import java.util.Arrays ;

import org.apache.jena.atlas.io.IO ;
import org.apache.jena.graph.Node ;
import org.apache.jena.sparql.core.Quad ;
import org.apache.jena.sparql.sse.SSE ;
import org.apache.jena.sparql.util.FmtUtils ;

public class L {

    public static void print(String fmt, Object... args) {
        System.out.printf(fmt, args);
        System.out.println();
    }

    public static String str(Node n) {
        return FmtUtils.stringForNode(n, SSE.defaultPrefixMapRead);
    }

    public static String str(Quad q) {
        return FmtUtils.stringForQuad(q, SSE.defaultPrefixMapRead);
    }
    
    /** Copy an array of bytes.*/ 
    public static byte[] copy(byte[] bytes) {
        if ( bytes == null )
            return null ;
        return Arrays.copyOf(bytes, bytes.length) ;
    }
    
    /** Copy the contents of an {@link InputStream} so it can be closed. */ 
    public static InputStream copy(InputStream inputStream) {
        if ( inputStream == null )
            return null ;
        byte[] b = IO.readWholeFile(inputStream) ;
        InputStream x = new ByteArrayInputStream(b) ;
        return x ;
    }
}