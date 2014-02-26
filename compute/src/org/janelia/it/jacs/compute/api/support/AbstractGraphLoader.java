package org.janelia.it.jacs.compute.api.support;

import org.janelia.it.jacs.model.graph.support.GraphLoader;

/**
 * An abstract graph loader with authorized access semantics. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AbstractGraphLoader implements GraphLoader {

    private Access access;
    
    public AbstractGraphLoader(Access access) {
        this.access = access;
    }

	public Access getAccess() {
		return access;
	}
}
