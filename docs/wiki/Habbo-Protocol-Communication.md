# Habbo Protocol Communication Guide

> A comprehensive guide to understanding how G-Earth communicates with the Habbo server.

## Table of Contents
1. [Overview](#overview)
2. [Core Concepts](#core-concepts)
3. [Packet Structure](#packet-structure)
4. [Message Directions](#message-directions)
5. [Packet Formats](#packet-formats)
6. [Reading & Writing Packets](#reading--writing-packets)
7. [Message Interception](#message-interception)
8. [Extension Communication](#extension-communication)
9. [Examples](#examples)

---

## Overview

G-Earth acts as a **proxy** between the Habbo client and the official Habbo servers. It intercepts all network traffic, allowing extensions to:
- Monitor messages flowing in both directions
- Modify packets before they reach their destination
- Inject new packets into the stream
- Parse message data using a structure-based system

The communication protocol is packet-based, using binary encoding optimized for performance and minimal bandwidth.

---

## Core Concepts

### HPacket
The fundamental unit of communication. Each packet contains:
- **Header ID**: A 2-byte identifier that uniquely identifies the packet type
- **Body**: Variable-length data containing the message payload
- **Format**: Different encoding formats (EVA_WIRE for legacy, WEDGIE for modern clients)

### HMessage
A wrapper around HPacket that adds:
- **Direction**: Whether the packet goes TO_SERVER or TO_CLIENT
- **Index**: Sequential numbering for message ordering
- **Blocked Flag**: Whether the message was intercepted/blocked

### PacketInfo
Contains metadata about a packet:
- **Header ID**: Numeric identifier
- **Name**: Human-readable name (e.g., "Chat")
- **Hash**: Unique identifier for the packet structure
- **Structure**: Format string describing the data layout (e.g., "is" = integer + string)

---

## Packet Structure

### Binary Layout

```
[Length: 4 bytes] [Header ID: 2 bytes] [Data: variable]
```

#### Legacy EVA_WIRE Format
- Bytes 0-3: Total packet length (including header and data)
- Bytes 4-5: Header ID (short, 2 bytes)
- Bytes 6+: Payload data

#### Modern WEDGIE Formats
Uses Base64-encoded headers and VL64 encoding for optimal bandwidth usage.

### Structure Notation

Packets have a **structure** that describes how data is organized:

| Notation | Type | Size | Details |
|----------|------|------|---------|
| `i` | Integer | 4 bytes | 32-bit signed integer |
| `s` | String | variable | Prefixed length (2 bytes) + ASCII data |
| `b` | Byte | 1 byte | Single signed byte (-128 to 127) |
| `B` | Boolean | 1 byte | 0 = false, 1 = true |
| `u` | Unsigned Short | 2 bytes | 16-bit unsigned integer (0-65535) |
| `l` | Long | 8 bytes | 64-bit signed integer |
| `d` | Double | 8 bytes | 64-bit floating point |
| `f` | Float | 4 bytes | 32-bit floating point |

**Examples:**
- `"is"` = Integer + String
- `"isi(b)"` = Integer + String + Integer + (Byte inside brackets = nested structure)
- `"sisl"` = String + Integer + String + Long

---

## Message Directions

### TOSERVER
Packets flowing from the **Habbo client to the Habbo server**
- User actions (chat, movement, room interactions)
- Login credentials
- Item usage

### TOCLIENT
Packets flowing from the **Habbo server to the Habbo client**
- Chat messages from other users
- Room updates
- User data
- Item information

---

## Packet Formats

G-Earth supports multiple packet encoding formats:

### EVA_WIRE (Legacy)
- Used by older Habbo clients
- Simple binary format with raw headers
- Wide compatibility

### WEDGIE_INCOMING
- Modern format for server → client packets
- Base64-encoded headers
- Uses VL64 encoding for integers
- Smaller bandwidth footprint

### WEDGIE_OUTGOING
- Modern format for client → server packets
- Similar to WEDGIE_INCOMING but optimized for outgoing traffic
- Base64 header encoding

---

## Reading & Writing Packets

### Creating a Packet (Writing)

```java
// Method 1: Using Header ID
HPacket packet = new HPacket(123); // Header ID 123
packet.appendInt(42);
packet.appendString("Hello");

// Method 2: Using Packet Name/Hash (incomplete until completed with packet info)
HPacket packet = new HPacket("Chat", HMessage.Direction.TOSERVER);
packet.appendInt(1);
packet.appendString("Hello everyone!");

// Method 3: From bytes
byte[] data = {...};
HPacket packet = new HPacket(data);

// Method 4: From string expression
HPacket packet = new HPacket("{h:123}[42]{s:\"Hello\"}");
```

### Reading a Packet (Parsing)

```java
HPacket packet = ...; // Received packet
packet.resetReadIndex(); // Start from beginning

// Read data in order
int userId = packet.readInteger();
String username = packet.readString();
boolean premium = packet.readBoolean();
byte level = packet.readByte();

// Check if we've reached end of packet
if (packet.isEOF() == 1) {
    // Successfully read all data
}
```

### Common Append Methods

```java
packet.appendByte((byte) 1);
packet.appendInt(42);
packet.appendString("text");
packet.appendLong(9999999L);
packet.appendDouble(3.14);
packet.appendFloat(2.71f);
packet.appendBoolean(true);
packet.appendBytes(new byte[]{1, 2, 3});
packet.appendShort((short) 100);
```

### Common Read Methods

```java
byte b = packet.readByte();
int i = packet.readInteger();
String s = packet.readString();
long l = packet.readLong();
double d = packet.readDouble();
float f = packet.readFloat();
boolean flag = packet.readBoolean();
short sh = packet.readShort();
byte[] bytes = packet.readBytes(5); // Read 5 bytes
```

---

## Message Interception

### Intercepting Messages in Extensions

```java
public class MyExtension extends Extension {
    
    public MyExtension(String[] args) {
        super(args);
    }

    @Override
    public void onStartConnection() {
        // Listen to ALL incoming messages from server
        intercept(HMessage.Direction.TOCLIENT, message -> {
            // This is called for EVERY message from server
            return true; // return true to block, false to allow
        });

        // Listen to specific message by Header ID
        intercept(HMessage.Direction.TOSERVER, 107, message -> {
            // Only for messages with header ID 107
            System.out.println("Intercepted packet: " + message.getPacket().toString());
            return false; // Allow message to pass
        });

        // Listen to specific message by Name/Hash
        intercept(HMessage.Direction.TOCLIENT, "Chat", message -> {
            HPacket packet = message.getPacket();
            packet.resetReadIndex();
            
            // Parse chat message
            int userId = packet.readInteger();
            String username = packet.readString();
            String text = packet.readString();
            int bubble = packet.readInteger();
            
            writeToConsole("Chat from " + username + ": " + text);
            return false; // Allow to be displayed
        });
    }

    @Override
    public void onEndConnection() {
        // Called when disconnected
    }
}
```

### Message Listener Interface

```java
// MessageListener is a functional interface
@FunctionalInterface
public interface MessageListener {
    boolean onMessage(HMessage message);
    // Return true = BLOCK the message
    // Return false = ALLOW the message to pass through
}
```

### Modifying Messages

```java
intercept(HMessage.Direction.TOSERVER, 60, message -> {
    HPacket packet = message.getPacket();
    packet.resetReadIndex();
    
    int roomId = packet.readInteger();
    int x = packet.readInteger();
    int y = packet.readInteger();
    
    // Modify the movement coordinates
    HPacket modified = new HPacket(60);
    modified.appendInt(roomId);
    modified.appendInt(x + 5);      // Move 5 units right
    modified.appendInt(y + 5);      // Move 5 units up
    
    // Replace the original packet
    message.getPacket().setBytes(modified.toBytes());
    
    return false; // Allow modified message
});
```

---

## Extension Communication

### Basic Extension Structure

```java
@ExtensionInfo(
    Title = "My Habbo Extension",
    Description = "Does amazing things",
    Version = "1.0",
    Author = "Me"
)
public class MyExtension extends Extension {
    
    public MyExtension(String[] args) {
        super(args);
    }

    @Override
    public void initExtension() {
        // Called when extension connects to G-Earth
        writeToConsole("green", "Extension initialized!");
    }

    @Override
    public void onStartConnection() {
        // Called when Habbo connection is established
        setupInterceptors();
    }

    @Override
    public void onEndConnection() {
        // Called when disconnected from Habbo
    }

    // Send packet to server from extension
    public void sendChatMessage(String text) {
        HPacket packet = new HPacket(60); // Chat header
        packet.appendString(text);
        packet.appendInt(0); // Bubble type
        sendToServer(packet);
    }

    // Send packet to client from extension
    public void sendSystemMessage(String text) {
        HPacket packet = new HPacket(103); // System notification
        packet.appendString(text);
        sendToClient(packet);
    }
}
```

### Writing to Console

```java
// Simple message
writeToConsole("Extension started");

// With color class
writeToConsole("green", "Success!");
writeToConsole("red", "Error occurred");
writeToConsole("yellow", "Warning!");
writeToConsole("blue", "Information");
```

### Common Packet Header IDs (Examples)

```
// TOSERVER (Client → Server)
60   = Move
61   = Sit/Stand
62   = Chat
...

// TOCLIENT (Server → Client)
0    = Avatar data
1    = Room data  
2    = Users in room
3    = Chat message
...
```

These IDs vary by hotel version and client type!

---

## Examples

### Example 1: Echo Chat Messages

```java
@ExtensionInfo(Title = "Chat Echo", Description = "Echoes all chats", Version = "1.0", Author = "You")
public class ChatEcho extends Extension {
    public ChatEcho(String[] args) {
        super(args);
    }

    @Override
    public void onStartConnection() {
        // Listen for chat messages from server
        intercept(HMessage.Direction.TOCLIENT, "Chat", message -> {
            HPacket p = message.getPacket();
            p.resetReadIndex();
            
            String username = p.readString();
            String text = p.readString();
            
            // Print to console
            writeToConsole("Chat: " + username + " -> " + text);
            
            return false; // Don't block the message
        });
    }
}
```

### Example 2: Auto-Respond to Messages

```java
@ExtensionInfo(Title = "Auto-Responder", Description = "Auto responds to messages", Version = "1.0", Author = "You")
public class AutoResponder extends Extension {
    public AutoResponder(String[] args) {
        super(args);
    }

    @Override
    public void onStartConnection() {
        intercept(HMessage.Direction.TOCLIENT, "Chat", message -> {
            HPacket p = message.getPacket();
            p.resetReadIndex();
            
            String username = p.readString();
            String text = p.readString();
            
            // Auto-respond to specific users
            if (username.equals("MyFriend")) {
                HPacket response = new HPacket(62); // Chat header
                response.appendString("Hi " + username + "!");
                response.appendInt(0);
                sendToServer(response);
            }
            
            return false;
        });
    }
}
```

### Example 3: Block Specific Messages

```java
@ExtensionInfo(Title = "Message Filter", Description = "Filters messages", Version = "1.0", Author = "You")
public class MessageFilter extends Extension {
    private Set<String> blockedWords = new HashSet<>(Arrays.asList("spam", "ads"));

    public MessageFilter(String[] args) {
        super(args);
    }

    @Override
    public void onStartConnection() {
        intercept(HMessage.Direction.TOCLIENT, "Chat", message -> {
            HPacket p = message.getPacket();
            p.resetReadIndex();
            
            String username = p.readString();
            String text = p.readString();
            
            // Block messages containing filtered words
            for (String word : blockedWords) {
                if (text.toLowerCase().contains(word.toLowerCase())) {
                    writeToConsole("red", "Blocked message from " + username);
                    return true; // Block this message!
                }
            }
            
            return false; // Allow message
        });
    }
}
```

### Example 4: Modify Outgoing Chat

```java
@ExtensionInfo(Title = "Chat Modifier", Description = "Modifies outgoing messages", Version = "1.0", Author = "You")
public class ChatModifier extends Extension {
    public ChatModifier(String[] args) {
        super(args);
    }

    @Override
    public void onStartConnection() {
        intercept(HMessage.Direction.TOSERVER, "Chat", message -> {
            HPacket p = message.getPacket();
            p.resetReadIndex();
            
            String text = p.readString();
            int bubble = p.readInteger();
            
            // Add prefix to all messages
            String modified = "[PREFIX] " + text;
            
            HPacket newPacket = new HPacket(62);
            newPacket.appendString(modified);
            newPacket.appendInt(bubble);
            
            message.getPacket().setBytes(newPacket.toBytes());
            
            return false;
        });
    }
}
```

---

## Packet Expression Format

Packets can be represented as strings for easier debugging and logging:

```
{h:123}[1][2][3]{s:"Hello"}{i:42}
```

Breaking it down:
- `{h:123}` = Header ID 123
- `[1]` = Byte 1
- `[2]` = Byte 2
- `[3]` = Byte 3
- `{s:"Hello"}` = String "Hello"
- `{i:42}` = Integer 42

With packet info:
```
{in:Chat}{s:"Hello"}[0]{i:1}
```
- `{in:Chat}` = Incoming packet named "Chat"
- `{out:Movement}` = Outgoing packet named "Movement"

---

## Best Practices

### ✅ DO
- Always reset read index before reading: `packet.resetReadIndex()`
- Check packet structure before parsing to avoid crashes
- Log important events for debugging
- Return `false` from listeners unless you specifically want to block
- Use packet names/hashes when available for compatibility

### ❌ DON'T
- Assume packet structure without verification
- Forget to handle corrupted packets
- Create packets with hard-coded header IDs without verification
- Block critical packets that may break the connection
- Read data in wrong order compared to the structure

---

## Packet Safety

G-Earth validates packets to prevent crashes:

```java
// Some packets are marked as "unsafe" and cannot be sent
// G-Earth will prevent sending them

public boolean isPacketSendingAllowed(HMessage.Direction direction, HPacket packet) {
    // Returns false for unsafe packets
}

public boolean canSendPacket(HMessage.Direction direction, HPacket packet) {
    // Checks against safety rules
}
```

Always listen to these validation warnings!

---

## Debugging Packets

### Enable Developer Mode
```java
extension.setDeveloperMode(true);
```

### Console Output
```java
writeToConsole("Packet: " + packet.toString());
writeToConsole("Expression: " + packet.toExpression());
writeToConsole("Header: " + packet.headerId());
```

### Get Packet Info
```java
PacketInfoManager manager = extension.getPacketInfoManager();
PacketInfo info = manager.getPacketInfo(headerID, direction);
System.out.println("Name: " + info.getName());
System.out.println("Structure: " + info.getStructure());
System.out.println("Hash: " + info.getHash());
```

---

## Connection Flow

```
Client              G-Earth              Server
  |                  |                    |
  |----HELLO-------->|                    |
  |                  |-----HELLO-------->|
  |                  |<----RESPONSE------|
  |<---RESPONSE------|                    |
  |                  |                    |
  | Extension Interception Starts         |
  |                  |                    |
  |=== DATA EXCHANGE ===                  |
  |   (all packets can be modified)       |
  |                  |                    |
  |----DISCONNECT--->|                    |
  |                  |-----DISCONNECT--->|
```

---

## Additional Resources

- [HPacket.java](../../../G-Earth-Api/src/main/java/gearth/protocol/HPacket.java) - Packet implementation
- [Extension.java](../../../G-Earth-Api/src/main/java/gearth/extensions/Extension.java) - Base extension class
- [PacketInfo.java](../../../G-Earth-Api/src/main/java/gearth/services/packet_info/PacketInfo.java) - Packet metadata

---

**Last Updated:** January 15, 2026
