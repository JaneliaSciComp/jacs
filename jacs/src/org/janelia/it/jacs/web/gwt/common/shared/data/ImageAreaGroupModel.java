
package org.janelia.it.jacs.web.gwt.common.shared.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cgoina
 * Date: Sep 26, 2007
 * Time: 9:13:01 AM
 */
public class ImageAreaGroupModel implements Serializable, IsSerializable {

    private String groupTips;
    private String groupTitle;
    private List<ImageAreaModel> groupedAreas;

    public ImageAreaGroupModel() {
    }

    public List<ImageAreaModel> getGroupedAreas() {
        return groupedAreas;
    }

    public void setGroupedAreas(List<ImageAreaModel> groupedAreas) {
        this.groupedAreas = groupedAreas;
    }

    public void addImageArea(ImageAreaModel imageArea) {
        groupedAreas.add(imageArea);
    }

    public String getGroupTitle() {
        return groupTitle;
    }

    public void setGroupTitle(String groupTitle) {
        this.groupTitle = groupTitle;
    }

    public String getGroupTips() {
        return groupTips;
    }

    public void setGroupTips(String groupTips) {
        this.groupTips = groupTips;
    }

}
