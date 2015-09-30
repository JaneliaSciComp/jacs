
package org.janelia.it.jacs.server.utils;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.janelia.it.jacs.model.download.DataFile;
import org.janelia.it.jacs.model.metadata.Sample;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Temporary use class, for making changes to the contents of the download database area.
 * <p/>
 * User: Lfoster
 * Date: Nov 2, 2006
 * Time: 2:57:31 PM
 */
public class DownloadDbModifier {
    // Setup default-configured log.
    private static Logger logger = Logger.getLogger(DownloadDbModifier.class);
    private HibernateSessionSource _sessionSource = new HibernateSessionSource();

    public static void main(String[] args) {
        try {
            new DownloadDbModifier().execute();
        }
        catch (Exception ex) {
            logger.error(ex);
            ex.printStackTrace();
        }
    }

    /**
     * Guts of this class.  This is where the executation takes place.
     *
     * @throws Exception
     */
    public void execute() throws Exception {
        linkDatFilesVsSamples();
    }

    /**
     * A set of paths were mistakenly setup with short versions of paths.  This will clean them up, so that they
     * contain corrected path locations.
     *
     * @throws Exception
     */
    private void modifyDataFilePaths() throws Exception {
        Session session = _sessionSource.getOrCreateSession();
        Transaction transaction = session.beginTransaction();
        Query query = session.createQuery("select d from org.janelia.it.jacs.model.download.DataFile as d where d.path like :var");
        query.setParameter("var", "%gos%JCVI_SITE_%.fasta");
        List dataFiles = query.list();
        for (Iterator it = dataFiles.iterator(); it.hasNext();) {
            DataFile dataFile = (DataFile) it.next();
            String path = dataFile.getPath();
            String prefix = "/data/gos/reads/individual/";
            int pos = path.indexOf(prefix);
            if (pos == 0) {
                String newPath = path.substring(5);
                dataFile.setPath(newPath);

                // Data file is in session, and now we can write it back--as a known OID.
                session.update(dataFile);
                logger.info(newPath);
            }
        }
        session.flush();
        transaction.commit();
        _sessionSource.closeSession();
    }

    /**
     * Convert all paths, to remove any dos-like file separators in favor of unix file separators.
     *
     * @throws Exception
     */
    private void unixifyDataFilePaths() throws Exception {
        Session session = _sessionSource.getOrCreateSession();
        Transaction transaction = session.beginTransaction();
        Query query = session.createQuery("select d from org.janelia.it.jacs.model.download.DataFile as d");

        List dataFiles = query.list();
        for (Iterator it = dataFiles.iterator(); it.hasNext();) {
            DataFile dataFile = (DataFile) it.next();
            String path = dataFile.getPath();

            String newPath = path.replace('\\', '/');   // Convert backs to forwards.
            newPath = newPath.replaceAll("//", "/");  // If any doubles exist, trim them.
            if (!path.equals(newPath)) {
                dataFile.setPath(newPath);
                session.update(dataFile);
            }
        }
        transaction.commit();
        _sessionSource.closeSession();
    }

    /**
     * Remove double / and replace with single / in data file paths.
     *
     * @throws Exception
     */
    private void uniquifyDataFilePaths() throws Exception {
        Session session = _sessionSource.getOrCreateSession();
        Transaction transaction = session.beginTransaction();
        Query query = session.createQuery("select d from org.janelia.it.jacs.model.download.DataFile as d");

        List dataFiles = query.list();
        for (Iterator it = dataFiles.iterator(); it.hasNext();) {
            DataFile dataFile = (DataFile) it.next();
            String path = dataFile.getPath();

            String newPath = path.replaceAll("//", "/");  // If any doubles exist, trim them.
            if (!path.equals(newPath)) {
                dataFile.setPath(newPath);
                session.update(dataFile);
            }
        }
        transaction.commit();
        _sessionSource.closeSession();
    }

