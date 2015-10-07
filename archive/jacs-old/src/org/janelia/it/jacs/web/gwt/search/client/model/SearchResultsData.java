
package org.janelia.it.jacs.web.gwt.search.client.model;

import org.janelia.it.jacs.model.tasks.search.SearchTask;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 29, 2007
 * Time: 10:40:05 AM
 */
public class SearchResultsData {
    private String searchId;
    private String priorSearchId;
    private Integer priorCategoryHits;
    private String searchString;
    private String selectedCategory;
    private String _entityDetailAcc;
    private Map searchHitCountByTopic;
    private boolean searchFailed = false;
    private boolean fireSearchFlag;
    private int matchOption = SearchTask.MATCH_ALL;

    public SearchResultsData() {
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public boolean isFireSearchFlag() {
        return fireSearchFlag;
    }

    public void setFireSearchFlag(boolean fireSearchFlag) {
        this.fireSearchFlag = fireSearchFlag;
    }

    public String getPriorSearchId() {
        return priorSearchId;
    }

    public void setPriorSearchId(String priorSearchId) {
        this.priorSearchId = priorSearchId;
    }

    public String getSearchId() {
        return searchId;
    }

    public void setPriorCategoryHits(Integer numHits) {
        this.priorCategoryHits = numHits;
    }

    public Integer getPriorCategoryHits() {
        return priorCategoryHits;
    }

    public void setSearchId(String searchId) {
        this.searchId = searchId;
    }

    public Map getSearchHitCountByTopic() {
        return searchHitCountByTopic;
    }

    public void setSearchHitCountByTopic(Map searchHitCountByTopic) {
        this.searchHitCountByTopic = searchHitCountByTopic;
    }

    public String getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(String selectedCategory) {
        this.selectedCategory = selectedCategory;
    }

    public boolean isSearchFailed() {
        return searchFailed;
    }

    public void setSearchFailed(boolean searchFailed) {
        this.searchFailed = searchFailed;
    }

    public int getMatchOption() {
        return matchOption;
    }

    public void setMatchOption(int matchOption) {
        this.matchOption = matchOption;
    }

    public void setEntityDetailAcc(String entityDetailAcc) {
        _entityDetailAcc = entityDetailAcc;
    }

    public String getEntityDetailAcc() {
        return _entityDetailAcc;
    }


}
