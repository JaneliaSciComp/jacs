
package org.janelia.it.jacs.server.api;

import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.search.SearchHit;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.search.SearchResultNode;
import org.janelia.it.jacs.server.access.UserDAO;
import org.janelia.it.jacs.test.JacswebTestCase;
import org.janelia.it.jacs.web.gwt.common.shared.data.ImageModel;
import org.janelia.it.jacs.web.gwt.search.client.model.CategoryResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: tdolafi
 * Date: Aug 4, 2006
 * Time: 11:25:09 AM
 *
 */
public class SearchAPITest extends JacswebTestCase {
    private static final String TEST_USER_NAME = SystemConfigurationProperties.getString("junit.test.username");

    private SearchAPI searchAPI;
    private UserDAO userDAO;

    public SearchAPITest() {
        super(SearchAPITest.class.getName());
        setAutowireMode(AUTOWIRE_BY_NAME);
    }

    public SearchAPI getSearchAPI() {
        return searchAPI;
    }

    public void setSearchAPI(SearchAPI searchAPI) {
        this.searchAPI = searchAPI;
    }

    public UserDAO getUserDAO() {
        return userDAO;
    }

    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public void testExecuteSearch() {
        try {
            String searchString = "measure temperature and salinity 50 miles off Halifax";
            ArrayList<String> searchTopics = new ArrayList<String>();
            searchTopics.add("project");
            searchTopics.add("publication");
            int matchFlags = SearchTask.MATCH_ANY;
            SearchTask searchTask = searchAPI.prepareSearch(TEST_USER_NAME,searchString,searchTopics,matchFlags);
            searchAPI.executeSearchTaskOnComputeResource(searchTask.getObjectId().toString());
            assertTrue(searchAPI.isSearchDone(TEST_USER_NAME,searchTask.getObjectId(),searchTopics));
            List<SearchHit> searchResults = searchAPI.getPagedSearchResultsByTaskId(TEST_USER_NAME,
                    searchTask.getObjectId(),
                    null,
                    0,
                    -1,
                    new SortArgument[] {
                            new SortArgument("topic", SortArgument.SORT_ASC),
                            new SortArgument("rank", SortArgument.SORT_DESC),
                            new SortArgument("headline",SortArgument.SORT_ASC)
                    });
            assertTrue(searchResults.size() > 0);
        } catch (Exception ex) {
            failFromException(ex);
        }
    }

    public void testRetrieveTopics() {
        try {
            User testUser = userDAO.getUserByName(User.SYSTEM_USER_LOGIN);
            String searchString = "measure temperature and salinity 50 miles off Halifax";
            ArrayList<String> searchTopics = new ArrayList<String>();
            searchTopics.add("project");
            searchTopics.add("publication");
            int matchFlags = SearchTask.MATCH_ANY;
            SearchTask searchTask = searchAPI.prepareSearch(TEST_USER_NAME,searchString,searchTopics,matchFlags);
            searchAPI.executeSearchTaskOnComputeResource(searchTask.getObjectId().toString());
            assertTrue(searchAPI.isSearchDone(TEST_USER_NAME,searchTask.getObjectId(),searchTopics));
            List<String> foundTopics = searchAPI.getSearchResultCategoriesByTaskId(testUser,searchTask.getObjectId());
            assertEquals(searchAPI.getSearchResultCategories(searchString, searchTopics, matchFlags),foundTopics);
        } catch (Exception ex) {
            failFromException(ex);
        }
    }

