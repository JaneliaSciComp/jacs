#!/bin/sh
/usr/bin/wget -v --post-data="action=invokeOpByName&name=ComputeServer%3Aservice%3DWorkstationDataManager&methodName=runLeetDataPipeline&argType=java.lang.String&arg0=leetlab&argType=java.lang.String&arg1=Central+Brain+Lineage+Samples&argType=java.lang.String&arg2=leet_central_brain_lineage&argType=java.lang.Boolean&arg3=False&argType=java.lang.Boolean&arg4=False" http://${1}:8180/jmx-console/HtmlAdaptor
sleep 5
/usr/bin/wget -v --post-data="action=invokeOpByName&name=ComputeServer%3Aservice%3DWorkstationDataManager&methodName=runLeetDataPipeline&argType=java.lang.String&arg0=leetlab&argType=java.lang.String&arg1=Pan+Lineage+Samples&argType=java.lang.String&arg2=leet_pan_lineage&argType=java.lang.Boolean&arg3=False&argType=java.lang.Boolean&arg4=False" http://${1}:8180/jmx-console/HtmlAdaptor

