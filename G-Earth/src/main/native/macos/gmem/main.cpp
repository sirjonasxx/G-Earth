#include <arpa/inet.h>
#include <libproc.h>
#include <mach/error.h>
#include <mach/mach.h>
#include <mach/mach_vm.h>
#include <netdb.h>
#include <sys/proc_info.h>
#include <unistd.h>

#include <algorithm>
#include <array>
#include <cerrno>
#include <cctype>
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <iomanip>
#include <iostream>
#include <limits>
#include <set>
#include <sstream>
#include <string>
#include <vector>

namespace {

constexpr std::size_t kRc4Entries = 256;
constexpr std::size_t kEntryBytes = sizeof(std::uint32_t);
constexpr std::size_t kRc4Bytes = kRc4Entries * kEntryBytes;
constexpr std::size_t kReadChunkBytes = 4 * 1024 * 1024;

struct Rc4Candidate {
    mach_vm_address_t address;
    std::array<std::uint8_t, kRc4Entries> table{};
};

struct Options {
    std::string host;
    std::uint16_t port = 0;
    bool cacheMode = false;
    std::vector<mach_vm_address_t> cachedAddresses;
};

std::string lowerCase(std::string value) {
    std::transform(value.begin(), value.end(), value.begin(), [](unsigned char character) {
        return static_cast<char>(std::tolower(character));
    });
    return value;
}

bool parseUnsigned(const std::string &value, int base, std::uint64_t &result) {
    if (value.empty()) {
        return false;
    }

    char *end = nullptr;
    errno = 0;
    const unsigned long long parsed = std::strtoull(value.c_str(), &end, base);
    if (errno != 0 || end == value.c_str() || *end != '\0') {
        return false;
    }
    result = static_cast<std::uint64_t>(parsed);
    return true;
}

bool parseOptions(int argc, char **argv, Options &options) {
    if (argc < 3) {
        std::cerr << "Usage: G-Mem <host> <port> [-c<address ...>]\n";
        return false;
    }

    std::uint64_t port = 0;
    if (!parseUnsigned(argv[2], 10, port) || port == 0 || port > 65535) {
        std::cerr << "Invalid TCP port: " << argv[2] << '\n';
        return false;
    }

    options.host = argv[1];
    options.port = static_cast<std::uint16_t>(port);

    if (argc == 3) {
        return true;
    }

    std::string cacheArguments;
    for (int index = 3; index < argc; ++index) {
        std::string argument = argv[index];
        if (index == 3) {
            if (argument.rfind("-c", 0) != 0) {
                std::cerr << "Unknown argument: " << argument << '\n';
                return false;
            }
            options.cacheMode = true;
            argument.erase(0, 2);
        }
        if (!cacheArguments.empty()) {
            cacheArguments.push_back(' ');
        }
        cacheArguments.append(argument);
    }

    std::istringstream addresses(cacheArguments);
    std::string address;
    while (addresses >> address) {
        std::uint64_t parsed = 0;
        if (!parseUnsigned(address, 16, parsed)) {
            std::cerr << "Invalid cached address: " << address << '\n';
            return false;
        }
        options.cachedAddresses.push_back(static_cast<mach_vm_address_t>(parsed));
    }
    return true;
}

std::set<std::string> resolveHost(const std::string &host) {
    std::set<std::string> addresses;
    addrinfo hints{};
    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;

    addrinfo *results = nullptr;
    if (getaddrinfo(host.c_str(), nullptr, &hints, &results) != 0) {
        return addresses;
    }

    for (addrinfo *entry = results; entry != nullptr; entry = entry->ai_next) {
        char text[INET6_ADDRSTRLEN] = {};
        const void *rawAddress = nullptr;
        if (entry->ai_family == AF_INET) {
            rawAddress = &reinterpret_cast<sockaddr_in *>(entry->ai_addr)->sin_addr;
        } else if (entry->ai_family == AF_INET6) {
            rawAddress = &reinterpret_cast<sockaddr_in6 *>(entry->ai_addr)->sin6_addr;
        }
        if (rawAddress != nullptr
                && inet_ntop(entry->ai_family, rawAddress, text, sizeof(text)) != nullptr) {
            addresses.emplace(text);
        }
    }
    freeaddrinfo(results);
    return addresses;
}

std::string remoteAddress(const in_sockinfo &info) {
    char text[INET6_ADDRSTRLEN] = {};
    if ((info.insi_vflag & INI_IPV4) != 0) {
        if (inet_ntop(AF_INET, &info.insi_faddr.ina_46.i46a_addr4, text, sizeof(text)) != nullptr) {
            return text;
        }
    } else if ((info.insi_vflag & INI_IPV6) != 0) {
        if (inet_ntop(AF_INET6, &info.insi_faddr.ina_6, text, sizeof(text)) != nullptr) {
            return text;
        }
    }
    return {};
}

bool hasMatchingSocket(pid_t pid, std::uint16_t port,
                       const std::set<std::string> &addresses, bool requireAddress) {
    const int requiredBytes = proc_pidinfo(pid, PROC_PIDLISTFDS, 0, nullptr, 0);
    if (requiredBytes <= 0) {
        return false;
    }

    const std::size_t entryCount = static_cast<std::size_t>(requiredBytes)
            / sizeof(proc_fdinfo) + 32;
    std::vector<proc_fdinfo> descriptors(entryCount);
    const int bytes = proc_pidinfo(
            pid,
            PROC_PIDLISTFDS,
            0,
            descriptors.data(),
            static_cast<int>(descriptors.size() * sizeof(proc_fdinfo))
    );
    if (bytes <= 0) {
        return false;
    }

    const std::size_t count = static_cast<std::size_t>(bytes) / sizeof(proc_fdinfo);
    for (std::size_t index = 0; index < count; ++index) {
        if (descriptors[index].proc_fdtype != PROX_FDTYPE_SOCKET) {
            continue;
        }

        socket_fdinfo socket{};
        const int socketBytes = proc_pidfdinfo(
                pid,
                descriptors[index].proc_fd,
                PROC_PIDFDSOCKETINFO,
                &socket,
                sizeof(socket)
        );
        if (socketBytes != sizeof(socket) || socket.psi.soi_kind != SOCKINFO_TCP) {
            continue;
        }

        const in_sockinfo &internet = socket.psi.soi_proto.pri_tcp.tcpsi_ini;
        if (ntohs(static_cast<std::uint16_t>(internet.insi_fport)) != port) {
            continue;
        }
        if (!requireAddress || addresses.empty()
                || addresses.count(remoteAddress(internet)) != 0) {
            return true;
        }
    }
    return false;
}

std::vector<pid_t> allProcesses() {
    const int estimatedCount = proc_listallpids(nullptr, 0);
    if (estimatedCount <= 0) {
        return {};
    }

    std::vector<pid_t> processes(static_cast<std::size_t>(estimatedCount) + 128);
    const int actualCount = proc_listallpids(
            processes.data(),
            static_cast<int>(processes.size() * sizeof(pid_t))
    );
    if (actualCount <= 0) {
        return {};
    }
    processes.resize(static_cast<std::size_t>(actualCount));
    return processes;
}

std::string processPath(pid_t pid) {
    std::array<char, PROC_PIDPATHINFO_MAXSIZE> path{};
    if (proc_pidpath(pid, path.data(), static_cast<uint32_t>(path.size())) <= 0) {
        return "pid " + std::to_string(pid);
    }
    return path.data();
}

bool looksLikeHabboClient(pid_t pid) {
    const std::string path = lowerCase(processPath(pid));
    const std::size_t separator = path.find_last_of('/');
    const std::string name = separator == std::string::npos ? path : path.substr(separator + 1);
    return name.find("habbo") != std::string::npos
            || name.find("flash") != std::string::npos
            || name.find("adobe air") != std::string::npos
            || name.find("adobeair") != std::string::npos;
}

std::vector<pid_t> findTargetProcesses(const Options &options) {
    const std::vector<pid_t> processes = allProcesses();
    const std::set<std::string> addresses = resolveHost(options.host);
    std::vector<pid_t> addressMatches;
    std::vector<pid_t> portMatches;
    std::vector<pid_t> nameMatches;

    for (pid_t pid : processes) {
        if (pid <= 0 || pid == getpid()) {
            continue;
        }
        if (hasMatchingSocket(pid, options.port, addresses, true)) {
            addressMatches.push_back(pid);
            continue;
        }
        if (hasMatchingSocket(pid, options.port, addresses, false)) {
            portMatches.push_back(pid);
            continue;
        }
        if (looksLikeHabboClient(pid)) {
            nameMatches.push_back(pid);
        }
    }

    if (!addressMatches.empty()) {
        return addressMatches;
    }
    if (!portMatches.empty()) {
        return portMatches;
    }
    return nameMatches;
}

void addWord(const std::uint8_t *word, std::array<unsigned int, 256> &counts,
             unsigned int &distinct, unsigned int &invalidWords) {
    if (word[1] != 0 || word[2] != 0 || word[3] != 0) {
        ++invalidWords;
    }
    if (counts[word[0]]++ == 0) {
        ++distinct;
    }
}

void removeWord(const std::uint8_t *word, std::array<unsigned int, 256> &counts,
                unsigned int &distinct, unsigned int &invalidWords) {
    if (word[1] != 0 || word[2] != 0 || word[3] != 0) {
        --invalidWords;
    }
    if (--counts[word[0]] == 0) {
        --distinct;
    }
}

std::vector<Rc4Candidate> scanBuffer(const std::uint8_t *memory, std::size_t size,
                                     mach_vm_address_t baseAddress,
                                     std::size_t startLimit) {
    std::vector<Rc4Candidate> candidates;
    if (size < kRc4Bytes || startLimit == 0) {
        return candidates;
    }

    const std::size_t alignment = static_cast<std::size_t>(
            (kEntryBytes - (baseAddress % kEntryBytes)) % kEntryBytes
    );
    if (alignment + kRc4Bytes > size || alignment >= startLimit) {
        return candidates;
    }

    std::array<unsigned int, 256> counts{};
    unsigned int distinct = 0;
    unsigned int invalidWords = 0;
    for (std::size_t entry = 0; entry < kRc4Entries; ++entry) {
        addWord(memory + alignment + entry * kEntryBytes, counts, distinct, invalidWords);
    }

    std::size_t offset = alignment;
    while (true) {
        if (distinct == kRc4Entries && invalidWords == 0) {
            Rc4Candidate candidate{};
            candidate.address = baseAddress + offset;
            for (std::size_t entry = 0; entry < kRc4Entries; ++entry) {
                candidate.table[entry] = memory[offset + entry * kEntryBytes];
            }
            candidates.push_back(candidate);
        }

        const std::size_t next = offset + kEntryBytes;
        if (next >= startLimit || next + kRc4Bytes > size) {
            break;
        }
        removeWord(memory + offset, counts, distinct, invalidWords);
        addWord(memory + offset + kRc4Bytes, counts, distinct, invalidWords);
        offset = next;
    }
    return candidates;
}

bool readMemory(mach_port_t task, mach_vm_address_t address, std::size_t size,
                std::vector<std::uint8_t> &memory) {
    memory.assign(size, 0);
    mach_vm_size_t bytesRead = 0;
    const kern_return_t result = mach_vm_read_overwrite(
            task,
            address,
            static_cast<mach_vm_size_t>(size),
            reinterpret_cast<mach_vm_address_t>(memory.data()),
            &bytesRead
    );
    if (result != KERN_SUCCESS || bytesRead == 0) {
        memory.clear();
        return false;
    }
    memory.resize(static_cast<std::size_t>(bytesRead));
    return true;
}

void printKey(const Rc4Candidate &candidate) {
    const std::ios::fmtflags previousFlags = std::cout.flags();
    const char previousFill = std::cout.fill();
    std::cout << std::hex << std::uppercase << std::setfill('0');
    for (std::uint8_t value : candidate.table) {
        // G-Earth consumes the RC4 state as 256 big-endian 32-bit integers.
        std::cout << std::setw(8) << static_cast<unsigned int>(value);
    }
    std::cout << '\n';
    std::cout.flags(previousFlags);
    std::cout.fill(previousFill);
}

void printCandidate(const Rc4Candidate &candidate, bool includeAddress) {
    if (includeAddress) {
        const std::ios::fmtflags previousFlags = std::cout.flags();
        std::cout << std::hex << candidate.address << '\n';
        std::cout.flags(previousFlags);
    }
    printKey(candidate);
}

bool scanRegion(mach_port_t task, mach_vm_address_t regionAddress, mach_vm_size_t regionSize,
                std::set<mach_vm_address_t> &emittedAddresses) {
    bool found = false;
    mach_vm_size_t consumed = 0;
    while (consumed < regionSize) {
        const mach_vm_size_t remaining = regionSize - consumed;
        const std::size_t primaryBytes = static_cast<std::size_t>(
                std::min<mach_vm_size_t>(remaining, kReadChunkBytes)
        );
        const std::size_t requestedBytes = static_cast<std::size_t>(
                std::min<mach_vm_size_t>(remaining, primaryBytes + kRc4Bytes - kEntryBytes)
        );
        std::vector<std::uint8_t> memory;
        const mach_vm_address_t address = regionAddress + consumed;
        if (readMemory(task, address, requestedBytes, memory)) {
            for (const Rc4Candidate &candidate
                    : scanBuffer(memory.data(), memory.size(), address, primaryBytes)) {
                if (emittedAddresses.insert(candidate.address).second) {
                    printCandidate(candidate, true);
                    found = true;
                }
            }
        }
        consumed += primaryBytes;
    }
    return found;
}

bool scanProcess(mach_port_t task) {
    bool found = false;
    std::set<mach_vm_address_t> emittedAddresses;
    mach_vm_address_t address = 0;

    while (true) {
        mach_vm_size_t size = 0;
        vm_region_basic_info_data_64_t information{};
        mach_msg_type_number_t count = VM_REGION_BASIC_INFO_COUNT_64;
        mach_port_t objectName = MACH_PORT_NULL;
        const kern_return_t result = mach_vm_region(
                task,
                &address,
                &size,
                VM_REGION_BASIC_INFO_64,
                reinterpret_cast<vm_region_info_t>(&information),
                &count,
                &objectName
        );
        if (result != KERN_SUCCESS) {
            break;
        }
        if (objectName != MACH_PORT_NULL) {
            mach_port_deallocate(mach_task_self(), objectName);
        }

        if (size >= kRc4Bytes
                && (information.protection & VM_PROT_READ) != 0
                && (information.protection & VM_PROT_WRITE) != 0) {
            found = scanRegion(task, address, size, emittedAddresses) || found;
        }

        const mach_vm_address_t next = address + size;
        if (next <= address) {
            break;
        }
        address = next;
    }
    return found;
}

bool scanCachedAddresses(mach_port_t task,
                         const std::vector<mach_vm_address_t> &addresses) {
    bool found = false;
    for (mach_vm_address_t address : addresses) {
        std::vector<std::uint8_t> memory;
        if (!readMemory(task, address, kRc4Bytes, memory) || memory.size() < kRc4Bytes) {
            continue;
        }
        const std::vector<Rc4Candidate> candidates = scanBuffer(
                memory.data(), memory.size(), address, 1
        );
        if (!candidates.empty()) {
            printCandidate(candidates.front(), false);
            found = true;
        }
    }
    return found;
}

int runSelfTest() {
    constexpr std::size_t tableOffset = 256;
    constexpr mach_vm_address_t baseAddress = 0x100000000;
    std::vector<std::uint8_t> memory(2048, 0xA5);
    for (std::size_t index = 0; index < kRc4Entries; ++index) {
        const std::uint8_t value = static_cast<std::uint8_t>((index * 73) % 256);
        const std::size_t offset = tableOffset + index * kEntryBytes;
        memory[offset] = value;
        memory[offset + 1] = 0;
        memory[offset + 2] = 0;
        memory[offset + 3] = 0;
    }

    std::vector<Rc4Candidate> candidates = scanBuffer(
            memory.data(), memory.size(), baseAddress, memory.size()
    );
    const auto expected = std::find_if(candidates.begin(), candidates.end(), [](const auto &candidate) {
        return candidate.address == baseAddress + tableOffset;
    });
    if (expected == candidates.end()) {
        std::cerr << "Self-test failed: RC4 table was not detected\n";
        return 1;
    }

    memory[tableOffset + 1] = 1;
    candidates = scanBuffer(memory.data(), memory.size(), baseAddress, memory.size());
    const auto invalid = std::find_if(candidates.begin(), candidates.end(), [](const auto &candidate) {
        return candidate.address == baseAddress + tableOffset;
    });
    if (invalid != candidates.end()) {
        std::cerr << "Self-test failed: invalid table was accepted\n";
        return 1;
    }

    std::cout << "G-Mem self-test passed\n";
    return 0;
}

} // namespace

