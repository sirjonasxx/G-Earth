**NOTE: Currently supported browsers: ONLY Firefox and Chromium, works on Habbo AIR too**

# MacOS Installation Guide

G-Earth depends on an application named [G-Mem](https://github.com/sirjonasxx/G-Mem), 
this application scans the memory contents of the Habbo client applicaton and extracts a cipher key 
that is used to decrypt packets coming from the Habbo server.

There is a few steps u have to complete in order to get it to work on MacOS.

## Code-Sign G-Mem file and make it executable 

### Certificate Creation
1. Open `Keychain Access` (press `⌘ + Enter`, type `KeyChain Access` to open it from spotlight)
2. Navigate from the top menu to `Keychain Access > Certificate Assistant > Create Certificate...`
![Screenshot 2023-03-30 at 14 36 47](https://user-images.githubusercontent.com/102377087/228837955-81182786-ac47-46e5-a5e2-1ca2e257751f.png)
3. In the `Create Your Certificate` do the following:
* Set `Name:` to `gmem-cert`
* Set `Cerificate Type:` to `Code Signing`
* Toggle the `Let me override default` button

![Screenshot 2023-03-30 at 14 40 28](https://user-images.githubusercontent.com/102377087/228838867-57e465bc-5b83-4b1a-a8cc-3dd6d1e95353.png)

5. Press `Continue` until you reach the `Specify a Location For The Certificate`, now do the following:
* Set `Keychain:` to `System`

![Screenshot 2023-03-30 at 14 42 50](https://user-images.githubusercontent.com/102377087/228839468-982365d9-925c-44cf-a87d-fc6c268d05c8.png)

6. Enter your login credentials when prompted and press `Done`

### Signing of G-Mem
1. Open `Terminal` (press `⌘ + Enter`, type `Terminal` to open it from spotlight)
2. Type `codesign -fs "gmem-cert" ` (do not press enter yet)
3. Drag the `G-Mem` file into your terminal window (this will append the path to the file)

Your terminal window should now resemble the following:

![Screenshot 2023-03-30 at 14 49 28](https://user-images.githubusercontent.com/102377087/228841126-77b0184b-4c7d-44e0-9f7c-56103a957a81.png)

4. Now press enter and enter your login credentials when prompted.

### Making G-Mem executable
1. Open `Terminal` (press `⌘ + Enter`, type `Terminal` to open it from spotlight)
2. Type `chmod 755 ` (do not press enter yet)
3. Drag the `G-Mem` file into your terminal window (this will append the path to the file)

Your terminal window should now resemble the following:

![Screenshot 2023-03-30 at 14 52 29](https://user-images.githubusercontent.com/102377087/228841918-3205014b-5de8-431d-ae4d-d10b8ceeed03.png)

4. Now press enter and verify the `Kind` of the `G-Mem` file is not `Unix Executable File`

![Screenshot 2023-03-30 at 14 54 15](https://user-images.githubusercontent.com/102377087/228842389-78ea857e-3414-43d0-8270-91f8185ab57f.png)

## Disabling SIP 

Modern machines running MacOS have a security feature that shields of the memory of processes from other processes. 
Depending on your machine you may have to disable SIP. 

**For M1 macs it is required to disable SIP.**

### :warning: CAUTION :warning:
Turning off SIP allows any program with sudo privileges to modify memory contents of other processes. If you use pirated software, or other unverified apps, DO NOT DO THIS for your own safety! See the following stackoverflow post for some more info: https://apple.stackexchange.com/a/412281.

A guide for disabling SIP can be found here: https://developer.apple.com/documentation/security/disabling_and_enabling_system_integrity_protection

## Launching G-Earth
1. Open `Terminal` (press `⌘ + Enter`, type `Terminal` to open it from spotlight)
2. Type `sudo java -jar `
3. Drag the `G-Earth.jar` file into your terminal window (this will append the path to the file)

Your terminal window should now resemble the following:

![Screenshot 2023-03-30 at 15 00 59](https://user-images.githubusercontent.com/102377087/228843994-f7713373-9f19-49b0-b7e7-0645a16c4fce.png)

5. Press enter and fill in your password if prompted

## Troubleshooting

If you experience any other issues and the [Troubleshooting Page](https://github.com/sirjonasxx/G-Earth/wiki/Troubleshooting) doesn't help, 

it might be useful to have a look at the following issues: [#67](../issues/67) [#10](../issues/10)

