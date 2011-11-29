#!/bin/sh
/usr/bin/wget -v --post-data="action=invokeOpByName&name=ComputeServer%3Aservice%3DWorkstationDataManager&methodName=runMCFOSampleViewCreation&argType=java.lang.String&arg0=${2}&argType=java.lang.String&arg1=FlyLight+Single+Neuron+Samples" http://${1}:8180/jmx-console/HtmlAdaptor

