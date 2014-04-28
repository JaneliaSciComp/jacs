package org.janelia.it.jacs.model.domain;

import java.util.Map;

public interface HasImages {

    public abstract Map<ImageType,String> getImages();

}