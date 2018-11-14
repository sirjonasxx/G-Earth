//
//  Process.hpp
//  G-Mem
//
//  Created by Eduardo Alonso on 12/11/2018.
//  Copyright © 2018 Eduardo Alonso. All rights reserved.
//

#ifndef Process_hpp
#define Process_hpp

#include <vector>

class MemoryChunk
{
public:
    MemoryChunk(void *start, size_t size);
    void *mStart;
    size_t mSize;
};

inline MemoryChunk::MemoryChunk(void *start, size_t size) :
mStart(start),
mSize(size)
{}


class Process
{
public:
    Process();
    Process(pid_t pid);
    void FindMaps();
    void CreateMapsForRC4();
    void CreateMapFromChunk(MemoryChunk *chunk);
    void PrintRC4Possibilities();
    void PrintCachedResults(std::vector<unsigned char *> cache);
    ~Process();
    std::vector<MemoryChunk*> GetChunks();
private:
    pid_t mPid;
    std::vector<MemoryChunk*> mChunks;
    std::vector<MemoryChunk*> mRC4Maps;
    std::vector<void*> mOutCache;
};

#endif /* Process_hpp */
