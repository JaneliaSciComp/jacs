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

package org.janelia.it.jacs.server.jaxb.reference_record.impl.runtime;

import com.sun.xml.bind.JAXBObject;
import org.xml.sax.SAXException;

/**
 * For a generated class to be serializable, it has to
 * implement this interface.
 *
 * @author Kohsuke Kawaguchi
 */
public interface XMLSerializable extends JAXBObject {
    /**
     * Serializes child elements and texts into the specified target.
     */
    void serializeBody(XMLSerializer target) throws SAXException;

    /**
     * Serializes attributes into the specified target.
     */
    void serializeAttributes(XMLSerializer target) throws SAXException;

    /**
     * Declares all the namespace URIs this object is using at
     * its top-level scope into the specified target.
     */
    void serializeURIs(XMLSerializer target) throws SAXException;

}
