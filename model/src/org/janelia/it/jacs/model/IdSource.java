/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.model;

import java.util.Iterator;
import java.util.List;

/**
 * This is a fixed-block-oriented ID source, pulling IDs from the
 * @see TimebasedIdentifierGenerator, in such a way to optimize database
 * use, but appear seemless to caller.
 *
 * @author fosterl
 */
public class IdSource implements Iterator<Long> {
    private int positionInList = 0;
    private List<Long> ids = null;
    private int blockSize;
    
    public IdSource(int blockSize) {
        this.blockSize = blockSize;
        refreshIdList();
    }
    
    public IdSource() {
        this(10000);
    }
    
    @Override
    public Long next() {
        if (positionInList >= blockSize) {
            refreshIdList();
        }
        return ids.get(positionInList ++);
    }
    
    private void refreshIdList() {
        ids = TimebasedIdentifierGenerator.generateIdList(blockSize);
        positionInList = 0;
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported.");
    }

}
