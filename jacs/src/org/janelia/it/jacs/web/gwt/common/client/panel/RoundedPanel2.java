
package org.janelia.it.jacs.web.gwt.common.client.panel;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.Iterator;

/**
 * <p>A Panel to create rounded corners similar to other known Google(tm)
 * applications. Basically the html of the rounded corners looks as follows
 * (with some extra styling to make it really work):</p>
 * <pre>
 * &lt;div&gt
 *      &lt;div style="margin:0 2px"&gt;&lt;/div&gt;
 *      &lt;div style="margin:0 1px"&gt;&lt;/div&gt;
 *      &lt;div&gt;your widget&lt;/div&gt;
 *      &lt;div style="margin:0 1px"&gt;&lt/div&gt;
 *      &lt;div style="margin:0 2px"&gt;&lt/div&gt;
 * &lt;/div&gt;
 * </pre>
 * <p>
 * Use the class as follows:</p>
 * <p>Create panel with all corners rounded:</p>
 * <pre>
 *     // all 4 corners are rounded.
 *     org.janelia.it.jacs.web.gwt.frv.user.panels.RoundedPanel2 rp = new org.janelia.it.jacs.web.gwt.frv.user.panels.RoundedPanel2();
 * </pre>
 * <p>or with custom set corners, like only on the left</p>
 * <pre>
 *     // custom set corners
 *     org.janelia.it.jacs.web.gwt.frv.user.panels.RoundedPanel2 rp = new org.janelia.it.jacs.web.gwt.frv.user.panels.RoundedPanel2(corners);
 * </pre>
 * <p/>
 * <p>By default the classname assigned to the rounded corners is "rp".
 * You need this to set the color of the rounded corners to match the rest
 * of you widget. e.g. <code>.rp { background-color:#c3d9ff; }</code>
 * If you want another class name, use the method <code>setLineStyleName</code>.
 * </p>
 *
 * @author Hilbrand Bouwkamp(hs@bouwkamp.com)
 * @version 1.0
 */
public class RoundedPanel2 extends ComplexPanel {
    /**
     * <code>TOPLEFT</code> top left rounded corner
     */
    public final static int TOPLEFT = 1;
    /**
     * <code>TOPRIGHT</code> top right rounded corner
     */
    public final static int TOPRIGHT = 2;
    /**
     * <code>BOTTOMLEFT</code> bottom left rounded corner
     */
    public final static int BOTTOMLEFT = 4;
    /**
     * <code>BOTTOMRIGHT</code> bottom right rounded corner
     */
    public final static int BOTTOMRIGHT = 8;
    /**
     * <code>BOTTOM</code> rounded corners at the top
     */
    public final static int TOP = TOPLEFT | TOPRIGHT;
    /**
     * <code>TOP</code> rounded corners at the bottom
     */
    public final static int BOTTOM = BOTTOMLEFT | BOTTOMRIGHT;
    /**
     * <code>LEFT</code> rounded corners on the left side
     */
    public final static int LEFT = TOPLEFT | BOTTOMLEFT;
    /**
     * <code>RIGHT</code> rounded corners on the right side
     */
    public final static int RIGHT = TOPRIGHT | BOTTOMRIGHT;
    /**
     * <code>ALL</code> rounded corners on all sides
     */
    public final static int ALL = TOP | BOTTOM;

    // private Element variables
    private Element body;  // body of widget
    private Element div5t; // margin 5 top line
    private Element div4t; // margin 4 top line
    private Element div3t; // margin 3 top line
    private Element div2t; // margin 2 top line
    private Element div1t; // margin 1 top line
    private Element divElement; // div element containing widget
    private Element div1b; // margin 1 bottom line
    private Element div2b; // margin 2 bottom line
    private Element div3b; // margin 3 bottom line
    private Element div4b; // margin 4 bottom line
    private Element div5b; // margin 5 bottom line
    public static final int ROUND_SMALL = 2;
    public static final int ROUND_MEDIUM = 3;
    public static final int ROUND_LARGE = 5;

    private final static String RPSTYLE = "rp";
    private String _cornerStyleName = RPSTYLE;


    public RoundedPanel2(Widget w, int corners) {
        this(w, corners, null);
    }

    public RoundedPanel2(Widget w, int corners, String borderColor) {
        this(w, corners, borderColor, ROUND_LARGE);
    }

    public RoundedPanel2(Widget w, int corners, String borderColor, int roundSize) {
        this(w, corners, borderColor, roundSize, null);
    }

