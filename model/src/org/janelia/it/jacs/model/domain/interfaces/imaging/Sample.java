package org.janelia.it.jacs.model.domain.interfaces.imaging;

import java.util.List;

import org.janelia.it.jacs.model.domain.interfaces.metamodel.DomainObject;
import org.janelia.it.jacs.model.domain.interfaces.metamodel.RelationshipLoader;

public interface Sample extends DomainObject, Viewable2d, Viewable3d, Viewable4d {

    public List<AlignmentContext> getAvailableAlignmentContexts(RelationshipLoader loader) throws Exception;

    public void loadContextualizedChildren(RelationshipLoader loader, AlignmentContext alignmentContext);
    
    public String getDataSetIdentifier();

    public String getChannelSpecification();

    public String getTilingPattern();

    public List<Neuron> getNeuronSet();

}
