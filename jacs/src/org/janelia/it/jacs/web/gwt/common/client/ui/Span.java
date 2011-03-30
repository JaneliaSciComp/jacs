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

package org.janelia.it.jacs.web.gwt.common.client.ui;

import com.google.gwt.user.client.rpc.IsSerializable;
import org.janelia.it.jacs.web.gwt.common.client.util.PerfStats;

import java.io.Serializable;


/**
 * @author Michael Press
 * @author Tareq Nabeel: Added Split and PcData logic
 */
public class Span implements Serializable, IsSerializable {
    private String _styleName = null;
    private String _html = "";
    private String _pcData = ""; // pure text ... no tags
    private String _id = null;

    public Span() {
    }

    public Span(String html, String pcData, String styleName) {
        setHtml(html);
        if (pcData == null) {
            setPcData();
        }
        else {
            setPcData(pcData);
        }
        setStyleName(styleName);
    }

    public Span(String html, String styleName) {
        setHtml(html);
        setPcData();
        setStyleName(styleName);
    }

    public Span setHtml(String html) {
        _html = (html != null) ? html : "";

        return this;
    }

    public String getHtml() {
        return _html;
    }

    public Span setPcData(String value) {
        _pcData = (value != null) ? value : "";
        return this;
    }

    public String getPcData() {
        return _pcData;
    }

    public Span setStyleName(String styleName) {
        _styleName = styleName;
        return this;
    }

    public String getStyleName() {
        return _styleName;
    }

    public String open() {
        return "<span " +
                ((getId() != null) ? printAttribute("id", getId()) : "") +
                ((getStyleName() != null) ? printAttribute(" class", getStyleName()) : "") +
                ">";
    }

    public String close() {
        return "</span>";
    }

    //TODO: move to utiltiy/superclass
    protected String printAttribute(String name, String value) {
        return name + "='" + value + "'";
    }

    public Span setId(String id) {
        _id = id;
        return this;
    }

    public String getId() {
        return _id;
    }

    public String toString() {
//        System.out.println(open() + getHtml() + close());
        return open() + getHtml() + close();
    }

    /**
     * This method strips the tags from the span content
     */
    private void setPcData() {
        if (_html == null) {
            return;
        }
        PerfStats.start("Span.setPcData()");
        if (_html.indexOf('<') == -1) {
            setPcData(_html);
            PerfStats.end("Span.setPcData()");
            return;
        }
        StringBuffer buff = new StringBuffer();
        boolean inTag = false;
        for (int i = 0; i < _html.length(); i++) {
            char c = _html.charAt(i);
            if (c == '>') {
                inTag = false;
                continue;
            }
            if (inTag || c == '<') {
                inTag = true;
                continue;
            }
            buff.append(c);
        }
        setPcData(buff.toString());
        PerfStats.end("Span.setPcData()");
    }

    /**
     * This method splits the span into two spans at the position specified by pcDataSplitPosition
     *
     * @param pcDataSplitPosition
     * @return Span[] the splits
     */
    public Span[] split(int pcDataSplitPosition) {
        StringBuffer firstSplitHtml = new StringBuffer();
        StringBuffer secondSplitHtml = new StringBuffer();
        boolean inTag = false;
        boolean postSplit = false;
        int pcDataCharsProcessed = 0;
        String html = getHtml();
        for (int i = 0; i < html.length(); i++) {
            char c = html.charAt(i);
            if (pcDataCharsProcessed == pcDataSplitPosition + 1) {
                postSplit = true;
            }
            if (inTag || c == '<') {
                inTag = true;
            }
            else {
                pcDataCharsProcessed++;
            }
            if (postSplit) {
                secondSplitHtml.append(c);
            }
            else {
                firstSplitHtml.append(c);
            }
            if (c == '>') {
                inTag = false;
            }
        }
        Span firstSpan = new Span(firstSplitHtml.toString(), getStyleName());
        Span secondSpan = new Span(secondSplitHtml.toString(), getStyleName());
        return new Span[]{firstSpan, secondSpan};
    }

}
