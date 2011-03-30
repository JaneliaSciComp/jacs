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

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.6-01/24/2006 06:08 PM(kohsuke)-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2006.10.27 at 12:05:11 PM EDT 
//


package org.janelia.it.jacs.server.jaxb.reference_record;


/**
 * Java content class for anonymous complex type.
 * <p>The following schema fragment specifies the expected content contained within this java content object. (defined at file:/C:/current_projects/cvsfiles/Camera/shared/schema/reference_record.xsd line 97)
 * <p/>
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="description" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="info_path" type="{}file_path_type" />
 *       &lt;attribute name="path" use="required" type="{}file_path_type" />
 *       &lt;attribute name="size" type="{}nonZeroLong" />
 *       &lt;attribute name="tar" type="{}tar_type" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public interface FileType {


    /**
     * Gets the value of the tar property.
     */
    int getTar();

    /**
     * Sets the value of the tar property.
     */
    void setTar(int value);

    /**
     * Gets the value of the description property.
     *
     * @return possible object is
     *         {@link java.lang.String}
     */
    java.lang.String getDescription();

    /**
     * Sets the value of the description property.
     *
     * @param value allowed object is
     *              {@link java.lang.String}
     */
    void setDescription(java.lang.String value);

    /**
     * Gets the value of the size property.
     */
    long getSize();

    /**
     * Sets the value of the size property.
     */
    void setSize(long value);

    /**
     * Gets the value of the path property.
     *
     * @return possible object is
     *         {@link java.lang.String}
     */
    java.lang.String getPath();

    /**
     * Sets the value of the path property.
     *
     * @param value allowed object is
     *              {@link java.lang.String}
     */
    void setPath(java.lang.String value);

    /**
     * Gets the value of the infoPath property.
     *
     * @return possible object is
     *         {@link java.lang.String}
     */
    java.lang.String getInfoPath();

    /**
     * Sets the value of the infoPath property.
     *
     * @param value allowed object is
     *              {@link java.lang.String}
     */
    void setInfoPath(java.lang.String value);

}
