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

package org.janelia.it.jacs.compute.service.neuronSeparator;

import org.ggf.drmaa.DrmaaException;
import org.janelia.it.jacs.compute.drmaa.SerializableJobTemplate;
import org.janelia.it.jacs.compute.engine.data.MissingDataException;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.common.grid.submit.sge.SubmitDrmaaJobService;
import org.janelia.it.jacs.compute.util.FileUtils;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorPipelineTask;
import org.janelia.it.jacs.model.user_data.neuronSeparator.NeuronSeparatorResultNode;
import org.janelia.it.jacs.model.vo.ParameterException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Dec 15, 2008
 * Time: 3:47:21 PM
 * @deprecated I'm pretty sure this is not used by anything and can be deleted, along with its process file.
 */
public class NeuronSeparatorSubmitJobService extends SubmitDrmaaJobService {

    private static final String CONFIG_PREFIX = "neuSepConfiguration.";
    private static final String REMOTE_SCRIPT = "runsep.fedora.sh";
    private static final String STDOUT_FILE = "runsep.out";
    private static final String STDERR_FILE = "runsep.err";

    /**
     * This method is intended to allow subclasses to define service-unique filenames which will be used
     * by the grid processes, within a given directory.
     *
     * @return - unique (subclass) service prefix name. ie "blast"
     */
    protected String getGridServicePrefixName() {
        return "neuSep";
    }

    /**
     * Method which defines the general job script and node configurations
     * which ultimately get executed by the grid nodes
     */
    protected void createJobScriptAndConfigurationFiles(FileWriter writer) throws Exception {
        File configFile = new File(getSGEConfigurationDirectory() + File.separator + CONFIG_PREFIX + "1");
        boolean fileSuccess = configFile.createNewFile();
        if (!fileSuccess){
            throw new ServiceException("Unable to create a config file for the Neuron Separation pipeline.");
        }

        createShellScript(writer);
        setJobIncrementStop(1);
    }

    protected void setQueue(SerializableJobTemplate jt) throws DrmaaException {
        logger.info("Drmaa job=" + jt.getJobName() + " assigned nativeSpec=" + NORMAL_QUEUE);
        jt.setNativeSpecification(NORMAL_QUEUE);
    }

    private void createShellScript(FileWriter writer)
            throws IOException, ParameterException, MissingDataException, InterruptedException, ServiceException {

        NeuronSeparatorResultNode parentNode = (NeuronSeparatorResultNode) ProcessDataHelper.getResultFileNode(processData);
        
        String script = NeuronSeparatorHelper.getNeuronSeparationCommands((NeuronSeparatorPipelineTask)task, parentNode, "mylib.fedora", " ; ");
    	File scriptFile = new File(parentNode.getDirectoryPath(), REMOTE_SCRIPT);
    	FileUtils.writeStringToFile(scriptFile, NeuronSeparatorHelper.covertPathsToRemoteServer(script));

    	File outFile = new File(parentNode.getDirectoryPath(), STDOUT_FILE);
        File errFile = new File(parentNode.getDirectoryPath(), STDERR_FILE);
        // Need to use bash to get process substitution for the tricksy tee stuff to work. It is explained here:
        // http://stackoverflow.com/questions/692000/how-do-i-write-stderr-to-a-file-while-using-tee-with-a-pipe
        String cmdLine = "bash "+ NeuronSeparatorHelper.covertPathsToRemoteServer(scriptFile.getAbsolutePath()) +
//        	" 1>>"+outFile.getAbsolutePath()+" 2>>"+errFile.getAbsolutePath()
        	" > >(tee "+outFile.getAbsolutePath()+") 2> >(tee "+errFile.getAbsolutePath()+" >&2)";

        StringBuffer wrapper = new StringBuffer();
        wrapper.append("set -o errexit\n");
        wrapper.append(cmdLine).append("\n");
        writer.write(script.toString());
    }

}