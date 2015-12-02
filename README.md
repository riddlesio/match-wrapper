Riddles.io Game Wrapper
============

Handles communication between a game engine and bots. To use the wrapper create gamewrapper.jar (see below) and provide at least 5 arguments when running the jar file. 

Arguments:

 1. Maximum size of bot's timebank (ms).
 2. Time added to bot's timebank per move (ms).
 3. Maximum amount of timeouts before the bot is shut down.
 4. Engine command.
 5. Bot command, additional arguments are commands for additional bots.

## Build
`make` in cmd to create .jar file of the Game Wrapper.

## Test
Run run_wrapper.sh to test Game Wrapper with provided test bot and test engine.