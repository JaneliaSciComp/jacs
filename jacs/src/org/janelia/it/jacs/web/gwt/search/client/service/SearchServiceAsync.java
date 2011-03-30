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

package org.janelia.it.jacs.web.gwt.search.client.service;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.janelia.it.jacs.model.common.SortArgument;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 28, 2007
 * Time: 4:31:32 PM
 */
public interface SearchServiceAsync {
    void prepareSearch(String searchString, List<String> searchCategories, int matchFlags, AsyncCallback callback);

    void runSearchStart(String searchId, AsyncCallback callback);

    void runSearchData(String searchId, String category, AsyncCallback callback);
//    void runSearchProteins(String searchId, AsyncCallback callback);
//    void runSearchProteinClusters(String searchId, AsyncCallback callback);
//    void runSearchPublications(String searchId, AsyncCallback callback);
//    void runSearchProjects(String searchId, AsyncCallback callback);
//    void runSearchSamples(String searchId, AsyncCallback callback);

    //    void runSearchWebsite(String searchId, AsyncCallback callback);
    void getAllCategoriesForSearchId(String searchId, AsyncCallback callback);

    void getNumSearchResultsForSearchId(String searchId, List<String> categories, AsyncCallback callback);

    void getPagedSearchResultsForSearchId(String searchId,
                                          List<String> categories,
                                          int startIndex,
                                          int numRows,
                                          SortArgument[] sortArgs,
                                          AsyncCallback callback);

    void getSearchResultChartsForSearchId(String searchId, String category, AsyncCallback callback);

    void getAllCategoriesForSearch(String searchString, List<String> categories, int matchFlags, AsyncCallback callback);

    void getNumSearchResults(String searchString, List<String> categories, int matchFlags, AsyncCallback callback);

    void getPagedSearchResults(String searchString,
                               List<String> categories,
                               int matchFlags,
                               int startIndex,
                               int numRows,
                               SortArgument[] sortArgs,
                               AsyncCallback callback);

    void getPagedCategoryResults(String searchId, String category, int startIndex,
                                 int numRows, SortArgument[] sortArgs, AsyncCallback callback);

    void getNumCategoryResults(String searchId, String category, AsyncCallback callback);

    void getMapInfoForSearchResultsBySearchId(String searchId, String category, AsyncCallback callback);
}
