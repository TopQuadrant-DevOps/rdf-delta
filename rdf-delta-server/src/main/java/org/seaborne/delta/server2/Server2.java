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

package org.seaborne.delta.server2;

import java.util.UUID ;

import org.apache.jena.shared.uuid.UUIDFactory ;
import org.apache.jena.shared.uuid.UUID_V1_Gen ;

public class Server2 {
    
    // Fix version as version 1 - these are guessable.
    private static UUIDFactory uuidFactory = new UUID_V1_Gen() ;
    
    /** {@link UUID}s are used to identify many things in Delta - the RDF Dataset being managed,
     * the patches applied (the UUID naming forms the history), registrations and channels,
     * amongst other things.
     */
    public static UUID genUUID() { return uuidFactory.generate().asUUID() ; } 

}
