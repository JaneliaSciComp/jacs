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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class can be used to make a list of spans.  It has functions for splitting spans and inserting
 * strings e.g. tags at a certain position
 *
 * @author Tareq Nabeel
 */
public class SpanList implements Serializable, IsSerializable {
    private List<Span> spans = new ArrayList<Span>();
    private static int sUid = 0;
    private int valueLength;
    private int pcDataLength;

    public SpanList() {
    }

    public static String createUniqueId() {
        return "SpanList_" + (++sUid);
    }

    /**
     * Adds a span to the current list of spans
     *
     * @param span
     */
    public void addSpan(Span span) {
        spans.add(span);
        if (span.getHtml() != null) {
            valueLength += span.getHtml().length();
            pcDataLength += span.getPcData().length();
        }
    }

    /**
     * Creates a span using html and styleName and adds it to the list of spans
     *
     * @param html
     * @param styleName
     */
    public void addSpan(String html, String styleName) {
        addSpan(html, null, styleName);
    }

    /**
     * Creates a span using html and styleName and adds it to the list of spans
     *
     * @param html
     * @param pcData
     * @param styleName
     */
    public void addSpan(String html, String pcData, String styleName) {
        Span span = new Span(html, pcData, styleName);
        span.setId(createUniqueId());
        addSpan(span);
    }

    /**
     * Sets the list spans
     *
     * @param spans
     */
    public void setSpans(List<Span> spans) {
        this.spans = spans;
        calculateValueLength();
    }

    /**
     * Returns the size of the list of spans
     *
     * @return int - the size of the list
     */
    public int size() {
        return spans.size();
    }

    /**
     * Returns the span at the specified index
     *
     * @param index
     * @return Span at specified index
     */
    public Span getSpan(int index) {
        return spans.get(index);
    }

    /**
     * Returns the last span in the list
     *
     * @return Span - the last span in the list
     */
    public Span getLastSpan() {
        if (size() > 0) {
            return spans.get(size() - 1);
        }
        else {
            return null;
        }
    }

    /**
     * Returns an iterator over the list of spans
     *
     * @return Iterator an iterator over the list of spans
     */
    public Iterator iterator() {
        return spans.iterator();
    }

    /**
     * Returns the entire contents of the list of spans i.e.
     * span tags plus the contents of the list of spans.
     * This is what you'd use to create an HTMLPanel
     *
     * @return the span tags plus the contents of the list of spans.
     */
    public String toString() {
        StringBuffer spanBuff = new StringBuffer();
        for (Iterator iterator = iterator(); iterator.hasNext();) {
            Span span = (Span) iterator.next();
            spanBuff.append(span.toString());
        }

        return spanBuff.toString();
    }

    /**
     * This method creates a split in the list of spans at the pcData
     * position specified by pcDataSplitPosition.  It returns the two splits
     * as two Spanlists
     *
     * @param pcDataSplitPosition
     * @return SpanList[] the SpanList splits
     */
    public SpanList[] split(int pcDataSplitPosition) {
        SpanList firstSplitList = new SpanList();
        SpanList secondSplitList = new SpanList();
        int currentTotalLength = 0;
        int previousTotallength;
        boolean pastSplitPosition = false;
        for (Iterator iterator = iterator(); iterator.hasNext();) {
            previousTotallength = currentTotalLength;
            Span span = (Span) iterator.next();
            // totalLength is the length of pcData that has been processed thus far
            currentTotalLength += span.getPcData().length();
            if (!pastSplitPosition && pcDataSplitPosition < currentTotalLength) {
                splitSpan(span, firstSplitList, secondSplitList, pcDataSplitPosition, previousTotallength);
                pastSplitPosition = true;
                continue;
            }
            if (pastSplitPosition) {
                secondSplitList.addSpan(span);
            }
            else {
                firstSplitList.addSpan(span);
            }
        }
        return new SpanList[]{firstSplitList, secondSplitList};
    }

