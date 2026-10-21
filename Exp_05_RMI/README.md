# Java RMI Calculator

## Aim
To demonstrate communication between a Java client and remote server using Java Remote Method Invocation (RMI).

## Features
- Remote calculator methods for addition and subtraction
- Separate server and client programs

## Technologies
Java, Java RMI, `rmiregistry`.

## How to Run
Compile the source files:

```bash
javac *.java
```

Start the registry, server, and client in separate terminals:

```bash
rmiregistry
java Server
java Client
```

Run each command in its own terminal. If package declarations are present, adjust the commands to match the source structure.
