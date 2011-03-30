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

package org.janelia.it.jacs.server.access.hibernate;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.search.SearchHit;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Site;
import org.janelia.it.jacs.web.gwt.common.shared.data.ImageModel;
import org.janelia.it.jacs.web.gwt.search.client.model.AccessionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * User: cgoina
 * Implementation of JaCS accession search DAO. This class is in a different
 * package than the other searchDAOs because this is not a hibernate based implementation.
 */
public class AccessionSearchDAOImpl extends SearchDAOImpl {
    private static Logger _logger = Logger.getLogger(AccessionSearchDAOImpl.class);

    protected AccessionSearchDAOImpl() {
    }

    public int executeSearchTask(SearchTask searchTask)
            throws DaoException {
        return -1;
    }

    public int getNumSearchHits(String searchString, int matchFlags)
            throws DaoException {
        return countSearchHits(searchString, SearchTask.TOPIC_ACCESSION, matchFlags);
    }

    public List<SearchHit> search(String searchString,
                                  int matchFlags,
                                  int startIndex,
                                  int numRows,
                                  SortArgument[] sortArgs)
            throws DaoException {
        return new ArrayList<SearchHit>();
    }

    public List<AccessionResult> getPagedCategoryResultsByNodeId(Long nodeId,
                                                                 int startIndex,
                                                                 int numRows,
                                                                 SortArgument[] sortArgs) throws DaoException {
        String sql = "select " +
                "accession,docname,doctype,headline " +
                "from accession_ts_result nt " +
                "where nt.node_id = :nodeId";
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        sqlQuery.setLong("nodeId", nodeId);
        if (startIndex >= 0) {
            sqlQuery.setFirstResult(startIndex);
        }
        if (numRows > 0) {
            sqlQuery.setMaxResults(numRows);
        }
        List<Object[]> results = sqlQuery.list();
        List<AccessionResult> accSearchResults = new ArrayList<AccessionResult>();
        for (Object[] res : results) {
            AccessionResult accSearchRes = new AccessionResult();
            accSearchRes.setAccession((String) res[0]);
            accSearchRes.setDescription((String) res[1]);
            accSearchRes.setAccessionType((String) res[2]);
            accSearchRes.setHeadline((String) res[3]);
            accSearchResults.add(accSearchRes);
        }
        return accSearchResults;
    }

    public int getNumCategoryResultsByNodeId(Long nodeId) throws DaoException {
        String sql = "select cast(count(1) as Integer) " +
                "from accession_ts_result nt " +
                "where nt.node_id = :nodeId";
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        sqlQuery.setLong("nodeId", nodeId);
        int count = ((Integer) sqlQuery.uniqueResult()).intValue();
        return count;
    }

    public List<ImageModel> getSearchResultCharts(Long searchId, String resultBaseDirectory)
            throws DaoException {
        return null;  // no charts for accession results
    }

    public Set<Site> getMapInfoForSearchResultsBySearchId(Long searchId, String category)
            throws DaoException {
        throw new UnsupportedOperationException("Map Info is not supported for " + category);
    }

}
