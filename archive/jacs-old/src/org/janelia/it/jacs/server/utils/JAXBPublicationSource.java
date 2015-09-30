
package org.janelia.it.jacs.server.utils;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.download.Author;
import org.janelia.it.jacs.model.download.DataFile;
import org.janelia.it.jacs.model.download.HierarchyNode;
import org.janelia.it.jacs.model.download.Publication;
import org.janelia.it.jacs.server.jaxb.reference_record.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Oct 5, 2006
 * Time: 2:45:40 PM
 */
public class JAXBPublicationSource {

    // Setup default-configured log.
    private Logger logger = Logger.getLogger(JAXBPublicationSource.class);
    protected static final List EMPTY_LIST = new ArrayList();
    protected static final String FILE_SEPARATOR = "/";
    //  ASSUME: always Unix.  System.getProperty("file.separator");    

    /**
     * Pull out the details page information from the location of an XML file.
     *
     * @param location where is the publication?
     * @return metadata about a publication.
     * @throws Exception
     */
    public Publication readPublication(String location) throws Exception {
        //  Setup for reading from the XML, using the JAXB parser.
        String contextPackage = ReferenceRecord.class.getPackage().toString();
        int spacepos = contextPackage.indexOf(' ');
        if (spacepos > -1)
            contextPackage = contextPackage.substring(spacepos + 1);

        JAXBContext context = JAXBContext.newInstance(
                contextPackage);

        Unmarshaller unmarshaller = context.createUnmarshaller();

        ReferenceRecord record = null;
        try {
            record = (ReferenceRecord) unmarshaller.unmarshal(new FileInputStream(location));

        }
        catch (Exception ex) {
            if (ex instanceof UnmarshalException) {
                UnmarshalException se = (UnmarshalException) ex;
                se.getLinkedException().printStackTrace();
            }
            logger.error("Unmarshalling exception.", ex);
            ex.printStackTrace();
        }

        // Build object representing the publication.
        Publication returnPublication = new Publication();
        returnPublication.setAbstractOfPublication(record.getAbstract());
        returnPublication.setSummary(record.getSummary());
        returnPublication.setTitle(record.getTitle());

        ReferenceRecord.AuthorsType authors = record.getAuthors();
        List authorList = new ArrayList();
        if (authors.getName() != null) {
            for (Iterator it = authors.getName().iterator(); it.hasNext();) {
                String authorName = (String) it.next();
                if (authorName != null) {
                    // NOTE: must trim to normalize, as there is a uniqueness issue to keep.
                    authorList.add(new Author(authorName.trim()));
                }
            }
        }
        returnPublication.setAuthors(authorList);

        // Build up the hierarchy of data files.
        returnPublication.setHierarchyRootNodes(getHierarchyNodes(record));

        DataFile subjectDocument = getSubjectDocument(record);
        returnPublication.setSubjectDocument(subjectDocument.getPath());

        returnPublication.setRolledUpArchives(getRolledUpArchives(record, location));
        return returnPublication;
    }

    /**
     * Run through the hierarchy of data files, one or more 'trees' of them.
     *
     * @param record all abuot this publication.
     * @return list of data files to which this publication refers.
     * @throws Exception
     */
    private List getHierarchyNodes(ReferenceRecord record) throws Exception {
        List returnList = new ArrayList();
        ReferenceRecord.DataType data = record.getData();

        String prefix = localizePrefixToOSEnvironment(data.getPathPrefix());
        List members = data.getSubdirOrFile();

        for (int i = 0; i < members.size(); i++) {
            Object nextMember = members.get(i);
            if (nextMember instanceof File) {
                logger.error("Encountered file member at top level");
            }
            else if (nextMember instanceof Subdir) {
                Object nextToAdd = treatSubdirOrFile(nextMember, prefix);
                if (nextToAdd != null)
                    returnList.add(nextToAdd);

            }
        }

        return returnList;
    }

