<%@ page import="org.janelia.it.jacs.web.control.report.BlastTaskReportController" %>
<%@ page import="org.janelia.it.jacs.model.tasks.blast.BlastTask" %>

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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"  %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"  %>
<html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <title>Task Report</title>
        <link type="text/css" rel="stylesheet" href="/jacs/css/jacs.css"/>
        <link rel="shortcut icon" href="/jacs/favicon.ico"/>
       <style type="text/css">
            .errorLink {
                    color: red;
                    font-style: normal;
                    text-decoration: underline;
                    cursor: pointer;
                    white-space: nowrap;
           }
       </style>

        <script type="text/javascript" language="JavaScript">
            function showHideMonth(selector)
            {
                var selectedValue = selector.options[selector.selectedIndex].value;
                var monthBlock = document.getElementById("monthEntryFields");
                if (selectedValue == "calendar month")
                    monthBlock.style.display = 'block';
                else
                    monthBlock.style.display = 'none';
            }

            function showTaskDetails(taskId, taskName, details)
            {
                var allParams = details.split(';');

                var w = window.open('',taskId + '_taskDetail',
                    'width=300,height=600,toolbar=0,resizable=1,location=0,menubar=0,directories=0');
                w.document.writeln('<p><b>Job  Details</b> ('+ taskId + ')</p>');
                w.document.writeln('Task type: '+ taskName + '<br>');
                w.document.writeln('<table border="1"><caption>Parameters</caption>');
                var oneParam;
                for (var i = 0; i < allParams.length; i++)
                {
                    oneParam=allParams[i].split('=');
                    w.document.writeln('<tr><td>'+oneParam[0]+'</td><td>'+oneParam[1] + '</td></tr>');
                }
                w.document.writeln('</table');
                w.document.close();
            }

            function toggleErrorText(errBlockId)
            {
                var errBlock=document.getElementById(errBlockId);
                showOrHideError(errBlock, (errBlock.style.display == 'none'));
            }

            function showOrHideError(errBlock, show)
            {
                if (show)
                    errBlock.style.display = 'block';
                else
                    errBlock.style.display = 'none';
            }

            function hideAllErrors()
            {
                showHideAllErrors(false);
            }
            function showAllErrors()
            {
                showHideAllErrors(true);
            }

            function showHideAllErrors(show)
            {
                var errBlocks=document.getElementsByName("errorElement");
                for(var i=0;i<errBlocks.length;i++)
                {
                    showOrHideError(errBlocks[i], show);
                }
            }
        </script>
        <jsp:include page="/WEB-INF/jsp/common/HeaderHead.jsp"/>
    </head>
    <body>
        <jsp:include page="/WEB-INF/jsp/common/HeaderBody.jsp"/>
        <div id="page_wrapper" style="display:none">
            <h1>Blast Task Report</h1>
            <form name="taskReportForm" action="/jacs/admin/blastTaskReport.htm" method="post">
                <table cellspacing="10" cellpadding="10" border="0">
                    <tbody>
                        <tr>
                            <td>Time period
                                <select name="period" onchange="showHideMonth(this)">
                                    <c:forEach items="${allPeriods}" var="curPeriod">
                                        <option value="${curPeriod}"
                                            <c:if test="${curPeriod == period}"> selected</c:if> >
                                            <c:out value="${curPeriod}"/>
                                        </option>
                                    </c:forEach>
                                </select>
                            </td>
                            <td>
                            <div id="monthEntryFields">
                                <select name="selectedMonth" >
                                    <option value="0" <c:if test="${curMonth == 0}">selected</c:if> >January</option>
                                    <option value="1" <c:if test="${curMonth == 1}">selected</c:if> >February</option>
                                    <option value="2" <c:if test="${curMonth == 2}">selected</c:if> >March</option>
                                    <option value="3" <c:if test="${curMonth == 3}">selected</c:if> >April</option>
                                    <option value="4" <c:if test="${curMonth == 4}">selected</c:if> >May</option>
                                    <option value="5" <c:if test="${curMonth == 5}">selected</c:if> >June</option>
                                    <option value="6" <c:if test="${curMonth == 6}">selected</c:if> >July</option>
                                    <option value="7" <c:if test="${curMonth == 7}">selected</c:if> >August</option>
                                    <option value="8" <c:if test="${curMonth == 8}">selected</c:if> >September</option>
                                    <option value="9" <c:if test="${curMonth == 9}">selected</c:if> >October</option>
                                    <option value="10" <c:if test="${curMonth == 10}">selected</c:if> >November</option>
                                    <option value="11" <c:if test="${curMonth == 11}">selected</c:if> >December</option>
                                </select>&nbsp;
                                <input name="selectedYear" value='<fmt:formatDate value="${now}" pattern="yyyy" />' type="text" size="4" />&nbsp;
                            </div></td>
                            <td><input type="checkbox" name="includeSystem"  <c:if test="${includeSystem == true}">checked</c:if>/>
                                Include system tasks</td>
                            <td><input type="submit" value="Submit"/></td>
                        </tr>
                    </tbody>
                </table>
            <hr>
                Report ran for ${reportDuration} seconds |
                <input type="button" value="Show error text" onclick="showAllErrors()"/> |
                <input type="button" value="Hide error text" onclick="hideAllErrors()"/>
                <br/>

                <h3>Report Statistics</h3>
                <table cellspacing="0" cellpadding="5" border="1">
                    <tbody>
                        <tr>
                            <td><b>Total records found</b></td>
                            <td><b>Completed tasks</b></td>
                            <td><b>Avg Duration<br/>(min.sec)</b></td>
                            <td><b>Avg Query Size</b></td>
                            <td><b>Avg Query Cnt<br/>(min.sec)</b></td>
                            <td><b>Avg Hits found</b></td>
                        </tr>
                        <tr>
                            <td>${fn:length(taskList)}</td>
                            <td>${stats.taskCount}</td>
                            <td>${stats.avgTaskDuration}</td>
                            <td>${stats.avgQuerySize}</td>
                            <td>${stats.avgNumOfSeqs}</td>
                            <td>${stats.avgNumHits}</td>
                         </tr>
                    </tbody>
                </table>
                <hr>
                <h3>Report</h3>
                <table cellspacing="0" cellpadding="5" border="1">
                    <tbody>
                        <tr>
                            <td><b>Task ID</b></td>
                            <td><b>User</b></td>
                            <td><b>Status</b></td>
                            <td><b>Start</b></td>
                            <td><b>Duration<br/>(min.sec)</b></td>
                            <td><b>Type</b></td>
                            <td><b>Query ID</b></td>
                            <td><b>Query Cnt</b></td>
                            <td><b>Query Size</b></td>
                            <td><b>Subject Datasets</b></td>
                            <td><b>e-value</b></td>
                            <td><b>Hits requested</b></td>
                            <td><b>Hits found</b></td>
                        </tr>
                        <%-- get some static vaues --%>
                        <c:set var="PARAM_evalue_Key" value="<%=BlastTask.PARAM_evalue %>" />
                        <c:set var="PARAM_databaseAlignments_Key" value="<%=BlastTask.PARAM_databaseAlignments %>" />
                        <c:forEach var="taskInfo" items="${taskList}" >
                            <tr>
                                <td>${taskInfo.taskID}</td>
                                <td>${taskInfo.taskOwner}</td>
                                <!-- status -->
                                <td style="font-weight: bold;">
                                <c:choose>
                                    <c:when test="${taskInfo.lastStatus.eventType eq 'completed'}">
                                        <span style="color:green;" >completed</span>
                                    </c:when>
                                    <c:when test="${taskInfo.lastStatus.eventType eq 'error'}">
                                        <span class="errorLink" onclick="toggleErrorText('${taskInfo.taskID}')">error</span>
                                        <div id="${taskInfo.taskID}" name="errorElement" style="display: none">
                                            <br>
                                        <c:out value="${taskInfo.lastStatus.description}" />
                                        </div>
                                    </c:when>
                                    <c:otherwise>
                                        <span style="color:black; ">${taskInfo.lastStatus.eventType}</span>
                                    </c:otherwise>
                                </c:choose>
                                    <c:if test="${taskInfo.lastEventType eq 'deleted'}">
                                        <br>(deleted)
                                    </c:if>
                                </td>

                                <!-- start time-->
                                <td><pre xml:space="preserve"><fmt:formatDate pattern="MMM d, kk:mm" value="${taskInfo.startTime}"/></pre></td>

                                <!-- duration -->
                                <td><c:out value="${taskInfo.taskDuration}" /></td>

                                <!-- type of task -->
                                <c:choose>
                                    <c:when test="${fn:length(taskInfo.parameters) > 0}">
                                        <td><a href="javascript:showTaskDetails(${taskInfo.taskID}, '${taskInfo.taskName}', '${taskInfo.taskParams}')" title="Show task details">${taskInfo.taskName}</a></td>
                                    </c:when>
                                    <c:otherwise>
                                        <td>${taskInfo.taskName}</td>
                                    </c:otherwise>
                                </c:choose>

                                <!-- Quey info -->
                                <c:choose>
                                <c:when test="${taskInfo.queryNode != null}">
                                    <!-- Query ID -->
                                    <td>${taskInfo.queryNode.databaseObjectId}</td>

                                    <!-- Query Cnt -->
                                    <td>${taskInfo.queryNode.sequenceCount}</td>

                                    <!-- Query Size -->
                                    <td>${taskInfo.queryNode.sequenceLength}</td>
                                </c:when>
                                <c:otherwise>
                                    <td>&nbsp;</td>
                                    <td>&nbsp;</td>
                                    <td>&nbsp;</td>
                                </c:otherwise>
                                </c:choose>
                                <!-- Subject Datasets -->
                                <td>
                                <c:forEach var="subjectNode" items="${taskInfo.subjectNodes}" varStatus="cnt">
                                    <c:if test="${cnt.count > 1}"><br></c:if>
                                    <pre xml:space="preserve">${subjectNode}</pre>
                                </c:forEach>

                                <!-- e-value -->
                                <td><c:out value='${taskInfo.parameters[PARAM_evalue_Key]}' /></td>

                                <!-- Hits requested -->
                                <td><c:out value='${taskInfo.parameters[PARAM_databaseAlignments_Key]}' /></td>

                                <!-- Hits found -->
                                <c:choose>
                                    <c:when test="${taskInfo.numHits!=null}">
                                        <td><c:out value='${taskInfo.numHits}'/></td>
                                    </c:when>
                                    <c:otherwise>
                                        <td>NA</td>
                                    </c:otherwise>
                                </c:choose>

                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            <c:if test="${errorMessage} != null">
                Error Message: ${errorMessage}<br>
            </c:if>
        </div>
        <script type="text/javascript" xml:space="preserve">
             showHideMonth(document.taskReportForm.period);
        </script>
        <div id="footer" style="display:none">
            <jsp:include page="/WEB-INF/jsp/common/BasePageBottom.jsp"/>
        </div>
    </body>
</html>