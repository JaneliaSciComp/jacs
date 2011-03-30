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

import org.hibernate.Query;
import org.hibernate.Session;
import org.janelia.it.jacs.model.metadata.Library;
import org.janelia.it.jacs.model.metadata.Sample;
import org.janelia.it.jacs.server.access.MetadataDAO;

import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: tnabeel
 * Date: Dec 28, 2006
 * Time: 9:49:38 AM
 */
public class MetadataDAOImpl extends DaoBaseImpl implements MetadataDAO {

    public int getNumSamples() throws DaoException {
        Session hSession = null;
        try {
            String hql = "select count(*) from Sample s";
            hSession = getSession();
            Query query = hSession.createQuery(hql);
            Long sampleCount = (Long) query.uniqueResult();
            return sampleCount.intValue();
        }
        catch (Exception e) {
            throw handleException(e, this.getClass().getName() + " - getPagedSamples");
        }
        finally {
            if (hSession != null) {
                releaseSession(hSession);
            }
        }
    }

    public List<Sample> getPagedSamples(int startIndex, int numRows) throws DaoException {
        Session hSession = null;
        try {
            String hql = "select s from Sample s";
            hSession = getSession();
            Query query = hSession.createQuery(hql);
            if (startIndex > 0) {
                query.setFirstResult(startIndex);
            }
            if (numRows > 0) {
                query.setMaxResults(numRows);
            }
            return (List<Sample>) query.list();
        }
        catch (Exception e) {
            throw handleException(e, this.getClass().getName() + " - getPagedSamples");
        }
        finally {
            if (hSession != null) {
                releaseSession(hSession);
            }
        }
    }

    /**
     * This method returns a list of Sample objects for the given libraryId
     *
     * @param libraryId
     * @return list of samples
     * @throws DaoException
     */
    public List<Sample> getSamplesByLibraryId(Long libraryId) throws DaoException {
        try {
            List<Sample> samples = (List) findByNamedQueryAndNamedParam("findSamplesByLibraryId", "libraryId", libraryId, false);
            return samples;   // keep samples local variable for debugger
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, this.getClass().getName() + " - getSamples");
        }
    }

    /**
     * This method returns a list of Sample objects with their sites loaded for the given libraryId
     *
     * @param libraryId
     * @return list of samples
     * @throws DaoException
     */
    public List<Sample> getSamplesWithSitesByLibraryId(Long libraryId) throws DaoException {
        try {
            List<Sample> samples = (List) findByNamedQueryAndNamedParam("findSamplesWithSitesByLibraryId", "libraryId", libraryId, false);
            return samples;   // keep samples local variable for debugger
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, this.getClass().getName() + " - getSamples");
        }
    }

    /**
     * This method returns a list of Sample objects with their sites loaded for the given readId.
     *
     * @param entityAcc
     * @return the first encountered sample associated with the entity
     * @throws DaoException
     */
    public Sample getSampleWithSitesByEntityAcc(String entityAcc) throws DaoException {
        try {
            List<Sample> samples = (List) findByNamedQueryAndNamedParam("findSamplesWithSitesByAcc", "entityAcc", entityAcc, false);
            return samples != null && samples.size() > 0 ? samples.get(0) : null;
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, this.getClass().getName() + " - getSamples");
        }
    }

    /**
     * This method returns a Sample object with their sites loaded for the given sampleAcc.
     *
     * @param sampleAcc
     * @return the sample with the given accession
     * @throws DaoException
     */
    public Sample getSampleWithSitesBySampleAcc(String sampleAcc) throws DaoException {
        try {
            List<Sample> samples = (List) findByNamedQueryAndNamedParam("findSamplesWithSitesBySampleAcc", "sampleAcc", sampleAcc, false);
            return samples != null && samples.size() > 0 ? samples.get(0) : null;
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, this.getClass().getName() + " - getSamples");
        }
    }

    /**
     * This method returns a list of Sample objects with their sites loaded for the given sampleName.
     *
     * @param sampleName
     * @return the first encountered sample associated with the entity
     * @throws DaoException
     */
    public Sample getSampleWithSitesBySampleName(String sampleName) throws DaoException {
        try {
            List<Sample> samples = (List) findByNamedQueryAndNamedParam("findSamplesWithSitesBySampleName", "sampleName", sampleName, false);
            return samples != null && samples.size() > 0 ? samples.get(0) : null;
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, this.getClass().getName() + " - getSamples");
        }
    }

    /**
     * This method returns a Library given a Read entityId
     *
     * @param readEntityId
     * @return Library for the given Read entityId
     * @throws DaoException
     */
    public Library getLibraryByReadEntityId(Long readEntityId) throws DaoException {
        try {
            Library library = (Library) findByNamedQueryAndNamedParam("findLibraryByBseEntityId", "entityId", readEntityId, true);
            return library;  // keep library local variable for debugger
        }
        catch (Exception e) {
            // No need to be granular with exception handling since we're going to wrap 'em all in DaoException
            throw handleException(e, this.getClass().getName() + " - getLibrary");
        }
    }


}
