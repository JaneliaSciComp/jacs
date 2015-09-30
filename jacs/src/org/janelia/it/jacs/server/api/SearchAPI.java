
package org.janelia.it.jacs.server.api;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.SearchBeanRemote;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.search.SearchHit;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.search.SearchResultNode;
import org.janelia.it.jacs.server.access.SearchDAO;
import org.janelia.it.jacs.server.access.SearchResultDAO;
import org.janelia.it.jacs.server.access.TaskDAO;
import org.janelia.it.jacs.server.utils.SystemException;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Site;
import org.janelia.it.jacs.web.gwt.common.shared.data.ImageModel;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 */
public class SearchAPI {
    static Logger logger = Logger.getLogger(SearchAPI.class.getName());

    private TaskDAO taskDAO;
    private SearchResultDAO searchResultDAO;
    private SearchDAOFactory searchDAOFactory;
    protected SearchBeanRemote searchBean;

    public SearchAPI() {
    }

    public void setSearchDAOFactory(SearchDAOFactory searchDAOFactory) {
        this.searchDAOFactory = searchDAOFactory;
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

    public void setSearchBean(SearchBeanRemote searchBeanRemote) {
        this.searchBean = searchBeanRemote;
    }

    public SearchDAO getSearchDAO(String category) {
        return searchDAOFactory.getSearchDAO(category);
    }

    public void executeSearchTaskOnComputeResource(String searchId) throws Exception {
        Long numericSearchId = new Long(searchId);
        searchBean.submitSearchTask(numericSearchId);
    }

    /**
     * This method checks to see if the task corresponding to taskId has completed
     *
     * @param requestingUser
     * @param taskId
     * @param categories
     * @return
     * @throws SystemException
     */
    public boolean isSearchDone(String requestingUser, Long taskId, List<String> categories)
            throws SystemException {
        try {
            Task t = taskDAO.getTaskById(taskId);
            if (t == null) {
                throw new IllegalArgumentException("No such task found for " + taskId);
            }
            if (!(t instanceof SearchTask)) {
                // the specified task is not a search task
                throw new SystemException("Invalid task ID " + taskId);
            }
            if (t.getOwner() != null && !t.getOwner().equals(requestingUser)) {
                throw new IllegalAccessException("User " + requestingUser +
                        " doesn't have access to task " + taskId);
            }
            SearchTask searchTask = (SearchTask) t;
            boolean bresult = false;
            if (searchTask.isDone()) {
                // we know that the task finished so return true
                bresult = true;
            }
            else {
                boolean checkAllSubTasks = false;
                List<String> searchTaskCategories = null;
                if (categories != null) {
                    searchTaskCategories = new ArrayList<String>(categories);
                }
                if (searchTaskCategories == null || searchTaskCategories.contains(SearchTask.TOPIC_ALL)) {
                    checkAllSubTasks = true;
                    searchTaskCategories = searchTask.getSearchTopics();
                    if (searchTaskCategories.contains(SearchTask.TOPIC_ALL)) {
                        searchTaskCategories = new ArrayList<String>();
                        searchTaskCategories.add(SearchTask.TOPIC_ACCESSION);
                        searchTaskCategories.add(SearchTask.TOPIC_PROTEIN);
                        searchTaskCategories.add(SearchTask.TOPIC_CLUSTER);
                        searchTaskCategories.add(SearchTask.TOPIC_PUBLICATION);
                        searchTaskCategories.add(SearchTask.TOPIC_PROJECT);
                        searchTaskCategories.add(SearchTask.TOPIC_SAMPLE);
                        searchTaskCategories.add(SearchTask.TOPIC_WEBSITE);
                    }
                }
                // otherwise we check if all subtasks have finished
                for (Object eventObject : searchTask.getEvents()) {
                    Event searchTaskEvent = (Event) eventObject;
                    if (searchTaskEvent.getEventType().equals(Event.SUBTASKCOMPLETED_EVENT) ||
                            searchTaskEvent.getEventType().equals(Event.SUBTASKERROR_EVENT)) {
                        String searchCategory = searchTaskEvent.getDescription();
                        searchTaskCategories.remove(searchCategory);
                    }
                }
                if (searchTaskCategories.isEmpty()) {
                    if (checkAllSubTasks) {
                        // all subtasks finished
                        Event completedEvent = new Event("Search", new Date(), Event.COMPLETED_EVENT);
                        addSearchTaskEvent(searchTask, completedEvent);
                    }
                    bresult = true;
                }
            }
            return bresult;
        }
        catch (IllegalArgumentException e) {
            throw e;
        }
        catch (SystemException e) {
            throw e;
        }
        catch (Exception e) {
            throw new SystemException(e);
        }
    }

    public SearchTask prepareSearch(String requestingUser,
                                    String searchString,
                                    List searchTopics,
                                    int matchFlags)
            throws SystemException {
        SearchTask searchTask;
        try {
            // create the task entry
            searchTask = new SearchTask();
            searchTask.setSearchString(searchString);
            searchTask.setSearchTopics(searchTopics);
            searchTask.setMatchFlags(matchFlags);
            searchTask.setOwner(requestingUser);
            // create the result node
            SearchResultNode searchResultNode = new SearchResultNode();
            searchResultNode.setTask(searchTask);
            searchResultNode.setOwner(requestingUser);
            searchTask.addOutputNode(searchResultNode);
            taskDAO.saveOrUpdateTask(searchTask);
        }
        catch (Exception e) {
            // any exception thrown up to this point doesn't result
            // in a task entry being created
            throw new SystemException(e);
        }
        return searchTask;
    }

    public List<String> getSearchResultCategoriesByTaskId(User requestingUser, Long searchTaskId)
            throws SystemException {
        try {
            Task t = taskDAO.getTaskById(searchTaskId);
            if (t == null) {
                throw new IllegalArgumentException("No such task found for " + searchTaskId);
            }
            if (!(t instanceof SearchTask)) {
                // the specified task is not a search task
                throw new SystemException("Invalid task ID " + searchTaskId);
            }
            if (t.getOwner() != null && !t.getOwner().equals(requestingUser.getUserLogin())) {
                throw new IllegalAccessException("User " + requestingUser.getUserLogin() +
                        " doesn't have access to task " + searchTaskId);
            }
            return searchResultDAO.getSearchResultCategoriesByTaskId(searchTaskId);
        }
        catch (IllegalArgumentException e) {
            throw e;
        }
        catch (SystemException e) {
            throw e;
        }
        catch (Exception e) {
            throw new SystemException(e);
        }
    }

    public int getNumSearchResultsByTaskId(String requestingUser, Long searchTaskId, List<String> categories)
            throws SystemException {
        int nSearchHits = 0;
        Map<String, Integer> categoryHitsMap = getCategorySearchResultsByTaskId(requestingUser, searchTaskId, categories);
        for (Map.Entry<String, Integer> categoryHitsCount : categoryHitsMap.entrySet())
            nSearchHits += categoryHitsCount.getValue();
        return nSearchHits;
    }

    public Map<String, Integer> getCategorySearchResultsByTaskId(String requestingUser, Long searchTaskId,
                                                                 Collection<String> categories)
            throws SystemException {
        try {
            return searchResultDAO.getNumCategoryResults(searchTaskId, categories);
        }
        catch (Exception e) {
            throw new SystemException(e);
        }
    }

    public List<SearchHit> getPagedSearchResultsByTaskId(String requestingUser,
                                                         Long searchTaskId,
                                                         List<String> topics,
                                                         int startIndex,
                                                         int numRows,
                                                         SortArgument[] sortArgs)
            throws SystemException {
        try {
            Task t = taskDAO.getTaskById(searchTaskId);
            if (t == null) {
                throw new IllegalArgumentException("No such task found for " + searchTaskId);
            }
            if (!(t instanceof SearchTask)) {
                // the specified task is not a search task
                throw new SystemException("Invalid task ID " + searchTaskId);
            }
            if (t.getOwner() != null && !t.getOwner().equals(requestingUser)) {
                throw new IllegalAccessException("User " + requestingUser +
                        " doesn't have access to task " + searchTaskId);
            }
            return searchResultDAO.getPagedSearchResultsByTaskId(searchTaskId, topics, startIndex, numRows, sortArgs);
        }
        catch (IllegalArgumentException e) {
            throw e;
        }
        catch (SystemException e) {
            throw e;
        }
        catch (Exception e) {
            throw new SystemException(e);
        }
    }

    public List<String> getSearchResultCategories(String searchString, List<String> searchCategories, int matchFlags)
            throws SystemException {
        try {
            List<String> foundCategories = new ArrayList<String>();
            Map<String, Integer> categoryHitsMap =
                    countHitsByCategory(searchString, searchCategories, matchFlags);
            for (Map.Entry<String, Integer> categoryHitsCount : categoryHitsMap.entrySet()) {
                if (categoryHitsCount.getValue() > 0) {
                    foundCategories.add(categoryHitsCount.getKey());
                }
            }
            return foundCategories;
        }
        catch (Throwable e) {
            // log the error
            logger.error(e);
            // rethrow the error
            throw new SystemException(e);
        }
    }

    public int getNumSearchHits(String searchString, List<String> searchCategories, int matchFlags)
            throws SystemException {
        int nSearchHits = 0;
        try {
            Map<String, Integer> categoryHitsMap =
                    countHitsByCategory(searchString, searchCategories, matchFlags);
            for (Map.Entry<String, Integer> categoryHitsCount : categoryHitsMap.entrySet()) {
                nSearchHits += categoryHitsCount.getValue();
            }
        }
        catch (Throwable e) {
            // log the error
            logger.error(e);
            // rethrow the error
            throw new SystemException(e);
        }
        return nSearchHits;
    }

    public SearchTask retrieveSearchTask(Long searchId) throws SystemException {
        try {
            return (SearchTask) taskDAO.getTaskById(searchId);
        }
        catch (Throwable e) {
            // log the error
            logger.error(e);
            // rethrow the error
            throw new SystemException(e);
        }
    }

    public SearchResultNode retrieveSearchNodeByTask(Long searchId)
            throws SystemException {
        try {
            SearchTask st = (SearchTask) taskDAO.getTaskById(searchId);
            return st.getSearchResultNode();
        }
        catch (Throwable e) {
            // log the error
            logger.error(e);
            // rethrow the error
            throw new SystemException(e);
        }
    }

    public List<SearchHit> search(String searchString,
                                  List<String> searchCategories,
                                  int matchFlags,
                                  int startIndex,
                                  int numRows,
                                  SortArgument[] sortArgs)
            throws SystemException {
        List<SearchHit> searchHits = new ArrayList<SearchHit>();
        try {
            if (searchCategories == null || searchCategories.size() == 0) {
                // if no category was provided fire off a search for all supported categories
                search(searchString, SearchTask.TOPIC_ACCESSION, matchFlags, startIndex, numRows, sortArgs, searchHits);
                search(searchString, SearchTask.TOPIC_PROTEIN, matchFlags, startIndex, numRows, sortArgs, searchHits);
                search(searchString, SearchTask.TOPIC_CLUSTER, matchFlags, startIndex, numRows, sortArgs, searchHits);
                search(searchString, SearchTask.TOPIC_PUBLICATION, matchFlags, startIndex, numRows, sortArgs, searchHits);
                search(searchString, SearchTask.TOPIC_PROJECT, matchFlags, startIndex, numRows, sortArgs, searchHits);
                search(searchString, SearchTask.TOPIC_SAMPLE, matchFlags, startIndex, numRows, sortArgs, searchHits);
                search(searchString, SearchTask.TOPIC_WEBSITE, matchFlags, startIndex, numRows, sortArgs, searchHits);
            }
            else {
                // otherwise go through the categories and fire off that search
                for (String category : searchCategories) {
                    search(searchString, category, matchFlags, startIndex, numRows, sortArgs, searchHits);
                }
            }
        }
        catch (Throwable e) {
            // log the error
            logger.error(e);
            // rethrow the error
            throw new SystemException(e);
        }
        return searchHits;
    }

//    public List<? extends CategoryResult> getPagedCategoryResults(User requestingUser,
//                                                                  Long searchTaskId,
//                                                                  String category,
//                                                                  int startIndex,
//                                                                  int numRows,
//                                                                  SortArgument[] sortArgs)
//            throws SystemException {
//        try {
//            Task t = taskDAO.getTaskById(searchTaskId);
//            if (t == null) {
//                throw new IllegalArgumentException("No such task found for " + searchTaskId);
//            }
//            if (!(t instanceof SearchTask)) {
//                // the specified task is not a search task
//                throw new SystemException("Invalid task ID " + searchTaskId);
//            }
//            if (t.getOwner() != null && !t.getOwner().equals(requestingUser.getUserLogin())) {
//                throw new IllegalAccessException("User " + requestingUser.getUserLogin() +
//                        " doesn't have access to task " + searchTaskId);
//            }
//            SearchDAO searchDAO = searchDAOFactory.getSearchDAO(category);
//            Long nodeId = ((SearchTask) t).getSearchResultNode().getObjectId();
//            return searchDAO.getPagedCategoryResultsByNodeId(nodeId, startIndex, numRows, sortArgs);
//        }
//        catch (IllegalArgumentException e) {
//            throw e;
//        }
//        catch (SystemException e) {
//            throw e;
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//            logger.error(e.getMessage());
//            throw new SystemException(e);
//        }
//    }
//
    public Integer getNumCategoryResults(User requestingUser,
                                         Long searchTaskId,
                                         String category)
            throws SystemException {
        try {
            Task t = taskDAO.getTaskById(searchTaskId);
            if (t == null) {
                throw new IllegalArgumentException("No such task found for " + searchTaskId);
            }
            if (!(t instanceof SearchTask)) {
                // the specified task is not a search task
                throw new SystemException("Invalid task ID " + searchTaskId);
            }
            if (t.getOwner() != null && !t.getOwner().equals(requestingUser.getUserLogin())) {
                throw new IllegalAccessException("User " + requestingUser.getUserLogin() +
                        " doesn't have access to task " + searchTaskId);
            }
            SearchDAO searchDAO = searchDAOFactory.getSearchDAO(category);
            Long nodeId = ((SearchTask) t).getSearchResultNode().getObjectId();
            return searchDAO.getNumCategoryResultsByNodeId(nodeId);
        }
        catch (IllegalArgumentException e) {
            throw e;
        }
        catch (SystemException e) {
            throw e;
        }
        catch (Exception e) {
            throw new SystemException(e);
        }
    }

    public List<ImageModel> getSearchResultCharts(String requestingUser,
                                                  Long searchTaskId,
                                                  String category,
                                                  String resultBaseDirectory)
            throws SystemException {
        try {
            Task t = taskDAO.getTaskById(searchTaskId);
            if (t == null) {
                throw new IllegalArgumentException("No such task found for " + searchTaskId);
            }
            if (!(t instanceof SearchTask)) {
                // the specified task is not a search task
                throw new SystemException("Invalid task ID " + searchTaskId);
            }
            if (t.getOwner() != null && !t.getOwner().equals(requestingUser)) {
                throw new IllegalAccessException("User " + requestingUser +
                        " doesn't have access to task " + searchTaskId);
            }
            SearchDAO searchDAO = searchDAOFactory.getSearchDAO(category);
            return searchDAO.getSearchResultCharts(searchTaskId, resultBaseDirectory);
        }
        catch (IllegalArgumentException e) {
            throw e;
        }
        catch (SystemException e) {
            throw e;
        }
        catch (Exception e) {
            throw new SystemException(e);
        }
    }

    public Set<Site> getMapInfoForSearchResultsBySearchId(User requestingUser,
                                                          Long searchTaskId,
                                                          String category)
            throws Exception {
        try {
            Task t = taskDAO.getTaskById(searchTaskId);
            if (t == null) {
                throw new IllegalArgumentException("No such task found for " + searchTaskId);
            }
            if (!(t instanceof SearchTask)) {
                // the specified task is not a search task
                throw new SystemException("Invalid task ID " + searchTaskId);
            }
            if (t.getOwner() != null && !t.getOwner().equals(requestingUser.getUserLogin())) {
                throw new IllegalAccessException("User " + requestingUser.getUserLogin() +
                        " doesn't have access to task " + searchTaskId);
            }
            SearchDAO searchDAO = searchDAOFactory.getSearchDAO(category);
            return searchDAO.getMapInfoForSearchResultsBySearchId(searchTaskId, category);
        }
        catch (IllegalArgumentException e) {
            throw e;
        }
        catch (SystemException e) {
            throw e;
        }
        catch (Exception e) {
            throw new SystemException(e);
        }
    }

    private synchronized void addSearchTaskEvent(SearchTask searchTask, Event searchEvent) throws Exception {
        searchTask.addEvent(searchEvent);
        taskDAO.saveOrUpdateTask(searchTask);
    }

    public synchronized void updateSearchTaskEventByTopic(SearchTask searchTask, String topic, String status) throws Exception {
        if (!status.equals(Event.SUBTASKCOMPLETED_EVENT) &&
                !status.equals(Event.SUBTASKRUNNING_EVENT) &&
                !status.equals(Event.SUBTASKERROR_EVENT))
            throw new Exception("Do not recognize search topic status=" + status);
        Event runningEvent = new Event(topic, new Date(), status);
        addSearchTaskEvent(searchTask, runningEvent);
    }

    public String getSearchTaskStatusByTopicFromId(String searchId, String topic) throws Exception {
        try {
            SearchTask searchTask = (SearchTask) taskDAO.getTaskById(new Long(searchId));
            List<Event> events = searchTask.getEvents();
            List<Event> eventsForTopicList = new ArrayList<Event>();
            for (Event e : events) {
                if (e.getDescription() != null && e.getDescription().equals(topic))
                    eventsForTopicList.add(e);
            }
            if (eventsForTopicList.size() == 0) {
//            logger.debug("getSearchTaskStatusByTopicFromId() id="+searchId+" topic="+topic+", 0 events - returnng RUNNING");
                return Event.SUBTASKRUNNING_EVENT; // it may not have started yet
            }
            else {
                Event mostRecentTopicEvent = eventsForTopicList.get(eventsForTopicList.size() - 1);
                logger.debug("getSearchTaskStatusByTopicFromId() id=" + searchId + " topic=" + topic + ", status is =" + mostRecentTopicEvent.getEventType());
                return mostRecentTopicEvent.getEventType();
            }
        }
        catch (Exception e) {
            logger.error("Exception in getSearchTaskStatusByTopicFromId() for topic=" + topic, e);
            throw new Exception(e);
        }
    }

    private Map<String, Integer> countHitsByCategory(String searchString, List<String> searchCategories, int matchFlags)
            throws SystemException {
        Map<String, Integer> categoryHitsMap;
        try {
            if (searchCategories == null || searchCategories.size() == 0) {
                // if no category was provided fire off a search for all supported categories
                categoryHitsMap = getHitsByCategory(searchString, matchFlags);
            }
            else {
                // otherwise go through the categories and fire off that search
                categoryHitsMap = new HashMap<String, Integer>();
                for (String category : searchCategories)
                    categoryHitsMap.put(category, countSearchHits(searchString, category, matchFlags));
            }
        }
        catch (Throwable e) {
            // log the error
            logger.error(e);
            // rethrow the error
            throw new SystemException(e);
        }
        return categoryHitsMap;
    }

    public Map<String, Integer> getHitsByCategory(String searchString, int matchFlags)
            throws SystemException {
        Map<String, Integer> categoryHitsMap = new HashMap();
        categoryHitsMap.put(SearchTask.TOPIC_ACCESSION, countSearchHits(searchString, SearchTask.TOPIC_ACCESSION, matchFlags));
        categoryHitsMap.put(SearchTask.TOPIC_PROTEIN, countSearchHits(searchString, SearchTask.TOPIC_PROTEIN, matchFlags));
        categoryHitsMap.put(SearchTask.TOPIC_CLUSTER, countSearchHits(searchString, SearchTask.TOPIC_CLUSTER, matchFlags));
        categoryHitsMap.put(SearchTask.TOPIC_PUBLICATION, countSearchHits(searchString, SearchTask.TOPIC_PUBLICATION, matchFlags));
        categoryHitsMap.put(SearchTask.TOPIC_PROJECT, countSearchHits(searchString, SearchTask.TOPIC_PROJECT, matchFlags));
        categoryHitsMap.put(SearchTask.TOPIC_SAMPLE, countSearchHits(searchString, SearchTask.TOPIC_SAMPLE, matchFlags));
        categoryHitsMap.put(SearchTask.TOPIC_WEBSITE, countSearchHits(searchString, SearchTask.TOPIC_WEBSITE, matchFlags));
        return categoryHitsMap;
    }

    private int countSearchHits(String searchString, String searchCategory, int matchFlags)
            throws SystemException {
        try {
            return searchDAOFactory.getSearchDAO(searchCategory).getNumSearchHits(searchString, matchFlags);
        }
        catch (Throwable e) {
            logger.error(e); // log the error
            throw new SystemException(e); // rethrow the error
        }
    }

    private void search(String searchString,
                        String searchCategory,
                        int matchFlags,
                        int startIndex,
                        int numRows,
                        SortArgument[] sortArgs,
                        List<SearchHit> results)
            throws SystemException {
        try {
            List<SearchHit> searchHits =
                    searchDAOFactory.getSearchDAO(searchCategory).search(searchString,
                            matchFlags,
                            startIndex,
                            numRows,
                            sortArgs);
            results.addAll(searchHits);
        }
        catch (Throwable e) {
            // log the error
            logger.error(e);
            // rethrow the error
            throw new SystemException(e);
        }
    }

}
