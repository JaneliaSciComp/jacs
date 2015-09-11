package org.janelia.it.jacs.model.tasks.mip;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by goinac on 9/9/15.
 */
@XmlRootElement(name = "singleMIPGeneration")
@XmlAccessorType(XmlAccessType.NONE)
public class SingleMIPGenerationTask extends AbstractMIPGenerationTask {
    transient public static final String TASK_NAME = "singleMIPGenerator";

    @Override
    protected String getDefaultTaskName() {
        return TASK_NAME;
    }
}
