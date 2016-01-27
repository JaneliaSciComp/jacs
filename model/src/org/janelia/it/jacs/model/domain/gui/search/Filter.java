package org.janelia.it.jacs.model.domain.gui.search;

import java.util.ArrayList;
import java.util.List;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.gui.search.criteria.Criteria;
import org.janelia.it.jacs.model.domain.interfaces.IsParent;
import org.janelia.it.jacs.model.domain.support.MongoMapped;
import org.janelia.it.jacs.model.domain.support.SearchAttribute;
import org.janelia.it.jacs.model.domain.support.SearchType;

/**
 * A saved filter on domain objects, acting against the SOLR server. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@MongoMapped(collectionName="filter",label="Filter")
@SearchType(key="filter",label="Filter")
public class Filter extends AbstractDomainObject implements IsParent {

    @SearchAttribute(key="search_class_txt",label="Search Class")
    private String searchClass;
    @SearchAttribute(key="search_string_txt",label="Search String")
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
    }
    
    /* EVERYTHING BELOW IS AUTO-GENERATED */

    public String getSearchClass() {
        return searchClass;
    }

    public void setSearchClass(String searchClass) {
        this.searchClass = searchClass;
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
