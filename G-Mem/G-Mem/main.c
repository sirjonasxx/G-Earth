#include <stdio.h>
#include <stdlib.h>
#include "process.h"
#include "memory.h"

int main(int argc, const char * argv[]) {
    pid_t pid = find_flash();
    int count;

    struct mem_chunk chunks;
    chunks.offset = 0;
    chunks.size = 0;
    chunks.next = NULL;
    
    create_memory_list(pid, &chunks, &count);
    
    print_rc4_possibilities(pid, &chunks);
    
    /* free map list */
    struct mem_chunk *node = chunks.next;
    while (node)
    {
        struct mem_chunk *to_free = node;
        node = node->next;
        free(to_free);
    }
    
    return 0;
}
