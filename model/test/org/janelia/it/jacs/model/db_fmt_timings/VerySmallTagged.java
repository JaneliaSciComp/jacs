/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.model.db_fmt_timings;

import io.protostuff.Tag;
import java.io.Serializable;

/**
 * This is a small class to ser/deser and test concepts to do with
 * protostuff tag marking.
 *
 * @author fosterl
 */
public class VerySmallTagged implements Serializable {
    @Tag(1)
    private int id=111111;
    
    @Tag(2)
    private String name = "I am, therefore I think.";

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    
    public static class InsideOut {
        @Tag(1)
        private int x = 777;
        public int getX() { return x; }
    }
}