    /**
     * <p>Creates a new <code>org.janelia.it.jacs.web.gwt.frv.user.panels.RoundedPanel2</code> with custom rounded corners on
     * the given widget <code>w</code>. Every combination of corners can be set
     * via the <code>corners</code> argument. Use the static constants to set
     * the corners. For example if you want to create a panel with only rounded
     * corners at the left, use:<br>
     * <code>new org.janelia.it.jacs.web.gwt.frv.user.panels.RoundedPanel2(yourWidget, org.janelia.it.jacs.web.gwt.frv.user.panels.RoundedPanel2.LEFT);</code></p>
     *
     * @param corners     set custom rounded corners.
     * @param w           widget to add corners to.
     * @param borderColor optional border color
     * @param roundSize   RoundedPanel2.ROUND_SMALL or RoundedPanel2.ROUND_LARGE
     */
    public RoundedPanel2(Widget w, int corners, String borderColor, int roundSize, Widget title) {
        super();
        init(w, corners, borderColor, roundSize, title);
    }

    private void init(Widget w, int corners, String borderColor, int roundSize, Widget title) {
        body = DOM.createDiv();

        if (title != null) {
            Element titleDiv = DOM.createDiv();
            super.add(title, titleDiv);
            DOM.appendChild(body, titleDiv);
        }

        if (is(corners, TOP)) {
            if (roundSize == ROUND_SMALL) {
                div2t = createLine(corners & TOP, '2', borderColor);
                div1t = createLine(corners & TOP, '1', '1', borderColor);
            }
            if (roundSize == ROUND_MEDIUM) {
                div4t = createLine(corners & TOP, '4', borderColor);
                div3t = createLine(corners & TOP, '2', '2', borderColor);
                div2t = createLine(corners & TOP, '1', '1', borderColor);
                div1t = createLine(corners & TOP, '1', '1', borderColor);
            }
            else {
                div5t = createLine(corners & TOP, '5', borderColor);
                div4t = createLine(corners & TOP, '3', '2', borderColor);
                div3t = createLine(corners & TOP, '2', '1', borderColor);
                div2t = createLine(corners & TOP, '1', '1', borderColor);
                div1t = createLine(corners & TOP, '1', '1', borderColor);
            }
            if (null != div5t) DOM.appendChild(body, div5t);
            if (null != div4t) DOM.appendChild(body, div4t);
            if (null != div3t) DOM.appendChild(body, div3t);
            if (null != div2t) DOM.appendChild(body, div2t);
            if (null != div1t) DOM.appendChild(body, div1t);
        }

        divElement = DOM.createDiv();
        DOM.appendChild(body, divElement);

        if (is(corners, BOTTOM)) {
            if (roundSize == ROUND_SMALL) {
                div1b = createLine(corners & BOTTOM, '1', '1', borderColor);
                div2b = createLine(corners & BOTTOM, '2', borderColor);
            }
            else if (roundSize == ROUND_MEDIUM) {
                div1b = createLine(corners & BOTTOM, '1', '1', borderColor);
                div2b = createLine(corners & BOTTOM, '1', '1', borderColor);
                div3b = createLine(corners & BOTTOM, '2', '2', borderColor);
                div4b = createLine(corners & BOTTOM, '4', borderColor);
            }
            else {
                div1b = createLine(corners & BOTTOM, '1', '1', borderColor);
                div2b = createLine(corners & BOTTOM, '1', '1', borderColor);
                div3b = createLine(corners & BOTTOM, '2', '1', borderColor);
                div4b = createLine(corners & BOTTOM, '3', '2', borderColor);
                div5b = createLine(corners & BOTTOM, '5', borderColor);
            }
            if (null != div1b) DOM.appendChild(body, div1b);
            if (null != div2b) DOM.appendChild(body, div2b);
            if (null != div3b) DOM.appendChild(body, div3b);
            if (null != div4b) DOM.appendChild(body, div4b);
            if (null != div5b) DOM.appendChild(body, div5b);
        }

        setCornerStyleName(RPSTYLE);
        setElement(body);
        add(w);
    }

    /**
     * <p>Creates a new <code>org.janelia.it.jacs.web.gwt.frv.user.panels.RoundedPanel2</code> with all corners rounded on
     * the given widget <code>w</code>.</p>
     *
     * @param w widget to add corners to.
     */
    public RoundedPanel2(Widget w) {
        this(w, ALL);
    }

    private Element createLine(int corner, char width, String bgColor) {
        Element element = createLine(corner, width);
        DOM.setStyleAttribute(element, "backgroundColor", bgColor);
        return element;
    }

    private Element createLine(int corner, char width, char borderWidth, String borderColor) {
        Element element = createLine(corner, width, null);
//        System.out.println("borderColor=" + borderColor);
//        System.out.println("cornerStyleName=" + _cornerStyleName);
//        System.out.println("border-color=" + DOM.getStyleAttribute(div1t, "borderColor"));
        if (borderColor != null) {
            DOM.setStyleAttribute(element, "borderLeft", borderWidth + "px solid " + borderColor);
            DOM.setStyleAttribute(element, "borderRight", borderWidth + "px solid " + borderColor);
        }
        return element;
    }

