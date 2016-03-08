package org.janelia.it.workstation.gui.browser.events.selection;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.workstation.gui.browser.events.Events;

import com.google.common.eventbus.Subscribe;

/**
 * A selection model which tracks all domain object selections, globally across all other selection models. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class GlobalDomainObjectSelectionModel extends SelectionModel<DomainObject,Reference> {

    public static GlobalDomainObjectSelectionModel instance;
    
    private GlobalDomainObjectSelectionModel() {
    }
    
    public static GlobalDomainObjectSelectionModel getInstance() {
        if (instance==null) {
            instance = new GlobalDomainObjectSelectionModel();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }

    @Subscribe
    public void domainObjectSelected(DomainObjectSelectionEvent event) {
        if (event.isSelect()) {
            select(event.getDomainObject(), event.isClearAll(), event.isUserDriven());
        }
        else {
            deselect(event.getDomainObject(), event.isUserDriven());
        }
    }
    
    @Override
    protected void selectionChanged(DomainObject domainObject, Reference id, boolean select, boolean clearAll, boolean isUserDriven) {
        // Since this is a meta-model, the relevant events were already on the bus, so this method 
        // does not need to do any additional work.
    }
    
    @Override
    public Reference getId(DomainObject domainObject) {
        return Reference.createFor(domainObject);
    }
}