    /**
     * Read to come up with a collection of rolled-up gz and zip archives for
     * subsequent use.
     *
     * @param record a top-level record.
     * @return list of broken-out archives.
     * @throws Exception any thrown by called methods.
     */
    private List getRolledUpArchives(ReferenceRecord record, String location) throws Exception {
        List returnList = new ArrayList();

        ReferenceRecord.CombinedDataType combinedData = record.getCombinedData();
        if (combinedData != null) {
            FileType file = combinedData.getFile();
            if (file != null) {
                String path = file.getPath();
                String description = file.getDescription();

                DataFile nodeZIP = new DataFile();

                nodeZIP.setDescription(path);
                nodeZIP.setPath(location + ".zip");
                setDescriptiveText(nodeZIP, description);

                DataFile nodeGZ = new DataFile();
                nodeGZ.setDescription(path);
                nodeGZ.setPath(location + ".gz");
                setDescriptiveText(nodeGZ, description);

                returnList.add(nodeZIP);
                returnList.add(nodeGZ);

            }
        }

        return returnList;
    }

    /**
     * Recursively handle whatever shows up--be it sub directory or actual file--in keeping
     * with what is required in the hierarchy of files/directories.
     *
     * @param nextMember expect either a Subdir or a File.
     * @param prefix     at this level, prefix to the actual location on disk.
     * @return Object build from the member, suitable for our use.
     */
    private Object treatSubdirOrFile(Object nextMember, String prefix) {

        Object returnObject = null;
        if (nextMember instanceof File) {
            DataFile dataFile = new DataFile();
            File file = (File) nextMember;
            String path = file.getPath();
            if (path == null)
                path = "";

            String nodeLocation;
            if (!path.startsWith("/")) {
                nodeLocation = prefix + path;
            }
            else {
                nodeLocation = path;
            }

            dataFile.setPath(nodeLocation);
            setDescriptiveText(dataFile, file.getDescription());
            resolveFileSize(new Long(file.getSize()).toString(), nodeLocation, dataFile, path);

            // Viewable/displayable name for the file--includes only file name, and not full path.
//            String text;
//            int fnPos = path.lastIndexOf('/');
//            if (fnPos == -1)
//                fnPos = path.lastIndexOf('\\');
//            if (fnPos != -1)
//                text = path.substring(fnPos + 1);
//            else
//                text = path;

            dataFile.setDescription(file.getDescription());
            int isMulti = file.getTar();
            if (isMulti == 1) {
                dataFile.setMultifileArchive(true);
            }
            String infoPath = file.getInfoPath();
            if (infoPath != null)
                dataFile.setInfoLocation(infoPath);

            returnObject = dataFile;
        }
        else if (nextMember instanceof Subdir) {
            HierarchyNode node = new HierarchyNode();

            Subdir subdir = (Subdir) nextMember;
            if (subdir == null)
                return node;

            String nameOfSubdir = subdir.getName();
            node.setName(nameOfSubdir);
            node.setDescription("Directory");
            String nodeLocation = prefix + FILE_SEPARATOR; // + nameOfSubdir + FILE_SEPARATOR;

            List outputChildList = new ArrayList();
            List outputDataFileList = new ArrayList();
            List inputChildList = subdir.getFileOrSubdir();
            for (int j = 0; inputChildList != null && j < inputChildList.size(); j++) {

                // Recursion.  Call self with new list.
                Object childNode = treatSubdirOrFile(
                        inputChildList.get(j),
                        nodeLocation);

                if (childNode != null && childNode instanceof HierarchyNode)
                    outputChildList.add(childNode);
                else if (childNode != null && childNode instanceof DataFile)
                    outputDataFileList.add(childNode);

            }
            node.setChildren(outputChildList);
            node.setDataFiles(outputDataFileList);

            returnObject = node;

        }
//        else if (nextMember instanceof SubdirType) {
//            HierarchyNode node = new HierarchyNode();
//
//            SubdirType subdir = (SubdirType) nextMember;
//            if (subdir == null)
//                return node;
//
//            List subdirContent = subdir.getFileOrSubdir();
//            Set outputChildSet = new HashSet();
//            Set outputDataFileSet = new HashSet();
//            for (int i = 0; i < subdirContent.size(); i++) {
//                String nodeLocation = prefix + FILE_SEPARATOR; // + nameOfSubdir + FILE_SEPARATOR;
//                Object childNode = treatSubdirOrFile(subdirContent.get(i), prefix);
//                if (childNode != null && childNode instanceof HierarchyNode)
//                    outputChildSet.add(childNode);
//                else if (childNode != null && childNode instanceof DataFile)
//                    outputDataFileSet.add(childNode);
//            }
//            node.setChildren(outputChildSet);
//            node.setDataFiles(outputDataFileSet);
//            node.setName(subdir.getName());
//            node.setDescription("Subdirectory");
//
//            returnObject = node;
//        }
        else {
            logger.info(nextMember.getClass().getName());
        }
        return returnObject;
    }

