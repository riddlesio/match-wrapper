// Copyright 2016 riddles.io (developers@riddles.io)

//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at

//        http://www.apache.org/licenses/LICENSE-2.0

//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//  
//    For the full copyright and license information, please view the LICENSE
//    file that was distributed with this source code.

package io.riddles.matchwrapper;

import io.riddles.matchwrapper.io.IOEngine;
import io.riddles.matchwrapper.io.IOPlayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * EngineAPI class
 *
 * Handles all communication between the game wrapper and
 * the engine and bot processes
 *
 * @author Sid Mijnders <sid@riddles.io>, Jim van Eeden <jim@riddles.io>
 */
public class EngineAPI {

    private static final Pattern BOTNR_ASK = Pattern.compile("^bot (\\d+) ask (.*)");
    private static final Pattern BOTNR_SEND = Pattern.compile("^bot (\\d+) send (.*)");
    private static final Pattern BOTNR_WARNING = Pattern.compile("^bot (\\d+) warning (.*)");
    private static final Pattern BOTALL_SEND = Pattern.compile("^bot all send (.*)");

    private IOEngine engine;
    private ArrayList<IOPlayer> bots;
    private boolean ended;

    public EngineAPI(IOEngine engine, ArrayList<IOPlayer> bots) {
        this.engine = engine;
        this.bots = bots;
        this.ended = false;
    }

    /**
     * Handles the engine's input. Always returns the next
     * message the engine sends.
     *
     * @param message Input from the engine
     * @throws IOException exception
     */
    public void handle(String message) throws IOException {

        Matcher m;

        if (message == null || message.length() <= 0 || message.equals("end")) {
            this.ended = true;
            return;
        }

        // TODO: Make it possible to toggle verbose mode
        // System.out.println(String.format("Received message: '%s'", message));

        if ((m = BOTNR_ASK.matcher(message)).find()) {
            this.engine.send(botAsk(Integer.parseInt(m.group(1)), m.group(2)));
        } else if ((m = BOTNR_SEND.matcher(message)).find()) {
            botSend(Integer.parseInt(m.group(1)), m.group(2));
        } else if ((m = BOTNR_WARNING.matcher(message)).find()) {
            botWarning(Integer.parseInt(m.group(1)), m.group(2));
        } else if ((m = BOTALL_SEND.matcher(message)).find()) {
            botBroadcast(m.group(1));
        } else if (message.equals("ok")) {
            // do nothing, continue
        } else {
            System.err.println(String.format("'%s' did not match any action", message));
            this.ended = true;
        }
    }

    /**
     * Run a whole game
     */
    public void run() throws IOException {

        // Have engine set up game settings
        if (!askAndExpect("initialize", "ok")) {
            return;
        }

        System.out.println("Engine initialized. Sending settings to engine..");
        this.engine.sendPlayers(bots);
        this.engine.sendConfiguration();

        System.out.println("Settings sent to engine. Sending settings to bots...");
        this.sendBotSettings();

        System.out.println("Settings sent to bots. Starting engine...");
        this.engine.send("start");

        System.out.println("Engine Started. Playing game...");

        while (!this.ended) {
            handle(this.engine.getMessage());
        }
    }

    /**
     * Sends settings to all bots that are required for
     * every game
     */
    private void sendBotSettings() {

        // create player names string
        String playerNames = "";
        String connector = "";
        for (IOPlayer bot : this.bots) {
            playerNames += String.format("%splayer%d", connector, bot.getId());
            connector = ",";
        }

        // send settings
        botBroadcast(String.format("settings player_names %s", playerNames));
        for (IOPlayer bot : this.bots) {
            bot.send(String.format("settings your_bot player%d", bot.getId()));
            bot.send(String.format("settings timebank %d", MatchWrapper.MAX_TIME_BANK));
            bot.send(String.format("settings time_per_move %d", MatchWrapper.TIME_PER_MOVE));
        }
    }

    /**
     * Asks the engine for the details of the game
     * i.e. winner, etc.
     *
     * @return Detail string
     */
    public String askGameDetails() {
        try {
            return this.engine.ask("details");
        } catch (IOException ex) {
            System.err.println(ex);
        }

        return "";
    }

    /**
     * Asks the engine for the game file for the
     * visualizer
     *
     * @return The played game in string representation
     */
    public String askPlayedGame() {
        try {
            return this.engine.ask("game");
        } catch (IOException ex) {
            System.err.println(ex);
        }

        return "";
    }

    /**
     * Blocking method
     * Asks something from given bot and waits for response.
     *
     * @param botIndex Bot to ask
     * @param message  Message to send
     * @return The response to send to the engine
     * @throws IOException
     */
    private String botAsk(int botIndex, String message) throws IOException {
        IOPlayer bot = bots.get(botIndex);
        return String.format("bot %d %s", botIndex, bot.ask(message));
    }

    /**
     * Sends message to a given bot
     *
     * @param botIndex Bot to send to
     * @param message  Message to send
     * @throws IOException
     */
    private void botSend(int botIndex, String message) throws IOException {
        IOPlayer bot = bots.get(botIndex);
        bot.send(message);
    }

    /**
     * Adds a warning from the engine to the bot's dump
     *
     * @param botIndex Bot for which the warning is meant
     * @param warning  Warning message
     */
    private void botWarning(int botIndex, String warning) {
        IOPlayer bot = bots.get(botIndex);
        bot.addToDump(warning);
    }

    /**
     * Sends a message to all bots
     *
     * @param message Message to send
     * @return False if message send failed, true otherwise
     */
    private void botBroadcast(String message) {
        for (IOPlayer bot : bots)
            bot.send(message);
    }

    /**
     * Asks something from the engine and compares its answers
     * to expected answer
     *
     * @param message  Message to send
     * @param expected Expected return from engine
     * @return True if expected answer was returned, false otherwise
     * @throws IOException
     */
    private boolean askAndExpect(String message, String expected) throws IOException {
        String response = this.engine.ask(message);
        if (!response.equals(expected)) {
            System.err.println(String.format(
                    "Unexpected response: %s\n to message: %s", response, message));
            return false;
        }
        return true;
    }
}
