# Common Habbo Packets Reference

> A quick reference guide for commonly used Habbo packets and their structures.

## Chat & Communication

### Outgoing: Send Chat Message
```
Header ID: 60
Direction: TOSERVER
Structure: s (String)

packet.appendString("Hello world!")

// Expression: {out:Chat}{s:"Hello world!"}
```

### Incoming: Receive Chat Message
```
Header ID: 3 (varies by hotel)
Direction: TOCLIENT
Structure: isis

packet.readInteger()  // User ID
packet.readString()   // Username
packet.readInteger()  // Message count or type
packet.readString()   // Message text
```

### Incoming: Whisper Message
```
Direction: TOCLIENT
Structure: ssi

packet.readString()   // Sender username
packet.readString()   // Message text
packet.readInteger()  // Bubble type
```

---

## Movement & Navigation

### Outgoing: Move Avatar
```
Header ID: 60
Direction: TOSERVER
Structure: ii

packet.appendInt(x)   // Target X coordinate
packet.appendInt(y)   // Target Y coordinate
```

### Outgoing: Sit/Stand
```
Header ID: 61
Direction: TOSERVER
Structure: (none)

// Just send with no data
```

### Incoming: User Movement
```
Direction: TOCLIENT
Structure: iiiii

packet.readInt()      // User ID
packet.readInt()      // X position
packet.readInt()      // Y position
packet.readInt()      // Direction
packet.readInt()      // State (walking, standing, etc)
```

---

## Room & Environment

### Incoming: Room Data
```
Direction: TOCLIENT

// Contains room info, items, users
// Highly variable by hotel version
```

### Incoming: Avatar Data
```
Direction: TOCLIENT
Structure: Complex

// User appearance, clothing, accessories
// Requires packet info to parse correctly
```

### Incoming: User List (Room)
```
Direction: TOCLIENT

packet.readInt()      // Number of users
// Then for each user:
packet.readInt()      // User ID
packet.readString()   // Username
packet.readString()   // Motto/Status
// ... more fields
```

---

## Item Interaction

### Outgoing: Interact with Item
```
Header ID: 79
Direction: TOSERVER
Structure: i

packet.appendInt(itemId)
```

### Outgoing: Place Furniture
```
Direction: TOSERVER
Structure: isi

packet.appendInt(itemId)
packet.appendString(coords)  // e.g., "5,3,0"
packet.appendInt(rotation)
```

### Incoming: Item Add/Update
```
Direction: TOCLIENT

// Very complex structure with item data
// Usually includes: item ID, position, item type, state
```

---

## Friend & User Management

### Outgoing: Send Friend Request
```
Header ID: 84
Direction: TOSERVER
Structure: s

packet.appendString(username)
```

### Outgoing: Accept Friend Request
```
Header ID: 85
Direction: TOSERVER
Structure: i

packet.appendInt(userId)
```

### Outgoing: Remove Friend
```
Header ID: 86
Direction: TOSERVER
Structure: i

packet.appendInt(userId)
```

### Incoming: Friend Online/Offline
```
Direction: TOCLIENT
Structure: is

packet.readInt()      // User ID
packet.readString()   // Username
```

---

## Avatar Customization

### Outgoing: Update Avatar
```
Header ID: 250
Direction: TOSERVER
Structure: sssssss

packet.appendString(gender)      // "M" or "F"
packet.appendString(head)        // Head ID
packet.appendString(body)        // Body ID
packet.appendString(feet)        // Feet ID
packet.appendString(colorHead)   // Color hex
packet.appendString(colorBody)   // Color hex
packet.appendString(colorFeet)   // Color hex
```

### Outgoing: Update Motto
```
Header ID: 220
Direction: TOSERVER
Structure: s

packet.appendString("My new motto!")
```

---

## Game & Activity

### Outgoing: Join Game/Activity
```
Direction: TOSERVER
Structure: i

packet.appendInt(activityId)
```

### Incoming: Game Start
```
Direction: TOCLIENT

// Game-specific data
```

### Incoming: Game Update
```
Direction: TOCLIENT

// Real-time game state updates
```

---

## Trade & Transactions

### Outgoing: Open Trade
```
Direction: TOSERVER
Structure: i

packet.appendInt(userId)  // User to trade with
```

### Outgoing: Add Item to Trade
```
Direction: TOSERVER
Structure: i

packet.appendInt(itemId)
```

### Outgoing: Accept Trade
```
Direction: TOSERVER
Structure: (none)

// No data needed
```

---

## Moderation & Admin

### Outgoing: Report User
```
Direction: TOSERVER
Structure: sis

packet.appendString(username)
packet.appendInt(reportType)
packet.appendString(comment)
```

### Outgoing: Mute User
```
Direction: TOSERVER
Structure: is

packet.appendInt(userId)
packet.appendString(duration)  // e.g., "5m", "1h"
```

---

## Messenger/Email

### Outgoing: Send Message
```
Direction: TOSERVER
Structure: ss

packet.appendString(recipient)
packet.appendString(message)
```

### Incoming: New Message Notification
```
Direction: TOCLIENT
Structure: is

packet.readInt()      // Sender user ID
packet.readString()   // Message
```

---

## Achievement & Reward System

### Incoming: Achievement Earned
```
Direction: TOCLIENT
Structure: iss

packet.readInt()      // Achievement ID
packet.readString()   // Achievement name
packet.readString()   // Description
```

### Incoming: Reward Given
```
Direction: TOCLIENT
Structure: is

packet.readInt()      // Reward amount
packet.readString()   // Reward type (coins, pixels, etc)
```

---

## Tips for Using This Reference

1. **Header IDs Vary by Hotel**: Different Habbo versions may use different header IDs
2. **Use Packet Info**: Always look up actual packet info from your hotel version
3. **Structure Codes**:
   - `i` = Integer (4 bytes)
   - `s` = String (variable)
   - `b` = Byte (1 byte)
   - `u` = Unsigned Short (2 bytes)
   - `B` = Boolean (1 byte)

4. **Testing**: Always test in a controlled environment first
5. **Safety**: Some packets are flagged as unsafe - G-Earth will warn you

---

## Debugging Packets

To find exact header IDs for your hotel:

```java
intercept(HMessage.Direction.TOSERVER, message -> {
    HPacket p = message.getPacket();
    writeToConsole("Incoming from server - Header: " + p.headerId() + " Data: " + p.toString());
    return false;
});

intercept(HMessage.Direction.TOCLIENT, message -> {
    HPacket p = message.getPacket();
    writeToConsole("Outgoing to client - Header: " + p.headerId() + " Data: " + p.toString());
    return false;
});
```

---

**Note**: These are approximate structures. Exact layouts vary by:
- Habbo version (US, EU, etc)
- Client type (Flash, Nitro, Mobile)
- Hotel age and customizations

Always verify with packet capture tools and packet info from your specific hotel!
