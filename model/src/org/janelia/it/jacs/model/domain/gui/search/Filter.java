package org.janelia.it.jacs.model.domain.gui.search;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.gui.search.criteria.Criteria;
import org.janelia.it.jacs.model.domain.support.MongoMapped;

@MongoMapped(collectionName = "filter")
public class Filter extends AbstractDomainObject {

    private String searchType;
    private String searchString;
    private List<Criteria> criteriaList;
    private String sort;

    public boolean hasCriteria() {
        return criteriaList!=null && !criteriaList.isEmpty();
    }
    
    public void addCriteria(Criteria criteria) {
        if (criteriaList==null) {
            this.criteriaList = new ArrayList<Criteria>();
        }
        else if (criteriaList.contains(criteria)) {
            return;
        }
        criteriaList.add(criteria);
    }

    public void removeCriteria(Criteria criteria) {
        if (criteriaList==null) {
            return;
        }
        criteriaList.remove(criteria);
        if (criteriaList.isEmpty()) {
        	criteriaList = null;
        }
    }
    
    /* EVERYTHING BELOW IS AUTO-GENERATED */

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public List<Criteria> getCriteriaList() {
        return criteriaList;
    }

    public void setCriteriaList(List<Criteria> criteriaList) {
        this.criteriaList = criteriaList;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

}
