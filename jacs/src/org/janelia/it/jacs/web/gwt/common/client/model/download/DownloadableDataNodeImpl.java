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

package org.janelia.it.jacs.web.gwt.common.client.model.download;

import org.janelia.it.jacs.web.gwt.common.client.model.metadata.Site;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Lfoster
 * Date: Aug 21, 2006
 * Time: 2:20:10 PM
 * <p/>
 * All the stuff it takes to make a data file for model.
 */
public class DownloadableDataNodeImpl implements DownloadableDataNode {
    private List<DownloadableDataNode> children;
    private String text;
    private List<String> attributeNames;
    private List<String> attributeValues;
    private String location;
    private String infoLocation;
    private long size;
    private boolean _isMultifileArchive; // = false;  // SHOULD default as this until explicitly set!
    private Site site;

    /**
     * Construct implementation of downloadable data node.
     */
    public DownloadableDataNodeImpl() {
    }

    /**
     * Construct implementation of downloadable data node.  Tells what it is, some meta data,
     * and how it can be fetched.
     *
     * @param children        more such nodes, at next level down.
     * @param text            how to represent on screen or other display.  Brief.
     * @param attributeNames  what terms exist?
     * @param attributeValues what are those terms?
     * @param location        disk location.  NOT a URL, unless this is located externally on the web.
     * @param size            how big is the resource or file which this describes.
     */
    public DownloadableDataNodeImpl(
            List<DownloadableDataNode> children,
            String text,
            String[] attributeNames,
            String[] attributeValues,
            String location,
            long size) {

        this.children = children;
        this.text = text;
        this.location = location;
        this.size = size;
        setAttributes(attributeNames, attributeValues);
    }

    public boolean isMultifileArchive() {
        return _isMultifileArchive;
    }

    public void setMultifileArchive(boolean isMultifileArchive) {
        _isMultifileArchive = isMultifileArchive;
    }

    public List<DownloadableDataNode> getChildren() {
        return children;
    }

    public void setChildren(List<DownloadableDataNode> children) {
        this.children = children;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAttribute(String attributeName) {
        String returnString;
        if (attributeName != null && attributeNames != null) {
            int index = attributeNames.indexOf(attributeName);
            if (index > -1)
                returnString = attributeValues.get(index);
            else {
                returnString = "Unknown " + attributeName;
            }
        }
        else {
            returnString = "Unknown " + attributeName;
        }
        return returnString;
    }

    public void setAttributes(String[] attributeNames, String[] attributeValues) {
        // NOTE: need to turn these into lists, because I have "find-like" functionality,
        // and wish to avoid doing scans of arrays.

        if (attributeNames == null || attributeValues == null)
            throw new RuntimeException("Both attribute names and values must be non-null");

        this.attributeNames = new ArrayList<String>();
        this.attributeValues = new ArrayList<String>();
        if (attributeNames.length != attributeValues.length)
            throw new RuntimeException("Invalid length of attribute names or values.  Must match.");

        for (int i = 0; i < attributeNames.length; i++) {
            this.attributeNames.add(attributeNames[i]);
            this.attributeValues.add(attributeValues[i]);
        }
    }

    /**
     * May need to know all the attribute names, so can get all such attributes for display, etc.
     *
     * @return names of all attributes known to this Data File.
     */
    public String[] getAttributeNames() {
        if (attributeNames == null)
            return new String[0];

        String[] returnArray = new String[attributeNames.size()];
        Object[] tempArray = attributeNames.toArray();
        for (int i = 0; i < attributeNames.size(); i++) {
            returnArray[i] = tempArray[i].toString();
        }

        return returnArray;
    }

    /**
     * Location on disk, if local, or on web if at another remote site (such as PLOS).
     *
     * @return where it is.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Location on disk, of the file that describes the format of the file which is subject of
     * this node.
     *
     * @return relative location of the meta-data file about the downloaded file.
     */
    public String getInfoLocation() {
        return infoLocation;
    }

    /**
     * Allows setting of where to get info about the downloaded file's format.
     *
     * @param infoLocation where it is, relative path.
     */
    public void setInfoLocation(String infoLocation) {
        this.infoLocation = infoLocation;
    }

    /**
     * Set location on disk, if local, or on web if external.
     *
     * @param location where is it?
     */
    public void setLocation(String location) {
        this.location = location;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }
}
