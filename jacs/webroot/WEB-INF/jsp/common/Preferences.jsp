<%@ page import="org.janelia.it.jacs.model.user_data.User" %>
<%@ page import="org.janelia.it.jacs.model.user_data.prefs.UserPreference" %>
<%@ page import="org.janelia.it.jacs.web.security.JacsSecurityUtils" %>
<%@ page import="java.util.Map" %>


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

<%
    // Get the name of the preference category from the JSP param - it'll (eventually) be the name of the
    // JavaScript array holding the prefs, which will be the Dictionary name within GWT
    String prefCategoryName = request.getParameter("prefCategoryNames");
    String[] categories = prefCategoryName.split(",");
    
    // Get the user's preferences for this category from the controller and cache each one in the JS Array
    User user = JacsSecurityUtils.getSessionUser(request);
%>
    <script type="text/javascript">
        /* This array will accrue the prefs in the category specified by the calling JSP */
        var jprefs = new Array();
    </script>
<%
    for (int i=0;i<categories.length;i++) {
        Map<String, UserPreference> prefs = user.getCategoryPreferences(categories[i]);
        if (prefs != null) {%>
    <script type="text/javascript">
            jprefs["<%=categories[i]%>"]=new Array();
    </script>
<%
            for (UserPreference pref: prefs.values()) {
%>
    <script type="text/javascript">
                jprefs["<%=categories[i]%>"]["<%=pref.getName()%>"] = "<%=pref.getValue()%>";
    </script>
<%
            }
%>
<%-- Finally, we have to name the JavaScript array using the category name provided by the including JSP.--%>
<%-- We couldn't do this initially because we can't save an array element using "<%=prefsCategoryName%>[key]=value"--%>
    <script type="text/javascript">
            var <%=categories[i]%> = jprefs["<%=categories[i]%>"];
    </script>
<%      }
    }
%>

