
package org.janelia.it.jacs.server.access.hibernate;

import org.hibernate.HibernateException;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;
import org.janelia.it.jacs.model.download.*;
import org.janelia.it.jacs.model.metadata.Sample;
import org.janelia.it.jacs.server.access.DownloadDAO;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Oct 24, 2006
 * Time: 2:28:22 PM
 * <p/>
 * Database Access Object to fetch data on downloads, and add downloads' metadata
 * to the database.
 */
public class DownloadDAOImpl extends DaoBaseImpl implements DownloadDAO {

    /**
     * Finder to get the project the given accession number
     *
     * @param publicationAccessionNo publication's accession number
     * @return the publication with the given accession number
     * @throws DataAccessException from methods called.
     * @throws DaoException        from methods called.
     */
    public Publication findPublicationByAccessionNo(String publicationAccessionNo) throws DataAccessException, DaoException {
        Publication result = null;
        List<Publication> searchResult = null;
        try {
            DetachedCriteria criteria = DetachedCriteria.forClass(Publication.class);
            criteria.add(Expression.eq("publicationAccession", publicationAccessionNo));
            searchResult = getHibernateTemplate().findByCriteria(criteria);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e,
                    this.getClass().getName() +
                            " - findPublicationByAccessionNo: " +
                            publicationAccessionNo);
        }
        catch (IllegalStateException e) {
            throw handleException(e,
                    this.getClass().getName() +
                            " - findPublicationByAccessionNo: " +
                            publicationAccessionNo);
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        if (searchResult != null) {
            if (searchResult.size() > 0) {
                result = searchResult.get(0);
            }
        }
        return result;
    }

    /**
     * Return list of all samples from a given project, or all (released) projects if projectSymbol is null.
     *
     * @return list found from database.
     */
    public List<Sample> findProjectSamples(String projectSymbol) throws DataAccessException, DaoException {
        StringBuffer hql = new StringBuffer()
                .append("select distinct sample ")
                .append("from BioMaterial as site ")
                .append("inner join site.samples as sample ");
        if (projectSymbol != null)
            hql.append("where site.project = '").append(projectSymbol).append("'");
        else
            hql.append("where site.project in (select project.symbol from Project as project where project.released = 'true')");
        logger.info("hql=" + hql);

        return getHibernateTemplate().find(hql.toString());
    }

    /**
     * @return List<Object[]> where Object[0] is Sample and Object[1] is project name as String
     */
    public List<Object[]> findProjectSamplesByProject() throws DataAccessException, DaoException {
        StringBuffer hql = new StringBuffer()
                .append("select distinct sample, project.name ")
                .append("from Project as project, BioMaterial as site ")
                .append("inner join site.samples as sample ")
                .append("where project.released='true' and site.project=project.symbol ")
                .append("order by project.name, sample.sampleName");
        logger.info("hql=" + hql);

        return getHibernateTemplate().find(hql.toString());
    }

