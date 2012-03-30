#!/bin/sh
/usr/bin/wget -v --post-data="action=invokeOpByName&name=ComputeServer%3Aservice%3DWorkstationDataManager&methodName=runMCFODataPipeline&argType=java.lang.String&arg0=system&argType=java.lang.String&arg2=FlyLight+Single+Neuron+Data&argType=java.lang.Boolean&arg3=False&argType=java.lang.Boolean&arg4=False&argType=java.lang.Boolean&arg5=False" http://${1}:8180/jmx-console/HtmlAdaptor

