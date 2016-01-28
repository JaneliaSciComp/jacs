/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.annotation.to;

import java.util.Date;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Neurons belong to this, which may or may not have reference to some
 * sample, by ID.
 *
 * @author fosterl
 */
@XmlRootElement
public class AnnotationPointCollection {
    //date stamp on it, maybe a name string, a version number, and an ID
    @XmlElement public Long guid;
    @XmlElement public Long sampleID = -1L; // May not be omitted.  Refers to ID in jacs.
    @XmlElement public Date creationDate;
    @XmlElement public String name;
    @XmlElement public Integer versionNumber = 1;
}
