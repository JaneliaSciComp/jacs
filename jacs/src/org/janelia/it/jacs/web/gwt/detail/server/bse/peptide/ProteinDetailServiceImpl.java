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

package org.janelia.it.jacs.web.gwt.detail.server.bse.peptide;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.genomics.ProteinAnnotation;
import org.janelia.it.jacs.model.genomics.ProteinClusterMember;
import org.janelia.it.jacs.server.access.FeatureDAO;
import org.janelia.it.jacs.web.gwt.common.client.service.GWTServiceException;
import org.janelia.it.jacs.web.gwt.common.server.JcviGWTSpringController;
import org.janelia.it.jacs.web.gwt.detail.client.service.protein.ProteinDetailService;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Nov 15, 2007
 * Time: 2:30:53 PM
 */
public class ProteinDetailServiceImpl extends JcviGWTSpringController implements ProteinDetailService {
    static transient Logger logger = Logger.getLogger(ProteinDetailServiceImpl.class.getName());

    private transient FeatureDAO featureDAO;

    public void setFeatureDAO(FeatureDAO featureDAO) {
        this.featureDAO = featureDAO;
    }

    public ProteinClusterMember getProteinClusterInfo(String proteinAcc) throws GWTServiceException {
        try {
            return featureDAO.getProteinClusterMemberInfo(proteinAcc);
        }
        catch (Exception e) {
            throw new GWTServiceException(e.toString(), e);
        }
    }

    public List<ProteinAnnotation> getProteinAnnotations(String proteinAcc, SortArgument[] sortArgs) throws GWTServiceException {
        try {
            return featureDAO.getProteinAnnotations(proteinAcc, sortArgs);
        }
        catch (Exception e) {
            throw new GWTServiceException(e.toString(), e);
        }
    }
}
