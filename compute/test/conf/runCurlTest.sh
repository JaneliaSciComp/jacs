#!/bin/bash
FILE=newNeuronPost.txt
CONTEXT=rest-v1

echo \{ \"pointGUID\":9999, \"sampleID\":8888, \"neuronGUID\":9996, \"x\":74000, \"y\":46000, \"z\":17000, \"structureID\":2, \"parentPointGUID\":9997 \} >${FILE}
curl -H"Content-Type:application/json"  --request POST --data @${FILE} http://foster-ws:8180/${CONTEXT}/NeuronAPI/addPointJSON/9999/

#
curl -H"Content-Type:text/plain"  --request POST http://foster-ws:8180/${CONTEXT}/NeuronAPI/addPointQP/9999/?pointGUID=9999\&sampleID=8888\&neuronGUID=9996\&x=74000\&y=46000\&z=17000\&structureID=2\&parentPointGUID=9997
