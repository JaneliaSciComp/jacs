
package org.janelia.it.jacs.web.gwt.download.server;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.server.jaxb.reference_record.*;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNodeImpl;
import org.janelia.it.jacs.web.gwt.download.client.model.Publication;
import org.janelia.it.jacs.web.gwt.download.client.model.PublicationImpl;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Lfoster
 * Date: Oct 5, 2006
 * Time: 2:45:40 PM
 */
public class JAXBPublicationSource extends AbstractPublicationSource {

    // Setup default-configured log.
    private Logger log = Logger.getLogger(JAXBPublicationSource.class);

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
            ex.printStackTrace();
        }

        // Build object representing the publication.
        PublicationImpl returnPublication = new PublicationImpl();
        returnPublication.setAbstract(record.getAbstract());
        returnPublication.setSummary(record.getSummary());
        returnPublication.setTitle(record.getTitle());

        // NOTE: authors.getName() returns SOME list implementation--not compatible
        //   with GWT serialization.
        ReferenceRecord.AuthorsType authors = record.getAuthors();
        List authorList = new ArrayList();
        if (authors.getName() != null)
            authorList.addAll(authors.getName());
        returnPublication.setAuthors(authorList);

        // Build up the hierarchy of data files.
        returnPublication.setDataFiles(getDataFiles(record));

        returnPublication.setSubjectDocument(getSubjectDocument(record));

        returnPublication.setRolledUpDataArchives(getRolledUpArchives(record, location));
        return returnPublication;
    }

    /**
     * Run through the hierarchy of data files, one or more 'trees' of them.
     *
     * @param record all abuot this publication.
     * @return list of data files to which this publication refers.
     * @throws Exception
     */
    private List getDataFiles(ReferenceRecord record) throws Exception {
        List returnList = new ArrayList();
        ReferenceRecord.DataType data = record.getData();

        String prefix = localizePrefixToOSEnvironment(data.getPathPrefix());
        List members = data.getSubdirOrFile();

        for (int i = 0; i < members.size(); i++) {
            Object nextMember = members.get(i);
            if (nextMember instanceof File) {
                log.error("Encountered file member at top level");
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
     * Read JDOM elements to come up with a collection of rolled-up gz and zip archives for
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

                DownloadableDataNodeImpl nodeZIP = new DownloadableDataNodeImpl();
                nodeZIP.setChildren(EMPTY_LIST);

                nodeZIP.setText(path);
                nodeZIP.setLocation(location + ".zip");
                setDescriptiveText(nodeZIP, description);

                DownloadableDataNodeImpl nodeGZ = new DownloadableDataNodeImpl();
                nodeGZ.setChildren(EMPTY_LIST);
                nodeGZ.setText(path);
                nodeGZ.setLocation(location + ".gz");
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

        DownloadableDataNodeImpl dataNode = null;
        if (nextMember instanceof File) {
            dataNode = new DownloadableDataNodeImpl();
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

            dataNode.setLocation(nodeLocation);
            setDescriptiveText(dataNode, file.getDescription());
            resolveFileSize(new Long(file.getSize()).toString(), nodeLocation, dataNode, path);

            // Viewable/displayable name for the file--includes only file name, and not full path.
            String text;
            int fnPos = path.lastIndexOf(FILE_SEPARATOR);
            if (fnPos != -1)
                text = path.substring(fnPos + 1);
            else
                text = path;

            dataNode.setText(text);
            int isMulti = file.getTar();
            if (isMulti == 1) {
                dataNode.setMultifileArchive(true);
            }
            String infoPath = file.getInfoPath();
            if (infoPath != null)
                dataNode.setInfoLocation(infoPath);
            dataNode.setChildren(EMPTY_LIST);

        }
        else if (nextMember instanceof Subdir) {
            dataNode = new DownloadableDataNodeImpl();

            Subdir subdir = (Subdir) nextMember;
            if (subdir == null)
                return dataNode;

            String nameOfSubdir = subdir.getName();
            dataNode.setText(nameOfSubdir);
            String nodeLocation = prefix + FILE_SEPARATOR; // + nameOfSubdir + FILE_SEPARATOR;
            dataNode.setLocation(nodeLocation);
            setDescriptiveText(dataNode, "Directory");

            List outputChildList = new ArrayList();
            List inputChildList = subdir.getFileOrSubdir();
            for (int j = 0; inputChildList != null && j < inputChildList.size(); j++) {

                // Recursion.  Call self with new list.
                Object childNode = treatSubdirOrFile(
                        inputChildList.get(j),
                        nodeLocation);

                if (childNode != null)
                    outputChildList.add(childNode);

            }
            dataNode.setChildren(outputChildList);

        }
        return dataNode;
    }

    /**
     * Given the record found, pull out its subject node--info on downloading the PDF
     * document.
     *
     * @param record all about this publication.
     * @return specifics of getting a copy as electronic PDF.
     */
    private DownloadableDataNode getSubjectDocument(ReferenceRecord record) {
        TextType textType = record.getFullText();
        String fileLocation = textType.getURL();
        return getSubjectDocumentHelper(
                fileLocation,
                Boolean.valueOf(textType.getLocal()));
    }

}
