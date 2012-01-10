
package org.janelia.it.jacs.server.access.hibernate;

import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.SQLQuery;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.search.SearchHit;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.server.utils.AnnotationUtil;
import org.janelia.it.jacs.web.gwt.common.shared.data.ChartData;
import org.janelia.it.jacs.web.gwt.common.shared.data.ChartDataEntry;
import org.janelia.it.jacs.web.gwt.common.shared.data.ImageModel;
import org.janelia.it.jacs.web.gwt.search.client.model.ProteinResult;

import java.util.ArrayList;
import java.util.List;

/**
 * User: cgoina
 * Implementation of protein specific search DAO
 */
public class ProteinSearchDAOImpl extends SearchDAOImpl {
    private static final int N_TOP_CLUSTERS = 5;
    private static Logger _logger = Logger.getLogger(ProteinSearchDAOImpl.class);

    public ProteinSearchDAOImpl() {
    }

    public int executeSearchTask(SearchTask searchTask)
            throws DaoException {
        return 0;
        //return populateSearchResult(searchTask,SearchTask.TOPIC_PROTEIN);
    }

    public int getNumSearchHits(String searchString, int matchFlags)
            throws DaoException {
        return countSearchHits(searchString, SearchTask.TOPIC_PROTEIN, matchFlags);
    }

    public List<SearchHit> search(String searchString,
                                  int matchFlags,
                                  int startIndex,
                                  int numRows,
                                  SortArgument[] sortArgs)
            throws DaoException {
        return performGenericSearch(searchString, SearchTask.TOPIC_PROTEIN, matchFlags, startIndex, numRows, sortArgs);
    }

//    protected String getSearchQuery() {
//        return "protein_ftsearch(:searchResultNode,:searchString,:matchFlags)";
//    }

    protected String getSearchResultInsertFields() {
        return "docid," +
                "accession," +
                "docname," +
                "category," +
                "doctype," +
                "method," +
                "headline," +
                "rank";
    }

    protected String getSearchQueryFields() {
        return "docid," +
                "accession," +
                "docname," +
                "category," +
                "doctype," +
                "method," +
                "headline," +
                "rank ";
    }

    protected void setSearchQueryParameters(SQLQuery sqlQuery,
                                            String searchString,
                                            String searchCategory,
                                            String matchFlags) {
        sqlQuery.setString("searchString", searchString);
        sqlQuery.setString("matchFlags", matchFlags);
    }

