
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
