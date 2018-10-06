#include <stdlib.h>
#include <sys/proc_info.h>
#include <libproc.h>

#include <mach/host_info.h>
#include <mach/mach_host.h>
#include <mach/mach.h>
#include <sys/sysctl.h>

#include "process.h"

static int get_process_list(struct kinfo_proc **list, size_t *count);

pid_t find_flash(void)
{
    pid_t pid = 0;
    int i;
    struct kinfo_proc *proc_list;
    size_t proc_count;
    
    get_process_list(&proc_list, &proc_count);
    for (i = 0; i < proc_count; i++) {
        char path[PROC_PIDPATHINFO_MAXSIZE] = {};
        proc_pidpath(proc_list[i].kp_proc.p_pid, path, sizeof(path));
        
        if (strstr(path, "WebKit.Plugin")) {
            pid = proc_list[i].kp_proc.p_pid;
        }
    }
    free(proc_list);
    return pid;
}

static int get_process_list(struct kinfo_proc **list, size_t *count)
{
    int ret = 0;
    static const int name[] = {CTL_KERN, KERN_PROC, KERN_PROC_ALL, 0};
    
    *list = NULL;
    /* Call with a null buffer to get the length */
    ret = sysctl((int *) name, (sizeof(name) / sizeof(*name)) - 1, NULL, count, NULL, 0);
    
    if (ret)
        goto err;
    
    *list = malloc(*count);
    
    if (!*list)
        goto err;
    
    ret = sysctl((int *) name, (sizeof(name) / sizeof(*name)) - 1, *list, count, NULL, 0);
    
    if (ret)
        goto err;
    
    *count /= sizeof(struct kinfo_proc);
    
    return EXIT_SUCCESS;
    
err:
    perror(NULL);
    if (*list)
        free(*list);
    return EXIT_FAILURE;
}
