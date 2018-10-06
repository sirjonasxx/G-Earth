#include <mach/host_info.h>
#include <mach/mach_host.h>
#include <mach/mach.h>
#include <sys/sysctl.h>
#include <stdlib.h>
#include "memory.h"
#include "process.h"

size_t _word_align(size_t size)
{
    size_t rsize = 0;
    
    rsize = ((size % sizeof(long)) > 0) ? (sizeof(long) - (size % sizeof(long))) : 0;
    rsize += size;
    
    return rsize;
}

void add_map(struct mem_chunk *head, pointer_t addr, int size)
{
    struct mem_chunk *node = head;
    
    while (node->next != NULL) {
        node = node->next;
    }
    
    node->next = malloc(sizeof(struct mem_chunk));
    node->next->offset = addr;
    node->next->size = size;
    node->next->next = NULL;
}

void create_memory_list(pid_t pid, struct mem_chunk *chunks, int *chunk_count)
{
    kern_return_t kret;
    mach_port_t task;
    int valid_chunks = 0, total_chunks = 0;
    
    if ((kret = task_for_pid(mach_task_self_, pid, &task)) != KERN_SUCCESS)
    {
        printf("task_for_pid() failed.\n");
        exit(EXIT_FAILURE);
    }
    
    mach_vm_offset_t address = 0;
    mach_vm_size_t size = 0, size_read;
    mach_port_t obj_name;
    vm_region_basic_info_data_64_t info;
    mach_msg_type_number_t count = VM_REGION_BASIC_INFO_COUNT_64;
    
    
    while (mach_vm_region(task, &address, &size, VM_REGION_BASIC_INFO_64, (vm_region_info_t) &info, &count, &obj_name) == KERN_SUCCESS) // This iterates through every map
    {
        pointer_t dataptr;
        int offset = 4;
        total_chunks++;
        
        if ((kret = mach_vm_read(task, (mach_vm_address_t) address, size, &dataptr,(mach_msg_type_number_t *) &size_read)) == KERN_SUCCESS) // This gets every map's content
        {
            
            int i;
            int mask_count = 0;
            int n_to_map[256] = {0};
            int remove_map[256] = {0};
            
            for (i = 0; i < 256; i++)
            {
                n_to_map[i] = -1;
                remove_map[i] = -1;
            }
            
            int match_start = -1;
            int match_end = -1;
            
            unsigned char *buffer =  (unsigned char *) dataptr;
            
            for (i = 0; i < size; i += offset)
            {
                int b =  (((int) buffer[i]) + 128) % 256;
                int ind_in_map = (i / 4) % 256;
                
                int deleted_number = remove_map[ind_in_map];
                
                if (deleted_number != -1) {
                    n_to_map[deleted_number] = -1;
                    mask_count--;
                    remove_map[ind_in_map] = -1;
                }
                
                if (n_to_map[b] == -1) {
                    mask_count++;
                    remove_map[ind_in_map] = b;
                    n_to_map[b] = ind_in_map;
                } else {
                    remove_map[n_to_map[b]] = -1;
                    remove_map[ind_in_map] = b;
                    n_to_map[b] = ind_in_map;
                }
                
                if (mask_count == 256) {
                    if (match_start == -1) {
                        match_start = i - ((256 - 1) * offset);
                        match_end = i;
                    }
                    
                    if (match_end < i - ((256 - 1) * offset)) {
                        valid_chunks++;
                        add_map(chunks, address + match_start, match_end - match_start + 4);
                        match_start = i - ((256 - 1) * offset);
                    }
                    match_end = i;
                }
            }
            if (match_start != - 1) {
                valid_chunks++;
                add_map(chunks, address + match_start, match_end - match_start + 4);
            }
        }
        address += size;
    }
    *chunk_count = valid_chunks;
}

void print_rc4_possibilities(pid_t pid, struct mem_chunk *maps)
{
    int j, k;
    int offset = 4;
    
    mach_vm_size_t size_read;
    kern_return_t kret;
    mach_port_t task;
    
    pointer_t dataptr;
    struct mem_chunk *node = maps;
    
    if ((kret = task_for_pid(mach_task_self(), pid, &task)) != KERN_SUCCESS)
    {
        printf("task_for_pid() failed.\n");
        exit(EXIT_FAILURE);
    }
    
    while (node)
    {
        if ((kret = mach_vm_read(task, (mach_vm_address_t) node->offset, node->size, &dataptr, (mach_msg_type_number_t *) &size_read)) == KERN_SUCCESS)
        {
            if (node->size >= 1024 && node->size <= 1024 + 2 * offset)
            {
                unsigned char *buffer = (unsigned char *) dataptr;
                for (j = 0; j < (node->size - ((256 - 1) * offset)); j += offset)
                {
                    unsigned char wannabeRC4data[1024];
                    memcpy(wannabeRC4data, &buffer[j], 1024);
                    
                    int valid = 1;
                    unsigned char data[256];
                    for (k = 0; k < 1024; k++)
                    {
                        if (k % 4 != 0 && wannabeRC4data[k] != 0)
                        {
                            valid = 0;
                            break;
                        }
                        else if (k % 4 == 0)
                        {
                            data[k/4] = wannabeRC4data[k];
                        }
                    }
                    if (valid == 1)
                    {
                        int idx;
                        for (idx = 0; idx < 256; idx++)
                        {
                            printf("%02X", (signed char) data[idx] & 0xFF);
                        }
                        printf("\n");
                    }
                }
            }
        }
        node = node->next;
    }
}
