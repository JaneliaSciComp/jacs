package org.janelia.it.jacs.web.gwt.common.client.panel.entity;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.model.entity.EntityType;
import org.janelia.it.jacs.web.gwt.common.client.popup.ModalPopupPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.ButtonSet;
import org.janelia.it.jacs.web.gwt.common.client.ui.RoundedButton;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

public class RemoveEntityTypePopup extends ModalPopupPanel {
    private EntityType entityType;
    private RemoveEntityTypeListener entityTypeRemover;

    public RemoveEntityTypePopup(EntityType entityType, RemoveEntityTypeListener entityTypeRemover, boolean realizeNow) {
        super("Confirm Delete", realizeNow);
        this.entityTypeRemover = entityTypeRemover;
        this.entityType = entityType;
    }

    protected ButtonSet createButtons() {
        RoundedButton[] tmpButtons = new RoundedButton[2];
        tmpButtons[0] = new RoundedButton("Delete", new ClickListener() {
            public void onClick(Widget widget) {
                entityTypeRemover.removeEntityType(entityType.getName());
                hide();
            }
        });
        tmpButtons[1] = new RoundedButton("Cancel", new ClickListener() {
            public void onClick(Widget widget) {
                hide();
            }
        });
        return new ButtonSet(tmpButtons);
    }

    /**
     * For subclasses to supply dialog content
     */
    protected void populateContent() {
        add(HtmlUtils.getHtml("Delete entity type \"" + entityType.getName() + "\" ?", "text"));
        add(HtmlUtils.getHtml("&nbsp;", "text"));
    }

}