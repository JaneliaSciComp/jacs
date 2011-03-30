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

package org.janelia.it.jacs.web.gwt.common.shared.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Sep 26, 2007
 * Time: 9:13:01 AM
 */
public class ImageModel implements Serializable, IsSerializable {

    private String name;
    private String location;

    private List<ImageAreaModel> imageAreas;
    /**
     * groupedImageAreas field contains areas of the image that relate to the same dataset
     */
    private List<ImageAreaGroupModel> groupedImageAreas;

    private String title;

    public ImageModel() {
        imageAreas = new ArrayList<ImageAreaModel>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<ImageAreaGroupModel> getGroupedImageAreas() {
        return groupedImageAreas;
    }

    public void setGroupedImageAreas(List<ImageAreaGroupModel> groupedImageAreas) {
        this.groupedImageAreas = groupedImageAreas;
    }

    public void addImageAreaGroup(org.janelia.it.jacs.web.gwt.common.shared.data.ImageAreaGroupModel imageAreaGroup) {
        groupedImageAreas.add(imageAreaGroup);
    }

    public List<ImageAreaModel> getImageAreas() {
        return imageAreas;
    }

    public void setImageAreas(List<ImageAreaModel> imageAreas) {
        this.imageAreas = imageAreas;
    }

    public void addImageArea(org.janelia.it.jacs.web.gwt.common.shared.data.ImageAreaModel imageArea) {
        imageAreas.add(imageArea);
    }

    public String getURL() {
        return location + "/" + name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
