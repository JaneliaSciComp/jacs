/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

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