    public List<ProteinResult> getPagedCategoryResultsByNodeId(Long searchId,
                                                               int startIndex,
                                                               int numRows,
                                                               SortArgument[] sortArgs)
            throws DaoException {
        String sql =
                "select " +
                        "pd.protein_acc as proteinAcc," +
                        "pd.external_source,pd.external_acc,pd.ncbi_gi_number," +
                        "pd.length,pd.gene_symbol,pd.gene_ontology,pd.enzyme_commission,pd.protein_function," +
                        "pd.defline," +
                        "pd.core_cluster_acc,pd.final_cluster_acc,pd.taxon_names, " +
                        "nt.rank " +
                        "from (select hit_id, rank from protein_ts_result where node_id=" + searchId + " order by rank desc) nt " +
                        "inner join protein_detail pd on pd.protein_id=nt.hit_id";

        sql = addOrderClauseToSql(sql, sortArgs);
        _logger.info("Executing protein search sql=" + sql);
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        if (startIndex >= 0) {
            sqlQuery.setFirstResult(startIndex);
        }
        if (numRows > 0) {
            sqlQuery.setMaxResults(numRows);
        }
        List<Object[]> results = sqlQuery.list();
        _logger.info("Protein search yielded result count=" + results.size());
        List<ProteinResult> proteins = new ArrayList<ProteinResult>();
        for (Object[] res : results) {
            ProteinResult proteinResult = new ProteinResult();
            proteinResult.setAccession((String) res[0]);
            proteinResult.setExternalSource((String) res[1]);
            proteinResult.setExternalAccession((String) res[2]);
            if (res[3] != null) proteinResult.setNcbiGiNumber((((Integer) res[3])).toString());
            proteinResult.setSequenceLength((Integer) res[4]);
            proteinResult.setGeneNames((String) res[5]);
            proteinResult.setGoAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) res[6], FeatureDAOImpl.ANNOT_ROW_SEP, FeatureDAOImpl.ANNOT_LIST_SEP));
            proteinResult.setEcAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) res[7], FeatureDAOImpl.ANNOT_ROW_SEP, FeatureDAOImpl.ANNOT_LIST_SEP));
            proteinResult.setProteinFunction((((String) res[8])));
            proteinResult.setDefline((((String) res[9])));
            proteinResult.setCoreCluster((((String) res[10])));
            proteinResult.setFinalCluster((((String) res[11])));
            proteinResult.setTaxonomy((String) res[12]);
            proteinResult.setRank((Float) res[13]);
            proteins.add(proteinResult);
        }
        _logger.info("Returning protein result of size=" + proteins.size());
        return proteins;
    }

    public int getNumCategoryResultsByNodeId(Long searchId) throws DaoException {
        String sql =
                "select cast(count(1) as Integer)" +
                        "from protein_ts_result " +
                        "where node_id=" + searchId;
        _logger.info("Executing row count protein search sql=" + sql);
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        int count = ((Integer) sqlQuery.uniqueResult()).intValue();
        return count;
    }

    public List<ImageModel> getSearchResultCharts(Long searchId, String resultBaseDirectory)
            throws DaoException {
        List<ImageModel> proteinSearchResultCharts = new ArrayList<ImageModel>();
        proteinSearchResultCharts.add(getGOTypesChart(searchId, resultBaseDirectory));
        proteinSearchResultCharts.add(getECSummaryChart(searchId, resultBaseDirectory));
/*
        the cluster distribution is no longer needed
        proteinSearchResultCharts.add(getClusterDistributionChart(searchId, resultBaseDirectory));
*/
        return proteinSearchResultCharts;
    }

    /**
     * Creates the chart data for protein search hits chart by GO type
     * (molecular function, biological process, cellular component)
     *
     * @param searchId
     * @return
     * @throws DaoException
     */
    private ChartData getGOAssignmentsChartData(Long searchId)
            throws DaoException {
        String chartTitle = "Gene Ontology Summary";
        ChartData dataset = new ChartData(chartTitle);
        retrieveGOAssignments(searchId, dataset);
/*
        Long resultsWithoutGO = retrieveUnclassifiedResultsByDoctype(searchId,"go");
        if(resultsWithoutGO > 0) {
            dataset.addChartDataEntry(new ChartDataEntry("No GO Annotation",resultsWithoutGO));
        }
*/
        return dataset;
    }

    /**
     * Creates a protein search hits chart by GO type
     * (molecular function, biological process, cellular component)
     *
     * @param searchId
     * @param resultBaseDirectory
     * @return
     * @throws DaoException
     */
    private ImageModel getGOTypesChart(final Long searchId, String resultBaseDirectory)
            throws DaoException {
        try {
            ChartData dataset = getChartData("goAssignmentChartData.jo", resultBaseDirectory,
                    new ChartDataCallback() {
                        public ChartData getChartDataMethod() throws DaoException {
                            return getGOAssignmentsChartData(searchId);
                        }
                    }
            );
            /*
            * Generate the pie chart.
            */
            return chartTool.createPieChart(dataset.getName(),
                    dataset,
                    PIE_CHART_WIDTH,  // width
                    PIE_CHART_HEIGHT, // height
                    resultBaseDirectory, getResultChartDirectory(searchId));
        }
        catch (Exception e) {
            _logger.error("Error creating the GO summary pie chart", e);
            throw new DaoException(e, "ProteinSearchDAOImpl.getGOTypesChart.createPieChart");
        }
    }

    private ChartData getECSummaryChartData(Long searchId)
            throws DaoException {
        String chartTitle = "Enzyme Summary";
        ChartData dataset = new ChartData(chartTitle);
        retrieveECPartitions(searchId, dataset);
/*
        Long resultsWithoutEC = retrieveUnclassifiedResultsByDoctype(searchId,"ec");
        if(resultsWithoutEC > 0) {
            dataset.addChartDataEntry(new ChartDataEntry("No Enzyme Annotation",resultsWithoutEC));
        }
*/
        return dataset;
    }

    private ImageModel getECSummaryChart(final Long searchId, String resultBaseDirectory)
            throws DaoException {
        try {
            ChartData dataset = getChartData("ecSummaryChartData.jo", resultBaseDirectory,
                    new ChartDataCallback() {
                        public ChartData getChartDataMethod() throws DaoException {
                            return getECSummaryChartData(searchId);
                        }
                    }
            );

            /*
            * Generate the pie chart.
            */
            return chartTool.createPieChart(dataset.getName(),
                    dataset,
                    PIE_CHART_WIDTH,  // width
                    PIE_CHART_HEIGHT, // height
                    resultBaseDirectory, getResultChartDirectory(searchId));
        }
        catch (Exception e) {
            _logger.error("Error creating the EC summary pie chart", e);
            throw new DaoException(e, "ProteinSearchDAOImpl.getECSummaryChart.createPieChart");
        }
    }

