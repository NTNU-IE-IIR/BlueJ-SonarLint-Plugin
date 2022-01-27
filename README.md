# BlueJ-SonarLint-Plugin
![release](https://img.shields.io/github/v/release/NTNU-IE-IIR/BlueJ-SonarLint-Plugin)
![license](https://img.shields.io/github/license/NTNU-IE-IIR/BlueJ-SonarLint-Plugin)

sonarlint4bluej is a BlueJ plugin that introduces the SonarLint code analysis tool to BlueJ. (Java only)
The plugin provides clear feedback with remediation guidance on flaws such as code smells, bugs and security vulnerabilities. 

## Installing the extension

1. Download the latest version of the extension found [here][1]
2. Move the downloaded JAR to a BlueJ extensions2 directory
3. Start BlueJ

  **BlueJ Extensions can be installed in three different directories:**
  - `User directory` installs for this user
  - `System directory` installs for all users of this system
  - `Project directory` installs for this project only
  
To install for a project, make a directory called `extensions2` in the projects root directory and move the JAR to that directory.


**In order to install for a user/system place the JAR in one of these directories:**

| Operating System | Install-type | Directories                                                  |
|------------------|--------------|--------------------------------------------------------------|
| **Mac**          | *User*       | `$HOME/Library/Preferences/org.bluej/extensions2`            |
|                  | *System*     | `<BLUEJ_HOME>/BlueJ.app/Contents/Resources/Java/extensions2` |
| **Unix**         | *User*       | `$HOME/.bluej/extensions2`                                   | 
|                  | *System*     | `<BLUEJ_HOME>/lib/extensions2`                               |
| **Windows**      | *User*       | `%USERNAME%\bluej\extensions2`                               | 
|                  | *System*     | `%PROGRAMFILES%\BlueJ\lib\extensions2`                       |

**Tip:** For Mac users, Control-click BlueJ.app and choose Show Package Contents to find the system directory.

For further information about Extensions in BlueJ see: [BlueJ Extensions][2]

## Usage 

The SonarLint Plugin runs checks in BlueJ when a Project/Package is opened and when a class file's state changes.

You can view the violations discovered by choosing `Show SonarLint overview` from the `Tools` menu.

Rules can be excluded from Code Analysis by disabling them through the BlueJ preferences.

You can find the preferences by choosing `Preferences...` from the `Tools` menu and navigating to the `Extensions` tab.

## Issues
Are you experiencing bugs/problems using this plugin? 

Submit a [bug report](https://github.com/NTNU-IE-IIR/BlueJ-SonarLint-Plugin/issues/new?assignees=&labels=&template=bug_report.md&title=) with detailed reproduction steps.


## Contributing
Contributions are welcome. Feel free to discuss the changes with us in a [feature request](https://github.com/NTNU-IE-IIR/BlueJ-SonarLint-Plugin/issues/new?assignees=&labels=&template=feature_request.md&title=) before submitting a Pull Request.

[1]: https://github.com/NTNU-IE-IIR/BlueJ-SonarLint-Plugin/releases/latest
[2]: https://www.bluej.org/extensions/extensions2.html