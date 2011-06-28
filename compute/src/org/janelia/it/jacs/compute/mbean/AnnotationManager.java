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

package org.janelia.it.jacs.compute.mbean;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.annotation.Annotation;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.colorSeparator.ColorSeparatorTask;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorPipelineTask;
import org.janelia.it.jacs.model.tasks.neuronSeparator.NeuronSeparatorTask;
import org.janelia.it.jacs.model.user_data.Node;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: tsafford
 * Date: Jul 2, 2007
 * Time: 5:00:41 PM
 */
public class AnnotationManager implements AnnotationManagerMBean {

    private static final Logger LOGGER = Logger.getLogger(AnnotationManager.class);

    public AnnotationManager() {
    }

    public String addAnnotation(String owner, String namespace, String term, String value, String comment, String conditional){
        return EJBFactory.getRemoteAnnotationBean().addAnnotation(owner, namespace, term, value, comment, conditional);
    }

    public void deleteAnnotation(String owner, String uniqueIdentifier){
        EJBFactory.getRemoteAnnotationBean().deleteAnnotation(owner, uniqueIdentifier);
    }

    public String getAnnotationsForUser(String owner){
        StringBuffer sbuf = new StringBuffer();
        ArrayList<Annotation> tmpAnnotations =  EJBFactory.getRemoteAnnotationBean().getAnnotationsForUser(owner);
        for (Annotation tmpAnnotation : tmpAnnotations) {
            sbuf.append(tmpAnnotation.toString()).append("\n");
        }
        return sbuf.toString();
    }

    public void editAnnotation(String owner, String uniqueIdentifier, String namespace, String term, String value,
                               String comment, String conditional){
        EJBFactory.getRemoteAnnotationBean().editAnnotation(owner, uniqueIdentifier, namespace, term, value, comment, conditional);
    }

    public void testNeuronSep(String inputFilePath) {
        try {
            NeuronSeparatorTask neuTask = new NeuronSeparatorTask(new HashSet<Node>(), "saffordt", new ArrayList<Event>(),
                    new HashSet<TaskParameter>());
            neuTask.setJobName("Neuron Separator Test");
            neuTask.setParameter(NeuronSeparatorTask.PARAM_inputTifFilePath, inputFilePath);
            neuTask = (NeuronSeparatorTask)EJBFactory.getLocalComputeBean().saveOrUpdateTask(neuTask);
            EJBFactory.getLocalComputeBean().submitJob("NeuronSeparation", neuTask.getObjectId());
        }
        catch (DaoException e) {
            e.printStackTrace();
        }
    }

    public void testColorSep(String inputFileDirectory) {
        try {
            File tmpFile = new File(inputFileDirectory);
            File[] tmpFiles = tmpFile.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String name) {
                    return (name.indexOf(".seg_")>0);
                }
            });
            StringBuffer sbuf = new StringBuffer();
            for (int i = 0; i < tmpFiles.length; i++) {
                File file = tmpFiles[i];
                sbuf.append(file.getAbsolutePath());
                if ((i+1)<tmpFiles.length) { sbuf.append(","); }
            }
            ColorSeparatorTask colorTask = new ColorSeparatorTask(new HashSet<Node>(), "saffordt", new ArrayList<Event>(),
                    new HashSet<TaskParameter>());
            colorTask.setJobName("Color Separator Test");
            colorTask.setParameter(ColorSeparatorTask.PARAM_inputFileList, sbuf.toString());
            colorTask = (ColorSeparatorTask)EJBFactory.getLocalComputeBean().saveOrUpdateTask(colorTask);
            EJBFactory.getLocalComputeBean().submitJob("ColorSeparation", colorTask.getObjectId());
        }
        catch (DaoException e) {
            e.printStackTrace();
        }
    }

    public void testNeuronSepPipeline(String inputFilePath) {
        try {
            NeuronSeparatorPipelineTask neuTask = new NeuronSeparatorPipelineTask(new HashSet<Node>(), "saffordt", new ArrayList<Event>(),
                    new HashSet<TaskParameter>());
            neuTask.setJobName("Neuron Separator Pipeline Test");
            neuTask.setParameter(NeuronSeparatorPipelineTask.PARAM_inputLsmFilePathList, inputFilePath);
            neuTask = (NeuronSeparatorPipelineTask)EJBFactory.getLocalComputeBean().saveOrUpdateTask(neuTask);
            EJBFactory.getLocalComputeBean().submitJob("NeuronSeparationPipeline", neuTask.getObjectId());
        }
        catch (DaoException e) {
            e.printStackTrace();
        }
    }
}