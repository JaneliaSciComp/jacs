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

package org.janelia.it.jacs.web.gwt.download.client;

import com.google.gwt.user.client.rpc.RemoteService;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.SampleItem;
import org.janelia.it.jacs.web.gwt.common.client.security.NotLoggedInException;
import org.janelia.it.jacs.web.gwt.download.client.model.Project;
import org.janelia.it.jacs.web.gwt.download.client.model.ProjectImpl;
import org.janelia.it.jacs.web.gwt.download.client.model.Publication;

import java.util.List;
import java.util.Map;

/**
 * Download-Page's Service.  Server-side of the Download equation.
 * User: lfoster
 * Date: Aug 24, 2006
 * Time: 5:43:39 PM
 */
public interface DownloadMetaDataService extends RemoteService {
    public Map<String, Project> getSymbolToProjectMapping();


    public Map<String, Publication> getAccessionToPublicationMapping();


    public void saveOrUpdateProject(ProjectImpl project);


    public Project getProjectByName(String projectName);

    public Project getProjectBySymbol(String projectSymbol);

    public Publication getPublicationByAccession(String publicationAccession);

    public List<String> getNewFiles();

    public Boolean checkFileLocation(String fileLocation) throws NotLoggedInException;

    public List<Sample> getProjectSamples(String projectSymbol);

    public Map<String, List<Sample>> getProjectSamplesByProject();

    public List<DownloadableDataNode> getDownloadableFilesBySampleAcc(String sampleAcc);

    public List<SampleItem> getProjectSampleInfo(int startIndex, int endIndex, SortArgument[] sortArgs, List<String> selectedProjectNames);

    public List<SampleItem> getProjectSampleInfo(SortArgument[] sortArgs);

    public Integer getNumProjectSampleInfo(List<String> selectedProjectNames);
}
