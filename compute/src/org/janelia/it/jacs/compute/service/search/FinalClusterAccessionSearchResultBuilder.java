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

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Oct 30, 2007
 * Time: 3:30:30 PM
 */
class FinalClusterAccessionSearchResultBuilder extends AccessionSearchResultBuilder {

    FinalClusterAccessionSearchResultBuilder() {
    }

    protected SQLQuery createSearchQuery(Object[] accessionQueryParams, Session session) {
        String searchedAcc = (String) accessionQueryParams[0];
        String sql = "select " +
                "final_cluster_acc," +
                ":accessionType," +
                "final_cluster_acc as docAccessionName " +
                "from " +
                "core_cluster cc " +
                "where final_cluster_acc = :accession ";
        SQLQuery sqlQuery = session.createSQLQuery(sql);
        _logger.debug("Final cluster accession search sql: " + sql + " for " + searchedAcc);
        sqlQuery.setString("accessionType", "Final cluster");
        sqlQuery.setString("accession", searchedAcc.toUpperCase());
        // since there may be more than one entry in core cluster table with the same final cluster
        // limit the results to one - we are only interested if there is one
        sqlQuery.setMaxResults(1);
        return sqlQuery;
    }

    private int populateCoreClusterAccessionSearchResult(String acc,
                                                         Long searchResultNodeId,
                                                         Session session)
            throws Exception {
        int nResults;
        String sql = "insert into accession_ts_result " +
                "(" +
                "node_id," +
                "docid," +
                "accession," +
                "docname," +
                "doctype," +
                "headline" +
                ") " +
                "select " +
                ":searchResultNodeId," +
                ":docid," +
                "core_cluster_acc," +
                ":docname," +
                "'Core cluster'," +
                ":accReference " +
                "from " +
                "core_cluster cc" +
                "where core_cluster_acc = :accession ";
        SQLQuery sqlQuery = session.createSQLQuery(sql);
        _logger.debug("Core cluster accession search result sql: " + sql);
        sqlQuery.setLong("searchResultNodeId", searchResultNodeId);
        sqlQuery.setLong("docid", searchResultNodeId);
        sqlQuery.setString("docname", acc);
        sqlQuery.setString("accReference", acc);
        sqlQuery.setString("accession", acc);
        // invoke the query
        nResults = sqlQuery.executeUpdate();
        return nResults;
    }

    private int populateFinalClusterAccessionSearchResult(String acc,
                                                          Long searchResultNodeId,
                                                          Session session)
            throws Exception {
        int nResults;
        String sql = "insert into accession_ts_result " +
                "(" +
                "node_id," +
                "docid," +
                "accession," +
                "docname," +
                "doctype," +
                "headline" +
                ") " +
                "select " +
                ":searchResultNodeId," +
                ":docid," +
                "final_cluster_acc," +
                ":docname," +
                "'Final cluster'" +
                ":accReference " +
                "from " +
                "final_cluster cc t" +
                "where final_cluster_acc = :accession ";
        SQLQuery sqlQuery = session.createSQLQuery(sql);
        // since there may be more than one entry in core cluster table with the same final cluster
        // limit the results to one - we are only interested if there is one
        sqlQuery.setMaxResults(1);
        _logger.debug("Core cluster accession search result sql: " + sql);
        sqlQuery.setLong("searchResultNodeId", searchResultNodeId);
        sqlQuery.setLong("docid", searchResultNodeId);
        sqlQuery.setString("docname", acc);
        sqlQuery.setString("accReference", acc);
        sqlQuery.setString("accession", acc);
        // invoke the query
        nResults = sqlQuery.executeUpdate();
        return nResults;
    }

}
