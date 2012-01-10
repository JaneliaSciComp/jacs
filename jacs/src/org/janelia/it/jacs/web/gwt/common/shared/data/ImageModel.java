
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
