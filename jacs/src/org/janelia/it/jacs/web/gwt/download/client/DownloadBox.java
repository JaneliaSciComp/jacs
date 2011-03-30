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

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import org.janelia.it.jacs.web.gwt.common.client.model.download.DownloadableDataNode;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ExternalLink;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.Link;
import org.janelia.it.jacs.web.gwt.download.client.formatter.DataFileFormatter;

import java.util.List;

/**
 * @author Michael Press
 */
public class DownloadBox extends TitledBox {
    public DownloadBox() {
        this(null);
    }

    public DownloadBox(String title) {
        this(title, false);
    }

    public DownloadBox(String title, boolean showActionLinks) {
        this(title, null, showActionLinks, true);
    }


    public DownloadBox(String title, List files, boolean showActionLinks, boolean showContent) {
        super(title, showActionLinks, showContent);
        addFiles(files);
    }

    protected void init() {
        setActionLinkBackgroundStyleName("downloadBoxActionLinkBackground"); // has to be before super.init()
        super.init();
        setContentsPanelStyleName("downloadBoxContentsPanel");
        setCornerStyleName("downloadBoxRounding");
        setLabelStyleName("downloadBoxLabel");
    }

    public Widget getDownloadableFileLink(final DownloadableDataNode node) {
        return getDownloadableFileLink(node, null, false, null);
    }

    public Widget getDownloadableFileLink(final DownloadableDataNode node, String linkText, boolean asExternalLink) {
        return getDownloadableFileLink(node, linkText, asExternalLink, null);
    }

    //TODO: sync this duplicated code with DataFileFormatter
    public Widget getDownloadableFileLink(final DownloadableDataNode node,
                                          String linkText,
                                          boolean asExternalLink,
                                          ClickListener linkEventListener) {
        Widget downloadWidget;
        String descriptiveText = (linkText != null) ? linkText : DataFileFormatter.getDescriptiveText(node);
        if (asExternalLink) {
            ExternalLink link = new ExternalLink(linkText, node.getLocation());
            if (linkEventListener != null) {
                link.addClickListener(linkEventListener);
            }
            downloadWidget = link;
        }
        else {
            Link link = new Link(descriptiveText, node.getLocation());
            if (linkEventListener != null) {
                link.addClickListener(linkEventListener);
            }
            downloadWidget = link;
        }
        return downloadWidget;
    }

    private static final String[] MILL_ABBREV = new String[]{"B", "KB", "MB", "GB"};

    public String abbreviateSize(long size) {
        String suffixMill = "B";
        double returnSize = size;
        for (int i = 0; i < 4; i++) {
            // Test: is this mantissa a presentable size?
            if (returnSize < 1000) {
                suffixMill = MILL_ABBREV[i];
                break;
            }

            // Iteratively reduce the mantissa by 1000.
            returnSize /= 1000;
        }

        String returnSizeStr = "" + returnSize;
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < 2; i++) {
            char theChar = returnSizeStr.charAt(i);
            buf.append(theChar);
            // Include one character after the period, if encountered.
            if (theChar == '.') {
                buf.append(returnSizeStr.charAt(i + 1));
            }
        }
        if (returnSize > 100) {
            buf.append("0");
        }

        return buf.toString() + " " + suffixMill;
    }

    protected String getBorderColor() {
        return "#99CCFF";
    }

    public void addFile(DownloadableDataNode file) {
        if (file != null)
            add(getDownloadableFileLink(file));
    }

    public void addFiles(List files) {
        if (files != null && files.size() > 0) {
            for (int i = 0; i < files.size(); i++)
                addFile((DownloadableDataNode) files.get(i));
        }
    }

    public void addFileAsExternalLink(DownloadableDataNode file, String linkText) {
        if (file != null) {
            add(getDownloadableFileLink(file, linkText, true));
        }
    }

    public void addFileAsExternalLink(DownloadableDataNode file, String linkText, ClickListener downloadEventListener) {
        if (file != null) {
            add(getDownloadableFileLink(file, linkText, true, downloadEventListener));
        }
    }

}
