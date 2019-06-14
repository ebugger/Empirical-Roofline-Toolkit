#!/usr/bin/env python

import os,math

class INPUT:
  size = 5

  wkey  = 0
  tkey  = 1
  msec  = 2
  bytes = 3
  flops = 4

class STATS:
  size = 5

  msec_min = 0
  msec_med = 1
  msec_max = 2
  bytes    = 3
  flops    = 4

MEGA =      1000*1000
GIGA = 1000*1000*1000
  
data = dict()

metadata = {}

for l in os.sys.stdin:
  m = l.split()

  is_metadata = False
  if len(m) > 0 and m[0].isupper():
    metadata[l[:-1]] = 1
    is_metadata = True

  if len(m) == 2 and m[0] == "OPENMP_THREADS":
    threads = int(m[1])
  if len(m) == 2 and m[0] == "MPI_PROCS":
    procs = int(m[1])

  if not is_metadata and len(m) == INPUT.size:
    wkey = int(m[INPUT.wkey])
    if not data.has_key(wkey):
      data[wkey] = dict()

    tkey = int(m[INPUT.tkey])
    if not data[wkey].has_key(tkey):
      data[wkey][tkey] = STATS.size*[0]
      first = True
    else:
      first = False

    entry = data[wkey][tkey]

    msec  = float(m[INPUT.msec ])
    bytes = int  (m[INPUT.bytes])
    flops = int  (m[INPUT.flops])

    if first:
      entry[STATS.msec_min] = msec
      entry[STATS.msec_med] = [msec]
      entry[STATS.msec_max] = msec
    else:
      if msec < entry[STATS.msec_min]:
        entry[STATS.msec_min] = msec
      entry[STATS.msec_med].append(msec)
      if msec > entry[STATS.msec_max]:
        entry[STATS.msec_max] = msec

    entry[STATS.bytes] = bytes
    entry[STATS.flops] = flops

for wkey in sorted(data.iterkeys()):
  tdict = data[wkey]
  for tkey in sorted(tdict.iterkeys()):
    stats = tdict[tkey]

    msec_min = stats[STATS.msec_min]

    msec_med = sorted(stats[STATS.msec_med])
    msec_med = msec_med[len(msec_med)/2]

    msec_max = stats[STATS.msec_max]

    gbytes = float(stats[STATS.bytes])/GIGA
    gflops = float(stats[STATS.flops])/GIGA

    if msec_min != 0.0 and msec_med != 0.0 and msec_max != 0.0:
      GB_sec_min = gbytes/(msec_max/MEGA)
      GB_sec_med = gbytes/(msec_med/MEGA)
      GB_sec_max = gbytes/(msec_min/MEGA)

      GFLOP_sec_min = gflops/(msec_max/MEGA)
      GFLOP_sec_med = gflops/(msec_med/MEGA)
      GFLOP_sec_max = gflops/(msec_min/MEGA)

      print wkey,          \
            tkey,          \
            msec_min,      \
            msec_med,      \
            msec_max,      \
            GB_sec_min,    \
            GB_sec_med,    \
            GB_sec_max,    \
            GFLOP_sec_min, \
            GFLOP_sec_med, \
            GFLOP_sec_max

  print ""

print "META_DATA"
for k,m in metadata.items():
  if k != "META_DATA":
    print k
