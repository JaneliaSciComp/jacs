package org.janelia.it.jacs.model.tasks.mip;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

/**
 * Created by goinac on 10/7/15.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class MIPInputImageData implements Serializable {
    @XmlElement
    public String filepath;
    @XmlElement
    public String outputPrefix;
    @XmlElement
    public String chanspec;
    @XmlElement
    public String colorspec;
    @XmlElement
    public String divspec;
    @XmlElement
    public Integer laser;
    @XmlElement
    public Integer gain;
    @XmlElement
    public String area;

    public boolean hasFilepath() {
        return filepath != null && filepath.trim().length() > 0;
    }

    public boolean hasChanSpec() {
        return chanspec != null && chanspec.trim().length() > 0;
    }

    public boolean hasColorSpec() {
        return colorspec != null && colorspec.trim().length() > 0;
    }

}
