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

package org.janelia.it.jacs.compute.service.search;

import org.hibernate.SQLQuery;
import org.hibernate.Session;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Oct 30, 2007
 * Time: 3:30:30 PM
 */
class ClusterAccessionSearchResultBuilder extends AccessionSearchResultBuilder {

    ClusterAccessionSearchResultBuilder() {
    }

    List<AccessionSearchResult> retrieveAccessionSearchResult(String acc,
                                                              Long searchResultNodeId,
                                                              Session session)
            throws Exception {
        List<AccessionSearchResult> results = null;
        AccessionSearchResultBuilder accSearchBuilder;
        String upperAcc = acc.toUpperCase();
        if (upperAcc.startsWith("CAM_CRCL_")) {
            accSearchBuilder = new CoreClusterAccessionSearchResultBuilder();
            results = accSearchBuilder.retrieveAccessionSearchResult(acc, searchResultNodeId, session);
        }
        else if (upperAcc.startsWith("CAM_CL_")) {
            accSearchBuilder = new FinalClusterAccessionSearchResultBuilder();
            results = accSearchBuilder.retrieveAccessionSearchResult(acc, searchResultNodeId, session);
        }
        else {
            _logger.info("Unrecognized cluster accession: " + "'" + acc + "'");
        }
        return results;
    }

    protected SQLQuery createSearchQuery(Object[] accessionQueryParams, Session session) {
        throw new UnsupportedOperationException();
    }
}