    public void testSearchExistingAccession() {
        searchCategory("jcvi_read_299866", 1, 90000, SearchTask.TOPIC_ACCESSION);
        searchCategory("jcvi_read_299866", 1, 90000, SearchTask.TOPIC_ACCESSION);
        searchCategory("JCVI_TGI_1096124280446", 1, 90000, SearchTask.TOPIC_ACCESSION);
        searchCategory("cam_crcl_1", 1, 90000, SearchTask.TOPIC_ACCESSION);
        searchCategory("cam_proj_gos", 1, 90000, SearchTask.TOPIC_ACCESSION);
        searchCategory("jcvi_smpl_1103283000030", 1, 90000, SearchTask.TOPIC_ACCESSION);
        searchCategory("gs000a", 1, 90000, SearchTask.TOPIC_ACCESSION);
        searchCategory("gi|63489920", 1, 90000, SearchTask.TOPIC_ACCESSION);
        searchCategory("63489920", 1, 90000, SearchTask.TOPIC_ACCESSION);
        searchCategory("aagw01523655.1", 1, 90000, SearchTask.TOPIC_ACCESSION);
        searchCategory("aagw01523655", 1, 90000, SearchTask.TOPIC_ACCESSION);
    }

    public void testSearchesWithoutHits() {
        searchCategory("nonexisting_read_-1", 0, 90000, SearchTask.TOPIC_ACCESSION);
        searchCategory("text_that_typically_is_not_in_a_search_result", 
                0,
                90000,
                SearchTask.TOPIC_CLUSTER,
                SearchTask.TOPIC_PROTEIN,
                SearchTask.TOPIC_PROJECT,
                SearchTask.TOPIC_PUBLICATION,
                SearchTask.TOPIC_SAMPLE,
                SearchTask.TOPIC_WEBSITE);
    }

    public void testSearchProjectsAndPublications() {
        // for now there's one GOS project with six publications and that is what we're looking for
        searchCategory("gos",
                1, // one GOS project
                90000,
                SearchTask.TOPIC_PROJECT);
        searchCategory("gos",
                6, // six GOS papers
                90000,
                SearchTask.TOPIC_PUBLICATION);
    }

    public void testSearchResultCharts() {
        final SearchTask searchTask = searchCategory("acetate",-1, 180000, SearchTask.TOPIC_PROTEIN);
        assertTrue(searchTask != null);
        final String chartDirectory = "jacs/test/testcharts";
        TimedTest resultChartTest = new TimedTest() {
            protected Object invokeTest(Object... testParams) throws Exception {
                return searchAPI.getSearchResultCharts(TEST_USER_NAME,
                        searchTask.getObjectId(),
                        SearchTask.TOPIC_PROTEIN,
                        chartDirectory);
            }
        };
        try {
            List<ImageModel> resultCharts = (List<ImageModel>)resultChartTest.runTest("TestSearchCharts",120000);
            assertTrue(resultCharts.size() > 0);
        } catch(Exception e) {
            fail("Test search result charts failed: " + e.toString());
        }
    }

    public int countSearchResults(Long searchId,String... searchCategory) {
        List searchCategories = Arrays.asList(searchCategory);
        try {
            return searchAPI.getNumSearchResultsByTaskId(TEST_USER_NAME,searchId,searchCategories);
        } catch(Exception e) {
            failFromException(e);
            return -1;
        }
    }

    public SearchTask searchCategory(String searchString, int nExpectedResults, long timeout, String... searchCategory) {
        SearchTask searchTask = null;
        try {
            List searchCategories = Arrays.asList(searchCategory);
            searchTask = startSearchAndWaitForCompletion(searchString,
                    searchCategories,
                    SearchTask.MATCH_ALL,
                    timeout);
            verifySearchResults(searchTask.getObjectId(),nExpectedResults);
        } catch(Exception e) {
            fail("Submit or execute search " + searchCategory + " for " + searchString + " failed: " + e.toString());
        }
        return searchTask;
    }

    private SearchTask createSearchTaskOnComputeResource(String searchString,
                                                         List<String> searchCategories,
                                                         int matchFlags)
            throws Exception {
        // create the task entry
        SearchTask searchTask = new SearchTask();
        searchTask.setSearchString(searchString);
        searchTask.setSearchTopics(searchCategories);
        searchTask.setMatchFlags(matchFlags);
        searchTask.setOwner(TEST_USER_NAME);
        // create the result node
        SearchResultNode searchResultNode = new SearchResultNode();
        searchResultNode.setTask(searchTask);
        searchResultNode.setOwner(TEST_USER_NAME);
        searchTask.addOutputNode(searchResultNode);
        searchTask = (SearchTask)searchAPI.searchBean.saveOrUpdateTask(searchTask);
        return searchTask;
    }