    /**
     * Finder to get the project the given accession number
     *
     * @param projectSymbol project's symbol
     * @return the project with the given symbol
     * @throws DataAccessException from methods called.
     * @throws DaoException        from methods called.
     */
    public Project findReleasedProjectBySymbol(String projectSymbol) throws DataAccessException, DaoException {
        Project result = null;
        List<Project> searchResult = null;
        try {
            DetachedCriteria criteria = DetachedCriteria.forClass(Project.class);
            criteria.add(Expression.eq("symbol", projectSymbol));
            criteria.add(Expression.eq("released", true));
            searchResult = getHibernateTemplate().findByCriteria(criteria);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e,
                    this.getClass().getName() +
                            " - findReleasedProjectBySymbol: " +
                            projectSymbol);
        }
        catch (IllegalStateException e) {
            throw handleException(e,
                    this.getClass().getName() +
                            " - findReleasedProjectBySymbol: " +
                            projectSymbol);
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }
        if (searchResult != null) {
            if (searchResult.size() > 0) {
                result = searchResult.get(0);
            }
        }
        return result;
    }

    public List<DataFile> findDataFilesBySampleAcc(String sampleAcc) throws DaoException {
        String hql = "select s.dataFiles from Sample s where s.sampleAcc = :sampleAcc";
        logger.debug("hql=" + hql);
        return getHibernateTemplate().findByNamedParam(hql, "sampleAcc", sampleAcc);
    }

    /**
     * Return list of all projects.
     *
     * @return list found from database.
     * @throws DataAccessException
     * @throws DaoException
     */
    public List<Project> findAllProjects() throws DataAccessException, DaoException {
        try {
            DetachedCriteria criteria = DetachedCriteria.forClass(Project.class);
            criteria.add(Expression.eq("released", true))
                    .addOrder(Order.asc("name"));

            return getHibernateTemplate().findByCriteria(criteria);
        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, this.getClass().getName() + " - findAllProjects");
        }
        catch (IllegalStateException e) {
            throw handleException(e, this.getClass().getName() + " - findAllProjects");
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }

    }

    /**
     * Return list of all publcations.
     *
     * @return list found from database
     */
    public List<Publication> findAllPublications() throws DataAccessException, DaoException {

        try {

            DetachedCriteria criteria = DetachedCriteria.forClass(Publication.class);
            criteria.addOrder(Order.asc("title"));

            /*
            DetachedCriteria criteria = DetachedCriteria.forClass(Publication.class);
            List<Publication> publications = getHibernateTemplate().findByCriteria(criteria);
            return publications;
            */

            return getHibernateTemplate().findByCriteria(criteria);

        }
        catch (DataAccessResourceFailureException e) {
            throw handleException(e, this.getClass().getName() + " - findAllPublications");
        }
        catch (IllegalStateException e) {
            throw handleException(e, this.getClass().getName() + " - findAllPublications");
        }
        catch (HibernateException e) {
            throw convertHibernateAccessException(e);
        }

    }


    public void saveOrUpdateProject(Project project) {
        getHibernateTemplate().saveOrUpdate(project);
    }


    /**
     * Return a new author model object.
     *
     * @param name of auth
     * @return the created author
     * @throws DataAccessException from called methods
     * @throws DaoException        from called methods
     */
    public Author createAuthor(String name) throws DataAccessException, DaoException {
        Author author = new Author(name);
        saveOrUpdateObject(author, "DownloadDAOImpl - createAuthor");
        return author;
    }

    /**
     * Return a new data file model object.
     *
     * @param path             to the file
     * @param infoLocation     where is its format description
     * @param description      what is the file
     * @param size             how big is the file
     * @param multifileArchive is it tar.gz
     * @param samples          what samples referred-to
     * @return the created author
     * @throws DataAccessException from called methods
     * @throws DaoException        from called methods
     */
    public DataFile createDataFile(String path, String infoLocation, String description, long size, boolean multifileArchive, Set samples)
            throws DataAccessException, DaoException {

        DataFile dataFile = new DataFile(path, infoLocation, description, size, multifileArchive, samples);
        // Get session for connection/transaction.
        saveOrUpdateObject(dataFile, "DownloadDAOImpl - createDataFile");
        return dataFile;
    }

    /**
     * Return a new publication model object.
     *
     * @param abstractOfPublication its abstract
     * @param summary               its summary
     * @param title                 its title
     * @param subjectDocument       pointer to document itself.
     * @param authors               who wrote it
     * @param rolledUpArchives      combined data if exists
     * @param hierarchyRootNodes    hierarchies of data files to which the document pertains.
     * @return the constructed object
     * @throws DataAccessException from called methods
     * @throws DaoException        from called methods
     */
    public Publication createPublication(String abstractOfPublication, String summary, String title, String subjectDocument, List authors, List rolledUpArchives, List hierarchyRootNodes)
            throws DataAccessException, DaoException {
        Publication publication = new Publication(abstractOfPublication, summary, title, subjectDocument, authors, rolledUpArchives, hierarchyRootNodes);
        // Get session for connection/transaction.
        saveOrUpdateObject(publication, "DownloadDAOImpl - createPublication");
        return publication;
    }

    /**
     * Retun a new project model object.
     *
     * @param symbol       short name for project.
     * @param description  long description for project.
     * @param publications list of pubs under project.
     * @return the created object
     * @throws DataAccessException from called methods
     * @throws DaoException        from called methods
     */
    public Project createProject(String symbol, String description, List publications)
            throws DataAccessException, DaoException {
        Project project = new Project(symbol, description, publications);
        // Get session for connection/transaction.
        saveOrUpdateObject(project, "DownloadDAOImpl - createProject");
        return project;
    }

    /**
     * Return a new hierarchy node object that points to the datafiles given, and has the
     * subnodes given.
     *
     * @param name        name of node
     * @param description description of node.
     * @param dataFiles   files under this node
     * @param children    sub-nodes.
     * @return the created node.
     * @throws DataAccessException from called methods
     * @throws DaoException        from called methods
     */
    public HierarchyNode createHierarchyNode(String name, String description, List dataFiles, List children)
            throws DataAccessException, DaoException {
        HierarchyNode node = new HierarchyNode(name, description, dataFiles, children);
        // Get session for connection/transaction.
        saveOrUpdateObject(node, "DownloadDAOImpl - createHierarchyNode");
        return node;
    }
}
