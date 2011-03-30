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

package org.janelia.it.jacs.web.control;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.download.Project;
import org.janelia.it.jacs.model.download.Publication;
import org.janelia.it.jacs.model.genomics.AccessionIdentifierUtil;
import org.janelia.it.jacs.server.access.DownloadDAO;
import org.janelia.it.jacs.web.NotYetImplementedException;
import org.springframework.web.bind.RequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.BaseCommandController;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Jan 10, 2007
 * Time: 10:30:36 AM
 */
public class DetailPageForwardingController extends BaseCommandController {
    public static final String ACC_PARAM_NAME = "acc";

    private Logger logger = Logger.getLogger(DetailPageForwardingController.class);
    private DownloadDAO downloadDAO;

    public void setDownloadDAO(DownloadDAO downloadDAO) {
        this.downloadDAO = downloadDAO;
    }

    protected ModelAndView handleRequestInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        // must contain an acc
        String acc = getAcc(httpServletRequest);
        int accType = AccessionIdentifierUtil.getAccType(acc);
        RedirectView rv = null;
        String ncbiDBName = null;
        switch (accType) {
            case AccessionIdentifierUtil.CAMERA_PROJECT_ACC:
                // forward to project page
                Project project = downloadDAO.findReleasedProjectBySymbol(acc);
                if (project != null) {
                    rv = new RedirectView("/gwt/BrowseProjectsPage" +
                            "/BrowseProjectsPage.oa?projectSymbol=" +
                            project.getSymbol(),
                            true);
                }
                else {
                    throw new Exception("Invalid project symbol: " + acc);
                }
                break;
            case AccessionIdentifierUtil.CAMERA_PUBLICATION_ACC:
                // forward to publication page
                Publication publication = downloadDAO.findPublicationByAccessionNo(acc);
                if (publication != null) {
                    rv = new RedirectView("/gwt/DownloadByPubPage" +
                            "/DownloadByPubPage.oa?publicationAccessionNo=" +
                            publication.getPublicationAccession(),
                            true);
                }
                else {
                    throw new Exception("Invalid publication accesion number: " + acc);
                }
                break;
            case AccessionIdentifierUtil.CAMERA_READ_ACC:
            case AccessionIdentifierUtil.CAMERA_ORF_ACC:
            case AccessionIdentifierUtil.CAMERA_PROTEIN_ACC:
            case AccessionIdentifierUtil.CAMERA_SCAFFOLD_ACC:
            case AccessionIdentifierUtil.NCBI_GENF_ACC:
            case AccessionIdentifierUtil.NCBI_CNTG_ACC:
            case AccessionIdentifierUtil.NCBI_NT_ACC:
            case AccessionIdentifierUtil.NCBI_AA_ACC:
            case AccessionIdentifierUtil.MISC_SEQ_ACC:
            case AccessionIdentifierUtil.CAMERA_PROTEIN_CLUSTER_ACC:
                logger.debug("Redirect to details for acc=" + acc);
                rv = new RedirectView("/gwt/DetailPage/DetailPage.htm?acc=" + acc, true);
                break;
            case AccessionIdentifierUtil.INVALID_ACC:
                logger.error("Unknown accession type for: " + acc);
                throw new Exception("Unknown accession type for: " + acc);
            default:
                throw new NotYetImplementedException("Show details for: " + acc);
        }

        return new ModelAndView(rv);
    }

    private String getAcc(HttpServletRequest httpServletRequest) throws Exception {
        // first try to get it from acc=<ID> parameter as in detailPage.htm?acc=23455
        String acc = RequestUtils.getStringParameter(httpServletRequest, "acc", null);

        // if not there, try to get it from id=<ID> as in jacs/id=1323445
        if (acc == null || "".equals(acc)) {
            acc = httpServletRequest.getQueryString();
        }
        return acc;
    }

}