int main(int argc, char **argv) {
    if (argc == 2 && std::strcmp(argv[1], "--self-test") == 0) {
        return runSelfTest();
    }

    Options options;
    if (!parseOptions(argc, argv, options)) {
        return 2;
    }

    const std::vector<pid_t> processes = findTargetProcesses(options);
    if (processes.empty()) {
        std::cerr << "No process owns a connection to " << options.host << ':' << options.port
                  << ", and no Habbo/Flash/AIR process was found\n";
        return 3;
    }

    bool attached = false;
    bool found = false;
    for (pid_t pid : processes) {
        mach_port_t task = MACH_PORT_NULL;
        const kern_return_t result = task_for_pid(mach_task_self(), pid, &task);
        if (result != KERN_SUCCESS) {
            std::cerr << "Unable to inspect " << processPath(pid) << " (pid " << pid << "): "
                      << mach_error_string(result) << ". macOS requires the target application "
                      << "to allow debugging (com.apple.security.get-task-allow).\n";
            continue;
        }

        attached = true;
        found = (options.cacheMode
                ? scanCachedAddresses(task, options.cachedAddresses)
                : scanProcess(task)) || found;
        mach_port_deallocate(mach_task_self(), task);
    }

    if (!attached) {
        std::cerr << "G-Mem is native for Apple Silicon, but macOS denied every task port. "
                  << "Check the target application's signing entitlements.\n";
        return 4;
    }
    if (!found) {
        std::cerr << "No RC4 table was found in the readable memory regions\n";
    }
    return 0;
}
