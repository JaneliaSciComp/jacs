
package org.janelia.it.jacs.web.gwt.search.server;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.search.SearchHit;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.server.api.SearchAPI;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Site;
import org.janelia.it.jacs.web.gwt.common.client.service.GWTServiceException;
import org.janelia.it.jacs.web.gwt.common.server.JcviGWTSpringController;
import org.janelia.it.jacs.web.gwt.common.shared.data.ImageModel;
import org.janelia.it.jacs.web.gwt.search.client.model.CategoryResult;
import org.janelia.it.jacs.web.gwt.search.client.service.SearchService;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Aug 28, 2007
 * Time: 4:30:53 PM
 */
public class SearchServiceImpl extends JcviGWTSpringController implements SearchService {
    static transient Logger logger = Logger.getLogger(SearchServiceImpl.class.getName());
    private transient SearchAPI _searchAPI;

    public void setSearchAPI(SearchAPI searchAPI) {
        _searchAPI = searchAPI;
    }

    public String prepareSearch(String searchString, List searchCategories, int matchFlags) throws GWTServiceException {
        try {
            SearchTask searchTask = _searchAPI.prepareSearch(getSessionUser().getUserLogin(), searchString, searchCategories, matchFlags);
            return searchTask.getObjectId().toString();
        }
        catch (Exception e) {
            throw new GWTServiceException(e.toString(), e);
        }
    }

    public void runSearchStart(String searchId) throws GWTServiceException {
        try {
            _searchAPI.executeSearchTaskOnComputeResource(searchId);
        }
        catch (Exception e) {
            logger.error("Exception in runSearchStart() for searchId=" + searchId + " :" + e.getMessage());
            throw new GWTServiceException(e);
        }
    }

    public String runSearchData(String searchId, String category) throws GWTServiceException {
        try {
            return _searchAPI.getSearchTaskStatusByTopicFromId(searchId, category);
        }
        catch (Exception e) {
            throw new GWTServiceException(e.getMessage(), e);
        }
    }

//    public void runSearchProteins(String searchId) throws GWTServiceException {
//        try {
//            SearchTask searchTask = _searchAPI.retrieveSearchTask(Long.valueOf(searchId));
//            _searchAPI.executeSearchTask(searchTask,SearchTask.TOPIC_PROTEIN);
//        } catch(Exception e) {
//            throw new GWTServiceException(e.toString(),e);
//        }
//    }
//
//    public void runSearchProteinClusters(String searchId) throws GWTServiceException {
//        try {
//            SearchTask searchTask = _searchAPI.retrieveSearchTask(Long.valueOf(searchId));
//            _searchAPI.executeSearchTask(searchTask, SearchTask.TOPIC_CLUSTER);
//        } catch(Exception e) {
//            throw new GWTServiceException(e.toString(),e);
//        }
//    }
//
//    public void runSearchPublications(String searchId) throws GWTServiceException {
//        try {
//            SearchTask searchTask = _searchAPI.retrieveSearchTask(Long.valueOf(searchId));
//            _searchAPI.executeSearchTask(searchTask, SearchTask.TOPIC_PUBLICATION);
//        } catch(Exception e) {
//            throw new GWTServiceException(e.toString(),e);
//        }
//    }
//
//    public void runSearchProjects(String searchId) throws GWTServiceException {
//        try {
//            SearchTask searchTask = _searchAPI.retrieveSearchTask(Long.valueOf(searchId));
//            _searchAPI.executeSearchTask(searchTask, SearchTask.TOPIC_PROJECT);
//        } catch(Exception e) {
//            throw new GWTServiceException(e.toString(),e);
//        }
//    }
//
//    public void runSearchSamples(String searchId) throws GWTServiceException {
//        try {
//            SearchTask searchTask = _searchAPI.retrieveSearchTask(Long.valueOf(searchId));
//            _searchAPI.executeSearchTask(searchTask, SearchTask.TOPIC_SAMPLE);
//        } catch(Exception e) {
//            throw new GWTServiceException(e.toString(),e);
//        }
//    }
//
//    public void runSearchWebsite(String searchId) throws GWTServiceException {
//        try {
//            SearchTask searchTask = _searchAPI.retrieveSearchTask(Long.valueOf(searchId));
//            _searchAPI.executeSearchTask(searchTask, SearchTask.TOPIC_WEBSITE);
//        } catch(Exception e) {
//            throw new GWTServiceException(e.toString(),e);
//        }
//    }

