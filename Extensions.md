G-Earth supports console & GUI extensions, this page is focused on extension structure and development.

## Features
* Packet hash/name support
* Interception of incoming/outgoing packets
* Full chat console support for I/O

## Console extensions

Console extensions are the most basic kind of extensions, a few lines of code can get us started.
A console extension is made of a Java class that extends _Extension_.

Let's start a sample extension creating a maven project:

Since G-Earth is built using maven, once we've compiled G-Earth, it's possible to add it as a project dependency:
```xml 
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>G-EarthGang</groupId>
    <artifactId>SampleExtension</artifactId>
    <version>1.0-SNAPSHOT</version>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>2.5</version>
                <configuration>
                    <outputDirectory>${project.build.directory}/bin</outputDirectory>
                    <archive>
                        <manifest>
                            <addDefaultImplementationEntries>true</addDefaultImplementationEntries>
                            <addClasspath>true</addClasspath>
                            <classpathPrefix>lib/</classpathPrefix>
                            <mainClass>SampleExtension</mainClass>
                            <useUniqueVersions>false</useUniqueVersions>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
            <plugin>
                <artifactId>maven-assembly-plugin</artifactId>
                <version>2.5</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <outputDirectory>${project.build.directory}/bin</outputDirectory>
                    <archive>
                        <manifest>
                            <mainClass>SampleExtension</mainClass>
                        </manifest>
                    </archive>
                    <descriptorRefs>
                        <descriptorRef>jar-with-dependencies</descriptorRef>
                    </descriptorRefs>
                    <finalName>SampleExtension</finalName>
                    <appendAssemblyId>false</appendAssemblyId>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>8</source>
                    <target>8</target>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <dependencies>
        <dependency>
            <groupId>G-Earth</groupId>
            <artifactId>G-Earth</artifactId>
            <version>0.2</version>
        </dependency>
    </dependencies>
</project>
```

That should be enough to configure maven. Now let's move on to the code.

Every extension needs to include an _ExtensionInfo_ annotation in order to identify the extension, our ExtensionInfo would look like this:
```java
@ExtensionInfo(
        Title = "Sample Extension",
        Description = "Used for the wiki",
        Version = "1.0",
        Author = "G-Earth"
)
```

The extension constitutes a standalone jar, so we'll need to include a main method to start it up like this:
```java
public class SampleExtension extends Extension {
    private SampleExtension(String[] args) {
        super(args);
    }

    public static void main(String[] args) {
        new SampleExtension(args).run();
    }
}
```

That would be a bare minimum to get it loaded into G-Earth, now let's have a look at the API to improve our little extension

## A quick look to the extensions API

In this section we'll build upon the previous example with the functionality provided by G-Earth's API

### Intercepting packets
The _intercept_ method lets us intercept a packet from its id and modify it if we want to.
```java
intercept(HMessage.Side.TOCLIENT, 4000, hMessage -> {
    // oops we got disconnected
});
```

### Sending packets
G-Earth includes the sendTo[Client/Server] method in order to send packets.

```java
sendToClient(new HPacket("{l}{u:1234}"));
// or
sendToClient(new HPacket(1234));
```
### Initializing our extension

_initExtension_  is called whenever G-Earth loads the extension, it's specially useful to init the _HashSupport_ and other features, we'll use it in the following examples

### Doing stuff upon client connection

```java
@Override
protected void onStartConnection() {
    // we're connected to habbo
}
```

### HashSupport

_HashSupport_ allows us to use hashes/names in order to intercept and send packets, using the aforementioned methods.

```java
private HashSupport mHashSupport;
@Override
protected void initExtension() {
    // This is called when G-Earth loads the extension
    mHashSupport = new HashSupport(this);

    mHashSupport.intercept(HMessage.Side.TOCLIENT, "RoomUserStartTyping", hMessage -> {
        mHashSupport.sendToServer("RoomUserStopTyping");
    });
}
```

### ChatConsole
_ChatConsole_ is a new G-Earth feature that allows us to use the in-game chat in order to communicate with the extension, it can read your input and write messages, you can also set a welcome message, we'll add it to our example.

```java
private HashSupport mHashSupport;
private ChatConsole mChatConsole;
@Override
protected void initExtension() {
    // This is called when G-Earth loads the extension
    mHashSupport = new HashSupport(this);
    mChatConsole = new ChatConsole(mHashSupport, this, "I'm a welcome message!");

    mChatConsole.onInput(input -> {
        if (input.equals("ping"))
            mChatConsole.writeOutput("pong", false);
    });
}
```

Once loaded it will appear to your friend's list<br/>
![](https://i.imgur.com/XqYFZmT.png)

It will show the welcome message after typing _:info_ <br/>
![](https://i.imgur.com/1mfBxYm.png)<br/>
![](https://i.imgur.com/294PNUE.png)

If used correctly it can be pretty powerful.





