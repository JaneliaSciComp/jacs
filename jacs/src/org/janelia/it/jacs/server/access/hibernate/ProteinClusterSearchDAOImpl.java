
package org.janelia.it.jacs.server.access.hibernate;

import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.SQLQuery;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.search.SearchHit;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.web.gwt.common.shared.data.ChartData;
import org.janelia.it.jacs.web.gwt.common.shared.data.ChartDataEntry;
import org.janelia.it.jacs.web.gwt.common.shared.data.ImageModel;

import java.util.ArrayList;
import java.util.List;

//import org.janelia.it.jacs.web.gwt.search.client.model.ClusterResult;

/**
 * User: cgoina
 * Implementation of protein cluster specific search DAO
 */
public class ProteinClusterSearchDAOImpl extends SearchDAOImpl {
    private static Logger _logger = Logger.getLogger(ProteinClusterSearchDAOImpl.class);

    public ProteinClusterSearchDAOImpl() {
    }

    public int executeSearchTask(SearchTask searchTask)
            throws DaoException {
        return 0;
        //return populateSearchResult(searchTask,SearchTask.TOPIC_CLUSTER);
    }

    public int getNumSearchHits(String searchString, int matchFlags)
            throws DaoException {
        return countSearchHits(searchString, SearchTask.TOPIC_CLUSTER, matchFlags);
    }

    public List<SearchHit> search(String searchString,
                                  int matchFlags,
                                  int startIndex,
                                  int numRows,
                                  SortArgument[] sortArgs)
            throws DaoException {
        return performGenericSearch(searchString, SearchTask.TOPIC_CLUSTER, matchFlags, startIndex, numRows, sortArgs);
    }

//    public List<ClusterResult> getPagedCategoryResultsByNodeId(Long nodeId,
//                                                               int startIndex,
//                                                               int numRows,
//                                                               SortArgument[] sortArgs) throws DaoException {
//        String sql = "select ts.rank, fd.final_cluster_acc, fd.num_core_cluster, fd.num_protein, fd.num_nonredundant, fd.gn_symbols, fd.protein_functions, fd.ec_List, fd.go_list " +
//                "from (select hit_id, rank from final_cluster_ts_result where node_id=" + nodeId + " order by rank desc) ts " +
//                "inner join final_cluster_detail fd on fd.final_cluster_id=ts.hit_id";
//        sql = addOrderClauseToSql(sql, sortArgs);
//        _logger.info("Executing cluster search sql=" + sql);
//        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
//        if (startIndex >= 0) {
//            sqlQuery.setFirstResult(startIndex);
//        }
//        if (numRows > 0) {
//            sqlQuery.setMaxResults(numRows);
//        }
//        List<Object[]> results = sqlQuery.list();
//        _logger.info("Cluster search yielded result count=" + results.size());
//        List<ClusterResult> clusters = new ArrayList<ClusterResult>();
//        for (Object[] res : results) {
//            ClusterResult clusterResult = new ClusterResult();
//            clusterResult.setRank((Float) res[0]);
//            clusterResult.setFinalAccession((String) res[1]);
//            clusterResult.setNumCoreClusters((Integer) res[2]);
//            clusterResult.setNumProteins((Integer) res[3]);
//            clusterResult.setNumNRProteins((Integer) res[4]);
//            clusterResult.setGeneSymbols((String) res[5]);
//            clusterResult.setProteinFunctions((String) res[6]);
//            List<AnnotationDescription> annoList = AnnotationUtil.createAnnotationListFromString((String) res[7]);
//            clusterResult.setEcAnnotationDescription(annoList);
//            annoList = AnnotationUtil.createAnnotationListFromString((String) res[8]);
//            clusterResult.setGoAnnotationDescription(annoList);
//            clusters.add(clusterResult);
//        }
//        _logger.info("Returning cluster result of size=" + clusters.size());
//        return clusters;
//    }
//
    public int getNumCategoryResultsByNodeId(Long nodeId) throws DaoException {
        String sql =
                "select cast(count(distinct hit_id) as Integer)" +
                        "from final_cluster_ts_result " +
                        "where node_id=" + nodeId;
        _logger.info("Executing row count cluster search sql=" + sql);
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        int count = ((Integer) sqlQuery.uniqueResult()).intValue();
        return count;
    }

