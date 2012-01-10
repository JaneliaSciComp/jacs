
package org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.ImageBundle;

/**
 * ImageBundle for small images such as controls (up/down arrows, delete images) and assorted other little
 * images (busy icon, help question mark, etc.)
 *
 * @author Michael Press
 */
public interface ControlImageBundle extends ImageBundle {
    @ImageBundle.Resource("imagebundle_images/controls/questionmark.png")
    public AbstractImagePrototype getHelpImage();

    @ImageBundle.Resource("imagebundle_images/controls/document.png")
    public AbstractImagePrototype getDocumentImage();

    @ImageBundle.Resource("imagebundle_images/controls/round-bullet.png")
    public AbstractImagePrototype getRoundBulletImage();

    @ImageBundle.Resource("imagebundle_images/controls/square-white-background.png")
    public AbstractImagePrototype getSquareBulletUnselectedImage();

    @ImageBundle.Resource("imagebundle_images/controls/square-grey-background.png")
    public AbstractImagePrototype getSquareBulletSelectedImage();

    @ImageBundle.Resource("imagebundle_images/controls/delete.png")
    public AbstractImagePrototype getDeleteImage();

    @ImageBundle.Resource("imagebundle_images/controls/delete-hover.png")
    public AbstractImagePrototype getDeleteHoverImage();

    @ImageBundle.Resource("imagebundle_images/controls/arrow-forward.png")
    public AbstractImagePrototype getArrowForwardImage();

    @ImageBundle.Resource("imagebundle_images/controls/arrow-reverse.png")
    public AbstractImagePrototype getArrowReverseImage();

    @ImageBundle.Resource("imagebundle_images/controls/arrow-up-button.png")
    public AbstractImagePrototype getArrowUpButtonImage();

    @ImageBundle.Resource("imagebundle_images/controls/arrow-up-button-hover.png")
    public AbstractImagePrototype getArrowUpButtonHoverImage();

    @ImageBundle.Resource("imagebundle_images/controls/arrow-down-button.png")
    public AbstractImagePrototype getArrowDownButtonImage();

    @ImageBundle.Resource("imagebundle_images/controls/arrow-down-button-hover.png")
    public AbstractImagePrototype getArrowDownButtonHoverImage();

    @ImageBundle.Resource("imagebundle_images/controls/arrow-left-disabled.png")
    public AbstractImagePrototype getArrowLeftDisabledImage();

    @ImageBundle.Resource("imagebundle_images/controls/arrow-left-enabled.png")
    public AbstractImagePrototype getArrowLeftEnabledImage();

    @ImageBundle.Resource("imagebundle_images/controls/arrow-right-disabled.png")
    public AbstractImagePrototype getArrowRightDisabledImage();

    @ImageBundle.Resource("imagebundle_images/controls/arrow-right-enabled.png")
    public AbstractImagePrototype getArrowRightEnabledImage();

    @ImageBundle.Resource("imagebundle_images/controls/arrow-down-enabled.gif")
    public AbstractImagePrototype getArrowDownEnabledImage();

    @ImageBundle.Resource("imagebundle_images/controls/arrow-down-disabled.png")
    public AbstractImagePrototype getArrowDownDisabledImage();

    @ImageBundle.Resource("imagebundle_images/controls/draggable5.png")
    public AbstractImagePrototype getDraggableImage();

    @ImageBundle.Resource("imagebundle_images/controls/external-link.png")
    public AbstractImagePrototype getExternalLinkImage();

    @ImageBundle.Resource("imagebundle_images/controls/info.png")
    public AbstractImagePrototype getInfoImage();

    @ImageBundle.Resource("imagebundle_images/controls/infoHover.png")
    public AbstractImagePrototype getInfoHoverImage();

    // Looks like a regular pulldown icon
    @ImageBundle.Resource("imagebundle_images/controls/pulldown.png")
    public AbstractImagePrototype getPulldownImage();

