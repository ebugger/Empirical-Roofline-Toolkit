#include "driver.h"
#include "kernel1.h"

double getTime()
{
  double time;

#ifdef ERT_OPENMP
  time = omp_get_wtime();
#elif ERT_MPI
  time = MPI_Wtime();
#else
  struct timeval tm;
  gettimeofday(&tm, NULL);
  time = tm.tv_sec + (tm.tv_usec / 1000000.0);
#endif
  return time;
}

int main(int argc, char *argv[]) {

#if 0
  if (argc != 3) {
    fprintf(stderr, "Usage: %s size unit(GB/MB/KB) \n", argv[0]);
    return -1;
  }
#endif

  int rank = 0;
  int nprocs = 1;
  int nthreads = 1;
  int id = 0;
  int provided = -1;
  int requested;

#ifdef ERT_MPI
  #ifdef ERT_OPENMP
  requested = MPI_THREAD_FUNNELED;
  MPI_Init_thread(&argc, &argv, requested, &provided);
  #else
  MPI_Init(&argc, &argv);
  #endif // ERT_OPENMP

  MPI_Comm_size(MPI_COMM_WORLD, &nprocs);
  MPI_Comm_rank(MPI_COMM_WORLD, &rank);

  /* printf("The MPI binding provided thread support of: %d\n", provided); */
#endif // ERT_MPI

#ifdef ERT_BGPM
  // Use SW mode
  Bgpm_Init(BGPM_MODE_SWDISTRIB);

  // Generate a Puint event set for counting CPU related events
  int evtSet = Bgpm_CreateEventSet();
  unsigned evtList[] = { PEVT_IU_IS1_STALL_CYC,
                         PEVT_IU_IS2_STALL_CYC,
                         PEVT_CYCLES,   // x1 cycles
                         PEVT_INST_ALL, // All instruction Completions
                        };
  Bgpm_AddEventList(evtSet, evtList, sizeof(evtList)/sizeof(unsigned));

  // Apply the event set to HW
  Bgpm_Apply(evtSet);
#endif

  uint64_t TSIZE = ERT_MEMORY_MAX;
  uint64_t PSIZE = TSIZE / nprocs;

#ifdef ERT_INTEL
  double * __restrict__ buf = (double *)_mm_malloc(PSIZE, ERT_ALIGN);
#elif ERT_BGPM
  double * __restrict__ buf = (double *)bgq_malloc(PSIZE, ERT_ALIGN);
#else
  double * __restrict__ buf = malloc(PSIZE);
#endif

  if (buf == NULL) {
    fprintf(stderr, "Out Of Memory!\n");
    return -1;
  }

  /* printf("array size: %10" PRIu64 " bytes/proc\n", PSIZE); */

#ifdef ERT_OPENMP
  #pragma omp parallel private(id)
#endif
  {
    uint64_t numTrials = 1 << 27;

#ifdef ERT_OPENMP
    id = omp_get_thread_num();
    nthreads = omp_get_num_threads();
#else
    id = 0;
    nthreads = 1;
#endif
        
    uint64_t nsize = PSIZE / nthreads;
    nsize = nsize & (~(ERT_ALIGN-1));
    nsize = nsize / sizeof(double);
    uint64_t nid =  nsize * id ;

    // initialize small chunck of buffer within each thread
    initialize(nsize, &buf[nid], 1.0);

    double startTime, endTime;
    uint64_t n,nNew;
    uint64_t t;
    int bytes_per_elem;
    int mem_accesses_per_elem;

    n = ERT_WORKING_SET_MIN;
    while (n < nsize) { // working set - nsize
      uint64_t ntrials = nsize / n;
      if (ntrials < 1)
        ntrials = 1;

      for (t = ERT_TRIALS_MIN; t <= ntrials; t = t * 2) { // working set - ntrials
#ifdef ERT_MPI
  #ifdef ERT_OPENMP        
        #pragma omp master
  #endif
        {
          MPI_Barrier(MPI_COMM_WORLD);
        }
#endif // ERT_MPI

#ifdef ERT_OPENMP
        #pragma omp barrier
#endif

        if ((id == 0) && (rank==0)) {
#ifdef ERT_BGPM
          Bgpm_Start(evtSet);
#endif
          startTime = getTime();
        }

#ifdef ERT_QPX // QPX intrinsics for IBM BGQ
        vecKernel(n, t, &buf[nid]);
#elif  ERT_SSE // SSE intrinsics for Hopper(AMD)
        sseKernel(n, t, &buf[nid]);
#elif  ERT_AVX // AVX intrinsics for Edison(intel xeon)
        avxKernel(n, t, &buf[nid]);
#elif  ERT_KNC // KNC intrinsics for Babbage(intel mic)
        kncKernel(n, t, &buf[nid]);
#else       // C-code
        kernel(n, t, &buf[nid], &bytes_per_elem, &mem_accesses_per_elem);
#endif


#ifdef ERT_OPENMP
        #pragma omp barrier
#endif

#ifdef ERT_MPI
  #ifdef ERT_OPENMP
        #pragma omp master
  #endif
        {
          MPI_Barrier(MPI_COMM_WORLD);
        }
#endif // ERT_MPI

        if ((id == 0) && (rank == 0)) {
          endTime = getTime();
#ifdef ERT_BGPM
          Bgpm_Stop(evtSet);
          bgpm_print(evtSet);
#endif
          double seconds = (double)(endTime - startTime);
          uint64_t working_set_size = n * nthreads * nprocs;
          uint64_t total_bytes = t * working_set_size * bytes_per_elem * mem_accesses_per_elem;
          uint64_t total_flops = t * working_set_size * ERT_FLOP;

          // nsize; trials; microseconds; bytes; single thread bandwidth; total bandwidth
          printf("%12" PRIu64 " %12" PRIu64 " %15.3lf %12" PRIu64 " %12" PRIu64 "\n",
                  working_set_size * bytes_per_elem,
                  t,
                  seconds * 1000000,
                  total_bytes,
                  total_flops);
        } // print
      } // working set - ntrials

      nNew = 1.1 * n;
      if (nNew == n) {
        nNew = n+1;
      }

      n = nNew;
    } // working set - nsize
  } // parallel region

#ifdef ERT_MPI
  MPI_Barrier(MPI_COMM_WORLD);
#endif

#ifdef ERT_BGPM
  Bgpm_Disable();
#endif

#ifdef ERT_MPI
  MPI_Finalize();
#endif

  printf("\n");
  printf("META_DATA\n");
  printf("MPI_PROCS      %d\n", nprocs);
  printf("OPENMP_THREADS %d\n", nthreads);
  printf("FLOPS          %d\n", ERT_FLOP);

  return 0;
}