    public List<String> getAllCategoriesForSearchId(String searchId) throws GWTServiceException {
        try {
            return _searchAPI.getSearchResultCategoriesByTaskId(getSessionUser(), Long.valueOf(searchId));
        }
        catch (Exception e) {
            throw new GWTServiceException(e.toString(), e);
        }
    }

    public Integer getNumSearchResultsForSearchId(String searchId, List<String> categories)
            throws GWTServiceException {
        try {
            return _searchAPI.getNumSearchResultsByTaskId(getSessionUser().getUserLogin(), Long.valueOf(searchId), categories);
        }
        catch (Exception e) {
            throw new GWTServiceException(e.toString(), e);
        }
    }

    public List<SearchHit> getPagedSearchResultsForSearchId(String searchId,
                                                            List<String> categories,
                                                            int startIndex,
                                                            int numRows,
                                                            SortArgument[] sortArgs)
            throws GWTServiceException {
        try {
            List<SearchHit> searchResults = _searchAPI.getPagedSearchResultsByTaskId(getSessionUser().getUserLogin(),
                    Long.valueOf(searchId),
                    categories,
                    startIndex,
                    numRows,
                    sortArgs);
            cleanForGWT(searchResults);
            return searchResults;
        }
        catch (Exception e) {
            throw new GWTServiceException(e.toString(), e);
        }
    }

    public List<ImageModel> getSearchResultChartsForSearchId(String searchId, String category) throws GWTServiceException {
        try {
            HttpSession httpSession = getThreadLocalRequest().getSession();
            String chartsRelativeLocation = "tmp" + "/" + "charts" + "/" + searchId;
            File chartLocationFile = new File(httpSession.getServletContext().getRealPath(chartsRelativeLocation));
            chartLocationFile.deleteOnExit();
            chartLocationFile.mkdirs();
            return _searchAPI.getSearchResultCharts(getSessionUser().getUserLogin(),
                    Long.valueOf(searchId),
                    category,
                    chartLocationFile.getAbsolutePath());
        }
        catch (Exception e) {
            throw new GWTServiceException(e.toString(), e);
        }
    }

    public List<String> getAllCategoriesForSearch(String searchString, List<String> categories, int matchFlags)
            throws GWTServiceException {
        try {
            return _searchAPI.getSearchResultCategories(searchString, categories, matchFlags);
        }
        catch (Exception e) {
            throw new GWTServiceException(e.toString(), e);
        }
    }

    public Integer getNumSearchResults(String searchString, List<String> categories, int matchFlags)
            throws GWTServiceException {
        try {
            return _searchAPI.getNumSearchHits(searchString, categories, matchFlags);
        }
        catch (Exception e) {
            throw new GWTServiceException(e.toString(), e);
        }
    }

    public List<SearchHit> getPagedSearchResults(String searchString,
                                                 List<String> categories,
                                                 int matchFlags,
                                                 int startIndex,
                                                 int numRows,
                                                 SortArgument[] sortArgs)
            throws GWTServiceException {
        try {
            return _searchAPI.search(searchString, categories, matchFlags, startIndex, numRows, sortArgs);
        }
        catch (Exception e) {
            throw new GWTServiceException(e.toString(), e);
        }
    }

    public List<? extends CategoryResult> getPagedCategoryResults(String searchIdString,
                                                                  String category,
                                                                  int startIndex,
                                                                  int numRows,
                                                                  SortArgument[] sortArgs)
            throws GWTServiceException {
        try {
            Long searchId = new Long(searchIdString);
            return _searchAPI.getPagedCategoryResults(getSessionUser(), searchId, category, startIndex, numRows, sortArgs);
        }
        catch (Exception e) {
            throw new GWTServiceException(e.toString(), e);
        }
    }

    public Integer getNumCategoryResults(String searchIdString, String category)
            throws GWTServiceException {
        try {
            Long searchId = new Long(searchIdString);
            return _searchAPI.getNumCategoryResults(getSessionUser(), searchId, category);
        }
        catch (Exception e) {
            throw new GWTServiceException(e.toString(), e);
        }
    }

    public Set<Site> getMapInfoForSearchResultsBySearchId(String searchIdString, String category)
            throws GWTServiceException {
        try {
            Long searchId = new Long(searchIdString);
            return _searchAPI.getMapInfoForSearchResultsBySearchId(getSessionUser(), searchId, category);
        }
        catch (Exception e) {
            throw new GWTServiceException(e.toString(), e);
        }
    }

}