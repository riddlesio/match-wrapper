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

package io.riddles.gamewrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import io.riddles.gamewrapper.io.IOEngine;
import io.riddles.gamewrapper.io.IOPlayer;

import java.util.regex.Matcher;

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
     * @param message Input from the engine
     * @return Next engine message
     * @throws IOException
     */
    public String handle(String message) throws IOException {
        Matcher m;
        if ((m = BOTNR_ASK.matcher(message)).find()) {
            return this.engine.ask(botAsk(Integer.parseInt(m.group(1)), m.group(2)));
        } else if ((m = BOTNR_SEND.matcher(message)).find()) {
        	botSend(Integer.parseInt(m.group(1)), m.group(2));
        	return this.engine.getResponse();
        } else if ((m = BOTALL_SEND.matcher(message)).find()) {
            botBroadcast(m.group(1));
            return this.engine.getResponse();
        } else if (message.equals("end")) {
            return stop();
        }

        System.err.println("No match");
        return stop();
    }

    /**
     * Run a whole game
     * @return The details of the game
     */
    public String run() throws IOException {
        String response;

        // Have engine set up game settings
        if (!askAndExpect("initialize", "ok")) {
            return "initialize failed";
        }
        System.out.println("Engine initialized. Sending settings..");
        this.engine.sendPlayers(bots);
        
        System.out.println("Settings sent to engine. Starting engine...");
        response = this.engine.ask("start");

        while (!this.ended) {
            response = handle(response);
        }

        return response;
    }

    /**
     * Stops running of API and asks engine for
     * game details
     * @return The final engine message
     * @throws IOException
     */
    public String stop() throws IOException {
        this.ended = true;

        // ask for the game details
        return this.engine.ask("details");
    }

    /**
     * Blocking method
     * Asks something from given bot and waits for reponse.
     * @param botIndex Bot to ask
     * @param message Message to send
     * @return The response to send to the engine
     * @throws IOException
     */
    private String botAsk(int botIndex, String message) throws IOException {
        IOPlayer bot = bots.get(botIndex);
        return String.format("bot %d %s", botIndex, bot.ask(message));
    }
    
    /**
     * Sends message to a given bot
     * @param botIndex Bot to send to
     * @param message Message to send
     * @return False if message send failed, true otherwise
     * @throws IOException
     */
    private void botSend(int botIndex, String message) throws IOException {
    	IOPlayer bot = bots.get(botIndex);
    	bot.send(message);
    }

    /**
     * Sends a message to all bots
     * @param message Message to send
     * @return False if message send failed, true otherwise
     */
    private void botBroadcast(String message) {
        for(IOPlayer bot : bots)
        	bot.send(message);
    }

    /**
     * Asks something from the engine and compares its answers
     * to expected answer
     * @param message Message to send
     * @param expected Expected return from engine
     * @param timeout Time the engine has to respond
     * @return True if expected answer was returned, false otherwise
     * @throws IOException
     */
    private boolean askAndExpect(String message, String expected) throws IOException {
        String response = this.engine.ask(message);
        if (!response.equals(expected)) {
            System.err.println(String.format("Unexpected response: %s\n to message: %s", response, message));
            return false;
        }
        return true;
    }
}