    /**
     * submits a search job and waits for it to end
     * @param searchString
     * @param searchCategories
     * @param matchFlags
     * @param timeout
     * @return the search task
     * @throws Exception
     */
    private SearchTask startSearchAndWaitForCompletion(String searchString,
                                                       List<String> searchCategories,
                                                       int matchFlags,
                                                       long timeout)
            throws Exception {
        SearchTask  searchTask = createSearchTaskOnComputeResource(searchString,searchCategories,matchFlags);
        // submit the task
        searchAPI.executeSearchTaskOnComputeResource(searchTask.getObjectId().toString());
        // check job's status
        int completionStatus = verifyCompletion(searchTask.getObjectId(),timeout);
        assertTrue("Searching " + searchCategories + " for " + searchString + " timed out",
                completionStatus == SearchTask.ALL_SEARCH_TOPICS_COMPLETED_SUCCESSFULLY);
        return searchTask;
    }

    private int verifyCompletion(Long taskId,long timeout) throws Exception {
        long startTime = System.currentTimeMillis();
        int completionStatus = 1;
        while(true) {
            searchAPI.getTaskDAO().getSessionFactory().getCurrentSession().clear();
            SearchTask st = searchAPI.retrieveSearchTask(taskId);
            completionStatus = st.hasCompleted();
            if(completionStatus == SearchTask.SEARCH_STILL_RUNNING) {
                Thread.sleep(500);
                if(timeout > 0 && System.currentTimeMillis() - startTime > timeout) {
                    return SearchTask.SEARCH_TIMEDOUT; // timed out
                }
            } else {
                break;
            }
        }
        return completionStatus;
    }

    /**
     * checks the search results for given search task
     * @param searchTaskId
     * @throws Exception
     */
    private void verifySearchResults(Long searchTaskId,int nExpectedResults) throws Exception {
        SearchTask st = searchAPI.retrieveSearchTask(searchTaskId);
        String searchString = st.getSearchString();
        List<String> searchCategories = st.getSearchTopics();
        User testUser = userDAO.getUserByName(st.getOwner());
        for(String searchCategory : searchCategories) {
            verifySearchResultsForCategory(st.getObjectId(),testUser,searchString,searchCategory,nExpectedResults);
        }
    }

    /**
     * checks the search results for given search task
     * @param searchTaskId
     * @throws Exception
     */
    private void verifySearchResultsForCategory(Long searchTaskId,
                                                User user,
                                                String searchString,
                                                String searchCategory,
                                                int expectedNumberOfResults)
            throws Exception {
        Integer numCategoryResults = searchAPI.getNumCategoryResults(user,searchTaskId,searchCategory);
        if(expectedNumberOfResults >= 0 && numCategoryResults >= 0) {            assertTrue("Mismatch in the number of search results: " + numCategoryResults  + " vs " +
                    expectedNumberOfResults  + " while searching " + searchCategory + " for " + searchString,
                    numCategoryResults == expectedNumberOfResults);
        }
        if(numCategoryResults <= 0) {
            return; // nothing to do
        }
        // randomly test up to 100 results
        int offset,n;
        if(numCategoryResults < 100) {
            offset = 0;
            n = numCategoryResults;
        } else {
            Random r = new Random(System.currentTimeMillis());
            offset = r.nextInt(numCategoryResults - 100);
            n = 100;
        }
        List<? extends CategoryResult> sresults = searchAPI.getPagedCategoryResults(user, searchTaskId, searchCategory, offset, n, null);
        assertTrue(sresults.size() == n);
        for(CategoryResult cr : sresults) {
            assertTrue("Searched string not found in the headline " + cr.getHeadline()  +
                    " while searching " + searchCategory + " for " + searchString,
                    cr.getHeadline().toUpperCase().contains(searchString.toUpperCase()));
        }
    }


}
