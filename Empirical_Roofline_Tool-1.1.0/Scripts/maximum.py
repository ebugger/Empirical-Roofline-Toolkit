#!/usr/bin/env python

import os

field0 = ""
lastLine = ""

found_metadata = False

for l in os.sys.stdin:
  m = l.split()

  if len(m) > 0 and m[0] == "META_DATA":
    found_metadata = True

    print lastLine,
    print

  if found_metadata:
    print l,
  else:
    if len(m) == 11 and m[0][0] != "#":
      if m[0] != field0:
        if lastLine != "":
          print lastLine,
        field0 = m[0]
      lastLine = l

