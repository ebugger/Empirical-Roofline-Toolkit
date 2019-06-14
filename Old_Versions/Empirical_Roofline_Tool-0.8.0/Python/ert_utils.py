import operator,subprocess,sys,os.path

def list_2_string(text_list):
  return reduce(operator.add,[t+" " for t in text_list])

def execute_noshell(command,echo=True):
  if echo:
    print " ",list_2_string(command)
    sys.stdout.flush()

  if subprocess.call(command,shell=False) != 0:
    sys.stderr.write("  Failure...\n")
    return 1
  return 0

def execute_shell(command,echo=True):
  if echo:
    if isinstance(command,list):
      print " ",command[0]
    else:
      print " ",command
    sys.stdout.flush()

  if subprocess.call(command,shell=True) != 0:
    sys.stderr.write("  Failure...\n")
    return 1
  return 0

def stdout_noshell(command,echo=True):
  if echo:
    print " ",list_2_string(command)
    sys.stdout.flush()

  p = subprocess.Popen(command,shell=False,stdout=subprocess.PIPE)
  output = p.communicate()[0]
  status = p.returncode
  if status != 0:
    sys.stderr.write("  Failure...\n")
    return (1,"Failure")
  return (0,output)

def stdout_shell(command,echo=True):
  if echo:
    if isinstance(command,list):
      print " ",command[0]
    else:
      print " ",command
    sys.stdout.flush()

  p = subprocess.Popen(command,shell=True,stdout=subprocess.PIPE)
  output = p.communicate()[0]
  status = p.returncode
  if status != 0:
    sys.stderr.write("  Failure...\n")
    return (1,"Failure")
  return (0,output)

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

def make_dir_if_needed(dir,name):
    if not os.path.exists(dir):
      command = ["mkdir",dir]
      if execute_noshell(command) != 0:
        sys.stderr.write("Unable to make %s directory, %s\n" % (name,dir))
        return 1