/*
    private ChartData getClusterDistributionChartData(Long searchId)
            throws DaoException {
        String chartTitle = "Protein Cluster Membership";
        ChartData dataset = new ChartData(chartTitle);
        retrieveTotalNumberOfClusters(searchId,dataset);
        retrieveNumberOfHitsPerCluster(searchId,dataset);
        return dataset;
    }
*/
/*
    private ImageModel getClusterDistributionChart(Long searchId, String resultBaseDirectory)
            throws DaoException {
        ChartData dataset = getClusterDistributionChartData(searchId);
        */
/*
        * Generate the bar chart.
        */
/*
        try {
            return chartTool.createBarChart(dataset.getName(),
                    null,
                    null,
                    dataset,
                    BAR_CHART_WIDTH,  // width
                    BAR_CHART_HEIGHT, // height
                    resultBaseDirectory,
                    getResultChartDirectory(searchId));
        } catch (Exception e) {
            _logger.error("Error creating protein cluster membership chart",e);
            throw new DaoException(e,"ProteinSearchDAOImpl.getClusterDistributionChart.createBarChart");
        }
    }
*/

    private void retrieveGOAssignments(Long searchId, ChartData dataset)
            throws DaoException {
        String sql =
                "select coalesce(go.name,'not assigned') as goName," +
                        " x.num as nGOAssignments" +
                        " from" +
                        " (select g.go_id, count(*) as num" +
                        " from protein_ts_result ts" +
                        " left outer join protein_go_id g on g.protein_id=ts.hit_id" +
                        " where ts.node_id=(select node_id from node where task_id=:searchId)" +
                        " group by g.go_id) x" +
                        " left outer join go on go.go_id=x.go_id";
        _logger.info("GO summary sql=" + sql);
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        sqlQuery.setLong("searchId", searchId);
        sqlQuery.addScalar("goName", Hibernate.STRING);
        sqlQuery.addScalar("nGOAssignments", Hibernate.LONG);
        List<Object[]> results = sqlQuery.list();
        for (Object[] result : results) {
            String goDescription = (String) result[0];
            Long n = (Long) result[1];
            ChartDataEntry chartEntry = new ChartDataEntry(goDescription, n);
            chartEntry.setDescription(goDescription);
            dataset.addChartDataEntry(chartEntry);
        }
    }

    private void retrieveECPartitions(Long searchId, ChartData dataset)
            throws DaoException {
        String sql =
                "select coalesce(pa.name,'not assigned') as ec_category, count(1) as nECCategories " +
                        "from protein_ts_result ts " +
                        "left outer join protein_ec pa on pa.protein_id=ts.hit_id " +
                        "where ts.node_id=(select node_id from node where task_id = :searchId) " +
                        "group by ec_category";
        _logger.info("EC category sql=" + sql);
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        sqlQuery.setLong("searchId", searchId);
        sqlQuery.addScalar("ec_category", Hibernate.STRING);
        sqlQuery.addScalar("nECCategories", Hibernate.LONG);
        List<Object[]> results = sqlQuery.list();
        for (Object[] result : results) {
            String ecDescription = (String) result[0];
            Long n = (Long) result[1];
            ChartDataEntry chartEntry = new ChartDataEntry(ecDescription, n);
            chartEntry.setDescription(ecDescription);
            dataset.addChartDataEntry(chartEntry);
        }
    }
}
