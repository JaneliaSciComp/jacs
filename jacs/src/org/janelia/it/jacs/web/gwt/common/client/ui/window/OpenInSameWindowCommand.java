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

package org.janelia.it.jacs.web.gwt.common.client.ui.window;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Michael Press
 */
public class OpenInSameWindowCommand implements Command, ClickListener {
    String _url;

    public OpenInSameWindowCommand(String url) {
        setUrl(url);
    }

    /**
     * From Command
     */
    public void execute() {
        open();
    }

    /**
     * From ClickListener
     */
    public void onClick(Widget sender) {
        open();
    }

    protected void open() {
        Window.open(_url, getWhichWindow(), "");
    }

    protected String getWhichWindow() {
        return "_self";
    }

    public void setUrl(String url) {
        _url = url;
    }

}