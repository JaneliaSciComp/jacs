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

package org.janelia.it.jacs.web.gwt.common.client.util;

import com.google.gwt.http.client.URL;

import java.util.HashMap;
import java.util.Map;

/**
 * The class implements functions to extract certain components from the URL
 * In some sense the same functionality is provided in gwtwidgets.Location class
 * but Location requires the query to be set, which is a protected method
 * so we would have to extend the class if we need its functionality for a given URL
 *
 * @author Cristian Goina
 */
public class URLUtils {

    public static String addParameter(String url, String parameter, String value) {
        int requestArgsStartPos = -1;
        if (url != null) {
            requestArgsStartPos = url.indexOf('?');
        }
        char argSeparator = '&';
        if (requestArgsStartPos == -1) {
            // if this is the first argument
            argSeparator = '?';
        }
        StringBuffer urlBuffer = new StringBuffer(url);
        urlBuffer.append(argSeparator);
        urlBuffer.append(parameter);
        if (value != null && value.length() > 0) {
            urlBuffer.append('=');
            urlBuffer.append(URL.encodeComponent(value));
        }
        return urlBuffer.toString();
    }

    /**
     * Returns the value of the specified parameter from an encoded URL
     *
     * @param url       the encoded URL from which to extract the parameter
     * @param parameter to be extracted from the URL
     * @return parameter's value or null if no parameter was found in the URL
     *         if the request arguments are "?arg1=val1&arg2" and the value of
     *         arg2 is requested the method returns arg2
     */
    public static String getParameterValue(String url, String parameter) {
        String paramValue;
        Map requestParams = parseRequestArgs(url);
        paramValue = (String) requestParams.get(parameter);
        if (paramValue == null && requestParams.containsKey(parameter)) {
            paramValue = parameter;
        }
        return paramValue;
    }

    private static Map parseRequestArgs(String url) {
        Map<String, String> requestArgs = new HashMap<String, String>();
        int requestArgsStartPos = -1;
        if (url != null) {
            requestArgsStartPos = url.indexOf('?');
        }
        if (requestArgsStartPos != -1) {
            int parsePos = requestArgsStartPos + 1;
            String requestArgsURL = url.substring(parsePos);
            do {
                parsePos = requestArgsURL.indexOf('&');
                String requestArg;
                if (parsePos != -1) {
                    requestArg = requestArgsURL.substring(0, parsePos);
                    requestArgsURL = requestArgsURL.substring(parsePos + 1);
                }
                else {
                    requestArg = requestArgsURL;
                }
                int assignPos = requestArg.indexOf('=');
                String param;
                String value = null;
                if (assignPos != -1) {
                    param = URL.decodeComponent(requestArg.substring(0, assignPos));
                    value = URL.decodeComponent(requestArg.substring(assignPos + 1));
                }
                else {
                    param = URL.decodeComponent(requestArg);
                }
                requestArgs.put(param, value);
            }
            while (parsePos != -1);
        }
        return requestArgs;
    }

}
