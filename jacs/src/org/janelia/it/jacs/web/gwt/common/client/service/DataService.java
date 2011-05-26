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

package org.janelia.it.jacs.web.gwt.common.client.service;

import com.google.gwt.user.client.rpc.RemoteService;
import org.janelia.it.jacs.model.common.BlastableNodeVO;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.common.UserDataNodeVO;
import org.janelia.it.jacs.model.common.UserDataVO;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.model.prokPipeline.ProkGenomeVO;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.web.gwt.common.client.model.genomics.BlastHit;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Site;

import java.util.*;

public interface DataService extends RemoteService {
    public Set<String> getTaskMessages(String taskId);

    public Boolean isTaskDone(String taskId) throws GWTServiceException;

    public UserDataNodeVO saveUserDefinedFastaNode(String nodeName, String fastaText, String visibility) throws Exception;

    public String getSequenceTypeForFASTA(String fastaText);

    public String getFilenodeByTaskId(String taskId, String contentType) throws Exception;

    public List<BlastHit> getPagedBlastHitsByTaskId(String taskId, int startIndex, int numRows, SortArgument[] sortArgs);

    public Map<Site, Integer> getSitesForBlastResult(String taskId);

    public String replaceNodeName(String nodeId, String nodeName);

    public void deleteNode(String nodeId);

    public void markUserForDeletion(String userId);

    public Integer getNumBlastableNodesForUser(String searchString, String sequenceType);

    public UserDataNodeVO[] getPagedBlastableNodesForUser(String searchString, String sequenceType, int startIndex, int numRows, SortArgument[] sortArgs);

    public List<String> getBlastableNodeNamesForUser(String searchString, String sequenceType);

    public void setTaskExpirationAndName(String taskId, Date expirationDate, String jobName) throws GWTServiceException;

    public List<String> getUserLogins() throws GWTServiceException;

    public Integer getNumUsers(String searchString) throws GWTServiceException;
    public UserDataVO[] getPagedUsers(String searchString, int startIndex, int numRows, SortArgument[] sortArgs) throws GWTServiceException;

    public BlastableNodeVO[] getReversePsiBlastDatasets() throws GWTServiceException;

    public String submitJob(Task newTask) throws GWTServiceException;

    public List<String> getFiles(String tmpDirectory, boolean directoriesOnly) throws GWTServiceException;

    public HashSet<String> getProjectCodes() throws GWTServiceException;

    public ProkGenomeVO getProkGenomeVO(String targetGenome) throws GWTServiceException;

    public void validateFilePath(String filePath) throws GWTServiceException;

    public Integer getNumNodesForUserByName(String nodeClassName);

    public UserDataNodeVO[] getPagedNodesForUserByName(String nodeClassName, int startIndex, int numRows, SortArgument[] sortArgs);

    public List<String> getEntityTypeNames() throws GWTServiceException;
    public Integer getNumEntityTypes(String searchString) throws GWTServiceException;
    public List<EntityType> getPagedEntityTypes(String searchString, int startIndex, int numRows, SortArgument[] sortArgs) throws GWTServiceException;
}