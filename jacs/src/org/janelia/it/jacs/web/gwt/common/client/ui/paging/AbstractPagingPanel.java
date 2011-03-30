/*
 * Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
 *
 * This file is part of JCVI VICS.
 *
 * JCVI VICS is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the Artistic License 2.0.  For
 * details, see the full text of the license in the file LICENSE.txt.  No
 * other rights are granted.  Any and all third party software rights to
 * remain with the original developer.
 *
 * JCVI VICS is distributed in the hope that it will be useful in
 * bioinformatics applications, but it is provided "AS IS" and WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
 * implied warranties of merchantability or fitness for any particular
 * purpose.  For details, see the full text of the license in the file
 * LICENSE.txt.
 *
 * You should have received a copy of the Artistic License 2.0 along with
 * JCVI VICS.  If not, the license can be obtained from
 * "http://www.perlfoundation.org/artistic_license_2_0."
 */

package org.janelia.it.jacs.web.gwt.common.client.ui.paging;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.paging.DataRetrievedListener;

/**
 * This class a container for paged data
 *
 * @author Cristian Goina
 */
abstract public class AbstractPagingPanel extends VerticalPanel {

    protected Panel dataPanel;
    private SimplePaginator paginator;
    private String[] pageSizeOptions;
    private SimplePanel _pagingControlPanel;

    protected AbstractPagingPanel() {
    }

    public AbstractPagingPanel(SimplePaginator paginator,
                               String[] pageSizeOptions) {
        this.paginator = paginator;
        this.pageSizeOptions = pageSizeOptions;
        initialize();
    }

    abstract public void render(Object data);

    abstract public void renderError(Throwable throwable);

    protected Panel createDataPanel() {
        VerticalPanel panel = new VerticalPanel();
        panel.setHorizontalAlignment(VerticalPanel.ALIGN_LEFT);
        return panel;
    }

    protected void initialize() {
        if (dataPanel == null) {
            dataPanel = createDataPanel();
        }
        add(dataPanel);

        SimplePagingControllerWidget pagingControl = new SimplePagingControllerWidget(paginator, pageSizeOptions);
        pagingControl.setWidth("100%");
        pagingControl.ensureDebugId("SimplePagingController");

        _pagingControlPanel = new SimplePanel();
        _pagingControlPanel.add(pagingControl);

        add(_pagingControlPanel);
        setDataChangeListener();
    }

    protected void setPagingControlPanelStyleName(String styleName) {
        _pagingControlPanel.setStyleName(styleName);
    }

    protected void setDataChangeListener() {
        // for now we don't bother unregistering the dataChangeListener
        DataRetrievedListener dataChangeListener = new DataRetrievedListener() {
            public void onSuccess(Object data) {
                render(data);
            }

            public void onFailure(Throwable throwable) {
                renderError(throwable);
            }

            public void onNoData() {
                render(null);
            }
        };
        paginator.addDataChangedListener(dataChangeListener);
    }

}
