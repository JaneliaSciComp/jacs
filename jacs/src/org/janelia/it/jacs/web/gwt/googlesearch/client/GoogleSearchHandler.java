
package org.janelia.it.jacs.web.gwt.googlesearch.client;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBoxBase;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;

import java.util.ArrayList;
import java.util.List;


/**
 * The class is an adapter for the Google search control. Currently the underlying
 * search control is the one from gwtwidgets package but in the future,
 * as gwt-google-apis project matures, we may decide to use that instead.
 * This adapter will isolate us from changing the web search code in too many places
 */
public class GoogleSearchHandler {

    public static final int SMALLRESULTSET = 1;
    public static final int LARGERESULTSET = 2;
    public static final int LOCALSEARCH = 0x1;
    public static final int WEBSEARCH = 0x2;
    public static final int VIDEOSEARCH = 0x4;

    private static Logger _logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.googlesearch.client.GoogleSearchHandler");

    private TextBoxBase textInput;
    private Panel brandingPanel;
    private Panel inputPanel;
    private Panel resultsPanel;
    private int resultSetSize;

    private List<String[]> searchableDomains;

    public GoogleSearchHandler(Panel resultsPanel) {
        this.resultsPanel = resultsPanel;
        searchableDomains = new ArrayList<String[]>();
    }

    public Panel getBrandingPanel() {
        return brandingPanel;
    }

    public void setBrandingPanel(Panel brandingPanel) {
        this.brandingPanel = brandingPanel;
    }

    public Panel getInputPanel() {
        return inputPanel;
    }

    public void setInputPanel(Panel inputPanel) {
        this.inputPanel = inputPanel;
    }

    public TextBoxBase getTextInput() {
        return textInput;
    }

    public void setTextInput(TextBoxBase textInput) {
        this.textInput = textInput;
    }

    public int getResultSetSize() {
        return resultSetSize;
    }

    public void setResultSetSize(int resultSetSize) {
        this.resultSetSize = resultSetSize;
    }

    public Panel getResultsPanel() {
        return resultsPanel;
    }

    public void setResultsPanel(Panel resultsPanel) {
        this.resultsPanel = resultsPanel;
    }

    public void addSearchableDomains(String label, String domain) {
        if (domain != null && domain.length() > 0) {
            searchableDomains.add(new String[]{
                    label,
                    domain
            });
        }
    }

    public void executeSearch(String searchId, String query, int whatToSearch) {
//        long t = System.currentTimeMillis();
//        if(brandingPanel != null) {
//            Element brandingElement;
//            String brandingId = "VICSGoogleBranding" + "-" + String.valueOf(t);
//            brandingPanel.add(new HTML("<div id='" + brandingId + "'/>"));
//            brandingElement = DOM.getElementById(brandingId);
//            GSearch.getBranding(brandingElement,GSearch.HORIZONTAL_BRANDING);
//        }
//        GSearchControl searchControl =  GSearchControl.create();
//        if((whatToSearch & LOCALSEARCH) != 0) {
//            searchControl.addSearcher(GlocalSearch.create());
//        }
//        if((whatToSearch & WEBSEARCH) != 0) {
//            if(searchableDomains.size() == 0) {
//                searchControl.addSearcher(GwebSearch.create());
//            } else {
//                for(Iterator domainItr = searchableDomains.iterator();domainItr.hasNext();) {
//                    String[] searchableDomain = (String[])domainItr.next();
//                    String domainLabel = searchableDomain[0];
//                    if(domainLabel == null) {
//                        domainLabel = searchableDomain[1];
//                    }
//                    GwebSearch webSearcher = GwebSearch.create();
//                    GsearcherOptions webSearcherOptions = new GsearcherOptions();
//                    webSearcherOptions.setNoResultsString("No result found");
//                    webSearcher.setUserDefinedLabel(domainLabel);
//                    webSearcher.setSiteRestriction(searchableDomain[1]);
//                    searchControl.addSearcher(webSearcher,webSearcherOptions);
//                }
//            }
//        }
//        if((whatToSearch & VIDEOSEARCH) != 0) {
//            searchControl.addSearcher(GvideoSearch.create());
//        }
//        GdrawOptions searchDrawOptions = new GdrawOptions();
//        // input panel and input seem to be exclusive
//        // so if the inputPanel is set don't attempt to set the input at all
//        if(inputPanel != null) {
//            Element searchFormElement;
//            String formId = "VICSGoogleSearchInputForm" + "-" + String.valueOf(t);
//            inputPanel.add(new HTML("<div id='" + formId + "'/>"));
//            searchFormElement = DOM.getElementById(formId);
//            searchDrawOptions.setSearchFormRootElement(searchFormElement);
//        } else {
//            if(textInput == null) {
//                searchDrawOptions.setInput(DOM.createDiv());
//            } else {
//                searchDrawOptions.setInput(textInput.getElement());
//            }
//        }
//        if(resultSetSize == LARGERESULTSET) {
//            searchControl.setResultSetSize(GSearch.LARGE_RESULTSET);
//        } else {
//            searchControl.setResultSetSize(GSearch.SMALL_RESULTSET);
//        }
//        searchDrawOptions.setDrawMode(GSearchControl.DRAW_MODE_TABBED);
//        /* IE doesn't seem to work if we use resultsPanel.getElement()
//         * so instead we add a DIV element to the results panel and
//         * use that one instead of resultsPanel's element
//         */
//        Element searchResultsElement;
//        String resId = "VICSGoogleSearchResults" + "-" + String.valueOf(t);
//        resultsPanel.add(new HTML("<div id='" + resId + "'/>"));
//        searchResultsElement = DOM.getElementById(resId);
//        searchControl.draw(searchResultsElement,searchDrawOptions);
//        searchControl.execute(query);
    }

}
