#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>

#include "rep.h"

void initialize(uint64_t nsize,
                double* __restrict__ A,
                double value)
{
#ifdef ERT_INTEL
   __assume_aligned(A, ERT_ALIGN);
#elif _QPX
   __alignx(ERT_ALIGN, A);
#endif

  uint64_t i;
  for (i = 0; i < nsize; ++i) {
    A[i] = value;
  }
}

void kernel(uint64_t nsize,
            uint64_t ntrials,
            double* __restrict__ A,
            int* bytes_per_elem,
            int* mem_accesses_per_elem)
{
  *bytes_per_elem        = sizeof(*A);
  *mem_accesses_per_elem = 2;

#ifdef ERT_INTEL
  __assume_aligned(A, ERT_ALIGN);
#elif _QPX
  __alignx(ERT_ALIGN, A);
#endif

  double alpha = 0.5;
  uint64_t i, j;
  for (j = 0; j < ntrials; ++j) {
    for (i = 0; i < nsize; ++i) {
      double beta = 0.8;
#if   ERT_FLOP ==  1 //  1 flop
      beta = A[i] + alpha;
#elif ERT_FLOP ==  2 //  2 flops
      beta = beta * A[i] + alpha;
#elif ERT_FLOP ==  4 //  4 flops
      REP2(beta = beta * A[i] + alpha);
#elif ERT_FLOP ==  8 //  8 flops
      REP4(beta = beta * A[i] + alpha);
#elif ERT_FLOP == 16 // 16 flops
      REP8(beta = beta * A[i] + alpha);
#elif ERT_FLOP == 32 // 32 flops
      REP16(beta = beta * A[i] + alpha);
#elif ERT_FLOP == 64 // 64 flops
      REP32(beta = beta * A[i] + alpha);
#else
      #erorr("Unsupported FLOP count!")
#endif
      A[i] = beta;
    }
    alpha = alpha * (1 - 1e-8);
  }
}
