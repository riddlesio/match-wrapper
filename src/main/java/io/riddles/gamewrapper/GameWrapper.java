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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

import io.riddles.gamewrapper.io.IOEngine;
import io.riddles.gamewrapper.io.IOPlayer;
import io.riddles.gamewrapper.io.IOWrapper;
import io.riddles.gamewrapper.runner.MatchRunner;
import io.riddles.gamewrapper.runner.Runnable;
import io.riddles.gamewrapper.runner.ScenarioRunner;
import jdk.nashorn.internal.parser.JSONParser;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * GameWrapper class
 * 
 * Runs all the processes needed for playing the game, namely
 * the engine and all bots. EngineAPI class to handle communication
 * between those processes
 * 
 * @author Sid Mijnders <sid@riddles.io>, Jim van Eeden <jim@riddles.io>
 */
public class GameWrapper {

    private long timebankMax = 10000L; // 10 seconds default
    private long timePerMove = 500L; // 0,5 seconds default
    private int maxTimeouts = 2; // 2 timeouts default before shutdown
    private String resultFilePath;
    private Runnable runner;
    
    /**
     * Sets timebank settings for the bots
     * @param config The JSON string which contains the settings
     */
     private void parseSettings(JSONObject config) {

         JSONObject wrapperConfig = config.getJSONObject("wrapper");

         if (wrapperConfig.has("timebankMax")) {
             timebankMax = wrapperConfig.getLong("timebankMax");
         }

         if (wrapperConfig.has("timePerMove")) {
             timePerMove = wrapperConfig.getLong("timePerMove");
         }

         if (wrapperConfig.has("maxTimeouts")) {
             maxTimeouts = wrapperConfig.getInt("maxTimeouts");
         }

         resultFilePath = wrapperConfig.getString("resultFilePath");
    }

    private void prepare(JSONObject config) throws IOException {

        JSONObject runnerConfig;
        parseSettings(config);

        if (config.has("match")) {
            runnerConfig = config.getJSONObject("match");
            runner = new MatchRunner(timebankMax, timePerMove, maxTimeouts);
            runner.prepare(runnerConfig);
            return;
        }

        if (config.has("scenario")) {
            runnerConfig = config.getJSONObject("scenario");
            runner = new ScenarioRunner(timebankMax, timePerMove, maxTimeouts);
            runner.prepare(runnerConfig);
            return;
        }

        throw new RuntimeException("Config does not contain either match or scenario");
    }

    private void run() throws IOException {
        runner.run();
    }

    /**
     * Starts the game
     */
    private void start() {
        
        System.out.println("Starting...");

        try {
            run();
        } catch (IOException exception) {
            exception.printStackTrace();
            return;
        }

        postrun();

        System.out.println("Stopping...");
        System.out.println("Done.");
        System.exit(0);
    }

    private void postrun() {

        runner.postrun();
        JSONObject resultSet = runner.getResultSet();

        System.out.println("Saving game...");
        saveGame(resultSet);
    }

    private void saveGame(JSONObject result) {

        try {
            System.out.println("Writing to result.json");

            FileWriter writer = new FileWriter(resultFilePath);
            writer.write(result.toString());
            writer.close();

            System.out.println("Finished writing to result.json");

        } catch (IOException e) {
            System.err.println("Failed to write to result.json");
            System.err.println(e.getMessage());
        }
    }

    public static void main(String[] args) {

//         for (String arg : args) System.out.println(arg);
        JSONObject config;
        GameWrapper game = new GameWrapper();

        try {
            config = new JSONObject(args[0]);
            game.prepare(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse settings.");
        }

//        try {
//            game.parseSettings(args[0], args[1], args[2], args[3]);
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new RuntimeException("Correct arguments not provided, failed to parse settings.");
//        }
//
//        try {
//            game.setEngine(args[4]);
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new RuntimeException("Failed to start engine.");
//        }
//
//        try {
//            for (int i = 5; i < args.length; i++) {
//                game.addPlayer(args[i]);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new RuntimeException("Failed to start bot.");
//        }
        
        try {
            game.start();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error while running game.");
        }
    }
}
