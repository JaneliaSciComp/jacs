package org.janelia.it.jacs.compute.xslt;

/**
 * Created by Leslie L Foster on 8/28/2016.
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.XMLConstants;

/**
 * This converter will run an XSLT style sheet, to convert between
 * XML formats.
 *
 * @author Leslie L Foster
 */
public class GeneralXsltConverter {
    //  These are all the different choices of XSLT sheets.
    private static final String XSLT_MAP_PROPS = "org/janelia/it/jacs/compute/xslt/xslt_map.properties";
    private static final String XSLT_EXTENSION_MAP_PROPS = "xslt_map.properties";
    private static final String XSLT_STRIP_PREFIX = "TYPE";
    private static final Properties TYPE_PROPS = ConfigUtils.getProperties(XSLT_MAP_PROPS);
    private static final Properties EXTENDED_TYPE_PROPS;
    static {
        // Has the effect of overriding internal settings with user extensions.
        EXTENDED_TYPE_PROPS = ConfigUtils.getProperties(XSLT_EXTENSION_MAP_PROPS);
        if (! EXTENDED_TYPE_PROPS.isEmpty()) {
            for (Iterator it = EXTENDED_TYPE_PROPS.keySet().iterator(); it.hasNext(); ) {
                String nextName = it.next().toString();
                TYPE_PROPS.setProperty(nextName, EXTENDED_TYPE_PROPS.getProperty(nextName));
                System.out.println("Added extended mapping "
                        + nextName
                        + " to "
                        + EXTENDED_TYPE_PROPS.getProperty(nextName)
                        + " as an XSLT conversion.");
            }
        }
    }

    //  Each converter converts a file using a stylesheet.
    private String filename;
    private String xslFile;

    /**
     * Factory method on this converter.  Will determine whether the filename
     * is something this converter can handle, and will return an instance
     * if so.  If not, will return null.
     *
     * @param filename filename to test.
     * @return new instance of this converter, or null.
     */
    public static GeneralXsltConverter getConverter(String filename) {
        if (filename.endsWith(".xml")) {
            for (Iterator it = TYPE_PROPS.keySet().iterator(); it.hasNext(); ) {
                String nextKey = it.next().toString();
                String suffix = nextKey.substring(XSLT_STRIP_PREFIX.length());
                if (filename.endsWith(suffix)) {
                    return new GeneralXsltConverter(filename, (String)TYPE_PROPS.get(nextKey));
                }
            }
        }

        return null;
    }

    /**
     * Caches the filename provided.
     *
     * @param filename
     */
    private GeneralXsltConverter(String filename, String xslFile) {
        this.filename = filename;
        this.xslFile = xslFile;
    }

    /**
     * Picks up conversion result.
     *
     * @return input stream that sources the target XML type.
     */
    public InputStream getStream() {
        Transformer transformer;
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "all");
        //factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "all");
        InputStream inStream = getClass().getClassLoader().getResourceAsStream(getXslFile());
        StreamSource stylesheet = new StreamSource(inStream);
        ByteArrayInputStream inputStream = null;
        //PipedOutputStream pout = new PipedOutputStream();
        //PipedInputStream pin;
        try {
            Templates templates = factory.newTemplates(stylesheet);
            transformer = templates.newTransformer();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            transformer.transform(new StreamSource(getFilename()), new StreamResult(outputStream));
            byte[] bytesOfXml = outputStream.toByteArray();

            inputStream = new ByteArrayInputStream(bytesOfXml);
            // DEBUG
            //System.out.println(new String(bytesOfXml));

            //pin = new PipedInputStream(pout);

            // Now, in order that the piped input stream will be filled with
            // good things to read, its "pump" thread must be started.
            //Thread thread = new Thread(new Piper(transformer, pout));
            //thread.start();
        } catch (Exception e) {
            System.out.println(getFilename() + " failed to convert to target XML required by this app.");
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
        //return pin;
        return inputStream;
    }

    /**
     * Template Method: provides the name of the file to convert to
     * internal format.
     *
     * @return the filename.
     */
    private String getFilename() {
        return filename;
    }

    /**
     * Template Method: provides the name of the XSLT stylesheet file.
     *
     * @return a valid XSLT spreadsheet.
     */
    private String getXslFile() {
        return xslFile;
    }

}

