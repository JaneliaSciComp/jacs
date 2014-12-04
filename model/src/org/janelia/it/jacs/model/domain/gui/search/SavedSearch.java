package org.janelia.it.jacs.model.domain.gui.search;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.gui.search.filters.Filter;
import org.janelia.it.jacs.model.domain.support.MongoMapped;

@MongoMapped(collectionName = "savedSearch")
public class SavedSearch extends AbstractDomainObject {

    private List<Filter> filters;
    protected String sortCriteria;
    
    public int getNumFilters() {
        return filters==null ? 0 : filters.size();
    }

    public void addFilter(Filter filter) {
        if (filters==null) {
            this.filters = new ArrayList<Filter>();
        }
        if (filters.contains(filter)) {
            return;
        }
        filters.add(filter);
    }

    public void removeFilter(Filter filter) {
        if (filters==null) {
            return;
        }
        filters.remove(filter);
        if (filters.isEmpty()) {
        	filters = null;
        }
    }
    /* EVERYTHING BELOW IS AUTO-GENERATED */
    public List<Filter> getFilters() {
        return filters;
    }

    public void setFilters(List<Filter> filters) {
        this.filters = filters;
    }

	public String getSortCriteria() {
		return sortCriteria;
	}

	public void setSortCriteria(String sortCriteria) {
		this.sortCriteria = sortCriteria;
	}
}
