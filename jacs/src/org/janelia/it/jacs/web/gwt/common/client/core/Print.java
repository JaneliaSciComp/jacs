
package org.janelia.it.jacs.web.gwt.common.client.core;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.UIObject;

/**
 * <pre>
 * Generic printing class can be used to print the Window itself, DOM.Elements, UIObjects
 * (Widgets) and plain HTML
 * <p/>
 * Usage:
 *      You must insert this iframe in your host page:
 *              <iframe id="__printingFrame" style="width:0;height:0;border:0"></iframe>
 * <p/>
 *      Window:
 *              Print.it();
 * <p/>
 *      Objects/HTML:
 *              Print.it(RootPanel.get("myId"));
 *              Print.it(DOM.getElementById("myId"));
 *              Print.it("Just <b>Print.it()</b>!");
 * <p/>
 *      Objects/HTML using styles:
 *              Print.it("<link rel='StyleSheet' type='text/css' media='paper' href='/paperStyle.css'>", RootPanel.get('myId'));
 *              Print.it("<style type='text/css' media='paper'> .newPage { page-break-after: always; } </style>",
 *                 "Hi<p class='newPage'></p>By");
 * </pre>
 */
public class Print {

    public static native void it() /*-{
        $wnd.print();
    }-*/;

    public static native void it(String html) /*-{
        var frame = $doc.getElementById('__printingFrame');
        if (!frame) {
            $wnd.alert("Error: Can't find printing frame.");
            return;
        }
        frame = frame.contentWindow;
        var doc = frame.document;
        doc.open();
        doc.write(html);
        doc.close();
        frame.focus();
        frame.print();
    }-*/;

    public static void it(UIObject obj) {
        it("", obj.getElement().toString());
    }

    public static void it(Element element) {
        it("", element.toString());
    }

    public static void it(String style, String it) {
        it("<html><header>" + style + "</header><body>" + it + "</body></html>");
    }

    public static void it(String style, UIObject obj) {
        it(style, obj.getElement().toString());
    }

    public static void it(String style, Element element) {
        it(style, element.toString());
    }
}
