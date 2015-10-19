
package org.janelia.it.jacs.compute.access.search;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.FeatureDAO;
import org.janelia.it.jacs.compute.access.SearchDAO;
import org.janelia.it.jacs.compute.service.export.util.AnnotationUtil;
import org.janelia.it.jacs.model.common.SortArgument;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Jul 14, 2008
 * Time: 1:46:50 PM
 */
public class ProteinSearchDAO extends SearchDAO {
    public ProteinSearchDAO(Logger logger) {
        super(logger);
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
        log.info("Executing protein search sql=" + sql);
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        if (startIndex >= 0) {
            sqlQuery.setFirstResult(startIndex);
        }
        if (numRows > 0) {
            sqlQuery.setMaxResults(numRows);
        }
        List<Object[]> results = sqlQuery.list();
        log.info("Protein search yielded result count=" + results.size());
        List<ProteinResult> proteins = new ArrayList<ProteinResult>();
        for (Object[] res : results) {
            ProteinResult proteinResult = new ProteinResult();
            proteinResult.setAccession((String) res[0]);
            proteinResult.setExternalSource((String) res[1]);
            proteinResult.setExternalAccession((String) res[2]);
            if (res[3] != null) proteinResult.setNcbiGiNumber((((Integer) res[3])).toString());
            proteinResult.setSequenceLength((Integer) res[4]);
            proteinResult.setGeneNames((String) res[5]);
            proteinResult.setGoAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) res[6],
                    FeatureDAO.ANNOT_ROW_SEP, FeatureDAO.ANNOT_LIST_SEP));
            proteinResult.setEcAnnotationDescription(AnnotationUtil.createAnnotationListFromString((String) res[7],
                    FeatureDAO.ANNOT_ROW_SEP, FeatureDAO.ANNOT_LIST_SEP));
            proteinResult.setProteinFunction((((String) res[8])));
            proteinResult.setDefline((((String) res[9])));
            proteinResult.setCoreCluster((((String) res[10])));
            proteinResult.setFinalCluster((((String) res[11])));
            proteinResult.setTaxonomy((String) res[12]);
            proteinResult.setRank((Float) res[13]);
            proteins.add(proteinResult);
        }
        log.info("Returning protein result of size=" + proteins.size());
        return proteins;
    }

    public int getNumCategoryResultsByNodeId(Long searchId) throws DaoException {
        String sql =
                "select cast(count(1) as Integer)" +
                        "from protein_ts_result " +
                        "where node_id=" + searchId;
        log.info("Executing row count protein search sql=" + sql);
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        return (Integer) sqlQuery.uniqueResult();
    }
}
