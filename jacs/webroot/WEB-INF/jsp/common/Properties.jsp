<%@ page import="org.janelia.it.jacs.model.common.SystemConfigurationProperties" %>


<%--
  ~ Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
  ~
  ~ This file is part of JCVI VICS.
  ~
  ~ JCVI VICS is free software; you can redistribute it and/or modify it
  ~ under the terms and conditions of the Artistic License 2.0.  For
  ~ details, see the full text of the license in the file LICENSE.txt.  No
  ~ other rights are granted.  Any and all third party software rights to
  ~ remain with the original developer.
  ~
  ~ JCVI VICS is distributed in the hope that it will be useful in
  ~ bioinformatics applications, but it is provided "AS IS" and WITHOUT
  ~ ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to
  ~ implied warranties of merchantability or fitness for any particular
  ~ purpose.  For details, see the full text of the license in the file
  ~ LICENSE.txt.
  ~
  ~ You should have received a copy of the Artistic License 2.0 along with
  ~ JCVI VICS.  If not, the license can be obtained from
  ~ "http://www.perlfoundation.org/artistic_license_2_0."
  --%>

<script type="text/javascript">
        /* This array will accrue all properties from jacs.properties */
        var _system_properties = new Array();

<%
    // Get the name of the preference category from the JSP param - it'll (eventually) be the name of the
    // JavaScript array holding the prefs, which will be the Dictionary name within GWT
    // ***NOTE***: Be aware of what properties are getting shoved into the page source.
    SystemConfigurationProperties cp = SystemConfigurationProperties.getInstance();
    String key, val;
    for (Object oKey: cp.keySet())
    {
        if (oKey != null)
        {
            key = oKey.toString();
            val = cp.getProperty(key, "");
            if (key.indexOf("password")<0) {
            // save the props in the JS array
%>         _system_properties["<%=key%>"] = "<%=val.trim().replace("\\", "\\\\")%>";
<%
            }
        }
    }
%>
</script>
