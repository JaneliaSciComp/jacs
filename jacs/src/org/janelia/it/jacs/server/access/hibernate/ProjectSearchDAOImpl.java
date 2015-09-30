
package org.janelia.it.jacs.server.access.hibernate;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.search.SearchHit;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.web.gwt.common.shared.data.ImageModel;

import java.util.List;

//import org.janelia.it.jacs.web.gwt.search.client.model.ProjectResult;

/**
 * User: cgoina
 * Implementation of project specific search DAO
 */
public class ProjectSearchDAOImpl extends SearchDAOImpl {
    private static Logger _logger = Logger.getLogger(ProjectSearchDAOImpl.class);


    public ProjectSearchDAOImpl() {
    }

    public int executeSearchTask(SearchTask searchTask)
            throws DaoException {
        return 0;
//        return populateSearchResult(searchTask,SearchTask.TOPIC_PROJECT);
    }

    public int getNumSearchHits(String searchString, int matchFlags)
            throws DaoException {
        return countSearchHits(searchString, SearchTask.TOPIC_PROJECT, matchFlags);
    }

    public List<SearchHit> search(String searchString,
                                  int matchFlags,
                                  int startIndex,
                                  int numRows,
                                  SortArgument[] sortArgs)
            throws DaoException {
        return performGenericSearch(searchString, SearchTask.TOPIC_PROJECT, matchFlags, startIndex, numRows, sortArgs);
    }

//    public List<ProjectResult> getPagedCategoryResultsByNodeId(Long nodeId,
//                                                               int startIndex,
//                                                               int numRows,
//                                                               SortArgument[] sortArgs) throws DaoException {
//        String sql = "select " +
//                "pr.symbol, " +
//                "pr.description, " +
//                "pr.principal_investigators, " +
//                "pr.organization, " +
//                "pr.email, " +
//                "pr.website_url, " +
//                "pr.name, " +
//                "pr.released, " +
//                "pr.funded_by, " +
//                "pr.institutional_affiliation, " +
//                "nt.rank " +
//                "from (select hit_id, rank from project_ts_result where node_id=" + nodeId + " order by rank desc) nt " +
//                "inner join project pr on pr.symbol=nt.hit_id " +
//                "where pr.released=true";
//        sql = addOrderClauseToSql(sql, sortArgs);
//        _logger.info("Executing project search sql=" + sql);
//        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
//        // NOTE: for this query, we are handling the start and number manually since we do not expect a large
//        // number of rows returned. This gives us flexibility in generating the site meta-data.
//        List<Object[]> results = sqlQuery.list();
//        _logger.info("Project search yielded result count=" + results.size());
//        List<ProjectResult> projects = new ArrayList<ProjectResult>();
//        for (Object[] res : results) {
//            ProjectResult projectResult = new ProjectResult();
//            String accession = (String) res[0];
//            projectResult.setAccession(accession);
//            projectResult.setDescription((String) res[1]);
//            projectResult.setInvestigators((String) res[2]);
//            projectResult.setOrganization((String) res[3]);
//            projectResult.setEmail((String) res[4]);
//            projectResult.setWebsiteUrl((String) res[5]);
//            projectResult.setName((String) res[6]);
//            projectResult.setReleased((Boolean) res[7]);
//            projectResult.setFundingSource((String) res[8]);
//            projectResult.setInstitution((String) res[9]);
//            projectResult.setRank((Float) res[10]);
//            projects.add(projectResult);
//        }
//        List<ProjectResult> finalProjectList = new ArrayList<ProjectResult>();
//        if (startIndex >= 0 || numRows > 0) {
//            if (numRows == 0)
//                numRows = projects.size() - startIndex;
//            for (int i = startIndex; i < startIndex + numRows; i++)
//                finalProjectList.add(projects.get(i));
//        }
//        else {
//            finalProjectList = projects;
//        }
//        //addDocumentsToCategoryResultsByNodeId(nodeId, finalProjectList);
//        _logger.info("Returning project result of size=" + finalProjectList.size());
//        return finalProjectList;
//    }
//
    public int getNumCategoryResultsByNodeId(Long nodeId) throws DaoException {
        String sql =
                "select cast(count(1) as Integer)" +
                        "from project_ts_result ts inner join project pr on ts.hit_id=pr.symbol " +
                        "where ts.node_id=" + nodeId + " and pr.released";
        _logger.info("Executing project search sql=" + sql);
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        int count = ((Integer) sqlQuery.uniqueResult()).intValue();
        return count;
    }

    public List<ImageModel> getSearchResultCharts(Long searchId, String resultBaseDirectory)
            throws DaoException {
        return null; // not yet implemented
    }

}
