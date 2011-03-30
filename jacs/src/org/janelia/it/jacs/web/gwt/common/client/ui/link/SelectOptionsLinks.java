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

package org.janelia.it.jacs.web.gwt.common.client.ui.link;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;

import java.util.List;

/**
 * Creates a Widget that looks like this:
 * <p/>
 * Select: this | that
 * <p/>
 * where "this" and "that" are links, and there's an optional leading icon image.
 *
 * @author Michael Press
 */
public class SelectOptionsLinks extends Composite {
    private String _prompt;
    private List<OptionItem> _options;

    public SelectOptionsLinks(String prompt, List<OptionItem> options) {
        _prompt = prompt;
        _options = options;
        init();
    }

    private void init() {
        HorizontalPanel panel = new HorizontalPanel();

        panel.add(HtmlUtils.getHtml(_prompt + ":&nbsp;&nbsp;", "infoPrompt"));
        int i = 0;
        for (OptionItem item : _options) {
            if (i++ > 0)
                panel.add(HtmlUtils.getHtml("&nbsp;|&nbsp;", "smallLinkSeparator"));
            panel.add(new SmallLink(item.getLabel(), item.getClickListener()));
        }

        initWidget(panel);
    }
}
