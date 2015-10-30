
package org.janelia.it.jacs.web.gwt.download.server;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNodeImpl;
import org.janelia.it.jacs.web.gwt.download.client.model.Publication;
import org.janelia.it.jacs.web.gwt.download.client.model.PublicationImpl;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Lfoster
 * Date: Oct 5, 2006
 * Time: 2:49:25 PM
 * <p/>
 * Reads a publication using JDOM.
 */
public class JDOMPublicationSource extends AbstractPublicationSource {

    // Setup default-configured log.
    private Logger log = Logger.getLogger(JAXBPublicationSource.class);

    /**
     * Read a publication XML document using JDOM.
     *
     * @param location where is the file.
     * @return the internal-model version.
     * @throws Exception thrown by any called methods.
     */
    public Publication readPublication(String location) throws Exception {
        PublicationImpl returnPublication = new PublicationImpl();

        // Build the document with SAX and Xerces, no validation
        SAXBuilder builder = new SAXBuilder();
        // Create the document
        Document doc = builder.build(new File(location));
        List belowRoot = doc.getRootElement().getChildren();
        for (int i = 0; i < belowRoot.size(); i++) {
            Object nextChild = belowRoot.get(i);
            if (nextChild instanceof Element) {
                Element nextElement = (Element) nextChild;
                String name = nextElement.getName().intern();
                if (name.equals("title")) {
                    returnPublication.setTitle(nextElement.getTextTrim());  // Leaves untrimmed internal.
                }
                else if (name.equals("authors")) {
                    List authorNameElements = nextElement.getChildren();
                    List authorNames = new ArrayList();
                    for (int j = 0; j < authorNameElements.size(); j++) {
                        if (authorNameElements.get(j) instanceof Element) {
                            Element nextName = (Element) authorNameElements.get(j);
                            if (nextName.getName().intern().equals("name")) {
                                authorNames.add(nextName.getTextNormalize());
                            }
                        }
                    }
                    returnPublication.setAuthors(authorNames);
                }
                else if (name.equals("publication")) {
                    // Unused as of now.
                }
                else if (name.equals("summary")) {
                    returnPublication.setSummary(nextElement.getTextTrim());
                }
                else if (name.equals("abstract")) {
                    returnPublication.setAbstract(nextElement.getTextTrim());
                }
                else if (name.equals("full_text")) {
                    String fullTextLocation = nextElement.getAttributeValue("URL");
                    DownloadableDataNode subjectNode = getSubjectDocumentHelper(
                            fullTextLocation,
                            Boolean.valueOf(nextElement.getAttributeValue("local")));
                    returnPublication.setSubjectDocument(subjectNode);
                }
                else if (name.equals("supplemental_text")) {
                    // Unused as of now.
                }
                else if (name.equals("data")) {
                    returnPublication.setDataFiles(getDataFiles(nextElement));
                }
                else if (name.equals("combined_data")) {
                    returnPublication.setRolledUpDataArchives(getRolledUpArchives(nextElement, location));
                }
            }
        }

        return returnPublication;
    }

    /**
     * Read JDOM elements to come up with a collection of rolled-up gz and zip archives for
     * subsequent use.
     *
     * @param element a combined data element.
     * @return list of broken-out archives.
     * @throws Exception any thrown by called methods.
     */
    private List getRolledUpArchives(Element element, String location) throws Exception {
        List returnList = new ArrayList();

        Element fileElement = element.getChild("file");
        if (fileElement != null) {
            String path = fileElement.getAttributeValue("path");
            String description = fileElement.getAttributeValue("description");

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

        return returnList;
    }

    /**
     * Run through the hierarchy of data files, one or more 'trees' of them.
     *
     * @param dataElement all about this publication, in JDOM model.
     * @return list of data files to which this publication refers.
     * @throws Exception
     */
    private List getDataFiles(Element dataElement) throws Exception {
        List returnList = new ArrayList();
        String pathPrefix = localizePrefixToOSEnvironment(dataElement.getAttributeValue("path_prefix"));

        List dataElements = dataElement.getChildren();
        for (int i = 0; i < dataElements.size(); i++) {
            Object next = dataElements.get(i);
            if (next instanceof Element) {
                Element nextElement = (Element) next;
                if (nextElement.getName().intern().equals("file")) {
                    log.error("Encountered file member at top level");
                }
                else if (nextElement.getName().intern().equals("subdir")) {
                    returnList.add(treatSubdirOrFile(nextElement, pathPrefix));
                }
            }
        }

        return returnList;
    }

    /**
     * Recursively handle whatever shows up--be it sub directory or actual file--in keeping
     * with what is required in the hierarchy of files/directories.
     *
     * @param nextElement expect either a Subdir or a File.
     * @param prefix      at this level, prefix to the actual location on disk.
     * @return Object build from the member, suitable for our use.
     */
    private Object treatSubdirOrFile(Element nextElement, String prefix) {

        DownloadableDataNodeImpl dataNode = new DownloadableDataNodeImpl();
        if (nextElement.getName().intern().equals("file")) {
            String path = nextElement.getAttributeValue("path");
            String nodeLocation;
            if (!path.startsWith("/")) {
                nodeLocation = prefix + path;
            }
            else {
                nodeLocation = path;
            }

            dataNode.setLocation(nodeLocation);
            setDescriptiveText(dataNode, nextElement.getAttributeValue("description"));
            resolveFileSize(nextElement, nodeLocation, dataNode, path);

            // Viewable/displayable name for the file--includes only file name, and not full path.
            String text;
            int fnPos = path.lastIndexOf(FILE_SEPARATOR);
            if (fnPos != -1)
                text = path.substring(fnPos + 1);
            else
                text = path;

            dataNode.setText(text);
            String isMultiString = nextElement.getAttributeValue("tar");
            if (isMultiString != null && isMultiString.equals("1")) {
                dataNode.setMultifileArchive(true);
            }
            String infoPath = nextElement.getAttributeValue("info_path");
            if (infoPath != null)
                dataNode.setInfoLocation(infoPath);
            dataNode.setChildren(EMPTY_LIST);
        }
        else if (nextElement.getName().intern().equals("subdir")) {
            String nameOfSubdir = nextElement.getAttributeValue("name");
            dataNode.setText(nameOfSubdir);
            String nodeLocation = prefix + FILE_SEPARATOR; // + nameOfSubdir + FILE_SEPARATOR;
            dataNode.setLocation(nodeLocation);
            setDescriptiveText(dataNode, "Directory");

            List outputChildList = new ArrayList();
            List inputChildList = nextElement.getChildren();
            for (int j = 0; j < inputChildList.size(); j++) {

                // Recursion.  Call self with new list.
                outputChildList.add(
                        treatSubdirOrFile(
                                (Element) inputChildList.get(j),
                                nodeLocation));

            }
            dataNode.setChildren(outputChildList);
        }
        return dataNode;
    }

    /**
     * Figure out what the size of the file is, by whatever means available.
     *
     * @param nextElement  where is the size from XML?
     * @param nodeLocation where is it on disk?
     * @param dataNode     where to set the info.
     * @param path         for reporting purposes.
     */
    private void resolveFileSize(
            Element nextElement,
            String nodeLocation,
            DownloadableDataNodeImpl dataNode,
            String path) {

        String sizeStr = nextElement.getAttributeValue("size");

        super.resolveFileSize(sizeStr, nodeLocation, dataNode, path);
    }
}
