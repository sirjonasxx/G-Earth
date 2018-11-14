//
//  main.cpp
//  G-Mem
//

#include <iostream>
#include <thread>
#include <stdlib.h>
#include <sys/proc_info.h>
#include <libproc.h>

#include <mach/host_info.h>
#include <mach/mach_host.h>
#include <mach/mach.h>
#include <sys/sysctl.h>
#include "Process.hpp"

static int get_process_list(struct kinfo_proc **list, size_t *count);
int is_flash_process(int pid);

std::vector<pid_t> find_flash(void)
{
    std::vector<pid_t> ret;
    int i;
    struct kinfo_proc *proc_list;
    size_t proc_count;
    
    get_process_list(&proc_list, &proc_count);
    for (i = 0; i < proc_count; i++) {
        char path[PROC_PIDPATHINFO_MAXSIZE] = {};
        proc_pidpath(proc_list[i].kp_proc.p_pid, path, sizeof(path));
        
        if (strstr(path, "Fire") || strstr(path, "Helper")) {
            if (is_flash_process(proc_list[i].kp_proc.p_pid))
                    ret.push_back(proc_list[i].kp_proc.p_pid);
        }
    }
    free(proc_list);
    return ret;
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
    
    *list = (struct kinfo_proc *) malloc(*count);
    
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

// https://gist.github.com/nonowarn/770696
int is_flash_process(int pid) {
    int    mib[3], argmax, nargs, c = 0;
    size_t    size;
    char    *procargs, *sp, *np, *cp;
    int show_args = 1;
    
    mib[0] = CTL_KERN;
    mib[1] = KERN_ARGMAX;
    
    size = sizeof(argmax);
    if (sysctl(mib, 2, &argmax, &size, NULL, 0) == -1) {
        goto ERROR_A;
    }
    
    /* Allocate space for the arguments. */
    procargs = (char *)malloc(argmax);
    if (procargs == NULL) {
        goto ERROR_A;
    }
    
    
    /*
     * Make a sysctl() call to get the raw argument space of the process.
     * The layout is documented in start.s, which is part of the Csu
     * project.  In summary, it looks like:
     *
     * /---------------\ 0x00000000
     * :               :
     * :               :
     * |---------------|
     * | argc          |
     * |---------------|
     * | arg[0]        |
     * |---------------|
     * :               :
     * :               :
     * |---------------|
     * | arg[argc - 1] |
     * |---------------|
     * | 0             |
     * |---------------|
     * | env[0]        |
     * |---------------|
     * :               :
     * :               :
     * |---------------|
     * | env[n]        |
     * |---------------|
     * | 0             |
     * |---------------| <-- Beginning of data returned by sysctl() is here.
     * | argc          |
     * |---------------|
     * | exec_path     |
     * |:::::::::::::::|
     * |               |
     * | String area.  |
     * |               |
     * |---------------| <-- Top of stack.
     * :               :
     * :               :
     * \---------------/ 0xffffffff
     */
    mib[0] = CTL_KERN;
    mib[1] = KERN_PROCARGS2;
    mib[2] = pid;
    
    
    size = (size_t)argmax;
    if (sysctl(mib, 3, procargs, &size, NULL, 0) == -1) {
        goto ERROR_B;
    }
    
    memcpy(&nargs, procargs, sizeof(nargs));
    cp = procargs + sizeof(nargs);
    
    /* Skip the saved exec_path. */
    for (; cp < &procargs[size]; cp++) {
        if (*cp == '\0') {
            /* End of exec_path reached. */
            break;
        }
    }
    if (cp == &procargs[size]) {
        goto ERROR_B;
    }
    
    /* Skip trailing '\0' characters. */
    for (; cp < &procargs[size]; cp++) {
        if (*cp != '\0') {
            /* Beginning of first argument reached. */
            break;
        }
    }
    if (cp == &procargs[size]) {
        goto ERROR_B;
    }
    /* Save where the argv[0] string starts. */
    sp = cp;
    
    /*
     * Iterate through the '\0'-terminated strings and convert '\0' to ' '
     * until a string is found that has a '=' character in it (or there are
     * no more strings in procargs).  There is no way to deterministically
     * know where the command arguments end and the environment strings
     * start, which is why the '=' character is searched for as a heuristic.
     */
    for (np = NULL; c < nargs && cp < &procargs[size]; cp++) {
        if (*cp == '\0') {
            c++;
            if (np != NULL) {
                /* Convert previous '\0'. */
                *np = ' ';
            } else {
                /* *argv0len = cp - sp; */
            }
            /* Note location of current '\0'. */
            np = cp;
            
            if (!show_args) {
                /*
                 * Don't convert '\0' characters to ' '.
                 * However, we needed to know that the
                 * command name was terminated, which we
                 * now know.
                 */
                break;
            }
        }
    }
    
    /*
     * sp points to the beginning of the arguments/environment string, and
     * np should point to the '\0' terminator for the string.
     */
    if (np == NULL || np == sp) {
        /* Empty or unterminated string. */
        goto ERROR_B;
    }
    
    
    
    /* Clean up. */
    free(procargs);
    
    if (strstr(sp, "Flash") || strstr(sp, "ppapi"))
        return 1;
    return 0;
    
ERROR_B:
    free(procargs);
ERROR_A:
    fprintf(stderr, "Sorry, failed\n");
    exit(2);
}

int main(int argc, char **argv)
{
    std::vector<u_char *> cachedOffsets;
    auto usingCache = false;
    
    if (argc > 3)
        if (!strncmp(argv[3], "-c", 2)) // Cache mode
        {
            usingCache = true;
            
            for (auto i = 0; i < argc - 4; i++)
                cachedOffsets.push_back(reinterpret_cast<u_char *>(strtoull(argv[4 + i], nullptr, 16)));
        }
    
    if (argc >= 3) {
        auto pids = find_flash();
        std::vector<std::thread> threads;
        
        for (auto pid : pids) {
                auto p = new Process(pid);
                if (usingCache)
                    p->PrintCachedResults(cachedOffsets);
                else
                    threads.push_back(std::thread (std::bind(&Process::PrintRC4Possibilities, p)));
        }
        
        for (auto i = 0; i < threads.size(); i++)
            if (threads[i].joinable())
                threads[i].join();
        
        if (pids.empty())
            std::cerr << "No pids found\n";
    }
    
    return 0;
}
