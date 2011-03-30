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

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Oct 30, 2007
 * Time: 3:30:30 PM
 */
class VersionedExternalAccessionSearchResultBuilder extends AccessionSearchResultBuilder {

    VersionedExternalAccessionSearchResultBuilder() {
    }

    protected SQLQuery createSearchQuery(Object[] accessionQueryParams, Session session) {
        String searchedAcc = (String) accessionQueryParams[0];
        String sql = "select " +
                "camera_acc as accession," +
                "et.name," +
                "external_acc as docAccessionName " +
                "from " +
                "sequence_entity se, entity_type et " +
                "where upper(se.external_acc) in (:accession ) " +
                "and se.entity_type_code = et.code " +
                "order by obs_flag desc, " +
                "externalAccSortValue(external_acc)";
        SQLQuery sqlQuery = session.createSQLQuery(sql);
        _logger.debug("Versioned external accession search sql: " + sql + " for " + searchedAcc);
        // add the first 100 accessions to searcheable versioned external accessions 
        ArrayList accessionVersions = new ArrayList();
        for (int i = 0; i < 100; i++) {
            accessionVersions.add(searchedAcc.toUpperCase() + "." + String.valueOf(i + 1));
        }
        sqlQuery.setParameterList("accession", accessionVersions);
        sqlQuery.setMaxResults(1); // interested only in 1 match
        return sqlQuery;
    }

}
