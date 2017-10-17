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

package io.riddles.matchwrapper.runner;

import io.riddles.matchwrapper.EngineAPI;
import io.riddles.matchwrapper.io.IOEngine;
import io.riddles.matchwrapper.io.IOPlayer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;


/**
 * MatchRunner class
 *
 * Handles the running of a match between one or more bots
 * and a game engine. Used for all types of matches on Riddles.io.
 *
 * @author Niko van Meurs <niko@riddles.io>, Jim van Eeden <jim@riddles.io>
 */
public class MatchRunner extends AbstractRunner implements Runnable, Reportable {

    private EngineAPI api;
    private IOEngine engine;
    private ArrayList<IOPlayer> players; // ArrayList containing player handlers

    public MatchRunner() {
        super();
        this.engine = null;
        this.players = new ArrayList<>();
    }

    /**
     * Config looks as follows:
     * {
     *     engine: {
     *         command: String,
     *         configuration: {
     *
     *         }
     *     },
     *     bots: [
     *         {
     *             command: String
     *         }
     *     ]
     * }
     * @param config Matchrunner configuration
     */
    @Override
    public void prepare(JSONObject config) {

        if (!config.has("engine")) {
            throw new RuntimeException("No configuration present for engine");
        }

        JSONObject engineConfig = config.getJSONObject("engine");
        prepareEngine(engineConfig);

        if (!config.has("bots")) {
            throw new RuntimeException("No bots found in configuration");
        }

        JSONArray bots = config.getJSONArray("bots");

        try {
            bots.forEach(botConfig -> this.prepareBot((JSONObject) botConfig));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to start bot.");
        }
    }

    @Override
    public void run() throws IOException {
        this.api = new EngineAPI(this.engine, this.players);
        this.api.run();
    }

    @Override
    public int postrun(long timeElapsed) {
        setResults(createResults(timeElapsed));

        int playerStatusSum = this.players.stream().mapToInt(IOPlayer::finish).sum();

        int engineStatus = this.engine.finish();

        return playerStatusSum + engineStatus > 0 ? 1 : 0;
    }

    private JSONObject createResults(long timeElapsed) {

        JSONObject output = new JSONObject();
        JSONArray players = new JSONArray();

        String details = this.api.askGameDetails();
        String playedGame = this.api.askPlayedGame();

        for (IOPlayer player : this.players) {

            String log    = player.getDump();
            String errors = player.getStderr();

            JSONArray responseTimes = new JSONArray(player.getResponseTimes());
            long totalResponseTime = player.getResponseTimes().stream()
                    .reduce(0L, (a, b) -> a + b);

            JSONObject playerOutput = new JSONObject();
            playerOutput.put("log", log);
            playerOutput.put("errors", errors);
            playerOutput.put("responseTimes", responseTimes);
            playerOutput.put("totalResponseTime", totalResponseTime);

            players.put(playerOutput);
        }

        output.put("timeElapsed", timeElapsed);
        output.put("details", details);
        output.put("game", playedGame);
        output.put("players", players);

        return output;
    }

    private void printGame() {

        System.out.println("Bot data:");
        for (IOPlayer bot : this.players) {
            System.out.println(bot.getDump());
            System.out.println(bot.getStdout());
            System.out.println(bot.getStderr());
        }
        System.out.println("Engine data:");
        System.out.println(this.engine.getStdout());
        System.out.println(this.engine.getStderr());
    }

    private void prepareBot(JSONObject config) {

        if (!config.has("command")) {
            throw new RuntimeException("No command specified for bot.");
        }

        try {
            if (commandIsString(config)) {
                addPlayer(config.getString("command"));
            } else {
                addPlayer(jsonArrayToStringArray(config.getJSONArray("command")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to start bot.");
        }
    }

    private void prepareEngine(JSONObject config) {

        if (!config.has("command")) {
            throw new RuntimeException("No command specified for engine.");
        }

        JSONObject engineConfig;
        try {
            engineConfig = config.getJSONObject("configuration");
        } catch (JSONException e) {
            engineConfig = new JSONObject();
        }

        try {
            if (commandIsString(config)) {
                setEngine(config.getString("command"), engineConfig);
            } else {
                setEngine(jsonArrayToStringArray(config.getJSONArray("command")), engineConfig);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to start engine.");
        }
    }

    /**
     * Creates and starts player (bot) process and adds them
     * to player list
     * @param command Command to start process
     * @throws IOException exception
     */
    private void addPlayer(String command) throws IOException {
        int id = this.players.size();
        this.players.add(createPlayer(command, id));
    }

    /**
     * Creates and starts player (bot) process and adds them
     * to player list
     * @param commandParts Command parts to start process
     * @throws IOException exception
     */
    private void addPlayer(String[] commandParts) throws IOException {
        int id = this.players.size();
        this.players.add(createPlayer(commandParts, id));
    }

    /**
     * Creates and starts engine process
     * @param command Command to start process
     * @throws IOException exception
     */
    private void setEngine(String command, JSONObject engineConfig) throws IOException {
        this.engine = createEngine(command, engineConfig);
    }

    /**
     * Creates and starts engine process
     * @param commandParts Command parts to start process
     * @throws IOException exception
     */
    private void setEngine(String[] commandParts, JSONObject engineConfig) throws IOException {
        this.engine = createEngine(commandParts, engineConfig);
    }
}
