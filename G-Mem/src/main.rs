use std::thread::JoinHandle;
use std::{thread, io, env, mem};
use std::net::{IpAddr, Ipv4Addr, Ipv6Addr};
use std::sync::mpsc;
use std::convert::TryInto;

#[cfg(unix)]
use procfs::process::MemoryMap;

use netstat::*;
use read_process_memory::*;

#[cfg(windows)]
use winapi::um::winnt::{MEMORY_BASIC_INFORMATION, PMEMORY_BASIC_INFORMATION, MEM_COMMIT, PAGE_GUARD, PAGE_NOACCESS, PROCESS_QUERY_INFORMATION, PROCESS_VM_OPERATION, PROCESS_VM_READ, HANDLE};
#[cfg(windows)]
use winapi::um::processthreadsapi::OpenProcess;
#[cfg(windows)]
use winapi::um::memoryapi::VirtualQueryEx;
#[cfg(windows)]
use winapi::um::sysinfoapi::{SYSTEM_INFO, GetSystemInfo, LPSYSTEM_INFO};
#[cfg(windows)]
use winapi::shared::minwindef::LPVOID;
use std::str::FromStr;

struct MemMap {
    start: usize,
    len: usize,
    mem: Vec<u8>
}

#[cfg(windows)]
struct MemoryMap {
    address: (u64, u64)
}

fn main() {
    let args: Vec<String> = env::args().collect();

    if args.len() < 3 {
        println!("Usage: G-Mem <IPAddress> <Port>");
        return;
    }
    let habbo_pid = get_proc_id(args[1].clone(), args[2].parse::<u16>().unwrap());
    get_snippet_list(get_mem_maps(habbo_pid.try_into().unwrap()), habbo_pid.try_into().unwrap());
}


fn read_mem(pid: Pid, address: usize, size: usize) -> io::Result<Vec<u8>> {
    let handle = pid.try_into_process_handle()?;
    let _bytes = copy_address(address, size, &handle)?;
    Ok(_bytes)
}

fn get_proc_id(ip: String, port: u16) -> u32 {
    let af_flags = AddressFamilyFlags::IPV4;
    let proto_flags = ProtocolFlags::TCP;
    let sockets_info = get_sockets_info(af_flags, proto_flags).unwrap();

    for si in sockets_info {
        match si.protocol_socket_info {
            ProtocolSocketInfo::Tcp(tcp_si) => {
                if tcp_si.remote_port == port && tcp_si.remote_addr == ip.parse::<IpAddr>().unwrap() {
                    return si.associated_pids[0];
                }
            }
            ProtocolSocketInfo::Udp(_) => {}
        }
    }
    return 0;
}

fn get_snippet_list(maps: Vec<MemoryMap>, pid: Pid) {
    let (tx, rx) = mpsc::channel();

    let mut handles: Vec<JoinHandle<()>> = Vec::new();

    for map in maps {
        let tx1 = mpsc::Sender::clone(&tx);
        let handle = thread::spawn(move || {
            let mut n_to_map: [i32; 256] = [-1; 256];
            let mut remove_map: [i32; 256] = [-1; 256];
            let mut mask_count = 0;
            let mut match_start : i64 = -1;
            let mut match_end: i64 = -1;

            let size = ((map.address.1 as u64) - (map.address.0 as u64)) as usize;
            let mem = read_mem(pid, map.address.0 as usize, size).unwrap();

            for (i, data) in mem.iter().step_by(4).enumerate() {
                let offset = 4;
                let b = ((*data as u16 + 128) % 256) as u8;
                let ind_in_map = (((i) as i64) % 256) as i32;

                let deleted_number = remove_map[ind_in_map as usize];
                if deleted_number != -1 {
                    n_to_map[deleted_number as usize] = -1;
                    mask_count -= 1;
                    remove_map[ind_in_map as usize] = -1;
                }

                if n_to_map[b as usize] == -1 {
                    mask_count += 1;
                    remove_map[ind_in_map as usize] = b as i32;
                    n_to_map[b as usize] = ind_in_map;
                } else {
                    remove_map[n_to_map[b as usize] as usize] = -1;
                    remove_map[ind_in_map as usize] = b as i32;
                    n_to_map[b as usize] = ind_in_map;
                }

                if mask_count == 256 {
                    if match_start == -1 {
                        match_start = (i * 4 - ((256 - 1) * offset)) as i64;
                        match_end = (i * 4) as i64;
                    }

                    if match_end < (i*4 - (256 - 1) * offset) as i64 {
                        let m = MemMap {
                            start: (map.address.0 as u64 + match_start as u64) as usize,
                            len: (match_end - match_start as i64 + 4) as usize,
                            mem: mem[match_start as usize..(match_end + 4) as usize].to_vec()
                        };
                        tx1.send(m).unwrap();
                        match_start = (i*4 - ((256 - 1) * offset)) as i64;
                    }
                    match_end = (i*4) as i64;
                }
            }
            if match_start != -1 {
                let m = MemMap {
                    start: (map.address.0 as u64 + match_start as u64) as usize,
                    len: (match_end - match_start + 4) as usize,
                    mem: mem[match_start as usize..(match_end + 4) as usize].to_vec()
                };
                tx1.send(m).unwrap();
            }
        });
        handles.push(handle);
    }

    thread::spawn(move || {
        for handle in handles {
            handle.join().unwrap();
        }
        let tx1 = mpsc::Sender::clone(&tx);
        tx1.send(MemMap{
            start: 0,
            len: 0,
            mem: Vec::new()
        }).unwrap();
    });

    for received in rx {
        if received.start == 0 { break; }
        get_rc4_possibilities(received);
    }
}

