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
#if ERT_GPU
  if (argc != 3) {
    fprintf(stderr, "Usage: %s gpu_blocks gpu_threads\n", argv[0]);
    return -1;
  }

  gpu_blocks  = atoi(argv[1]);
  gpu_threads = atoi(argv[2]);
#endif

  int rank = 0;
  int nprocs = 1;
  int nthreads = 1;
  int id = 0;
#ifdef ERT_MPI
  int provided = -1;
  int requested;

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

  uint64_t TSIZE = ERT_MEMORY_MAX;
  uint64_t PSIZE = TSIZE / nprocs;

#ifdef ERT_INTEL
  #ifdef ERT_GPU
  double *              buf = (double *)_mm_malloc(PSIZE, ERT_ALIGN);
  #else
  double * __restrict__ buf = (double *)_mm_malloc(PSIZE, ERT_ALIGN);
  #endif
#else
  #ifdef ERT_GPU
  double *              buf = (double *)malloc(PSIZE);
  #else
  double * __restrict__ buf = (double *)malloc(PSIZE);
  #endif
#endif

  if (buf == NULL) {
    fprintf(stderr, "Out of memory!\n");
    return -1;
  }

#ifdef ERT_OPENMP
  #pragma omp parallel private(id)
#endif

  {
#ifdef ERT_OPENMP
    id = omp_get_thread_num();
    nthreads = omp_get_num_threads();
#else
    id = 0;
    nthreads = 1;
#endif

#if ERT_GPU
    int num_gpus = 0;
    int gpu;
    int gpu_id;
    int numSMs;

    cudaGetDeviceCount(&num_gpus);
    if (num_gpus < 1) {
      fprintf(stderr, "No CUDA device detected.\n");
      return -1;
    }

    for (gpu = 0; gpu < num_gpus; gpu++) {
      cudaDeviceProp dprop;
      cudaGetDeviceProperties(&dprop,gpu);
      /* printf("%d: %s\n",gpu,dprop.name); */
    }

    cudaSetDevice(id % num_gpus);
    cudaGetDevice(&gpu_id);
    cudaDeviceGetAttribute(&numSMs, cudaDevAttrMultiProcessorCount, gpu_id);
#endif
        
    uint64_t nsize = PSIZE / nthreads;
    nsize = nsize & (~(ERT_ALIGN-1));
    nsize = nsize / sizeof(double);
    uint64_t nid =  nsize * id ;

    // initialize small chunck of buffer within each thread
    initialize(nsize, &buf[nid], 1.0);

#if ERT_GPU
    double *d_buf;
    cudaMalloc((void **)&d_buf, nsize*sizeof(double));
    cudaMemset(d_buf, 0, nsize*sizeof(double));
    cudaDeviceSynchronize();
#endif

    double startTime, endTime;
    uint64_t n,nNew;
    uint64_t t;
    int bytes_per_elem;
    int mem_accesses_per_elem;

    n = ERT_WORKING_SET_MIN;
    while (n <= nsize) { // working set - nsize
      uint64_t ntrials = nsize / n;
      if (ntrials < 1)
        ntrials = 1;

      for (t = ERT_TRIALS_MIN; t <= ntrials; t = t * 2) { // working set - ntrials
#ifdef ERT_GPU
        cudaMemcpy(d_buf, &buf[nid], n*sizeof(double), cudaMemcpyHostToDevice);
        cudaDeviceSynchronize();
#endif

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
          startTime = getTime();
        }

#if    ERT_AVX // AVX intrinsics for Edison(intel xeon)
        avxKernel(n, t, &buf[nid]);
#elif  ERT_KNC // KNC intrinsics for Babbage(intel mic)
        kncKernel(n, t, &buf[nid]);
#elif  ERT_GPU // CUDA code
        gpuKernel(n, t, d_buf, &bytes_per_elem, &mem_accesses_per_elem);
        cudaDeviceSynchronize();
#else          // C-code
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
          double seconds = (double)(endTime - startTime);
          uint64_t working_set_size = n * nthreads * nprocs;
          uint64_t total_bytes = t * working_set_size * bytes_per_elem * mem_accesses_per_elem;
          uint64_t total_flops = t * working_set_size * ERT_FLOP;

          // nsize; trials; microseconds; bytes; single thread bandwidth; total bandwidth
#if ERT_GPU
          printf("%12lld %12lld %15.3lf %12lld %12lld\n",
                  working_set_size * bytes_per_elem,
                  t,
                  seconds * 1000000,
                  total_bytes,
                  total_flops);
#else
          printf("%12" PRIu64 " %12" PRIu64 " %15.3lf %12" PRIu64 " %12" PRIu64 "\n",
                  working_set_size * bytes_per_elem,
                  t,
                  seconds * 1000000,
                  total_bytes,
                  total_flops);
#endif
        } // print

#if ERT_GPU
        cudaMemcpy(&buf[nid], d_buf, n*sizeof(double), cudaMemcpyDeviceToHost);
        cudaDeviceSynchronize();
#endif
      } // working set - ntrials

      nNew = 1.1 * n;
      if (nNew == n) {
        nNew = n+1;
      }

      n = nNew;
    } // working set - nsize

#if ERT_GPU
    cudaFree(d_buf);

    if (cudaGetLastError() != cudaSuccess) {
      printf("Last cuda error: %s\n",cudaGetErrorString(cudaGetLastError()));
    }

    cudaDeviceReset();
#endif
  } // parallel region

#ifdef ERT_INTEL
  _mm_free(buf);
#else
  free(buf);
#endif

#ifdef ERT_MPI
  MPI_Barrier(MPI_COMM_WORLD);
#endif

#ifdef ERT_MPI
  MPI_Finalize();
#endif

  printf("\n");
  printf("META_DATA\n");
  printf("FLOPS          %d\n", ERT_FLOP);

#ifdef ERT_MPI
  printf("MPI_PROCS      %d\n", nprocs);
#endif

#ifdef ERT_OPENMP
  printf("OPENMP_THREADS %d\n", nthreads);
#endif

#ifdef ERT_GPU
  printf("GPU_BLOCKS     %d\n", gpu_blocks);
  printf("GPU_THREADS    %d\n", gpu_threads);
#endif

  return 0;
}
