/**
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

package org.seaborne.delta;

import java.util.function.Supplier ;

import org.apache.jena.query.ReadWrite ;
import org.apache.jena.sparql.core.Transactional ;

//COPY from org.seaborne.dboe.transaction.Txn

/** Application utilities for transactions. 
 *  "Autocommit" provided. 
 *  Nested transaction are not supported but calling inside an existing transaction,
 *  which must be compatible, (i.e. a write needs a WRITE transaction). 
 */
public class Txn {
    /** Execute the Runnable in a read transaction. */
    public static <T extends Transactional> void execRead(T txn, Runnable r) {
        boolean b = txn.isInTransaction() ;
        if ( !b )
            txn.begin(ReadWrite.READ) ;
        try { r.run() ; }
        catch (Throwable th) {
            txn.abort() ;
            txn.end() ;
            throw th ;
        }
        if ( ! b )
            txn.end() ;
    }

    /** Execute and return a value in a read transaction */
    public static <T extends Transactional, X> X execReadRtn(T txn, Supplier<X> r) {
        boolean b = txn.isInTransaction() ;
        if ( !b )
            txn.begin(ReadWrite.READ) ;
        try {
            X x = r.get() ;
            if ( !b )
                txn.end() ;
            return x ;
        } catch (Throwable th) {
            txn.abort() ;
            txn.end() ;
            throw th ;
        }
    }

    /** Execute the Runnable in a write transaction */
    public static <T extends Transactional> void execWrite(T txn, Runnable r) {
        boolean b = txn.isInTransaction() ;
        if ( !b )
            txn.begin(ReadWrite.WRITE) ;
        try { r.run() ; }
        catch (Throwable th) {
            txn.abort() ;
            txn.end() ;
            throw th ;
        }
        if ( !b ) {
            if ( txn.isInTransaction() )
                // May have been explicit commit or abort.
                txn.commit() ;
            txn.end() ;
        }
    }
    
    /** Execute and return a value in a write transaction. */
    public static <T extends Transactional, X> X execWriteRtn(Transactional txn, Supplier<X> r) {
        boolean b = txn.isInTransaction() ;
        if ( !b )
            txn.begin(ReadWrite.WRITE) ;
        X x = null ;
        try { x = r.get() ; } 
        catch (Throwable th) {
            txn.abort() ;
            txn.end() ;
            throw th ;
        }
        if ( !b ) {
            if ( txn.isInTransaction() )
                // May have been explicit commit or abort.
                txn.commit() ;
            txn.end() ;
        }
        return x ;
    }
}
