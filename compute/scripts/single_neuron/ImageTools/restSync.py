#!/usr/bin/python

from __future__ import print_function
import os, time, sys, commands

TRIES = 3

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
    cmd = "curl --write-out %%{http_code} --silent --output /dev/null -X GET %s > %s" % (source, target)
elif cmd=="PUT":
    cmd = "curl --write-out %%{http_code} --silent --output /dev/null -X PUT %s --data-binary @%s" % (target, source)
else:
    print("Command must be GET or PUT.")
    sys.exit(1)

success = False
c = 0
while True:
    if c>=TRIES: break
    if c>0:
        print("Communication failure, retrying...")
    print(cmd)
    (retval,httpcode) = commands.getstatusoutput(cmd)
    print("Exit code %d, HTTP status code %s"%(retval,httpcode))
    if retval==0 and httpcode=='200':
        success = True
        break
    c += 1

if success:
    print("Result: success")
else:
    print("Result: failure after %d tries"%TRIES)

elapsed = time.time()-st

print("Timing: %i,%f,%f" % (numbytes, elapsed, getGbps(numbytes, elapsed)))
