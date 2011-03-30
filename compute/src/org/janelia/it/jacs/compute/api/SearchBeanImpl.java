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

package org.janelia.it.jacs.compute.api;

import org.apache.log4j.Logger;
import org.apache.lucene.search.Hit;
import org.apache.lucene.search.Hits;
import org.jboss.annotation.ejb.PoolClass;
import org.jboss.annotation.ejb.TransactionTimeout;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.engine.launcher.ProcessManager;
import org.janelia.it.jacs.compute.service.search.SearchQuerySessionContainer;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.model.user_data.search.SearchResultNode;
import org.janelia.it.jacs.shared.lucene.LuceneDataFactory;
import org.janelia.it.jacs.shared.lucene.searchers.LuceneSearcher;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 26, 2007
 * Time: 2:06:55 PM
 */
@Stateless(name = "SearchEJB")
@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
@TransactionTimeout(432000)
@PoolClass(value = org.jboss.ejb3.StrictMaxPool.class, maxSize = 100, timeout = 10000)
public class SearchBeanImpl implements SearchBeanLocal, SearchBeanRemote {
    private Logger _logger = Logger.getLogger(this.getClass());
    public static final String SEARCH_EJB_PROP = "SearchEJB.Name";

    public SearchBeanImpl() {
    }

    /**
     * Duplicate to ComputeBean method, kind of.  Could call submitJob(process, taskId) from the front-end.
     */
    public void submitSearchTask(long taskId) {
        ProcessManager processManager = new ProcessManager();
        processManager.launch("Search", taskId);
    }

    /**
     * Duplicate to ComputeBean method
     */
    public Task saveOrUpdateTask(Task task) throws DaoException {
        new ComputeDAO(_logger).saveOrUpdate(task);
        return task;
    }

    /**
     * Duplicate to ComputeBean method
     */
    public Task getTaskById(long taskId) {
        ComputeDAO _computeDAO = new ComputeDAO(_logger);
        return _computeDAO.getTaskById(taskId);
    }

    public SearchResultNode getSearchTaskResultNode(long searchTaskId) throws DaoException {
        ComputeDAO _computeDAO = new ComputeDAO(_logger);
        return _computeDAO.getSearchTaskResultNode(searchTaskId);
    }

    public Task getTaskWithEventsById(long taskId) throws DaoException {
        ComputeDAO _computeDAO = new ComputeDAO(_logger);
        return _computeDAO.getTaskWithEventsById(taskId);
    }

    public Task getTaskWithResultsById(long taskId) throws DaoException {
        ComputeDAO _computeDAO = new ComputeDAO(_logger);
        return _computeDAO.getTaskWithResultsById(taskId);
    }

//    public Event saveEvent(Task task, String eventType, String description, Date timestamp) throws DaoException {
//        return _computeDAO.createEvent(task.getObjectId(),eventType,description,timestamp);
//    }

    public int populateSearchResult(Long searchTaskId, List<String> topics) throws DaoException {
        int numResults;
        ComputeDAO _computeDAO = new ComputeDAO(_logger);
        SearchQuerySessionContainer searchSessionContainer =
                new SearchQuerySessionContainer(SearchQuerySessionContainer.SEARCH_ENGINE_LUCENE, _computeDAO);
        SearchTask searchTask = (SearchTask) _computeDAO.getTaskById(searchTaskId);
        try {
            numResults = searchSessionContainer.populateSearchResult(searchTask, topics);
            _logger.debug("**************SearchBean.populateSearchResult() for searchId=" + searchTaskId +
                    " topics=" + topics + " returned numResults=" + numResults);
        }
        catch (Exception e) {
            throw new DaoException(e.getMessage(), e);
        }
        return numResults;
    }


    /**
     * Method used by MBean to fire off Lucene searches
     *
     * @param searchType   - index type to search
     * @param searchString - what to look for
     */
    public void search(String searchType, String searchString) {
        try {
            LuceneSearcher searcher = LuceneDataFactory.getDocumentSearcher(searchType);
            Hits hits = searcher.search(searchString);
            for (Iterator iterator = hits.iterator(); iterator.hasNext();) {
                Hit o = (Hit) iterator.next();
                System.out.println("Hit: " + o.getId() + ", score:" + o.getScore() + ", name:" + o.get("name") + ", description:" + o.get("description"));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
