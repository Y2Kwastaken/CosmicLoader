# CosmicLoader

Allows the ability to mod with fabric mods on Cosmic Reach. Works flawlessly with Mixin-Extras.

Thanks to FowarD-NerN for getting this started would not have been possible without him. A link to his 
original repository [CosmicReach-Example-Mod](https://github.com/ForwarD-NerN/CosmicReach-Example-Mod).

# How to Use

## Building From Scratch

To Build CosmicLoader from scratch
- run `git clone https://github.com/Y2Kwastaken/CosmicLoader.git`
- cd into the cloned directory e.g. `cd CosmicLoader`
- run the gradlew bundle task `./gradlew bundle` on linux or `gradlew.bat bundle` on windows
- `cd build`
- unzip `CosmicLoader-[version]-SNAPSHOT.zip`

## Running from Release

Download the latest release
- unzip CosmicLoader-[version]-SNAPSHOT.zip
- obtain a copy of the desired version of CosmicReach, You can also use my tool [CosmicTools](https://github.com/Y2Kwastaken/CosmicTools)
- move Cosmic Reach into the CosmicLoader-[version]-SNAPSHOT directory and **rename the Cosmic Reach Jar `cosmic-reach.jar`**
- run the launcher for your platform linux (launcher.sh), windows (launcher.bat)
