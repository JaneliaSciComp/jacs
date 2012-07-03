#!/bin/sh
/usr/bin/wget -v --post-data="action=invokeOpByName&name=ComputeServer%3Aservice%3DWorkstationDataManager&methodName=runLeetSageBasedDataPipeline&argType=java.lang.String&arg0=leetlab&argType=java.lang.String&arg1=Pan+Lineage+Samples&argType=java.lang.Boolean&arg2=False&argType=java.lang.Boolean&arg3=False&argType=java.lang.Boolean&arg4=False" http://${1}:8180/jmx-console/HtmlAdaptor

