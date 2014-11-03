#!/usr/bin/python

from __future__ import print_function
import os, time, sys

def getGbps(numbytes, secs): return numbytes*8.0/1000/1000/1000/secs

if len(sys.argv)!=4:
    script = os.path.basename(__file__)
    print("Usage: %s [GET|PUT] [source] [target]"%script, file=sys.stderr)
    sys.exit(1)

(script,cmd,source,target) = sys.argv

statinfo = os.stat(source)
numbytes = statinfo.st_size

st = time.time()

if cmd=="GET":
    os.system("curl -sS -X GET %s > %s" % (source, target))
elif cmd=="PUT":
    shm = "/dev/shm/tmp"
    os.system("cp -a %s %s"%(source, shm))
    os.system("curl -sS -X PUT %s --data-binary @%s" % (target, shm))
else:
    print("Command must be GET or PUT.")
    sys.exit(1)

elapsed = time.time()-st

print("%i,%f,%f" % (numbytes, elapsed, getGbps(numbytes, elapsed)))

