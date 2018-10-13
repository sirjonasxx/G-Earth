#pragma once
#include <Windows.h>
#include <vector>

class MemoryChunk
{
public:
	MemoryChunk(LPVOID start, SIZE_T size);
	LPVOID mStart;
	SIZE_T mSize;
};

inline MemoryChunk::MemoryChunk(LPVOID start, SIZE_T size) :
	mStart(start),
	mSize(size)
{}


class Process
{
public:
	Process();
	Process(int pid);
	bool Open();
	void Close();
	void FindMaps(SYSTEM_INFO sys_info);
	void CreateMapsForRC4();
	void CreateMapFromChunk(MemoryChunk *chunk);
	void PrintRC4Possibilities();
	void PrintCachedResults(std::vector<u_char *> cache);
	~Process();
	std::vector<MemoryChunk*> GetChunks();
private:
	int mPid;
	HANDLE mHandle;
	std::vector<MemoryChunk*> mChunks;
	std::vector<MemoryChunk*> mRC4Maps;
	std::vector<LPVOID> mOutCache;
};

