
package org.janelia.it.jacs.web.gwt.home.client.panel;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import org.janelia.it.jacs.web.gwt.common.client.panel.TitledBox;
import org.janelia.it.jacs.web.gwt.common.client.ui.imagebundles.ImageBundleFactory;
import org.janelia.it.jacs.web.gwt.common.client.util.HtmlUtils;
import org.janelia.it.jacs.web.gwt.common.client.util.UrlBuilder;

/**
 * @author Michael Press
 */
public class FeaturedProjectPanel extends Composite {
    public FeaturedProjectPanel() {
        init();
    }

    private void init() {
        TitledBox _titledBox = new TitledBox("Featured Project", true);
        _titledBox.setWidth("200px"); // min when iconified
        _titledBox.getContentPanel().setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
        //_titledBox.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));

        VerticalPanel panel = new VerticalPanel();
        panel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
        _titledBox.add(panel);

        final String url = UrlBuilder.getProjectsUrl() + "?projectSymbol=CAM_PROJ_MarineMicrobes";
        HTML link = HtmlUtils.getHtml("Moore Marine Microbial<br/>Sequencing Project", "wrappableTextLink");
        link.addClickListener(new ClickListener() { // allows text to wrap, unlike Link

            public void onClick(Widget sender) {
                Window.open(url, "_self", "");
            }
        });

        Image image = ImageBundleFactory.getCategoryImageBundle().getMooreProjectLogo().createImage();
        image.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                Window.open(url, "_self", "");
            }
        });
        image.setStyleName("featuredProjectImage");

        panel.add(image);
        panel.add(HtmlUtils.getHtml("&nbsp;", "smallSpacer"));
        panel.add(link);

        initWidget(_titledBox);
    }
}
