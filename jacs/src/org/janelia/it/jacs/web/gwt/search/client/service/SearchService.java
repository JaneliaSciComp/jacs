
package org.janelia.it.jacs.web.gwt.search.client.service;

import com.google.gwt.user.client.rpc.RemoteService;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.search.SearchHit;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Site;
import org.janelia.it.jacs.web.gwt.common.client.service.GWTServiceException;
import org.janelia.it.jacs.web.gwt.common.shared.data.ImageModel;
import org.janelia.it.jacs.web.gwt.search.client.model.CategoryResult;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 28, 2007
 * Time: 4:31:14 PM
 * <p/>
 * Note:
 * Even though for the internal representation of the task ID we use long values
 * on the GWT client side we have to use it as a String
 * because of javascript handling of long values which appears to be buggy!
 */
public interface SearchService extends RemoteService {

    String prepareSearch(String searchString, List<String> searchCategories, int matchFlags)
            throws GWTServiceException;

    /*
     * runs the search task on the remote compute resource
     */
    void runSearchStart(String searchId)
            throws GWTServiceException;

    /**
     * runs the search task for the given list of categories;
     * the given categories must be a subset of the ones for which the task was created
     */
//    void runSearchStart(String searchId,List categories)
//            throws GWTServiceException;

    /**
     * runs the search task for the given list of categories;
     * the given categories must be a subset of the ones for which the task was created
     */
    String runSearchData(String searchId, String category)
            throws GWTServiceException;

//    void runSearchProteins(String searchId)
//            throws GWTServiceException;
//
//    void runSearchProteinClusters(String searchId)
//            throws GWTServiceException;
//
//    void runSearchPublications(String searchId)
//            throws GWTServiceException;
//
//    void runSearchProjects(String searchId)
//            throws GWTServiceException;
//
//    void runSearchSamples(String searchId)
//            throws GWTServiceException;

//    void runSearchWebsite(String searchId)
//            throws GWTServiceException;

    //
    /**
     * Returns a list of all distinct categories present
     * in the specified search result set
     */
    List<String> getAllCategoriesForSearchId(String searchId)
            throws GWTServiceException;

    /**
     * Returns the number of search hits for the given category
     */
    Integer getNumSearchResultsForSearchId(String searchId, List<String> categories)
            throws GWTServiceException;

    /**
     * Returns the search hits for the given categories
     */
    List<SearchHit> getPagedSearchResultsForSearchId(String searchId,
                                                     List<String> categories,
                                                     int startIndex,
                                                     int numRows,
                                                     SortArgument[] sortArgs)
            throws GWTServiceException;

    List<ImageModel> getSearchResultChartsForSearchId(String searchId, String category)
            throws GWTServiceException;


    /**
     * Returns a list of all distinct categories that have hits
     */
    List<String> getAllCategoriesForSearch(String searchString, List<String> categories, int matchFlags)
            throws GWTServiceException;

    /**
     * Returns the number of search hits for the given categories
     */
    Integer getNumSearchResults(String searchString, List<String> categories, int matchFlags)
            throws GWTServiceException;

    /**
     * Returns the search hits for the given categories
     */
    List<SearchHit> getPagedSearchResults(String searchString,
                                          List<String> categories,
                                          int matchFlags,
                                          int startIndex,
                                          int numRows,
                                          SortArgument[] sortArgs)
            throws GWTServiceException;

    /**
     * Returns the search hits for the given categories
     */
    List<? extends CategoryResult> getPagedCategoryResults(String searchId,
                                                           String category,
                                                           int startIndex,
                                                           int numRows,
                                                           SortArgument[] sortArgs)
            throws GWTServiceException;

    Integer getNumCategoryResults(String searchId, String category) throws GWTServiceException;

    /**
     * Returns the search hits for the given categories
     */
    Set<Site> getMapInfoForSearchResultsBySearchId(String searchId, String category) throws GWTServiceException;

}