    /**
     * <p>Creates div element representing part of the rounded corner.</p>
     *
     * @param corner corner mask to set rounded corner.
     * @param width  margin width for line.
     * @return div
     */
    private Element createLine(int corner, char width) {
        // margin 2 fields : top/bottom right/left  => "0 <width>px"
        // margin 4 fields : top right bottom left  => "0 <width>px 0 <width>px"
        String margin;
        if (corner == TOP || corner == BOTTOM)
            margin = "0 " + width + "px";
        else if (corner == LEFT)
            margin = "0 0 0 " + width + "px";
        else
            margin = "0 " + width + "px 0 0";

        Element div = DOM.createDiv();
        DOM.setStyleAttribute(div, "fontSize", "0px");
        DOM.setStyleAttribute(div, "height", "1px");
        DOM.setStyleAttribute(div, "lineHeight", "1px");
        DOM.setStyleAttribute(div, "margin", margin);
        DOM.setInnerHTML(div, "&nbsp;");

        return div;
    }

    // convience method for mask test
    private boolean is(int input, int mask) {
        return (input & mask) > 0;
    }

    /**
     * <p>Set the style of the org.janelia.it.jacs.web.gwt.frv.user.panels.RoundedPanel2. In most cases this is not necessary
     * and setting the style on the widget to which the <code>org.janelia.it.jacs.web.gwt.frv.user.panels.RoundedPanel2</code>
     * is applied should be set, as is done when not using the org.janelia.it.jacs.web.gwt.frv.user.panels.RoundedPanel2</code>
     * </p>
     *
     * @param style css style name
     */
    public void setStyleName(String style) {
        DOM.setElementProperty(body, "className", style);
    }

    /**
     * <p>Set the css style name of the rounded corners div's. Default the css stylename
     * is <code>rp</code>. Use it to set the colors of the corner. For example:
     * <code>.rp { background-color:#c3d9ff; }</code>.</p>
     * <p>A custom style might be needed when the corners are visible only when a panel
     * is selected. Use this method to set the custom style name and add something
     * like the following to the stylesheet:<br>
     * .selected .rp { background-color:#c3d9ff; }
     * </p>
     *
     * @param style css style name.
     */
    public void setCornerStyleName(String style) {
//        setDivCornerStyleName(div5t, style);
//        setDivCornerStyleName(div4t, style);
//        setDivCornerStyleName(div3t, style);
//        setDivCornerStyleName(div2t, style);
//        setDivCornerStyleName(div1t, style);
//        setDivCornerStyleName(div1b, style);
//        setDivCornerStyleName(div2b, style);
//        setDivCornerStyleName(div3b, style);
//        setDivCornerStyleName(div4b, style);
//        setDivCornerStyleName(div5b, style);
        _cornerStyleName = style;
        if (null != div5t) DOM.setElementProperty(div5t, "className", style);
        if (null != div4t) DOM.setElementProperty(div4t, "className", style);
        if (null != div3t) DOM.setElementProperty(div3t, "className", style);
        if (null != div2t) DOM.setElementProperty(div2t, "className", style);
        if (null != div1t) DOM.setElementProperty(div1t, "className", style);
        if (null != div1b) DOM.setElementProperty(div1b, "className", style);
        if (null != div2b) DOM.setElementProperty(div2b, "className", style);
        if (null != div3b) DOM.setElementProperty(div3b, "className", style);
        if (null != div4b) DOM.setElementProperty(div4b, "className", style);
        if (null != div5b) DOM.setElementProperty(div5b, "className", style);
    }
//
//    private void setDivCornerStyleName(Element element, String style)
//    {
//        // TODO: get border color from style name
//        if(element != null) {
//            DOM.setElementProperty(element, "className", style);
//            DOM.setStyleAttribute(element, "borderLeft", "1px solid #FFE999");
//            DOM.setStyleAttribute(element, "borderRight", "1px solid #FFE999");
//        }
//    }

    /**
     * <p>Sets the Widget to which the rounded corners will be added. The
     * constructor will call this method. But can be used when Widget to
     * which the rounded corners are applied needs to be changed.
     * Normally it is probably simpler to create a new
     * <code>org.janelia.it.jacs.web.gwt.frv.user.panels.RoundedPanel2</code>.</p>
     *
     * @param w widget to apply the rounded corners to.
     */
    public void add(Widget w) {
//    	if(DOM.getChildCount(divElement) == 0) {
//            DOM.appendChild(divElement, w.getElement());
//            super.add(w);
//        }
        super.add(w, divElement);
    }

    /**
     * Removes widget to which the corners are set.
     */
    public void clear() {
        super.clear();
    }

    /**
     * <p>Removes the widget to which the rounded corners have been added.</p>
     *
     * @param w widget to which the rounded corners are applied.
     * @return true is removal was succesfull
     */
    public boolean remove(Widget w) {
        if (w.getParent() != this)
            return false;

        DOM.removeChild(divElement, w.getElement());
        return super.remove(w);
    }

    /**
     * <p>Iterator. Not really used, since only one widget is available.
     * But this method comes with the <code>ComplexPanel</code></p>
     *
     * @return returns Iterator with one element, the widget to which the
     *         rounded corners are applied.
     */
	public Iterator iterator() {
		return super.iterator();
	}
}
