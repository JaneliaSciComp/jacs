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

package org.janelia.it.jacs.web.gwt.download.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.model.download.DataFile;

import java.io.Serializable;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Sep 25, 2006
 * Time: 10:56:22 AM
 * <p/>
 * Project, which has publications.  One such project is "CAMERA".
 */
public interface Project extends Serializable, IsSerializable, Comparable {
    public static final String NAME_SORT = "name";
    public static final String PI_SORT = "principal_investigators";
    public static final String ORG_SORT = "organization";
    public static final String HABITAT_SORT = "habitat";//TODO
    public static final String DESCRIPTION_SORT = "description";

    List<Publication> getPublications(); // Return a list of publication objects.

    String getProjectSymbol(); // This is the actual ID - added for reference purposes

    String getProjectName(); // There might be portions of the code which use the name for dentification.

    String getDescription();

    String getPrincipalInvestigators();     // comma-separated list

    String getOrganization();

    String getEmail();

    String getWebsite();

    String getFundedBy();                   // comma-separated list

    String getInstitutionalAffiliation();   // comma-separated list

    List<DataFile> getRolledUpArchivesOfPublications(); // Return list of all-pubs-compressed archives.
}
