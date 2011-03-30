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

package org.janelia.it.jacs.web.gwt.download.client;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.SystemWebTracker;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;
import org.janelia.it.jacs.web.gwt.common.client.popup.download.DownloadPopup;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.download.client.formatter.DataFileFormatter;
import org.janelia.it.jacs.web.gwt.download.client.formatter.DownloadLinkBuilder;

/**
 * @author Michael Press
 */
public class DownloadFilePopup extends DownloadPopup {
    private DownloadableDataNode _dataNode;

    private static final String GZ_FORMAT = ".gz";
    private static final String ZIP_FORMAT = ".zip";
    private static final String FILE_FORMAT_DOWNLOAD_VERBIAGE = "file format";

    //TODO: move download model to common so DownloadFilePopup can move to common
    public DownloadFilePopup(DownloadableDataNode node) {
        this(node, false);
    }

    public DownloadFilePopup(DownloadableDataNode dataNode, boolean realizeNow) {
        super("Download File", realizeNow);
        _dataNode = dataNode;
    }

    protected void populateContent() {
        if (_dataNode == null) {
            add(HtmlUtils.getHtml("An error occurred retrieving this file", "error"));
        }
        else {
            addDescription("File description", DataFileFormatter.getDescriptiveText(_dataNode));

            add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));

            //TODO: take available formats as array in constructor
            add(createFileFormatDownloadLink(ZIP_FORMAT));
            add(createFileFormatDownloadLink(GZ_FORMAT));
            Widget infoLink = createInfoLink();
            if (null != infoLink) {
                add(infoLink);
            }
        }

        // Here's how to get rid of that pesky popup!
        addCloseLink();
    }

    protected Panel createFileFormatDownloadLink(String format) {
        String formatExtension;
        if (format.equals(ZIP_FORMAT)) {
            formatExtension = format;
        }
        else {
            if (_dataNode.isMultifileArchive())
                formatExtension = ".tar" + GZ_FORMAT;
            else {
                formatExtension = GZ_FORMAT;
            }
        }
        return createDownloadLink("Download as", new Link(format, new DownloadFullfiller(formatExtension)));
    }

    private Panel createInfoLink() {
        if (_dataNode == null || _dataNode.getInfoLocation() == null)
            return null;

        return createDownloadLink("Download", new Link(FILE_FORMAT_DOWNLOAD_VERBIAGE,
                new DownloadHardLocationFullfiller(_dataNode.getInfoLocation())));
    }


    public class DownloadHardLocationFullfiller implements ClickListener {
        private String _location;

        public DownloadHardLocationFullfiller(String location) {
            _location = location;
        }

        public void onClick(Widget w) {
            if (_location == null) {
                SystemWebTracker.trackActivity("DownloadFile", new String[]{_dataNode.getLocation()});
                Window.open(DownloadLinkBuilder.getDownloadLink(_dataNode.getLocation()), "_self", "");
            }
            else {
                SystemWebTracker.trackActivity("DownloadFile", new String[]{_location});
                Window.open(DownloadLinkBuilder.getDownloadLink(_location), "_self", "");
            }
        }
    }

    public class DownloadFullfiller implements ClickListener {
        private String _format;

        public DownloadFullfiller(String format) {
            _format = format;
        }

        public void onClick(Widget w) {
            SystemWebTracker.trackActivity("DownloadFile", new String[]{_dataNode.getLocation() + _format});
            String location = DownloadLinkBuilder.getDownloadLink(_dataNode.getLocation() + _format);
            Window.open(location, "_self", "");
        }
    }

    //TODO - make this generic PopupCloser, with option to unrealize the popup (so it takes no more memory)
    //public class PopupCloser implements ClickListener {
    //    private BasePopupPanel _popup;
    //
    //    public PopupCloser(BasePopupPanel popup) {
    //        _popup = popup;
    //    }
    //
    //    public void onClick(Widget sender) {
    //        _popup.hide();
    //    }
    //}
}
