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

package io.riddles.matchwrapper.io;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import io.riddles.matchwrapper.MatchWrapper;

/**
 * IOEngine class
 *
 * Extends IOWrapper class by adding stuff specifically
 * for engine processes
 *
 * @author Sid Mijnders <sid@riddles.io>, Jim van Eeden <jim@starapple.nl>
 */
public class IOEngine extends IOWrapper {

    private JSONObject configuration;

    public IOEngine(Process process, JSONObject configuration) {
        super(process);
        this.timebank = 10000;  // 10 seconds
        this.configuration = configuration;
        this.inputQueue = new ConcurrentLinkedQueue<>();
    }

    /**
     * Send line to engine
     * @param message Message to send
     * @return True if write was successful, false otherwise
     */
    public boolean send(String message) {
        if (!MatchWrapper.SUPPRESS_ENGINE_IO) {
            System.out.println(String.format("Engine in: '%s'", message));
        }
        return write(message);
    }

    /**
     * Send line to engine and waits for response
     * @param line Message to send
     * @return Engine's response
     * @throws IOException exception
     */
    public String ask(String line) throws IOException {
        this.response = null;

        send(line);

        return getResponse(this.timebank);
    }

    /**
     * Waits until the engine returns one or multiple messages
     * and returns the first given, returns empty string if there
     * is a timeout
     * @return Message from the engine, empty string if timeout
     */
    public String getMessage() {
        long timeStart = System.nanoTime();
        String message = this.inputQueue.poll();

        while (message == null) {
            long timeElapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timeStart);

            if (timeElapsed >= this.timebank) {
                return handleResponseTimeout(this.timebank);
            }

            try {
                Thread.sleep(2);
            } catch (InterruptedException ignored) {}

            message = this.inputQueue.poll();
        }

        this.response = null;

        if (!MatchWrapper.SUPPRESS_ENGINE_IO) {
            System.out.println(String.format("Engine out: '%s'", message));
        }

        return message;
    }

    /**
     * Shuts down the engine
     */
    public int finish() {
        int exitStatus = super.finish();

        System.out.println("Engine shut down.");

        printErrors();

        return exitStatus;
    }

    /**
     * Handles engine response time out
     * @param timeout Time before timeout
     * @return Empty string
     */
    protected String handleResponseTimeout(long timeout) {
        System.err.println(String.format("Engine took too long! (%dms)", this.timebank));
        this.errored = true;
        return "";
    }

    /**
     * Sends the bot IDs to the engine
     * @param bots All the bots for this game
     * @return False if write failed, true otherwise
     */
    public boolean sendPlayers(ArrayList<IOPlayer> bots) {
        StringBuilder message = new StringBuilder();
        message.append("bot_ids ");
        String connector = "";

        for (int i=0; i < bots.size(); i++) {
            message.append(String.format("%s%d", connector, i));
            connector = ",";
        }

        return send(message.toString());
    }

    public boolean sendConfiguration() {
        return send("configuration " + this.configuration.toString());
    }

    private void printErrors() {
        System.out.println("ENGINE ERROR LOG:\n");
        System.out.println(this.getStderr());
        System.out.println("\nEND ENGINE ERROR LOG");
    }
}