    public List<ImageModel> getSearchResultCharts(Long searchId, String resultBaseDirectory)
            throws DaoException {
        List<ImageModel> clusterSearchResultCharts = new ArrayList<ImageModel>();
        clusterSearchResultCharts.add(getClustersByGOAssignment(searchId, "Biological Process", resultBaseDirectory));
        clusterSearchResultCharts.add(getClustersByGOAssignment(searchId, "Molecular Function", resultBaseDirectory));
/*
        the cluster distribution is no longer needed
        clusterSearchResultCharts.add(getClusterDistributionChart(searchId, resultBaseDirectory));
*/
        return clusterSearchResultCharts;
    }

    private ImageModel getClustersByGOAssignment(final Long searchId, final String goCategory, String resultBaseDirectory)
            throws DaoException {
        String chartTitle = goCategory + " Summary";
        try {
            ChartData dataset = getChartData("cluster" + goCategory + ".jo", resultBaseDirectory,
                    new ChartDataCallback() {
                        public ChartData getChartDataMethod() throws DaoException {
                            ChartData dataset = new ChartData();
                            retrieveGOAssignments(searchId, goCategory, dataset);
                            return dataset;
                        }
                    }
            );
            if (dataset.getNumberOfEntries() == 0) {
                return null;
            }
            /*
            * Generate the pie chart.
            */
            return chartTool.createPieChart(chartTitle,
                    dataset,
                    PIE_CHART_WIDTH,  // width
                    PIE_CHART_HEIGHT, // height
                    resultBaseDirectory, getResultChartDirectory(searchId));
        }
        catch (Exception e) {
            _logger.error("Error creating the GO assignment chart", e);
            throw new DaoException(e, "ProteinClusterSearchDAOImpl.getClustersByGOAssignment.createPieChart");
        }
    }

    private void retrieveGOAssignments(Long searchId, String goCategory, ChartData dataset)
            throws DaoException {
        String sql =
                "select coalesce(replace(clgo.name,'_',' '),'not assigned') as goName, count(1) as nClusters " +
                        "from final_cluster_ts_result ts " +
                        "left outer join final_cluster_annotation clgo on clgo.final_cluster_id = ts.hit_id " +
                        "and clgo.id like 'GO:%' and clgo.category = replace(lower(:goCategory),'_',' ')  " +
                        "where ts.node_id=(select node_id from node where task_id = :searchId) " +
                        "group by clgo.name";
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        sqlQuery.setLong("searchId", searchId);
        sqlQuery.setString("goCategory", goCategory);
        sqlQuery.addScalar("goName", Hibernate.STRING);
        sqlQuery.addScalar("nClusters", Hibernate.LONG);
        List<Object[]> results = sqlQuery.list();
        for (Object[] result : results) {
            String goDescription = (String) result[0];
            Long n = (Long) result[1];
            ChartDataEntry chartEntry = new ChartDataEntry(goDescription, n);
            chartEntry.setDescription(goDescription);
            dataset.addChartDataEntry(chartEntry);
        }
    }

    private void retrieveTotalNumberOfClusters(Long searchId, ChartData dataset)
            throws DaoException {
        String sql =
                "select count(1) as nClusters " +
                        "from final_cluster_ts_result " +
                        "where node_id = (select node_id from node where task_id = :searchId)";
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        sqlQuery.setLong("searchId", searchId);
        sqlQuery.addScalar("nClusters", Hibernate.LONG);
        List<Long> results = sqlQuery.list();
        dataset.setTotal(results.get(0));
    }

}
