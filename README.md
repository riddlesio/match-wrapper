# Riddles.io Match Wrapper

The match wrapper is used to run matches on Riddles.io. It takes care of starting
up the processes, handles communication between the processes, handles broken/non-responding
processes and stores the results of the match.

## Build

Building the project is done using [Gradle](http://gradle.org/), a
generic build and dependency management tool for JVM languages.

Creating the jar file is done by running the following command:

```
gradle build jar
```

The match wrapper .jar file will be located in `build/libs/`. Use this .jar file to run matches.

## Run

You can use the `run_wrapper.sh` script to easily run a match on your local computer.
This takes the JSON from `test/wrapper-command.json` and gives it as an argument to
the match wrapper. The contents of `wrapper-commands.json` determine how a match
runs (see below).

## Wrapper Commands

Example wrapper commands:

````
{
  "wrapper": {
    "timebankMax": 2000,
    "timePerMove": 100,
    "maxTimeouts": 2,
    "resultFile": "./resultfile.json",
    "propagateBotExitCode": false,
    "debug": true
  },
  "match": {
    "bots": [{
      "command": "python /home/jim/workspace/match-wrapper/test/hello_bot.py"
    },{
      "command": "python /home/jim/workspace/match-wrapper/test/hello_bot.py"
    }],
    "engine": {
      "command": "python /home/jim/workspace/match-wrapper/test/hello_engine.py",
      "configuration": {}
    }
  }
}
````

- **wrapper** Configuration for the wrapper
  - **timebankMax** Maximum amount of time bots get in their timebank (in milliseconds).
  - **timePerMove** Time the bots get extra in their timebank each action request (in milliseconds).
  - **maxTimeouts** Maximum amount of timeouts a bot can have before it's shut down. *Note: On Riddles.io, this value will always be 0.*
  - **resultFile** Name and location of the file with the match results.
  - **propagateBotExitCode** Propagates the exit code of the bot to the exit value of the match wrapper. This is only used for the test match after the input test on Riddles.io.
  - **debug** If true, will print the engine error streams.
- **match** Configuration for the match processes
  - **bots** An array with all bot configurations
    - **command** The command to start the bot process. Can be any command; if it works in your console, it should work here. If your command contains a path with spaces, enter it as an array as follows:  
    `"command": "java -jar \"/home/me/My Projects/bot.jar\""` => `"command": ["java", "-jar", "/home/me/My Projects/bot.jar"]`
  - **engine** Configuration for the engine process
    - **command** The command to start the engine process. Paths with spaces need to be entered as above.
    - **configuration** Any configuration you might want to send to the game engine.
    
The configuration for most current Riddles.io (java) game engines looks like this (example):

````
"configuration": {
    "fieldWidth": {
        "type": "integer",
        "value": 15
    },
    "randomSeed": {
        "type": "string",
        "value": "abcdefg"
    }
}
````
In this case, `{"fieldWidth":{"type":"integer","value": 15},"randomSeed":{"type":"string","value":"abcdefg"}}`
will be sent directly to the game engine.
