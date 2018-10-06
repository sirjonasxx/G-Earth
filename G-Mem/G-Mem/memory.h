#ifndef memory_h
#define memory_h

#include <mach/host_info.h>
#include <mach/mach_host.h>
#include <mach/mach.h>

struct mem_chunk {
    pointer_t offset;
    int size;
    struct mem_chunk *next;
};

unsigned char *dump_mem(pid_t pid, mach_vm_offset_t **map);
void get_memory_map(pid_t pid, mach_vm_offset_t *map);
void create_memory_list(pid_t pid, struct mem_chunk *chunks, int *chunk_count);
void print_rc4_possibilities(pid_t pid, struct mem_chunk *maps);
#endif /* memory_h */
