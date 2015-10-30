<%@ page import="org.janelia.it.jacs.compute.web.MetaGenoPipeController" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
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
<html xmlns="http://www.w3.org/1999/xhtml">
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
<link href="MetaGenoPipeStyle.css" rel="stylesheet" type="text/css" />

<%
    String STATUS_KEY = "status";
    String STATUS_TYPE_NODE = "node";
    String STATUS_TYPE_ORF = "orf";
    String STATUS_TYPE_ANNO = "anno";
    String STATUS_TYPE_ALL = "all";

    Long inputNodeId=0L;
    Long taskId=0L;
    String statusType=request.getParameter(STATUS_KEY);
    if (statusType.equals(STATUS_TYPE_NODE)) {
        inputNodeId = (Long)request.getSession().getAttribute(MetaGenoPipeController.SESSION_INPUT_NODE_ID);
    } else if (statusType.equals(STATUS_TYPE_ORF)) {
        taskId = (Long)request.getSession().getAttribute(MetaGenoPipeController.SESSION_MG_ORF_TASK_ID);
    } else if (statusType.equals(STATUS_TYPE_ANNO)) {
        taskId = (Long)request.getSession().getAttribute(MetaGenoPipeController.SESSION_MG_ANNO_TASK_ID);
    }
%>

<head>
<title>J. Craig Venter Institute</title>
</head>

<body>

<div id="title_section">
    Metagenomics Pipeline Status
</div>

<% if (statusType.equals(STATUS_TYPE_NODE)) { %>
<div class="top_submit">
    FastaFileNode created. Node Id=<%=inputNodeId%>
</div>
<% } else if (statusType.equals(STATUS_TYPE_ORF)) { %>
<div class="top_submit">
    Metagenomic ORF Calling Pipeline started. Task Id=<%=taskId%>
</div>
<% } else if (statusType.equals(STATUS_TYPE_ANNO)) { %>
<div class="top_submit">
    Metagenomic Annotation Pipeline started. Task Id=<%=taskId%>
</div>
<% } else if (statusType.equals(STATUS_TYPE_ALL)) { %>

<div class="top_submit">
    Metagenomics Pipeline Status Table (return to <a href="/compute/MetaGenoPipeHome">home</a> page)
</div>

<table class="submit_example_table" cellspacing="5" cellpadding="5" border="0">
    <col width="800"/>
    <tbody>
        <tr>
            <td>Example upload file: /usr/local/annotation/METAGENOMIC/BENCHMARK/bearpaw_reads.fasta</td>
        </tr>
    </tbody>
</table>

<% } else { %>
<div class="top_submit">
    Do not understand status request
</div>
<% } %>

</body>
</html>