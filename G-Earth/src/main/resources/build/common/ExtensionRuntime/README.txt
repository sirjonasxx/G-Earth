G-Earth uses this directory for the optional Java 8 extension runtime.

The main application does not run on Java 8. It continues to use Java 17-21
(Java 21 is bundled on macOS). Only legacy Java extension processes use the
runtime placed here.

Accepted layouts include:

  ExtensionRuntime/bin/java
  ExtensionRuntime/Contents/Home/bin/java
  ExtensionRuntime/<distribution>/Contents/Home/bin/java

On macOS, run Install-Extension-Java.command without sudo to download and
verify an ARM64/x64 Azul Zulu Java 8u442 JRE with JavaFX 8.

For a separately installed runtime, set GEARTH_EXTENSION_JAVA to either its
java executable or its JRE/JDK home directory. Graphical legacy extensions
need a Java 8 distribution that includes JavaFX 8.
