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

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.user_data.search.SearchResultNode;

import javax.ejb.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Oct 26, 2007
 * Time: 2:07:36 PM
 */
@Remote
public interface SearchBeanRemote {

    public void submitSearchTask(long searchTaskId) throws RemoteException;

    public Task saveOrUpdateTask(Task task) throws DaoException, RemoteException;

    public Task getTaskById(long taskId) throws RemoteException;

    public SearchResultNode getSearchTaskResultNode(long searchTaskId) throws DaoException, RemoteException;

    public Task getTaskWithEventsById(long taskId) throws DaoException, RemoteException;

    public Task getTaskWithResultsById(long taskId) throws DaoException, RemoteException;

    //    public Event saveEvent(Task task, String eventType, String description, Date timestamp) throws DaoException, RemoteException;
    public int populateSearchResult(Long searchTaskId, List<String> topic) throws DaoException, RemoteException;

    public void search(String searchType, String searchString) throws RemoteException;
}
