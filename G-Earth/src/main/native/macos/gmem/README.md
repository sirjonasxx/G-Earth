# G-MemZ for macOS

This directory contains the source for G-Earth's macOS memory helper. It has no third-party dependencies and is compiled by the `mac-bundle` Maven profile as a universal `arm64`/`x86_64` executable.

The helper locates the process that owns the requested Habbo TCP connection, requests its Mach task port, scans readable/writable regions in bounded chunks and writes protocol-compatible address/key pairs to standard output. Cached mode accepts `-c` followed by hexadecimal addresses and writes keys only.

Run the native algorithm test with:

```shell
G-MemZ --self-test
```

The packaged executable is ad-hoc signed with Hardened Runtime and `com.apple.security.cs.debugger`. macOS still requires the inspected target to opt into debugging with `com.apple.security.get-task-allow`; the helper does not weaken or rewrite the target application's signature.