    /**
     * Helper method that splits the span and adds the first split to firstSplitList
     * and second split to secondSplitList
     *
     * @param span
     * @param firstSplitList
     * @param secondSplitList
     * @param pcDataSplitPosition
     * @param previousTotallength
     */
    private void splitSpan(Span span, SpanList firstSplitList, SpanList secondSplitList, int pcDataSplitPosition, int previousTotallength) {
        pcDataSplitPosition = pcDataSplitPosition - previousTotallength;
        Span[] splits = span.split(pcDataSplitPosition);
        firstSplitList.addSpan(splits[0]);
        secondSplitList.addSpan(splits[1]);
    }

    /**
     * Inserts string every <code>everyXNumOfPcdataChars</code> number of PCData characters.
     * Userful if you need to create breaks in the list of spans every so many characters.
     *
     * @param stringToInsert
     * @param everyXNumOfPcdataChars
     * @see org.janelia.it.jacs.web.gwt.detail.client.bse.read.ReadPanel
     */
    public void insertString(String stringToInsert, int everyXNumOfPcdataChars) {
        if (stringToInsert == null || everyXNumOfPcdataChars > getPcDataLength()) {
            return;
        }
        PerfStats.start("SpanList.insertString()");
        List<Span> newList = new ArrayList<Span>();
        int totalTextCharsProcessed = 0;
        boolean insertStringContainsNoMarkup = (stringToInsert.indexOf('<') == -1);
        for (Iterator iterator = iterator(); iterator.hasNext();) {
            Span span = (Span) iterator.next();
            if (span.getPcData().length() + totalTextCharsProcessed < everyXNumOfPcdataChars) {
                newList.add(span);
                totalTextCharsProcessed += span.getPcData().length();
                continue;
            }
            StringBuffer newHtml = new StringBuffer();
            StringBuffer newPcdata = new StringBuffer();
            boolean inTag = false;
            for (int i = 0; i < span.getHtml().length(); i++) {
                char c = span.getHtml().charAt(i);
                newHtml.append(c);
                if (c == '<') {
                    inTag = true;
                    continue;
                }
                else {
                    newPcdata.append(c);
                }
                if (!inTag && c != '>') {
                    totalTextCharsProcessed++;
                    if (totalTextCharsProcessed != 0 && totalTextCharsProcessed % everyXNumOfPcdataChars == 0) {
                        // We've reached the point of insertion
                        newHtml.append(stringToInsert);
                        if (insertStringContainsNoMarkup) {
                            newPcdata.append(stringToInsert);
                        }
                    }
                }
                if (c == '>') {
                    inTag = false;
                }
            }
            if (newHtml.length() != span.getHtml().length()) {
                span = new Span(newHtml.toString(), newPcdata.toString(), span.getStyleName());
            }
            newList.add(span);
        }
        setSpans(newList);
        PerfStats.end("SpanList.insertString()");
    }

    /**
     * Calculates the length of the contents of the list of spans
     */
    private void calculateValueLength() {
        for (Iterator iterator = iterator(); iterator.hasNext();) {
            Span span = (Span) iterator.next();
            if (span.getHtml() != null) {
                valueLength += span.getHtml().length();
            }
        }
    }

    /**
     * Adds SpanList specified by listToAdd to the current list of spans
     *
     * @param listToAdd
     */
    public void appendList(SpanList listToAdd) {
        this.spans.addAll(listToAdd.spans);
        setSpans(this.spans); // to recalculate value length
    }

    /**
     * Returns the span tags of the list of spans without the values
     *
     * @return the span tags of the list of spans without the values
     */
    public String toStringWithoutValues() {
        StringBuffer buff = new StringBuffer();
        for (Object span1 : spans) {
            Span span = (Span) span1;
            buff.append(span.open());
            buff.append(span.close());
        }
        return buff.toString();
    }

    /**
     * Returns the size of the html contents of the list of spans
     *
     * @return the size of the html contents of the list of spans
     */
    public int getValueLength() {
        return valueLength;
    }

    public int getPcDataLength() {
        return pcDataLength;
    }

    public void setPcDataLength(int pcDataLength) {
        this.pcDataLength = pcDataLength;
    }


    /**
     * Returns the html contents of the list spans without the span tags
     *
     * @return the html contents of the list spans without the span tags
     */
    public String toStringValuesOnly() {
        StringBuffer buff = new StringBuffer();
        for (Object span1 : spans) {
            Span span = (Span) span1;
            buff.append(span.getHtml());
        }
        return buff.toString();
    }


}
