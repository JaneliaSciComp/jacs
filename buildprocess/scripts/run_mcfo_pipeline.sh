#!/bin/sh
/usr/bin/wget -v --post-data="action=invokeOpByName&name=ComputeServer%3Aservice%3DWorkstationDataManager&methodName=runMCFODataPipeline&argType=java.lang.String&arg0=system&argType=boolean&arg1=False&argType=java.lang.String&arg2=/groups/flylight/flylight/flip/SecData/tiles&argType=java.lang.String&arg3=FlyLight+Single+Neuron+Data" http://$1:8180/jmx-console/HtmlAdaptor

