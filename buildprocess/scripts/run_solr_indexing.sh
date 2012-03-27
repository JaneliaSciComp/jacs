#!/bin/sh
/usr/bin/wget -v --post-data="action=invokeOpByName&name=ComputeServer%3Aservice%3DWorkstationDataManager&methodName=runSolrIndexSync&argType=java.lang.Boolean&arg0=True" http://${1}:8180/jmx-console/HtmlAdaptor

