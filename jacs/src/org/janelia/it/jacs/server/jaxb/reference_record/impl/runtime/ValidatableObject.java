
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.6-01/24/2006 06:08 PM(kohsuke)-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2006.10.27 at 12:05:11 PM EDT 
//

package org.janelia.it.jacs.server.jaxb.reference_record.impl.runtime;

import com.sun.msv.verifier.DocumentDeclaration;

/**
 * This interface is implemented by generated classes
 * to indicate that the class supports validation.
 */
public interface ValidatableObject extends XMLSerializable {
    /**
     * Gets the schema fragment associated with this class.
     */
    DocumentDeclaration createRawValidator();

    /**
     * Gets the main interface that this object implements.
     * <p/>
     * For example, <code>FooImpl</code> will return <code>Foo</code>
     * from this method.
     */
    Class getPrimaryInterface();
}
