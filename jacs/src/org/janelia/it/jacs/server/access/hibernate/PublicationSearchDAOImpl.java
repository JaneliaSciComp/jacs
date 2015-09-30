
package org.janelia.it.jacs.server.access.hibernate;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.janelia.it.jacs.model.common.SortArgument;
import org.janelia.it.jacs.model.search.SearchHit;
import org.janelia.it.jacs.model.tasks.search.SearchTask;
import org.janelia.it.jacs.web.gwt.common.shared.data.ImageModel;

import java.text.SimpleDateFormat;
import java.util.List;

//import org.janelia.it.jacs.web.gwt.search.client.model.PublicationResult;

/**
 * User: cgoina
 * Implementation of publication specific search DAO
 */
public class PublicationSearchDAOImpl extends SearchDAOImpl {
    private static Logger _logger = Logger.getLogger(PublicationSearchDAOImpl.class);
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public PublicationSearchDAOImpl() {
    }

    public int executeSearchTask(SearchTask searchTask)
            throws DaoException {
        return 0;
        //return populateSearchResult(searchTask,SearchTask.TOPIC_PUBLICATION);
    }

    public int getNumSearchHits(String searchString, int matchFlags)
            throws DaoException {
        return countSearchHits(searchString, SearchTask.TOPIC_PUBLICATION, matchFlags);
    }

    public List<SearchHit> search(String searchString,
                                  int matchFlags,
                                  int startIndex,
                                  int numRows,
                                  SortArgument[] sortArgs)
            throws DaoException {
        return performGenericSearch(searchString, SearchTask.TOPIC_PUBLICATION, matchFlags, startIndex, numRows, sortArgs);
    }

//    public List<PublicationResult> getPagedCategoryResultsByNodeId(Long nodeId,
//                                                                   int startIndex,
//                                                                   int numRows,
//                                                                   SortArgument[] sortArgs) throws DaoException {
//        String sql = "select " +
//                "cast(p.oid as text) as publication_id, " +
//                "p.publication_acc, " +
//                "p.title, " +
//                "p.pub_date, " +
//                "p.journal_entry, " +
//                "nt.rank " +
//                "from (select hit_id, rank from publication_ts_result where node_id=" + nodeId + " order by rank desc) nt " +
//                "inner join publication p on p.oid=nt.hit_id";
//        sql = addOrderClauseToSql(sql, sortArgs);
//        _logger.info("Executing publication search sql=" + sql);
//        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
//        List<Object[]> results = sqlQuery.list();
//        _logger.info("Publication search yielded result count=" + results.size());
//        List<PublicationResult> publications = new ArrayList<PublicationResult>();
//        Map<String, PublicationResult> publicationMap = new HashMap<String, PublicationResult>();
//        StringBuffer publicationSb = new StringBuffer("");
//        for (Object[] res : results) {
//            String publicationId = ((String) res[0]);
//            if (publicationSb.toString().equals("")) {
//                publicationSb.append("\'" + publicationId + "\'");
//            }
//            else {
//                publicationSb.append(", \'" + publicationId + "\'");
//            }
//            String accession = (String) res[1];
//            PublicationResult publication = new PublicationResult();
//            publication.setAccession(accession);
//            publication.setTitle((String) res[2]);
//            publication.setPublicationDate(res[3] == null ? "" : dateFormat.format((Date) res[3]));
//            publication.setJournalEntry((String) res[4]);
//            publications.add(publication);
//            publication.setRank((Float) res[5]);
//            publicationMap.put(publicationId, publication);
//        }
//
//        // Add author info
//        String authorSql = "select distinct " +
//                "pa.publication_id, " +
//                "pa.position, " +
//                "pa.author_id " +
//                "from publication_author_link pa " +
//                "where pa.publication_id in (" + publicationSb.toString() + ") " +
//                "order by pa.publication_id desc, pa.position asc";
//        SQLQuery authorSqlQuery = getSession().createSQLQuery(authorSql);
//        List<Object[]> authorResults = authorSqlQuery.list();
//        Map<String, String> publicationAuthorMap = new HashMap<String, String>();
//        for (Object[] res : authorResults) {
//            String publicationId = ((BigInteger) res[0]).toString();
//            Integer position = (Integer) res[1];
//            String authorName = (String) res[2];
//            String authors = publicationAuthorMap.get(publicationId);
//            if (authors == null) {
//                publicationAuthorMap.put(publicationId, authorName);
//            }
//            else {
//                authors += (", " + authorName);
//                publicationAuthorMap.put(publicationId, authors);
//            }
//        }
//        for (String publicationId : publicationMap.keySet()) {
//            String authors = publicationAuthorMap.get(publicationId);
//            PublicationResult pr = publicationMap.get(publicationId);
//            pr.setAuthors(authors);
//        }
//
//        // Add project info
//        String projectSql = "select distinct " +
//                "pl.publication_id, " +
//                "pr.symbol, " +
//                "pr.name, " +
//                "pl.position " +
//                "from project_publication_link pl, " +
//                "project pr " +
//                "where pl.publication_id in (" + publicationSb.toString() + ") " +
//                "and pr.symbol = pl.project_id " +
//                "order by pl.publication_id desc, pl.position asc";
//        SQLQuery projectSqlQuery = getSession().createSQLQuery(projectSql);
//        List<Object[]> projectResults = projectSqlQuery.list();
//        Map<String, String> publicationProjectMap = new HashMap<String, String>();
//        for (Object[] res : projectResults) {
//            String publicationId = ((BigInteger) res[0]).toString();
//            String projectSymbol = (String) res[1];
//            String projectName = (String) res[2];
//            String projects = publicationProjectMap.get(publicationId);
//            if (projects == null) {
//                publicationProjectMap.put(publicationId, projectName);
//            }
//            else {
//                projects += (", " + projectName);
//                publicationProjectMap.put(publicationId, projects);
//            }
//        }
//        for (String publicationId : publicationMap.keySet()) {
//            String projects = publicationProjectMap.get(publicationId);
//            PublicationResult pr = publicationMap.get(publicationId);
//            pr.setProjects(projects);
//        }
//
//        // Get subset results
//        List<PublicationResult> finalPublicationList = new ArrayList<PublicationResult>();
//        if (startIndex >= 0 || numRows > 0) {
//            if (numRows == 0)
//                numRows = publications.size() - startIndex;
//            for (int i = startIndex; i < startIndex + numRows; i++)
//                finalPublicationList.add(publications.get(i));
//        }
//        else {
//            finalPublicationList = publications;
//        }
//        //addDocumentsToCategoryResultsByNodeId(nodeId, finalPublicationList);
//        _logger.info("Returning publication result of size=" + finalPublicationList.size());
//        return finalPublicationList;
//    }
//
    public int getNumCategoryResultsByNodeId(Long nodeId) throws DaoException {
        String sql =
                "select cast(count(1) as Integer)" +
                        "from publication_ts_result " +
                        "where node_id=" + nodeId;
        SQLQuery sqlQuery = getSession().createSQLQuery(sql);
        int count = ((Integer) sqlQuery.uniqueResult()).intValue();
        return count;
    }

    public List<ImageModel> getSearchResultCharts(Long searchId, String resultBaseDirectory)
            throws DaoException {
        return null;  // not yet implemented
    }

}
