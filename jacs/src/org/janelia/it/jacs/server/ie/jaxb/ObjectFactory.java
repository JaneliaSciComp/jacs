//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.05.21 at 10:07:39 AM EDT 
//


package org.janelia.it.jacs.server.ie.jaxb;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.janelia.it.jacs.server.ie.jaxb package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Project_QNAME = new QName("", "project");
    private final static QName _Publication_QNAME = new QName("", "publication");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.janelia.it.jacs.server.ie.jaxb
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link DataFilesType }
     * 
     */
    public DataFilesType createDataFilesType() {
        return new DataFilesType();
    }

    /**
     * Create an instance of {@link HierarchyNodesType }
     * 
     */
    public HierarchyNodesType createHierarchyNodesType() {
        return new HierarchyNodesType();
    }

    /**
     * Create an instance of {@link JournalEntryType }
     * 
     */
    public JournalEntryType createJournalEntryType() {
        return new JournalEntryType();
    }

    /**
     * Create an instance of {@link SampleType }
     * 
     */
    public SampleType createSampleType() {
        return new SampleType();
    }

    /**
     * Create an instance of {@link ProjectType }
     * 
     */
    public ProjectType createProjectType() {
        return new ProjectType();
    }

    /**
     * Create an instance of {@link PubProjectType }
     * 
     */
    public PubProjectType createPubProjectType() {
        return new PubProjectType();
    }

    /**
     * Create an instance of {@link HierarchyNodeType }
     * 
     */
    public HierarchyNodeType createHierarchyNodeType() {
        return new HierarchyNodeType();
    }

    /**
     * Create an instance of {@link ProjectsType }
     * 
     */
    public ProjectsType createProjectsType() {
        return new ProjectsType();
    }

    /**
     * Create an instance of {@link AuthorType }
     * 
     */
    public AuthorType createAuthorType() {
        return new AuthorType();
    }

    /**
     * Create an instance of {@link DataFileType }
     * 
     */
    public DataFileType createDataFileType() {
        return new DataFileType();
    }

    /**
     * Create an instance of {@link LocationType }
     * 
     */
    public LocationType createLocationType() {
        return new LocationType();
    }

    /**
     * Create an instance of {@link AuthorsType }
     * 
     */
    public AuthorsType createAuthorsType() {
        return new AuthorsType();
    }

    /**
     * Create an instance of {@link PublicationType }
     * 
     */
    public PublicationType createPublicationType() {
        return new PublicationType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ProjectType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "project")
    public JAXBElement<ProjectType> createProject(ProjectType value) {
        return new JAXBElement<ProjectType>(_Project_QNAME, ProjectType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PublicationType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "publication")
    public JAXBElement<PublicationType> createPublication(PublicationType value) {
        return new JAXBElement<PublicationType>(_Publication_QNAME, PublicationType.class, null, value);
    }

}
