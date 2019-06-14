import operator,subprocess,sys,os.path

# Make a list into a space seperated string
def list_2_string(text_list):
  return reduce(operator.add,[t+" " for t in text_list])

# Execute a command without generating a new shell
def execute_noshell(command,echo=True):
  if echo:
    print "   ",list_2_string(command)
    sys.stdout.flush()

  if subprocess.call(command,shell=False) != 0:
    sys.stderr.write("  Failure...\n")
    return 1
  return 0

# Execute a command within a new shell
def execute_shell(command,echo=True):
  if echo:
    if isinstance(command,list):
      print "   ",command[0]
    else:
      print "   ",command
    sys.stdout.flush()

  if subprocess.call(command,shell=True) != 0:
    sys.stderr.write("  Failure...\n")
    return 1
  return 0

# Execute a command without generating a new shell
# and return any output from "stdout"
def stdout_noshell(command,echo=True):
  if echo:
    print "   ",list_2_string(command)
    sys.stdout.flush()

  p = subprocess.Popen(command,shell=False,stdout=subprocess.PIPE)
  output = p.communicate()[0]
  status = p.returncode
  if status != 0:
    sys.stderr.write("  Failure...\n")
    return (1,"Failure")
  return (0,output)

# Execute a command within a new shell
# and return any output from "stdout"
def stdout_shell(command,echo=True):
  if echo:
    if isinstance(command,list):
      print "   ",command[0]
    else:
      print "   ",command
    sys.stdout.flush()

  p = subprocess.Popen(command,shell=True,stdout=subprocess.PIPE)
  output = p.communicate()[0]
  status = p.returncode
  if status != 0:
    sys.stderr.write("  Failure...\n")
    return (1,"Failure")
  return (0,output)

# Return a list of integers after parsing a string of integers, commas, and
# dashes:  # specifies an integer and #-# specifies an integer range.  An
# number of these integers and integer ranges can be specified as part of a
# comma seperated list.  For example,
#
#     2          -> [2]
#     1,2,4,8    -> [1,2,4,8]
#     1-2,4,8-16 -> [1,2,4,8,9,10,11,12,13,14,15,16]
#
def parse_int_list(input):
  retlist = []

  elems = input.replace(" ","").replace("\t","").split(",")
  for elem in elems:
    minmax = elem.split("-")
    if len(minmax) == 1:
      retlist.append(int(minmax[0]))
    else:
      for i in xrange(int(minmax[0]),int(minmax[1])+1):
        retlist.append(i)

  return sorted(list(set(retlist)))

# Make a new directory if it doesn't already exist
def make_dir_if_needed(dir,name,echo=True):
  if not os.path.exists(dir):
    command = ["mkdir",dir]
    if execute_noshell(command,echo) != 0:
      sys.stderr.write("Unable to make %s directory, %s\n" % (name,dir))
  else:
    return False

  return True
