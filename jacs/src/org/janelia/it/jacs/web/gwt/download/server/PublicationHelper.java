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

package org.janelia.it.jacs.web.gwt.download.server;

import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Sample;
import org.janelia.it.jacs.web.gwt.download.client.model.Project;
import org.janelia.it.jacs.web.gwt.download.client.model.Publication;

import java.util.List;
import java.util.Map;

/**
 * Implement this to provide information about publications and their supporting data.
 * <p/>
 * User: Lfoster
 * Date: Aug 25, 2006
 * Time: 10:09:11 AM
 */
public interface PublicationHelper {
    public static final String DESCRIPTIVE_TEXT = "Description";
    public static final String COMING_SOON = "(Coming Soon)";

    Map<String, Project> getSymbolToProjectMapping();

    Map<String, Publication> getAccessionToPublicationMapping();

    void saveOrUpdateProject(org.janelia.it.jacs.web.gwt.download.client.model.ProjectImpl project);

    Project getProjectByName(String projectName);

    Project getProjectBySymbol(String projectSymbol);

    Publication getPublicationByAccession(String publicationAccession);

    List<String> getNewFiles();

    Boolean checkFileLocation(String fileLocation);

    List<Sample> getProjectSamples(String projectSymbol);

    Map<String, List<Sample>> getProjectSamplesByProject();

    List<DownloadableDataNode> getDownloadableFilesBySampleAcc(String sampleAcc);
}
