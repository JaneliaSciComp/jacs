/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.annotation.to;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Transfer Object: for moving points around.  Encapsulates for purposes
 * of JSON exchange.
 *
 * @author fosterl
 */
@XmlRootElement
public class AnnotationPoint {
    // NOTE: re naming convention, GUIDs are _generated_ for the purpose of
    // using this API, and generated as unique values.  IDs are obtained
    // from _existing_ objects in JACS database, or some documentation.
    @XmlElement public Long pointGUID;
    @XmlElement public Long collectionGUID;
    @XmlElement public Long neuronGUID = -1L; // Omissible
    @XmlElement public int x;
    @XmlElement public int y;
    @XmlElement public int z;
    @XmlElement public Double radius = -1.0;  // Omissible
    @XmlElement public int structureID;
    @XmlElement public Long parentPointGUID;
}
