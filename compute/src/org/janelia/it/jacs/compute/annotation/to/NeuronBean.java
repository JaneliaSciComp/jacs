/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.annotation.to;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Bean for holding all info about a neuron. Suitable for import.
 *
 * @author fosterl
 */
@XmlRootElement
public class NeuronBean {
    @XmlElement public AnnotationPoint[] points;
    @XmlElement public String name;    
    @XmlElement public Long id;
    @XmlElement public Long collectionGUID;
    @XmlElement public Long sampleID;
    // id vs tag will wind up in a map on the TmVersionedNeuronCollection.
    @XmlElement public String tag;  // Omissible.
}
