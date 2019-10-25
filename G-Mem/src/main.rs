use netstat::*;
use read_process_memory::*;
use std::io;
use std::thread;
use std::sync::mpsc;
use procfs::MemoryMap;
use std::thread::JoinHandle;

fn main() {
//    get_rc4_possibilities();
    let pid = get_proc_id() as Pid;
    get_snippet_list(get_mem_maps(pid), pid);
    print!("\n");
}

fn get_mem_maps(pid: Pid) -> Vec<MemoryMap>{
    let mut ret: Vec<MemoryMap> = Vec::new();
    let habbo_proc = procfs::Process::new(pid as i32).unwrap();

    let maps = habbo_proc.maps().unwrap();

    for map in maps {
        if map.perms == String::from("rw-p") {
            ret.push(map);
        }
    }
    return ret;
}

struct MemMap {
    start: usize,
    len: usize,
    mem: Vec<u8>
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

            let mem = read_mem(pid, map.address.0 as usize, (map.address.1 - map.address.0) as usize).
                unwrap();

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
                            start: (map.address.0 + match_start as u64) as usize,
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
                    start: (map.address.0 + match_start as u64) as usize,
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


fn read_mem(pid: Pid, address: usize, size: usize) -> io::Result<Vec<u8>> {
    let handle = pid.try_into_process_handle()?;
    let _bytes = copy_address(address, size, &handle)?;
    Ok(_bytes)
}

fn get_proc_id() -> u32 {
    let sockets_info = get_sockets_info(AddressFamilyFlags::IPV4,
                                        ProtocolFlags::TCP).unwrap();
    for si in sockets_info {
        match si.protocol_socket_info {
            ProtocolSocketInfo::Tcp(tcp_si) => {
                if tcp_si.remote_port == 30000 {
                    return si.associated_pids[0];
                }
            }
            ProtocolSocketInfo::Udp(_) => {}
        }
    }

    return 0;
}