    /**
     * Given the record found, pull out its subject node--info on downloading the PDF
     * document.
     *
     * @param record all about this publication.
     * @return specifics of getting a copy as electronic PDF.
     */
    private DataFile getSubjectDocument(ReferenceRecord record) {
        TextType textType = record.getFullText();
        String fileLocation = textType.getURL();
        return getSubjectDocumentHelper(
                fileLocation,
                Boolean.valueOf(textType.getLocal()));
    }

    /**
     * Simple helper to use the description as the ONLY attribute.
     *
     * @param dataFile    what gets a description.
     * @param description to describe the data.
     */
    protected void setDescriptiveText(DataFile dataFile, String description) {
        if (description == null)
            return;

        dataFile.setDescription(description);
    }

    /**
     * Real guts of get subj. doc.
     */
    protected DataFile getSubjectDocumentHelper(String location) {
        return getSubjectDocumentHelper(location, false);
    }

    protected DataFile getSubjectDocumentHelper(String location, boolean isLocal) {
        long size = isLocal ? getFileSize(location) : getUrlSize(location);

        DataFile subjectNode = new DataFile();
        subjectNode.setDescription("Download Paper (PDF)");
        subjectNode.setMultifileArchive(false);
        subjectNode.setPath(location);
        subjectNode.setSize(size);

        return subjectNode;
    }

    /**
     * Given a URL, get length of its content.
     *
     * @param location where to go look.
     * @return how long is it?
     */
    protected long getUrlSize(String location) {
        long returnValue = 0L;
        try {
            URL url = new URL(location);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            // NOTE: this does not get a long worth of data--just an int.
            //  An int is not sufficient for most downloads, but may be okay
            //  for just PDFs.  However, if this "getContentLength()" is
            //  triggering a GET, then that must change.
            conn.setRequestMethod("HEAD");
            returnValue = conn.getContentLength();

        }
        catch (IOException ioe) {
            logger.error("Failed to find proper size for entity at " + location, ioe);

        }

        return returnValue;
    }

    /**
     * Given a URL, get length of its content.
     *
     * @param location where to go look.
     * @return how long is it?
     */
    protected long getFileSize(String location) {
        java.io.File file = new java.io.File(location);
        return file.length();
    }

    /**
     * Ensure that the directory location prefix is appropriate to whatever OS this
     * application is running under, by replacing all instances of known file
     * separators, with the local file separator.
     *
     * @param prefix raw from XML
     * @return adjusted to OS.
     */
    protected String localizePrefixToOSEnvironment(String prefix) {
        String returnStr = prefix.replace("/", FILE_SEPARATOR).replace("\\", FILE_SEPARATOR);

        if (returnStr.endsWith(FILE_SEPARATOR)) {
            returnStr = returnStr.substring(0, returnStr.length() - FILE_SEPARATOR.length());
        }

        return returnStr;
    }

    /**
     * Figure out what the size of the file is, by whatever means available.
     *
     * @param sizeAttibuteValue where is the size from XML?
     * @param nodeLocation      where is it on disk?
     * @param dataNode          where to set the info.
     * @param path              for reporting purposes.
     */
    protected void resolveFileSize(
            String sizeAttibuteValue,
            String nodeLocation,
            DataFile dataNode,
            String path) {

        String sizeStr = sizeAttibuteValue;
        if (sizeStr == null) {
            dataNode.setSize(getFileSize(nodeLocation));
            logger.warn("Had to setSize by stating file " + path + ", because no size was provided");
        }
        else {
            try {
                Long longSize = Long.parseLong(sizeStr.trim());
                dataNode.setSize(longSize.longValue());
            }
            catch (Exception ex) {
                // Got exception.  Bail to setsize by found location.
                dataNode.setSize(getFileSize(nodeLocation));
                logger.warn("Had to setSize by stat-ing file " + path + ", because size provided was invalid: " + sizeStr);
            }
        }

    }


}
