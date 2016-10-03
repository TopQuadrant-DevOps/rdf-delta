package org.seaborne.patch.changes;

import org.apache.jena.graph.Node ;
import org.apache.jena.query.ReadWrite ;
import org.seaborne.patch.RDFChanges ;

/** Wrapper for {@link RDFChanges} */
public class RDFChangesWrapper implements RDFChanges {

    private RDFChanges other ;
    protected RDFChanges get() { return other ; }
    
    public RDFChangesWrapper(RDFChanges other) {
        this.other = other ;
    }
    
    @Override
    public void start() {
        get().start();
    }

    @Override
    public void finish() {
        get().finish();
    }

    @Override
    public void add(Node g, Node s, Node p, Node o) {
        get().add(g, s, p, o);
    }

    @Override
    public void delete(Node g, Node s, Node p, Node o) {
        get().delete(g, s, p, o);
    }

    @Override
    public void addPrefix(Node gn, String prefix, String uriStr) {
        get().addPrefix(gn, prefix, uriStr);
    }

    @Override
    public void deletePrefix(Node gn, String prefix) {
        get().deletePrefix(gn, prefix);
    }

    @Override
    public void setBase(String uriStr) {
        get().setBase(uriStr);
    }

    @Override
    public void txnBegin(ReadWrite mode) {
        get().txnBegin(mode);
    }

    @Override
    public void txnCommit() {
        get().txnCommit();
    }

    @Override
    public void txnAbort() {
        get().txnAbort();
    }
}