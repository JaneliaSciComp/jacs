
package org.janelia.it.jacs.server.access;

import org.janelia.it.jacs.model.genomics.Sample;
import org.janelia.it.jacs.model.genomics.Library;
import org.janelia.it.jacs.server.access.hibernate.DaoException;

import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Dec 28, 2006
 * Time: 9:47:55 AM
 */
public interface MetadataDAO extends DAO {

    public int getNumSamples() throws DaoException;

    /**
     * This method returns a list of Sample objects
     *
     * @param startIndex
     * @param numRows
     * @return list of samples
     * @throws DaoException
     */
    public List<Sample> getPagedSamples(int startIndex, int numRows) throws DaoException;

    /**
     * This method returns a list of Sample objects for the given libraryId
     *
     * @param libraryId
     * @return list of samples
     * @throws DaoException
     */
    public List<Sample> getSamplesByLibraryId(Long libraryId) throws DaoException;

    /**
     * This method returns a list of Sample objects with their sites loaded for the given libraryId
     *
     * @param libraryId
     * @return list of samples
     * @throws DaoException
     */
    public List<Sample> getSamplesWithSitesByLibraryId(Long libraryId) throws DaoException;

    /**
     * This method returns a list of Sample objects with their sites loaded for the given readId
     *
     * @param entityAcc
     * @return the first encountered sample associated with the entity
     * @throws DaoException
     */
    public Sample getSampleWithSitesByEntityAcc(String entityAcc) throws DaoException;

    /**
     * This method returns a Sample object with their sites loaded for the given sampleAcc.
     *
     * @param sampleAcc
     * @return the sample with the given accession
     * @throws DaoException
     */
    public Sample getSampleWithSitesBySampleAcc(String sampleAcc) throws DaoException;

    /**
     * This method returns a Sample object with their sites loaded for the given sample name
     *
     * @param sampleName
     * @return the first encountered sample associated with the entity
     * @throws DaoException
     */
    public Sample getSampleWithSitesBySampleName(String sampleName) throws DaoException;

    /**
     * This method returns a Library given a Read entityId
     *
     * @param readEntityId
     * @return Library for the given Read entityId
     * @throws DaoException
     */
    public Library getLibraryByReadEntityId(Long readEntityId) throws DaoException;
}
