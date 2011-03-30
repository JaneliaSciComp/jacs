<%@ page import="org.janelia.it.jacs.model.user_data.User" %>
<%@ page import="org.janelia.it.jacs.web.security.JacsSecurityUtils" %>
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

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>Prokaryotic Annotation</title>
    <meta name='gwt:module' content='org.janelia.it.jacs.web.gwt.prokAnnot.ProkAnnot'>
    <%
        User user = JacsSecurityUtils.getSessionUser(request);
    %>
    <jsp:include page="/WEB-INF/jsp/common/GWTIncludes.jsp"/>
    <jsp:include page="/WEB-INF/jsp/common/Preferences.jsp">
        <jsp:param name="prefCategoryNames" value="tasks,BasePaginatorRowsPerPage,ProkPipeline"/>
    </jsp:include>
    <script type="text/javascript">
        var tmpUsername = "<%=user.getUserLogin()%>";
    </script>
</head>
    <body>
        <script language="javascript" src="/jacs/gwt/ProkAnnot/ProkAnnot.nocache.js"></script>
        <jsp:include page="/WEB-INF/jsp/common/BasePageBody.jsp"/>
    </body>
</html>