    // Looks like a regular pulldown icon except it has 2 upside-down v's
    @ImageBundle.Resource("imagebundle_images/controls/pulldown2.png")
    public AbstractImagePrototype getPulldownImage2();

    @ImageBundle.Resource("imagebundle_images/controls/pulldown2Hover.png")
    public AbstractImagePrototype getPulldownHoverImage2();

    @ImageBundle.Resource("imagebundle_images/controls/sort_asc.gif")
    public AbstractImagePrototype getSortAscendingImage();

    @ImageBundle.Resource("imagebundle_images/controls/sort_desc.gif")
    public AbstractImagePrototype getSortDescendingImage();

    @ImageBundle.Resource("imagebundle_images/controls/sort_blank.gif")
    public AbstractImagePrototype getSortBlankImage();

    @ImageBundle.Resource("imagebundle_images/controls/close.png")
    public AbstractImagePrototype getCloseImage();

    @ImageBundle.Resource("imagebundle_images/controls/close-hover.png")
    public AbstractImagePrototype getCloseHoverImage();

    @ImageBundle.Resource("imagebundle_images/controls/legend-hide.png")
    public AbstractImagePrototype getLegendHideImage();

    @ImageBundle.Resource("imagebundle_images/controls/legend-show.png")
    public AbstractImagePrototype getLegendShowImage();

    @ImageBundle.Resource("imagebundle_images/controls/export.png")
    public AbstractImagePrototype getExportImage();

    @ImageBundle.Resource("imagebundle_images/controls/annotations.png")
    public AbstractImagePrototype getAnnotationsImage();

    @ImageBundle.Resource("imagebundle_images/controls/hide.png")
    public AbstractImagePrototype getHidePanelImage();

    @ImageBundle.Resource("imagebundle_images/controls/show.png")
    public AbstractImagePrototype getShowPanelImage();

    @ImageBundle.Resource("imagebundle_images/controls/advanced-sort.png")
    public AbstractImagePrototype getAdvancedSortImage();

    @ImageBundle.Resource("imagebundle_images/controls/show-rows.png")
    public AbstractImagePrototype getShowRowsImage();

    @ImageBundle.Resource("imagebundle_images/controls/detail-view.png")
    public AbstractImagePrototype getDetailViewImage();

    @ImageBundle.Resource("imagebundle_images/controls/select-all.png")
    public AbstractImagePrototype getSelectAllImage();

    @ImageBundle.Resource("imagebundle_images/controls/select-none.png")
    public AbstractImagePrototype getSelectNoneImage();

    @ImageBundle.Resource("imagebundle_images/controls/project.png")
    public AbstractImagePrototype getProjectImage();

    @ImageBundle.Resource("imagebundle_images/controls/janelia-logo.gif")
    public AbstractImagePrototype getHeaderLogoImage();

    @ImageBundle.Resource("imagebundle_images/controls/breadcrumb-arrow.png")
    public AbstractImagePrototype getBreadcrumbAnchorImage();

    @ImageBundle.Resource("imagebundle_images/controls/link.png")
    public AbstractImagePrototype getLinkImage();

    @ImageBundle.Resource("imagebundle_images/controls/download.png")
    public AbstractImagePrototype getDownloadImage();

    @ImageBundle.Resource("imagebundle_images/controls/download-not-logged-in.png")
    public AbstractImagePrototype getDownloadNotLoggedInImage();

    @ImageBundle.Resource("imagebundle_images/controls/download-hover.png")
    public AbstractImagePrototype getDownloadHoverImage();

    @ImageBundle.Resource("imagebundle_images/controls/download-not-logged-in-hover.png")
    public AbstractImagePrototype getDownloadNotLoggedInHoverImage();

    @ImageBundle.Resource("imagebundle_images/controls/check.png")
    public AbstractImagePrototype getCheckImage();

    @ImageBundle.Resource("imagebundle_images/controls/blank.png")
    public AbstractImagePrototype getBlankImage();
}
