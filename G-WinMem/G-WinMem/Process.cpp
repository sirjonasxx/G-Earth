
#include "ctpl_stl.h"
#include "Process.h"

#include <iostream>
#include <stdio.h>

Process::Process() : Process(0)
{}

Process::Process(int pid)
	: mPid(pid),
	  mHandle(nullptr)
{}

bool Process::Open()
{
	mHandle = OpenProcess(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ | PROCESS_VM_OPERATION, false, mPid);

	return true;
}

std::vector<MemoryChunk*> Process::GetChunks()
{
	return mChunks;
}

void Process::Close()
{
	CloseHandle(mHandle);
}

void Process::PrintCachedResults(std::vector<u_char *> cache)
{
	const auto offset = 4;
	static std::mutex m;
	Open();

	for (auto addr : cache) {
		u_char rawMem[1024] = { 0 };

		if (!ReadProcessMemory(mHandle, addr, rawMem, 1024, nullptr))
		{
			std::cerr << "Failed to read memory at " << addr << std::endl;
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
	Close();
}

void Process::PrintRC4Possibilities()
{
	SYSTEM_INFO sys_info;

	static std::mutex m;

	GetSystemInfo(&sys_info);

	Open();
	
	FindMaps(sys_info);

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
	Close();
}

void Process::CreateMapFromChunk(MemoryChunk *chunk)
{
	const auto offset = 4;
	const auto dump = new unsigned char[chunk->mSize + 1];

	memset(dump, 0, chunk->mSize + 1);

	if (!ReadProcessMemory(mHandle, chunk->mStart, dump, chunk->mSize, nullptr))
	{
		std::cerr << "Failed to read memory at: " << chunk->mStart << std::endl;
		return;
	}

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
				//printf("maybeValid -> %p\n", static_cast<u_char*>(chunk->mStart) + matchStart);
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



void Process::FindMaps(SYSTEM_INFO sys_info)
{

	auto addr = reinterpret_cast<uintptr_t>(sys_info.lpMinimumApplicationAddress);
	const auto end = reinterpret_cast<uintptr_t>(sys_info.lpMaximumApplicationAddress);

	MEMORY_BASIC_INFORMATION mbi;

	while (addr < end) {
		if (!VirtualQueryEx(mHandle, reinterpret_cast<LPCVOID>(addr), &mbi, sizeof(mbi))) {
			std::cerr << "Failed to get memory maps\n";
			return;
		}

		if (mbi.State == MEM_COMMIT && ((mbi.Protect & PAGE_GUARD) == 0) && ((mbi.Protect & PAGE_NOACCESS) == 0)) {
			mChunks.push_back(new MemoryChunk(reinterpret_cast<LPVOID>(addr), mbi.RegionSize));
		}
		addr += mbi.RegionSize;
	}
}



Process::~Process()
{
	for (auto m : mChunks)
		delete m;

	for (auto m : mRC4Maps)
		delete m;
}
