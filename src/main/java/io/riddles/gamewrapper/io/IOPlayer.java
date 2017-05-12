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

package io.riddles.gamewrapper.io;

import java.io.IOException;
import java.util.ArrayList;

/**
 * IOPlayer class
 * 
 * Extends IOWrapper class by adding stuff specifically
 * for bot processes
 * 
 * @author Sid Mijnders <sid@riddles.io>, Jim van Eeden <jim@starapple.nl>
 */
public class IOPlayer extends IOWrapper {

    private int id;
    private long timebank;
    private long timebankMax;
    private long timePerMove;
    private int maxTimeouts;
    private StringBuilder dump;
    private int errorCounter;
    private ArrayList<Long> responseTimes;

    private final String NULL_MOVE1 = "no_moves";
    private final String NULL_MOVE2 = "pass";

    public IOPlayer(Process process, int id, long timebankMax, long timePerMove, int maxTimeouts) {
        super(process);
        this.id = id;
        this.timebank = timebankMax;
        this.timebankMax = timebankMax;
        this.timePerMove = timePerMove;
        this.maxTimeouts = maxTimeouts;
        this.dump = new StringBuilder();
        this.errorCounter = 0;
        this.responseTimes = new ArrayList<>();
    }
 
    /**
     * Send line to bot
     * @param line Line to send
     */
    public void send(String line) {
        addToDump(line);
        if (!super.write(line) && !this.finished) {
            addToDump("Write to bot failed, shutting down...");
        }
    }
    
    /**
     * Send line to bot and waits for response taking
     * the bot's timebank into account
     * @param line Line to output
     * @return Bot's response
     * @throws IOException exception
     */
    public String ask(String line) throws IOException {
        this.response = null;
        send(String.format("%s %d", line, this.timebank));
        return getResponse();
    }

    /**
     * Waits until bot returns a response and returns it
     * @return Bot's response, returns and empty string when there is no response
     */
    public String getResponse() {
        
        if (this.errorCounter > this.maxTimeouts) {
            addToDump(String.format("Maximum number (%d) of time-outs reached: " +
                    "skipping all moves.", this.maxTimeouts));
            return "null";
        }

        long startTime = System.currentTimeMillis();
        
        String response = super.getResponse(this.timebank);
        
        long timeElapsed = System.currentTimeMillis() - startTime;

        this.responseTimes.add(timeElapsed);
        updateTimeBank(timeElapsed);

        if (response.equalsIgnoreCase(NULL_MOVE1)) {
            botDump(NULL_MOVE1);
            return "pass";
        }
        if (response.equalsIgnoreCase(NULL_MOVE2)) {
            botDump(NULL_MOVE2);
            return "pass";
        }
        if (response.isEmpty()) {
            botDump("");
            return "null";
        }

        botDump(response);
        return response;
    }

    /**
     * Handles everything when a bot response
     * times out
     * @param timeout Time before timeout
     * @return Empty string
     */
    protected String handleResponseTimeout(long timeout) {
        addToDump(String.format("Response timed out (%dms), let your bot return '%s'"
            + " instead of nothing or make it faster.", timeout, NULL_MOVE1));
        addError();
        return "";
    }
    
    /**
     * Increases error counter, call this method
     * when a write fails or when there is no
     * response
     */
    private void addError() {
        this.errored = true;
        this.errorCounter++;

        if (this.errorCounter > this.maxTimeouts) {
            finish();
        }
    }
    
    /**
     * Shuts down the bot
     */
    public int finish() {
        int exitStatus = super.finish();

        System.out.println("Bot shut down.");

        return exitStatus;
    }
    
    /**
     * Updates the time bank for this player, cannot get bigger 
     * than timebankMax or smaller than zero
     * @param timeElapsed Time consumed from the time bank
     */
    private void updateTimeBank(long timeElapsed) {
        this.timebank = Math.max(this.timebank - timeElapsed, 0);
        this.timebank = Math.min(this.timebank + this.timePerMove, this.timebankMax);
    }

    /**
     * Adds the bot's outputs to dump
     * @param dumpy Bot output
     */
    private void botDump(String dumpy) {
        String engineSays = "Output from your bot: \"%s\"";
        addToDump(String.format(engineSays, dumpy));
    }
    
    /**
     * Adds a string to the bot dump
     * @param dumpy String to add to the dump
     */
    public void addToDump(String dumpy) {
        this.dump.append(dumpy).append("\n");
    }
    
    /**
     * @return The dump of all the IO
     */
    public String getDump() {
        return dump.toString();
    }
    
    /**
     * @return This bot's ID
     */
    public int getId() {
        return this.id;
    }
    
    /**
     * @return This bot's max timebank setting
     */
    public long getTimebankMax() {
        return this.timebankMax;
    }
    
    /**
     * @return This bot's time per move setting
     */
    public long getTimePerMove() {
        return this.timePerMove;
    }

    /***
     * @return A list with all response times
     */
    public ArrayList<Long> getResponseTimes() {
        return this.responseTimes;
    }
}
