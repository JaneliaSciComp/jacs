
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.6-01/24/2006 06:08 PM(kohsuke)-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2006.10.27 at 12:05:11 PM EDT 
//


package org.janelia.it.jacs.server.jaxb.reference_record;


/**
 * Java content class for text_type complex type.
 * <p>The following schema fragment specifies the expected content contained within this java content object. (defined at file:/C:/current_projects/cvsfiles/Camera/shared/schema/reference_record.xsd line 129)
 * <p/>
 * <pre>
 * &lt;complexType name="text_type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="URL" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
 *       &lt;attribute name="local" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public interface TextType {


    /**
     * Gets the value of the local property.
     *
     * @return possible object is
     *         {@link java.lang.String}
     */
    java.lang.String getLocal();

    /**
     * Sets the value of the local property.
     *
     * @param value allowed object is
     *              {@link java.lang.String}
     */
    void setLocal(java.lang.String value);

    /**
     * Gets the value of the url property.
     *
     * @return possible object is
     *         {@link java.lang.String}
     */
    java.lang.String getURL();

    /**
     * Sets the value of the url property.
     *
     * @param value allowed object is
     *              {@link java.lang.String}
     */
    void setURL(java.lang.String value);

}
