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

package org.seaborne.patch.system;

import org.apache.jena.graph.Node;
import org.seaborne.patch.PatchHeader;
import org.seaborne.patch.RDFChanges;
import org.seaborne.patch.RDFPatch;
import org.seaborne.patch.changes.RDFChangesWrapper;

/**
 * An {@link RDFPatch} where the header is taken from one place and the body
 * (everything that isn't header) from an existing patch.
 */
public class RDFPatchAltHeader implements RDFPatch {

    private final RDFPatch body;
    private final PatchHeader header;

    public RDFPatchAltHeader(PatchHeader header, RDFPatch body) {
        this.body = body;
        this.header = header;
    }

    @Override
    public PatchHeader header() {
        return header;
    }

    @Override
    public void apply(RDFChanges changes) {
        // Ignore the header.
        RDFChanges x = new RDFChangesWrapper(changes) {
            @Override public void header(String field, Node value) { }
        };
        header.apply(changes);
        body.apply(x);
    }

    @Override
    public boolean repeatable() {
        return body.repeatable();
    }
}