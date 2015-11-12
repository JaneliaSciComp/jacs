package org.janelia.it.workstation.gui.browser.model;

import java.lang.reflect.Method;

/**
 * An indexed attribute on a domain object. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainObjectAttribute {

    private final String name;
    private final String label;
    private final String searchKey;
    private final boolean facet;
    private final boolean display;
    private final boolean sortable;
    private final Method getter;
    
    public DomainObjectAttribute(String name, String label, String searchKey, boolean facet, boolean display, boolean sortable, Method getter) {
        this.name = name;
        this.label = label;
        this.searchKey = searchKey;
        this.facet = facet;
        this.display = display;
        this.sortable = sortable;
        this.getter = getter;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }
    
    public String getSearchKey() {
        return searchKey;
    }

    public boolean isFacet() {
        return facet;
    }

    public boolean isDisplay() {
        return display;
    }

    public boolean isSortable() {
        return sortable;
    }

    public Method getGetter() {
        return getter;
    }
    
    @Override
    public String toString() {
        return label;
    }
}
