# Contributing

## Before you start
- First of all, thank you for contributing your time in the development of SystemTrayFX :D
- You must fork the repository to your own GitHub account and clone it locally using: `https://github.com/SundenJaeger/SystemTrayFX.git`

## Tools Needed
1. **IntelliJ IDEA (Latest Version)** is highly recommended. While you may use any IDE of your choice, the project structure and configurations are optimized for IntelliJ.
2. JDK 25

## Running the Build
You must do `mvn clean install` first to install the modular version of SWT into your local repository. This is required because the project includes a sampler that uses `jlink`, which does not support the automatic modules found in standard SWT distributions.


## Pull Request Process
- You must create a new branch for every feature or bug fix.
- The PR doesn’t need to be fully descriptive, just be clear about what you added or removed and what the code does.