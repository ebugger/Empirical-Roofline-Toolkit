#!/usr/bin/env python

import os,sys

max_gflops_value = 0.0
max_gflops_name  = ""

max_weight = 0.0

save_band = False
max_band  = []

save_band_meta = False
band_metadata = []

save_flop_meta = False
flop_metadata = []

found_meta = False

for line in sys.stdin:
  parts = line.split()

  if save_band_meta or save_flop_meta:
    if line[:-1] == "META_DATA":
      found_meta = True

      if save_band_meta:
        band_metadata = []

      if save_flop_meta:
        flop_metadata = []

    if found_meta:
      if len(parts) == 2 and parts[1] == "GFLOPs":
        save_band = False

        save_band_meta = False
        save_flop_meta = False

        found_meta = False
      else:
        if save_band_meta:
          band_metadata.append(line[:-1])

        if save_flop_meta:
          flop_metadata.append(line[:-1])

  if not found_meta:
    if len(parts) == 2:
      if parts[1] == "GFLOPs":
        gflops_value = float(parts[0])
        gflops_name  = parts[1]

        if gflops_value > max_gflops_value:
          max_gflops_value = gflops_value
          max_gflops_name  = gflops_name

          save_flop_meta = True
          found_meta = False
      elif parts[1] == "Weight":
        weight = float(parts[0])

        if weight > max_weight:
          max_weight = weight

          save_band = True
          max_band  = []

          save_band_meta = True
          found_meta = False
      elif save_band:
        max_band.append(line[:-1])

print "  %7.2f %s EMP" % (max_gflops_value,max_gflops_name)

for m in flop_metadata:
  print m

print

for b in max_band:
  print "%s EMP" % b

for m in band_metadata:
  print m
