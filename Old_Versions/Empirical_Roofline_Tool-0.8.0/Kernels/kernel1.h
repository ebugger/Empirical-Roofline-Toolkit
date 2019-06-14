#ifndef KERNEL_H
#define KERNEL_H

void initialize(uint64_t nsize,
                double* __restrict__ array,
                double value);

void kernel(uint64_t nsize,
            uint64_t ntrials,
            double* __restrict__ array,
            int* bytes_per_elem,
            int* mem_accesses_per_elem);

#endif
