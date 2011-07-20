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

package org.janelia.it.jacs.compute.service.blast.createtask;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.service.blast.BlastProcessDataConstants;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskFactory;
import org.janelia.it.jacs.model.tasks.blast.BlastNTask;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.vo.MultiSelectVO;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * @author Tareq Nabeel
 */
public class CreateTaskService implements IService {

    protected IProcessData processData;
    protected ComputeDAO computeDAO;

    private String blastType;
    private String jobName;
    private String userLogin;
    private String datasetName;
    private BlastTask blastTask;
    private Map blastParameters;

    public void execute(IProcessData processData) throws CreateTaskException {
        try {
            Logger logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            this.processData = processData;
            computeDAO = new ComputeDAO(logger);
            blastType = (String) processData.getMandatoryItem(BlastProcessDataConstants.BLAST_TYPE);
            jobName = (String) processData.getMandatoryItem(IProcessData.JOB_NAME);
            datasetName = (String) processData.getMandatoryItem(BlastProcessDataConstants.DATA_SET_NAME);
            userLogin = (String) processData.getMandatoryItem(IProcessData.USER_NAME);
            blastParameters = (Map) processData.getMandatoryItem(BlastProcessDataConstants.BLAST_PARAMETERS);
            validateUserLogin(userLogin);
            createBlastTask();
            configureBlastTask();
            saveBlastTask();
            processData.putItem(IProcessData.PROCESS_ID, blastTask.getObjectId());
            processData.putItem(BlastProcessDataConstants.TASK, blastTask);
        }
        catch (Exception e) {
            throw new CreateTaskException(e);
        }
    }

    private void createBlastTask() {
        this.blastTask = (BlastTask) TaskFactory.createTask(this.blastType);
    }

    private void configureBlastTask() {
        blastTask.setJobName(jobName);
        blastTask.setOwner(userLogin);
        setBlastDataSet();
        setBlastParameters();
    }

    private void setBlastDataSet() {
        Node node = computeDAO.getBlastDatabaseFileNodeByName(datasetName);
        MultiSelectVO ms = new MultiSelectVO();
        ArrayList<String> dbList = new ArrayList<String>();
        dbList.add(String.valueOf(node.getObjectId()));
        ms.setPotentialChoices(dbList);
        ms.setActualUserChoices(dbList);
        blastTask.setParameter(BlastNTask.PARAM_subjectDatabases, Task.csvStringFromCollection(ms.getActualUserChoices()));
    }

    private void setBlastParameters() {
        Set keySet = blastParameters.keySet();
        for (Object aKeySet : keySet) {
            String key = (String) aKeySet;
            blastTask.setParameter(key, (String) blastParameters.get(key));
        }
    }

    private void validateUserLogin(String userLogin) throws MissingDataException {
        User user = computeDAO.getUserByName(userLogin);
        if (user == null) {
            throw new MissingDataException("User " + userLogin + " does not exist");
        }
    }

    private void saveBlastTask() throws DaoException {
        computeDAO.saveOrUpdate(blastTask);
    }
}
