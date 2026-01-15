# Habbo-CLI: Instagram-Style Habbo Client

> A terminal-based Habbo client inspired by Instagram-CLI, allowing users to chat, manage friends, and explore rooms from the command line.

## Project Overview

**Habbo-CLI** is a lightweight, terminal-based Habbo client built on top of G-Earth's API. It provides an Instagram-like CLI experience while maintaining access to core Habbo features.

### Key Features

1. **Chat System**
   - Real-time room chat
   - Whisper/Direct messages
   - Message history
   - Message notifications

2. **Social Management**
   - Friend list with online status
   - Friend requests
   - View friend profiles
   - Add/remove friends

3. **Room Navigation**
   - Browse available rooms
   - Enter/exit rooms
   - See who's in room
   - Room information

4. **Avatar Management**
   - View avatar details
   - Update appearance (clothing, colors)
   - Update motto/status
   - View other users' profiles

5. **Notifications**
   - New messages alert
   - Friend online/offline
   - Friend requests
   - Achievement notifications

## Project Structure

```
habbo-cli/
├── pom.xml
├── README.md
├── src/
│   ├── main/java/com/habbo_cli/
│   │   ├── HabboCLI.java                 # Main entry point
│   │   ├── config/
│   │   │   ├── ConfigManager.java        # Load/save settings
│   │   │   └── Config.java               # Config model
│   │   ├── network/
│   │   │   ├── HabboExtension.java       # G-Earth extension
│   │   │   ├── ConnectionManager.java    # Manage connection
│   │   │   └── PacketHandler.java        # Parse packets
│   │   ├── ui/
│   │   │   ├── CLI.java                  # CLI main loop
│   │   │   ├── Menu.java                 # Menu system
│   │   │   ├── OutputFormatter.java      # Pretty printing
│   │   │   └── commands/
│   │   │       ├── ChatCommand.java
│   │   │       ├── FriendsCommand.java
│   │   │       ├── RoomCommand.java
│   │   │       ├── ProfileCommand.java
│   │   │       └── SettingsCommand.java
│   │   ├── models/
│   │   │   ├── User.java
│   │   │   ├── Room.java
│   │   │   ├── Message.java
│   │   │   ├── Friend.java
│   │   │   └── Avatar.java
│   │   └── util/
│   │       ├── Logger.java
│   │       ├── StringUtils.java
│   │       └── DateUtils.java
│   └── test/java/...
└── docs/
    ├── SETUP.md
    ├── USAGE.md
    └── ARCHITECTURE.md
```

## Technology Stack

- **Java 11+**: Core language
- **G-Earth API**: Protocol handling
- **Picocli**: CLI framework (optional, for better command parsing)
- **Maven**: Build system
- **SQLite**: Local message storage (optional)

## Core Components

### 1. HabboExtension
```java
/**
 * G-Earth extension that handles protocol communication.
 * Runs in the background communicating with Habbo.
 */
@ExtensionInfo(
    Title = "Habbo-CLI",
    Description = "Terminal-based Habbo client",
    Version = "1.0",
    Author = "Your Name"
)
public class HabboExtension extends Extension {
    // Handles all packet interception
    // Bridges between CLI and Habbo protocol
}
```

### 2. ConnectionManager
```java
/**
 * Manages the connection state and coordinated communication
 * with both G-Earth and the Habbo server.
 */
public class ConnectionManager {
    - isConnected(): boolean
    - connect(): void
    - disconnect(): void
    - sendChatMessage(text): void
    - sendFriendRequest(username): void
    - getOnlineFriends(): List<User>
}
```

### 3. CLI (Main UI Loop)
```java
/**
 * Main command-line interface loop.
 * Handles user input and displays output.
 */
public class CLI {
    - showMainMenu(): void
    - showChatScreen(): void
    - showFriendsScreen(): void
    - processCommand(input): void
    - displayNotification(message): void
}
```

### 4. PacketHandler
```java
/**
 * Parses and handles Habbo protocol packets.
 * Converts binary packets to model objects.
 */
public class PacketHandler {
    - handleChatMessage(packet): Message
    - handleUserList(packet): List<User>
    - handleFriendRequest(packet): Friend
    - handleAvatarUpdate(packet): Avatar
}
```

## User Interface Design

### Main Menu
```
╔════════════════════════════════════════════╗
║        Welcome to Habbo-CLI v1.0           ║
║     Instagram-Style Habbo Client          ║
╚════════════════════════════════════════════╝

[1] Chat              💬 Room chat & messages
[2] Friends           👥 Friend list & requests
[3] Rooms             🏠 Browse & navigate rooms
[4] Profile           👤 Avatar & profile settings
[5] Settings          ⚙️  Configuration
[6] Quit              ❌ Exit application

Select option (1-6): _
```

### Chat Screen
```
╔═══════════════════════════════════════════════════╗
║ Current Room: "Lobby"  [25 users online]         ║
╚═══════════════════════════════════════════════════╝

[14:32] User123: Hey everyone!
[14:33] User456: Hi User123!
[14:34] You: Hello room!

--- New Message from Friend123 ---
[14:35] Friend123: Are you free?

Input message (type /help for commands): _
```