    /**
     * Goes through a driver file to find all the relationships between a data file and a sample, and then
     * creates a link between them, by updating the data file defintion in the database.
     *
     * @throws Exception from any called methods.
     */
    private void linkDataFilesAndSamples() throws Exception {
        Session session = _sessionSource.getOrCreateSession();
        Transaction transaction = session.beginTransaction();

        BufferedReader reader = new BufferedReader(new FileReader("c:/test_and_learn/download/SampleIDs_to_FileMapping.txt"));
        try {
            String inbuf = null;
            while (null != (inbuf = reader.readLine())) {
                String[] sampleIdVsPath = inbuf.split("\t");
                // First get the ID for the sample.
                Query query = session.createQuery("select s from org.janelia.it.jacs.model.metadata.Sample as s where s.sampleName=:var");
                query.setParameter("var", sampleIdVsPath[0]);
                List list = query.list();
                if (list.size() > 1) {
                    logger.error("Invalid assumption.");
                    throw new IllegalStateException("Invalid Assumption: only one matching sample");
                }
                Sample sample = (Sample) list.get(0);

                // Next get the ID for the data file.
                query = session.createQuery("select d from org.janelia.it.jacs.model.download.DataFile as d where d.path=:var");
                query.setParameter("var", "/" + sampleIdVsPath[1]);
                DataFile dataFile = (DataFile) query.uniqueResult();

                // Finally, create the link.
                if (dataFile != null) {
                    dataFile.getSamples().add(sample);
                    session.update(dataFile);
                }
            }
        }
        finally {
            reader.close();
        }

        transaction.commit();
        _sessionSource.closeSession();

    }

    private void linkDatFilesVsSamples() throws Exception {
        Map sampleAccVsFile = getTabbedRelation("c:/test_and_learn/download/sample_id_mapping/SampleIDs_to_FileMapping.txt");
        Map dataFilePathVsOid = getInvertedTabbedRelation("c:/test_and_learn/download/sample_id_mapping/datafileoid_vs_path.txt");
        Map sampleAccVsId = getTabbedRelation("c:/test_and_learn/download/sample_id_mapping/sampleacc_to_id.txt");

        // Now construct a mapping of data file ID vs sample ID.
        for (Iterator it = sampleAccVsFile.keySet().iterator(); it.hasNext();) {
            String sampleAcc = (String) it.next();
            String file = (String) sampleAccVsFile.get(sampleAcc);
            String sampleId = (String) sampleAccVsId.get(sampleAcc);
            String fullFile = null;
            if (file.startsWith("/")) {
                fullFile = file;
            }
            else {
                fullFile = "/" + file;
            }
            String dataFileOid = (String) dataFilePathVsOid.get(fullFile);

            // Now have sampleId and dataFileOid, corresponding to original relation between
            // the sample accession and the data file to which it is to link.
            logger.info("insert into data_file_sample_link values (" + sampleId + ", " + dataFileOid + ");");
        }
    }

    /**
     * Given a file with tab-delimited two-part lines, make each line a key-vs-value, entry in a map.
     *
     * @param fileName what to read
     * @return map that resulted
     * @throws Exception from any called methods
     */
    private Map getTabbedRelation(String fileName) throws Exception {
        return getTabbedRelation(fileName, true);
    }

    private Map getTabbedRelation(String fileName, boolean direct) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        HashMap returnMap = new HashMap();
        try {
            String inbuf = null;
            int keyCol = direct ? 0 : 1;
            int valCol = direct ? 1 : 0;
            while (null != (inbuf = reader.readLine())) {
                String[] keyVsValue = inbuf.split("\t");
                if (keyVsValue.length != 2)
                    keyVsValue = inbuf.split(" ");
                if (keyVsValue.length != 2)
                    throw new IllegalArgumentException("Illegal tab-split line: " + inbuf);
                returnMap.put(keyVsValue[keyCol], keyVsValue[valCol]);
            }
        }
        finally {
            reader.close();
        }
        return returnMap;
    }

    private Map getInvertedTabbedRelation(String fileName) throws Exception {
        return getTabbedRelation(fileName, false);
    }
}