fn get_rc4_possibilities(snippet: MemMap) {
    let offset = 4;

    if snippet.len >= 1024 && snippet.len <= 1024 + 2 * offset {

        for i in (0..snippet.len - ((256 - 1) * offset)).step_by(4) {
            let wannabe_rc4_data = snippet.mem[i..1024 + i].to_vec();
            let mut data: [u8; 256] = [0xff; 256];

            let mut is_valid = true;
            for j in 0..1024 {
                if j % 4 != 0 && wannabe_rc4_data[j] != 0 {
                    is_valid = false;
                    break;
                }
                if j % 4 == 0 {
                    data[j / 4] = wannabe_rc4_data[j];
                }
            }
            if is_valid == true {
                for byte in data.iter() {
                    print!("{:02x}", byte);
                }
                print!("\n");
            }
        }
    }
}

#[cfg(windows)]
fn get_handle(pid: Pid) -> HANDLE {
    unsafe {
        return OpenProcess(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ | PROCESS_VM_OPERATION, 0, pid);
    }
}

#[cfg(windows)]
fn get_mem_maps(pid: Pid) -> Vec<MemoryMap> {

    let mut s_info: SYSTEM_INFO = unsafe {mem::zeroed()};
    let s_info_ptr = &mut s_info as LPSYSTEM_INFO;

    let mut maps : Vec<MemoryMap> = Vec::new();
    unsafe {
        GetSystemInfo(s_info_ptr);

        let mut addr = s_info.lpMinimumApplicationAddress;
        let end = s_info.lpMaximumApplicationAddress;
        let handle = get_handle(pid);


        while (addr as u64) < (end as u64) {
            let mut mbi: MEMORY_BASIC_INFORMATION = mem::zeroed();
            let mbi_ptr = &mut mbi as PMEMORY_BASIC_INFORMATION;

            VirtualQueryEx(handle, addr, mbi_ptr, mem::size_of::<MEMORY_BASIC_INFORMATION>());

            if mbi.State == MEM_COMMIT && ((mbi.Protect & PAGE_GUARD) == 0) && ((mbi.Protect & PAGE_NOACCESS) == 0) {
                maps.push(MemoryMap{
                    address: (addr as u64, (addr as u64) + mbi.RegionSize as u64)
                });
            }

            addr = (addr as u64 + mbi.RegionSize as u64) as LPVOID;
        }
    }
    return maps;
}

#[cfg(unix)]
fn get_mem_maps(pid: Pid) -> Vec<MemoryMap>{
    let mut ret: Vec<MemoryMap> = Vec::new();
    let habbo_proc = procfs::process::Process::new(pid as i32).unwrap();

    let maps = habbo_proc.maps().unwrap();

    for map in maps {
        if map.perms == String::from("rw-p") {
            ret.push(map);
        }
    }
    return ret;
}