### Friends Screen
```
╔═══════════════════════════════════════════════════╗
║ Friends List (15 online / 42 total)              ║
╚═══════════════════════════════════════════════════╝

🟢 User123           → Online in "Lobby"
🟢 Friend456         → Online in "Garden"
🔴 Friend789         → Offline (last seen 2h ago)
🟢 NewUser           → Online in "Lounge"
...

[1] View friend profile
[2] Send whisper
[3] Add friend
[4] Remove friend
[5] Back to menu

Select option (1-5): _
```

## Command Examples

### Chat Commands
```bash
# Send regular message
> Hello everyone!

# Send whisper
> /whisper User123 Private message

# Search message history
> /search gaming

# View recent messages
> /history 10

# Clear screen
> /clear

# Change room
> /room "Lobby"

# Room info
> /roominfo
```

### Friend Commands
```bash
# Add friend
> /addfriend Username

# Send friend request
> /friendrequest Username

# Accept friend request
> /acceptfriend Username

# Remove friend
> /removefriend Username

# View friend profile
> /profile Username

# Check online friends
> /friends online
```

### Profile Commands
```bash
# View your profile
> /me

# Update motto
> /motto "My new motto!"

# Change appearance
> /appearance

# View achievements
> /achievements

# Check inventory
> /inventory
```

## Data Models

### User
```java
public class User {
    int id;
    String username;
    String motto;
    boolean online;
    String currentRoom;
    Avatar avatar;
    
    // Methods
    sendWhisper(text): void
    addAsFriend(): void
    viewProfile(): void
}
```

### Message
```java
public class Message {
    long timestamp;
    String sender;
    String text;
    MessageType type;  // CHAT, WHISPER, SYSTEM, NOTIFICATION
    int bubbleType;
    
    // Methods
    getFormattedTime(): String
    toString(): String
}
```

### Room
```java
public class Room {
    int id;
    String name;
    String description;
    int capacity;
    int currentUsers;
    List<User> users;
    List<Furniture> items;
    
    // Methods
    enter(): void
    exit(): void
    getUsers(): List<User>
}
```

## Development Phases

### Phase 1: Foundation (Week 1)
- [ ] Project setup with Maven
- [ ] Create G-Earth extension wrapper
- [ ] Establish basic CLI framework
- [ ] Connection to G-Earth

### Phase 2: Chat System (Week 2)
- [ ] Intercept chat packets
- [ ] Parse chat messages
- [ ] Real-time message display
- [ ] Send chat messages
- [ ] Message history storage

### Phase 3: Social Features (Week 2-3)
- [ ] Friend list integration
- [ ] Online status tracking
- [ ] Friend requests handling
- [ ] Whisper/DM system
- [ ] Friend profiles

### Phase 4: UI Polish (Week 3)
- [ ] Better terminal formatting
- [ ] Color support
- [ ] Notification system
- [ ] Command auto-complete

### Phase 5: Advanced Features (Week 4)
- [ ] Avatar customization
- [ ] Room browsing
- [ ] Game integration
- [ ] Trading system

## Build & Run Instructions

### Prerequisites
- Java 11+
- Maven 3.6+
- G-Earth running and configured

### Build
```bash
cd habbo-cli
mvn clean package
```

### Run
```bash
java -jar target/habbo-cli-1.0.jar \
  --gearth-port 9339 \
  --username "YourUsername"
```

### Development (with hot reload)
```bash
mvn spring-boot:run
# or
mvn exec:java
```

## Configuration File (~/.habbo-cli/config.json)

```json
{
  "gearth": {
    "host": "localhost",
    "port": 9339,
    "timeout": 5000
  },
  "user": {
    "lastUsername": "MyUsername",
    "rememberMe": false
  },
  "ui": {
    "theme": "dark",
    "language": "en",
    "showNotifications": true,
    "messageHistory": 100
  },
  "storage": {
    "database": "~/.habbo-cli/messages.db",
    "cacheSize": 10000
  }
}
```

## Future Enhancements

1. **Multi-account support** - Switch between accounts
2. **Message encryption** - Secure DMs
3. **Trading system** - CLI-based trading interface
4. **Pet management** - View and interact with pets
5. **Catalog access** - Browse and buy items
6. **Club/Guild system** - Manage groups
7. **Web dashboard** - Web interface for viewing chats
8. **Mobile app** - iOS/Android companion app
9. **Plugin system** - Allow community extensions
10. **Game integration** - Play mini-games from CLI

## Contributing

Guidelines for contributors:
1. Follow Java style guide
2. Write unit tests for new features
3. Update documentation
4. Submit pull requests with clear descriptions

## License

MIT License - See LICENSE file

---

## Getting Started

See [SETUP.md](SETUP.md) for installation instructions.
See [USAGE.md](USAGE.md) for usage guide.
See [ARCHITECTURE.md](ARCHITECTURE.md) for technical details.

---

**Project Status**: Planning Phase
**Last Updated**: January 15, 2026
