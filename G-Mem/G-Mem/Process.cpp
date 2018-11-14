//
//  Process.cpp
//  G-Mem
//

#include "ctpl_stl.h"
#include "Process.hpp"

#include <mach/host_info.h>
#include <mach/mach_host.h>
#include <mach/mach.h>
#include <mach/mach_vm.h>
#include <sys/sysctl.h>
#include <iostream>
#include <stdio.h>

Process::Process() : Process(0)
{}

Process::Process(int pid)
: mPid(pid)
{}

std::vector<MemoryChunk*> Process::GetChunks()
{
    return mChunks;
}

void Process::PrintCachedResults(std::vector<u_char *> cache)
{
    const auto offset = 4;
    static std::mutex m;
    kern_return_t kret;
    mach_port_t task;
    
    if ((kret = task_for_pid(mach_task_self_, mPid, &task)) != KERN_SUCCESS) {
        printf("task_for_pid() failed.\n");
        exit(EXIT_FAILURE);
    }
        
    for (auto addr : cache) {
        u_char rawMem[1024] = { 0 };
        
        if ((kret = ::mach_vm_read(task, (mach_vm_address_t) addr, 1024, (vm_offset_t *) rawMem, nullptr)))
        {
            //std::cerr << "Failed to read memory at " << addr << std::endl; The Java counterpart isn't ready to receive this data
            return;
        }
        
        for (auto i = 0; i < (1024 - ((256 - 1) * offset)); i += offset)
        {
            unsigned char wannabeRC4data[1024] = { 0 };
            unsigned char data[256] = { 0 };
            memcpy(wannabeRC4data, rawMem + i, 1024);
            
            auto isvalid = true;
            
            for (auto j = 0; j < 1024; j++)
            {
                if (j % 4 != 0 && wannabeRC4data[j] != 0)
                {
                    isvalid = false;
                    break;
                }
                if (j % 4 == 0)
                {
                    data[j / 4] = wannabeRC4data[j];
                }
            }
            if (isvalid)
            {
                m.lock();
                for (auto idx : data)
                    printf("%02X", static_cast<signed char>(idx) & 0xFF);
                
                std::cout << std::endl;
                m.unlock();
            }
        }
    }
}

void Process::PrintRC4Possibilities()
{
    static std::mutex m;
 
    FindMaps();
    
    const auto offset = 4;
    
    CreateMapsForRC4();
    
    for (auto k = 0; k < mRC4Maps.size(); k++)
    {
        auto mem = mRC4Maps[k];
        
        if (mem->mSize >= 1024 && mem->mSize <= 1024 + 2 * offset)
        {
            for (auto i = 0; i < (mem->mSize - ((256 - 1) * offset)); i += offset)
            {
                unsigned char wannabeRC4data[1024] = { 0 };
                unsigned char data[256] = { 0 };
                memcpy(wannabeRC4data, static_cast<unsigned char *>(mem->mStart) + i, 1024);
                
                auto isvalid = true;
                
                for (auto j = 0; j < 1024; j++)
                {
                    if (j % 4 != 0 && wannabeRC4data[j] != 0)
                    {
                        isvalid = false;
                        break;
                    }
                    if (j % 4 == 0)
                    {
                        data[j / 4] = wannabeRC4data[j];
                    }
                }
                if (isvalid)
                {
                    m.lock();
                    printf("%llx\n",reinterpret_cast<unsigned long long>(mOutCache[k]));
                    for (auto idx : data)
                        printf("%02X", static_cast<signed char>(idx) & 0xFF);
                    
                    std::cout << std::endl;
                    m.unlock();
                }
            }
        }
        delete mem;
    }
}

void Process::CreateMapFromChunk(MemoryChunk *chunk)
{
    const auto offset = 4;
    
    kern_return_t kret;
    mach_port_t task;
    mach_vm_size_t size_read;
    pointer_t dataptr;
    
    
    if ((kret = task_for_pid(mach_task_self_, mPid, &task)) != KERN_SUCCESS) {
        printf("task_for_pid() failed.\n");
        exit(EXIT_FAILURE);
    }
    
    if ((kret = ::mach_vm_read(task, (mach_vm_address_t) chunk->mStart, chunk->mSize, &dataptr, (mach_msg_type_number_t *) &size_read)) != KERN_SUCCESS)
    {
        //std::cerr << "Failed to read memory at " << addr << std::endl; The Java counterpart isn't ready to receive this data
        return;
    }
    
    u_char *dump = (unsigned char *) dataptr;
    
    auto maskCount = 0;
    int nToMap[256] = { 0 };
    int removeMap[256] = { 0 };
    
    for (auto i = 0; i < 256; i++) {
        nToMap[i] = -1;
        removeMap[i] = -1;
    }
    
    auto matchStart = -1;
    auto matchEnd = -1;
    
    for (auto i = 0; i < chunk->mSize; i += offset)
    {
        const auto b = (static_cast<int>(dump[i]) + 128) % 256;
        const auto indInMap = (i / 4) % 256;
        
        const auto deletedNumber = removeMap[indInMap];
        
        if (deletedNumber != -1)
        {
            nToMap[deletedNumber] = -1;
            maskCount--;
            removeMap[indInMap] = -1;
        }
        
        if (nToMap[b] == -1)
        {
            maskCount++;
            removeMap[indInMap] = b;
            nToMap[b] = indInMap;
        }
        else
        {
            removeMap[nToMap[b]] = -1;
            removeMap[indInMap] = b;
            nToMap[b] = indInMap;
        }
        
        if (maskCount == 256)
        {
            if (matchStart == -1)
            {
                matchStart = i - ((256 - 1) * offset);
                matchEnd = i;
            }
            
            if (matchEnd < i - ((256 - 1) * offset))
            {
                mOutCache.push_back(static_cast<u_char *>(chunk->mStart) + matchStart);
                mRC4Maps.push_back(new MemoryChunk(dump + matchStart, matchEnd - matchStart + 4));
                
                matchStart = i - ((256 - 1) * offset);
            }
            matchEnd = i;
        }
    }
    if (matchStart != -1)
    {
        mOutCache.push_back(static_cast<u_char*>(chunk->mStart) + matchStart);
        mRC4Maps.push_back(new MemoryChunk(dump + matchStart, matchEnd - matchStart + 4));
    }
    delete chunk;
}

void Process::CreateMapsForRC4()
{
    ctpl::thread_pool p(5);
    for (auto chunk : mChunks) {
        p.push(std::bind(&Process::CreateMapFromChunk, this, chunk));
    }
    
    p.stop(true);
}



void Process::FindMaps()
{
    
    kern_return_t kret;
    mach_port_t task;
    
    if ((kret = task_for_pid(mach_task_self_, mPid, &task)) != KERN_SUCCESS)
    {
        printf("task_for_pid() failed.\n");
        exit(EXIT_FAILURE);
    }
    
    mach_vm_offset_t address = 0;
    mach_vm_size_t size = 0;
    mach_port_t obj_name;
    vm_region_basic_info_data_64_t info;
    mach_msg_type_number_t count = VM_REGION_BASIC_INFO_COUNT_64;
    
    while (::mach_vm_region(task, &address, &size, VM_REGION_BASIC_INFO_64, (vm_region_info_t) &info, &count, &obj_name) == KERN_SUCCESS) {
        mChunks.push_back(new MemoryChunk(reinterpret_cast<void *>(address), size));
        address += size;
    }
}
    



Process::~Process()
{
    for (auto m : mChunks)
        delete m;
    
    for (auto m : mRC4Maps)
        delete m;
}
