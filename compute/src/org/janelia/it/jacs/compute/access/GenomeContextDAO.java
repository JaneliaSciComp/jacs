
package org.janelia.it.jacs.compute.access;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.janelia.it.jacs.model.metadata.GeoPoint;
import org.janelia.it.jacs.model.metadata.Sample;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: smurphy
 * Date: Nov 12, 2007
 * Time: 11:20:40 AM
 */
public class GenomeContextDAO extends ComputeBaseDAO {
    public GenomeContextDAO(Logger logger) {
        super(logger);
    }

    public GenomeContextDAO(Session externalSession) {
        super(externalSession);
    }

    public List<Sample> getSamplesByProject(String projectId) throws DaoException {
        Session session = getCurrentSession();
        Query query = session.createSQLQuery(
                "select bs.sample_id from " +
                        "  project_publication_link ppl, " +
                        "  publication_hierarchy_node_link phn, " +
                        "  hierarchy_node hn, " +
                        "  hierarchy_node_data_file_link hndf, " +
                        "  data_file df, " +
                        "  data_file_sample_link dfs, " +
                        "  bio_sample bs " +
                        "where ppl.project_id=\'" + projectId + "\' " +
                        "  and phn.publication_id=ppl.publication_id " +
                        "  and hn.oid=phn.hierarchy_node_id " +
                        "  and hndf.hierarchy_node_id=hn.oid " +
                        "  and df.oid=hndf.data_file_id " +
                        "  and dfs.data_file_id=df.oid " +
                        "  and bs.sample_id=dfs.sample_id");
        List results = query.list();
        List<Sample> sampleList = new ArrayList<Sample>();
        for (Object result : results) {
            Long sampleId = ((BigInteger) result).longValue();
            Sample sample = (Sample) this.genericLoad(Sample.class, sampleId);
            // Marshall into new sample for client independent of Hibernate
//            Sample newSample=new Sample();
//            newSample.setSampleId(sample.getSampleId());
//            newSample.setSampleName(sample.getSampleName());
//            newSample.set
            sampleList.add(sample);
        }
        return sampleList;
    }

    public List<String> getAssemblyAccessionsByProjectName(String projectName) throws DaoException {
        Session session = getCurrentSession();
        Query query = session.createSQLQuery(
                "select asm.assembly_acc from " +
                        "  assembly asm " +
                        "where asm.project=\'" + projectName + "\' ");
        List results = query.list();
        List<String> accessionList = new ArrayList<String>();
        for (Object result : results) {
            String accession = (String) result;
            accessionList.add(accession);
        }
        return accessionList;
    }

    public GeoPoint getGeoPointByCollectionSiteId(Long collectionSiteId) {
        Session session = getCurrentSession();
        Query query = session.createSQLQuery(
                "select gp.location_id, " +
                        "       gp.longitude, " +
                        "       gp.latitude, " +
                        "       gp.altitude, " +
                        "       gp.depth, " +
                        "       gp.country " +
                        "from geo_point gp where gp.location_id=" + collectionSiteId);
        List results = query.list();
        if (results == null || results.size() == 0)
            return null;
        GeoPoint gp = new GeoPoint();
        Object[] resultArr = (Object[]) results.get(0);
        gp.setLongitude((String) resultArr[1]);
        gp.setLatitude((String) resultArr[2]);
        gp.setAltitude((String) resultArr[3]);
        gp.setDepth((String) resultArr[4]);
        gp.setCountry((String) resultArr[5]);
        return gp;
    }

}
