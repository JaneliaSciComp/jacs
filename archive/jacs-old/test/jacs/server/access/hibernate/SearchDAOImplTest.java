
package org.janelia.it.jacs.server.access.hibernate;

import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.search.SearchHit;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.search.SearchResultNode;
import org.janelia.it.jacs.server.access.SearchDAO;
import org.janelia.it.jacs.server.access.SearchResultDAO;
import org.janelia.it.jacs.server.access.TaskDAO;
import org.janelia.it.jacs.server.access.UserDAO;
import org.janelia.it.jacs.test.JacswebTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 *
 */
public class SearchDAOImplTest extends JacswebTestCase {
    private SearchDAO projectSearchDAO;
    private SearchDAO publicationSearchDAO;
    private SearchResultDAO searchResultDAO;
    private TaskDAO taskDAO;
    private UserDAO userDAO;

    public SearchDAOImplTest() {
        super(SearchDAOImplTest.class.getName());
        setAutowireMode(AUTOWIRE_BY_NAME);
    }

    public SearchDAO getProjectSearchDAO() {
        return projectSearchDAO;
    }

    public void setProjectSearchDAO(SearchDAO projectSearchDAO) {
        this.projectSearchDAO = projectSearchDAO;
    }

    public SearchDAO getPublicationSearchDAO() {
        return publicationSearchDAO;
    }

    public void setPublicationSearchDAO(SearchDAO publicationSearchDAO) {
        this.publicationSearchDAO = publicationSearchDAO;
    }

    public SearchResultDAO getSearchResultDAO() {
        return searchResultDAO;
    }

    public void setSearchResultDAO(SearchResultDAO searchResultDAO) {
        this.searchResultDAO = searchResultDAO;
    }

    public TaskDAO getTaskDAO() {
        return taskDAO;
    }

    public void setTaskDAO(TaskDAO taskDAO) {
        this.taskDAO = taskDAO;
    }

    public UserDAO getUserDAO() {
        return userDAO;
    }

    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public void testExecuteSearch() {
        try {
            User testUser = userDAO.getUserByName(User.SYSTEM_USER_LOGIN);
            String searchString = "measure temperature and salinity 50 miles off Halifax";
            ArrayList<String> searchTopics = new ArrayList<String>();
            searchTopics.add("project");
            searchTopics.add("publication");
            int matchFlags = SearchTask.MATCH_ANY;
            // create the search task
            SearchTask searchTask = new SearchTask();
            searchTask.setSearchString(searchString);
            searchTask.setSearchTopics(searchTopics);
            searchTask.setMatchFlags(matchFlags);
            searchTask.setOwner(testUser.getUserLogin());
            SearchResultNode searchResultNode = new SearchResultNode();
            searchResultNode.setTask(searchTask);
            searchResultNode.setOwner(testUser.getUserLogin());
            searchTask.addOutputNode(searchResultNode);
            taskDAO.saveOrUpdateTask(searchTask);
            // search and save the hits associated w/ the task
            int nProjectHits = projectSearchDAO.executeSearchTask(searchTask);
            int nPublicationHits = publicationSearchDAO.executeSearchTask(searchTask);
            assertEquals(searchResultDAO.getNumSearchResultsByTaskId(searchTask.getObjectId(),null),
                    nProjectHits+nPublicationHits);
            assertEquals(searchResultDAO.getNumSearchResultsByTaskId(searchTask.getObjectId(),searchTopics),
                    nProjectHits+nPublicationHits);
            List<SearchHit> projectHits =
                    searchResultDAO.getPagedSearchResultsByTaskId(searchTask. getObjectId(),
                            Arrays.asList("project"),
                            0,
                            -1,
                            new SortArgument[] {
                    new SortArgument("topic", SortArgument.SORT_ASC),
                    new SortArgument("rank",SortArgument.SORT_DESC),
                    new SortArgument("headline",SortArgument.SORT_ASC)});
            assertTrue(projectHits.size() == nProjectHits);
            List<SearchHit> publicationHits =
                    searchResultDAO.getPagedSearchResultsByTaskId(searchTask. getObjectId(),
                            Arrays.asList("publication"),
                            0,
                            -1,
                            new SortArgument[] {
                    new SortArgument("topic",SortArgument.SORT_ASC),
                    new SortArgument("rank",SortArgument.SORT_DESC),
                    new SortArgument("headline", SortArgument.SORT_ASC)});
            assertTrue(publicationHits.size() == nPublicationHits);
            List<SearchHit> searchHits =
                    searchResultDAO.getPagedSearchResultsByTaskId(searchTask. getObjectId(),
                            searchTopics,
                            0,
                            -1,
                            new SortArgument[] {
                    new SortArgument("topic",SortArgument.SORT_ASC),
                    new SortArgument("rank",SortArgument.SORT_DESC),
                    new SortArgument("headline",SortArgument.SORT_ASC)});
            assertTrue(searchHits.size() == nProjectHits+nPublicationHits);
        } catch (Exception ex) {
            failFromException(ex);
        }
    }

    public void testSearch() {
        try {
            String searchString = "measure temperature and salinity 50 miles off Halifax";
            int matchFlags = SearchTask.MATCH_ANY;
            int nhits = projectSearchDAO.getNumSearchHits(searchString, matchFlags);
            List<SearchHit> searchHits = projectSearchDAO.search(searchString,
                    matchFlags,
                    0,
                    -1,
                    new SortArgument[] {
                            new SortArgument("topic",SortArgument.SORT_ASC),
                            new SortArgument("rank", SortArgument.SORT_DESC),
                            new SortArgument("headline",SortArgument.SORT_ASC)});
            assertTrue(searchHits.size() == nhits);
        } catch (Exception ex) {
            failFromException(ex);
        }
    }

}
