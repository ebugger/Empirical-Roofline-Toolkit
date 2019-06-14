import sys,operator,subprocess,os,glob,filecmp,math
import socket,platform,time,json

from ert_utils import *

def text_list_2_string(text_list):
  return reduce(operator.add,[t+" " for t in text_list])

class ert_core:

  def __init__(self):
    self.ert_version = "0.8.0"

    self.dict = {}
    self.metadata = {}

    self.metadata["ERT_VERSION"] = self.ert_version

    hostname = socket.gethostname()
    try:
      new_hostname = socket.gethostbyaddr(hostname)
    except socket.herror:
      new_hostname = hostname

    hostname = new_hostname

    hostname = os.getenv("NERSC_HOST",hostname)

    self.metadata["HOSTNAME"] = hostname
    self.metadata["UNAME"] = platform.uname()

    print "Running ERT version %s..." % self.ert_version

  def flags(self):
    argc = len(sys.argv)

    if argc != 2:
      sys.stderr.write("Usage: ert config_file\n")
      return 1
    
    self.configure_filename = sys.argv[1]
    return 0

  def configure(self):
    print
    print "Reading configuration from '%s'..." % self.configure_filename

    try:
      configure_file = open(self.configure_filename,"r")
    except IOError:
      print "Unable to open '%s'..." % self.configure_filename
      return 1

    for line in configure_file:
      line = line[:-1]

      if len(line) > 0 and line[0] != "#":
        line = line.split()
        target = line[0]
        value = line[1:]

        if len(target) > 0:
          self.dict[target] = value

    self.results_dir = self.dict["ERT_RESULTS"][0]
    make_dir_if_needed(self.results_dir,"results")

    run_files = glob.glob("%s/Run.[0-9][0-9][0-9]" % self.results_dir)
    used_list = []
    no_dir = True
    for run_file in run_files:
      run_configure_filename = "%s/config.ert" % run_file
      if os.path.exists(run_configure_filename):
        if filecmp.cmp(self.configure_filename,run_configure_filename):
          self.results_dir = run_file
          no_dir = False
          print "  Using existing run directory, %s..." % self.results_dir
          break
        else:
          used_list.append(int(run_file[-3:]))
      else:
        used_list.append(int(run_file[-3:]))

    if no_dir:
      if len(used_list) == 0:
        used_list = [0]
      for n in xrange(1,max(used_list)+2):
        if n not in used_list:
          self.results_dir = "%s/Run.%03d" % (self.results_dir,n)
          print "  Making new run directory, %s..." % self.results_dir

          command = ["mkdir",self.results_dir]
          if execute_noshell(command) != 0:
            sys.stderr.write("Unable to make new run directory, %s\n" % self.results_dir)
            return 1

          command = ["cp",self.configure_filename,"%s/config.ert" % self.results_dir]
          if execute_noshell(command) != 0:
            sys.stderr.write("Unable to copy configuration file, %s, into new run directory, %s\n" % (self.configure_filename,self.results_dir))
            return 1

          break

    return 0

  def build(self):
    print
    print "Building ERT core code..."

    command_prefix =                                                       \
      self.dict["ERT_CC"]                                                + \
      self.dict["ERT_CFLAGS"]                                            + \
      ["-I%s/Kernels" % self.exe_path]                                   + \
      ["-DERT_FLOP=%d" % self.flop]                                      + \
      ["-DERT_ALIGN=%s" % self.dict["ERT_ALIGN"][0]]                     + \
      ["-DERT_MEMORY_MAX=%s" % self.dict["ERT_MEMORY_MAX"][0]]           + \
      ["-DERT_WORKING_SET_MIN=%s" % self.dict["ERT_WORKING_SET_MIN"][0]] + \
      ["-DERT_TRIALS_MIN=%s" % self.dict["ERT_TRIALS_MIN"][0]]

    if self.dict["ERT_MPI"][0] == "True":
      command_prefix += ["-DERT_MPI"] + self.dict["ERT_MPI_CFLAGS"]

    if self.dict["ERT_OPENMP"][0] == "True":
      command_prefix += ["-DERT_OPENMP"] + self.dict["ERT_OPENMP_CFLAGS"]

    command = command_prefix + \
              ["-c","%s/Drivers/%s.c" % (self.exe_path,self.dict["ERT_DRIVER"][0])] + \
              ["-o","%s/%s.o" % (self.flop_dir,self.dict["ERT_DRIVER"][0])]
    if execute_noshell(command) != 0:
      sys.stderr.write("Compiling driver, %s, failed\n" % self.dict["ERT_DRIVER"][0])
      return 1

    command = command_prefix + \
              ["-c","%s/Kernels/%s.c" % (self.exe_path,self.dict["ERT_KERNEL"][0])] + \
              ["-o","%s/%s.o" % (self.flop_dir,self.dict["ERT_KERNEL"][0])]
    if execute_noshell(command) != 0:
      sys.stderr.write("Compiling kernel, %s, failed\n" % self.dict["ERT_KERNEL"][0])
      return 1

    command = self.dict["ERT_LD"]      + \
              self.dict["ERT_LDFLAGS"]

    if self.dict["ERT_MPI"][0] == "True":
      command += self.dict["ERT_MPI_LDFLAGS"]

    if self.dict["ERT_OPENMP"][0] == "True":
      command += self.dict["ERT_OPENMP_LDFLAGS"]

    command += ["%s/%s.o" % (self.flop_dir,self.dict["ERT_DRIVER"][0])] + \
               ["%s/%s.o" % (self.flop_dir,self.dict["ERT_KERNEL"][0])] + \
               self.dict["ERT_LDLIBS"]                                  + \
               ["-o","%s/%s.%s" % (self.flop_dir,self.dict["ERT_DRIVER"][0],self.dict["ERT_KERNEL"][0])]
    if execute_noshell(command) != 0:
      sys.stderr.write("Linking code failed\n")
      return 1

    return 0

  def add_metadata(self,outputname):
    try:
      output = open(outputname,"a")
    except IOError:
      sys.stderr.write("Unable to open output file, %s, to add metadata\n" % outputfile)
      return 1

    for k,v in self.metadata.items():
      output.write("%s  %s\n" % (k,v))

    for k,v in self.dict.items():
      output.write("%s  %s\n" % (k,v))

    output.close()

    return 0

  def run(self):
    print
    print "Running ERT core code..."

    self.run_list = []

    procs_threads_list  = parse_int_list(self.dict["ERT_PROCS_THREADS"][0])
    mpi_procs_list      = parse_int_list(self.dict["ERT_MPI_PROCS"][0])
    openmp_threads_list = parse_int_list(self.dict["ERT_OPENMP_THREADS"][0])

    num_experiments = int(self.dict["ERT_NUM_EXPERIMENTS"][0])

    base_command = list_2_string(self.dict["ERT_RUN"])

    for mpi_procs in mpi_procs_list:
      for openmp_threads in openmp_threads_list:
        if mpi_procs * openmp_threads in procs_threads_list:
          mpi_dir = "%s/MPI.%03d" % (self.flop_dir,mpi_procs)
          make_dir_if_needed(mpi_dir,"run")

          run_dir = "%s/OpenMP.%03d" % (mpi_dir,openmp_threads)
          make_dir_if_needed(run_dir,"run")

          self.run_list.append(run_dir)

          if os.path.exists("%s/run.done" % run_dir):
            print "  Skipping MPI %d, OpenMP %d - already run" % (mpi_procs,openmp_threads)
            print
          else:
            command = base_command

            command = command.replace("ERT_OPENMP_THREADS",str(openmp_threads))
            command = command.replace("ERT_MPI_PROCS",str(mpi_procs))
            command = command.replace("ERT_CODE","%s/%s.%s" % (self.flop_dir,self.dict["ERT_DRIVER"][0],self.dict["ERT_KERNEL"][0]))
            command = "(" + command + ") > %s/try.ERT_TRY_NUM 2>&1 " % run_dir

            for t in xrange(1,num_experiments+1):
              output = "%s/try.%03d" % (run_dir,t) 

              cur_command = command
              cur_command = cur_command.replace("ERT_TRY_NUM","%03d" % t)

              self.metadata["TIMESTAMP_DATA"] = time.time()

              if execute_shell(cur_command) != 0:
                sys.stderr.write("Unable to complete %s, experiment %d\n" % (run_dir,t))
                return 1

              if self.add_metadata(output) != 0:
                return 1

            command = ["touch","%s/run.done" % run_dir]
            if execute_noshell(command) != 0:
              sys.stderr.write("Unable to make 'run.done' file in %s\n" % run_dir)
              return 1

            print

    return 0

  def process(self):
    print
    print "Processing results..."

    for run in self.run_list:
      print run

      command = ["cat %s/try.* | %s/Scripts/preprocess.py > %s/pre" % (run,self.exe_path,run)]
      if execute_shell(command) != 0:
        sys.stderr.write("Unable to process %s\n" % run)
        return 1

      command = ["%s/Scripts/maximum.py < %s/pre > %s/max" % (self.exe_path,run,run)]
      if execute_shell(command) != 0:
        sys.stderr.write("Unable to process %s\n" % run)
        return 1

      command = ["%s/Scripts/summary.py < %s/max > %s/sum" % (self.exe_path,run,run)]
      if execute_shell(command) != 0:
        sys.stderr.write("Unable to process %s\n" % run)
        return 1

    return 0

  def make_graph(self,run_dir,title,name):
    command  = "sed "
    command += "-e 's#ERT_TITLE#%s#g' " % title
    command += "-e 's#ERT_XRANGE_MIN#\*#g' "
    command += "-e 's#ERT_XRANGE_MAX#\*#g' "
    command += "-e 's#ERT_YRANGE_MIN#\*#g' "
    command += "-e 's#ERT_YRANGE_MAX#\*#g' "
    command += "-e 's#ERT_RAW_DATA#%s/pre#g' " % run_dir
    command += "-e 's#ERT_MAX_DATA#%s/max#g' " % run_dir
    command += "-e 's#ERT_GRAPH#%s/%s#g' " % (run_dir,name)

    command += "< %s/Plot/%s.gnu.template > %s/%s.gnu" % (self.exe_path,name,run_dir,name)
    if execute_shell(command,False) != 0:
      sys.stderr.write("Unable to produce a '%s' gnuplot file for %s\n" % (name,run_dir))
      return 1

    command = "echo 'load \"%s/%s.gnu\"' | %s" % (run_dir,name,self.dict["ERT_GNUPLOT"][0])
    if execute_shell(command) != 0:
      sys.stderr.write("Unable to produce a '%s' for %s\n" % (name,run_dir))
      return 1

    return 0

  def graphs(self):
    print
    for run_dir in self.run_list:
      if self.make_graph(run_dir,"Graph 1 (%s)" % run_dir,"graph1") != 0:
        return 1

      if self.make_graph(run_dir,"Graph 2 (%s)" % run_dir,"graph2") != 0:
        return 1

      if self.make_graph(run_dir,"Graph 3 (%s)" % run_dir,"graph3") != 0:
        return 1

      if self.make_graph(run_dir,"Graph 4 (%s)" % run_dir,"graph4") != 0:
        return 1
    return 0

  def database(self):
 #   print
 #   print "Database not implemented..."

    return 0

  def build_database(self,gflop,gbyte):
    gflop0 = gflop[0].split()

    emp_gflops_data = []
    emp_gflops_data.append([gflop0[1],float(gflop0[0])])

    emp_gflops_metadata = {}
    for metadata in gflop[1:]:
      parts = metadata.partition(" ")
      key = parts[0].strip()
      if key != "META_DATA":
        if key in emp_gflops_metadata:
          value = emp_gflops_metadata[key]

          if isinstance(value,list):
            value.append(parts[2].strip())
          else:
            value = [value,parts[2].strip()]

          emp_gflops_metadata[key] = value
        else:
          emp_gflops_metadata[parts[0].strip()] = parts[2].strip()

    emp_gflops_metadata["TIMESTAMP_DB"] = time.time()

    emp_gflops = {}
    emp_gflops['metadata'] = emp_gflops_metadata
    emp_gflops['data']     = emp_gflops_data

    emp_gbytes_metadata = {}
    emp_gbytes_data = []

    for i in xrange(0,len(gbyte)):
      if gbyte[i] == "META_DATA":
        break
      else:
        gbyte_split = gbyte[i].split()
        emp_gbytes_data.append([gbyte_split[1],float(gbyte_split[0])])

    for j in xrange(i+1,len(gbyte)):
      metadata = gbyte[j]

      parts = metadata.partition(" ")
      key = parts[0].strip()
      if key != "META_DATA":
        if key in emp_gbytes_metadata:
          value = emp_gbytes_metadata[key]

          if isinstance(value,list):
            value.append(parts[2].strip())
          else:
            value = [value,parts[2].strip()]

          emp_gbytes_metadata[key] = value
        else:
          emp_gbytes_metadata[parts[0].strip()] = parts[2].strip()

    emp_gbytes_metadata["TIMESTAMP_DB"] = time.time()

    emp_gbytes = {}
    emp_gbytes['metadata'] = emp_gbytes_metadata
    emp_gbytes['data']     = emp_gbytes_data

    empirical = {}
    empirical['gflops'] = emp_gflops
    empirical['gbytes'] = emp_gbytes

    spec_gflops_data = []
    if 'ERT_SPEC_GFLOPS' in self.dict:
      spec_gflops_data.append(['GFLOPs',float(self.dict['ERT_SPEC_GFLOPS'][0])])

    spec_gflops = {}
    spec_gflops['data'] = spec_gflops_data

    spec_gbytes_data = []
    for k in self.dict:
      if k.find('ERT_SPEC_GBYTES') == 0:
        spec_gbytes_data.append([k[len('ERT_SPEC_GBYTES')+1:],float(self.dict[k][0])])

    spec_gbytes = {}
    spec_gbytes['data'] = spec_gbytes_data

    spec = {}
    spec['gflops'] = spec_gflops
    spec['gbytes'] = spec_gbytes

    result = {}
    result['empirical'] = empirical
    result['spec']      = spec

    return result

  def roofline(self):
    print "Gathering the final roofline results..."

    command = "cat %s/*/*/*/sum | %s/Scripts/roofline.py" % (self.results_dir,self.exe_path)
    result = stdout_shell(command)
    if result[0] != 0:
      sys.stderr.write("Unable to create final roofline results\n")
      return 1

    lines = result[1].split("\n")

    for i in xrange(0,len(lines)):
      if len(lines[i]) == 0:
        break

    gflop_lines = lines[:i]
    gbyte_lines = lines[i+1:]

    database = self.build_database(gflop_lines,gbyte_lines)

    database_filename = "%s/roofline.json" % self.results_dir
    try:
      database_file = open(database_filename,"w")
    except IOError:
      sys.stderr.write("Unable to open database file, %s\n" % database_filename)
      return 1

    json.dump(database,database_file,indent=3)

    database_file.close()

    line = gflop_lines[0].split()
    gflops_emp = [float(line[0]),line[1]]

    for i in xrange(0,len(gbyte_lines)):
      if gbyte_lines[i] == "META_DATA":
        break

    num_mem = i
    gbytes_emp = num_mem * [0]

    for i in xrange(0,num_mem):
      line = gbyte_lines[i].split()
      gbytes_emp[i] = [float(line[0]),line[1]]

    x = num_mem * [0.0]
    for i in xrange(0,len(gbytes_emp)):
      x[i] = gflops_emp[0]/gbytes_emp[i][0]

    basename = "roofline"
    loadname = "%s/%s.gnu" % (self.results_dir,basename)

    xmin =   0.01
    xmax = 100.00

    ymin = 10 ** int(math.floor(math.log10(gbytes_emp[0][0] * xmin)))

    title = "Empirical Roofline Graph (%s)" % self.results_dir

    command  = "sed "
    command += "-e 's#ERT_TITLE#%s#g' " % title
    command += "-e 's#ERT_XRANGE_MIN#%le#g' " % xmin
    command += "-e 's#ERT_XRANGE_MAX#%le#g' " % xmax
    command += "-e 's#ERT_YRANGE_MIN#%le#g' " % ymin
    command += "-e 's#ERT_YRANGE_MAX#\*#g' "
    command += "-e 's#ERT_GRAPH#%s/%s#g' " % (self.results_dir,basename)

    command += "< %s/Plot/%s.gnu.template > %s" % (self.exe_path,basename,loadname)
    if execute_shell(command,False) != 0:
      sys.stderr.write("Unable to produce a '%s' gnuplot file for %s\n" % (loadname,self.results_dir))
      return 1

    try:
      plotfile = open(loadname,"a")
    except IOError:
      print "Unable to open '%s'..." % loadname
      return 1

    xgflops = 2.0
    label = '%.1f %s/sec (Maximum)' % (gflops_emp[0],gflops_emp[1])
    plotfile.write("set label '%s' at %.7le,%.7le left textcolor rgb '#000080'\n" % (label,xgflops,1.2*gflops_emp[0]))

    xleft  = xmin
    xright = x[0]

    xmid = math.sqrt(xleft * xright)
    ymid = gbytes_emp[0][0] * xmid

    y0gbytes = ymid
    x0gbytes = y0gbytes/gbytes_emp[0][0]

    C = x0gbytes * y0gbytes

    alpha = 1.065

    label_over = True
    for i in xrange(0,len(gbytes_emp)):
      if i > 0:
        if label_over and gbytes_emp[i-1][0] / gbytes_emp[i][0] < 1.5:
          label_over = False

        if not label_over and gbytes_emp[i-1][0] / gbytes_emp[i][0] > 3.0:
          label_over = True

      if label_over:
        ygbytes = math.sqrt(C * gbytes_emp[i][0]) / math.pow(alpha,len(gbytes_emp[i][1]))
        xgbytes = ygbytes/gbytes_emp[i][0]

        ygbytes *= 1.1
        xgbytes /= 1.1
      else:
        ygbytes = math.sqrt(C * gbytes_emp[i][0]) / math.pow(alpha,len(gbytes_emp[i][1]))
        xgbytes = ygbytes/gbytes_emp[i][0]

        ygbytes /= 1.1
        xgbytes *= 1.1

      label = "%s - %.1lf GB/s" % (gbytes_emp[i][1],gbytes_emp[i][0])

      plotfile.write("set label '%s' at %.7le,%.7le left rotate by 45 textcolor rgb '#800000'\n" % (label,xgbytes,ygbytes))

    plotfile.write("plot \\\n")

    for i in xrange(0,len(gbytes_emp)):
      plotfile.write("     (x <= %.7le ? %.7le * x : 1/0) lc 1 lw 2,\\\n" % (x[i],gbytes_emp[i][0]))

    plotfile.write("     (x >= %.7le ? %.7le : 1/0) lc 3 lw 2\n" % (x[0],gflops_emp[0]))

    plotfile.close()

    command = "echo 'load \"%s\"' | %s" % (loadname,self.dict["ERT_GNUPLOT"][0])
    if execute_shell(command) != 0:
      sys.stderr.write("Unable to produce a '%s' for %s\n" % (basename,self.results_dir))
      return 1

    return 0
