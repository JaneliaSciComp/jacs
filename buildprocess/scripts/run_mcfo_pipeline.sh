#!/bin/sh
#action=invokeOp&name=ComputeServer%3Aservice%3DWorkstationDataManager&methodIndex=2&arg0=system&arg1=False" 
/usr/bin/wget -v --post-data="action=invokeOpByName&name=ComputeServer%3Aservice%3DWorkstationDataManager&methodName=runMCFODataPipelineService&argType=java.lang.String&arg0=system&argType=boolean&arg1=False" http://$1:8180/jmx-console/HtmlAdaptor

