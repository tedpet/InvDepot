# Prerequisites

- Java 25
- Maven

# WebObjects Development Tools

You need to install the WOLips plugin for Eclipse found at https://wocommunity.github.io/wolips/repository/

When you first import this project, you may need to right click the project folder and choose Maven -> Update Project in order for it to build correctly.

# Build instructions

1. Install WebObjects from Apple into your ~/.m2 repository.
    1. mvn io.github.wocommunity:woinstall-maven-plugin:woinstall
2. Build & install the womodular project.
    1. git clone https://github.com/wocommunity/womodular.git
    2. mvn clean install -f womodular/pom.xml
    3. rm -rf womodular
3. Check out and build this project in eclipse or on the command line.
    1. In eclipse, you should be able to right click on InvDepot.launch and Run as/Debug as the application.


There is a separate framework InvDepotFW needed for this app
