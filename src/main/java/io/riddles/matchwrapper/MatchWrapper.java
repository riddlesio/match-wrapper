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

import io.riddles.matchwrapper.runner.MatchRunner;
import io.riddles.matchwrapper.runner.Reportable;
import io.riddles.matchwrapper.runner.Runnable;
import io.riddles.matchwrapper.runner.ScenarioRunner;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * MatchWrapper class
 *
 * Runs all the processes needed for playing the game, namely
 * the engine and all bots. EngineAPI class to handle communication
 * between those processes
 *
 * @author Sid Mijnders <sid@riddles.io>, Jim van Eeden <jim@riddles.io>
 */
public class MatchWrapper implements Runnable {

    public static boolean DEBUG = false;
    public static boolean SUPPRESS_ENGINE_IO = false; // when true: engine I/O is not printed
    public static boolean PROPAGATE_BOT_EXIT_CODE = false; // when true: if a bot crashes, wrapper exits with code 1
    public static long MAX_TIME_BANK = 10000L; // 10 seconds default
    public static long TIME_PER_MOVE = 500L; // 0,5 seconds default
    public static int MAX_TIMEOUTS = 0; // 0 timeouts default before shutdown
    public static long MAX_MEMORY = 250000; // over 250MB triggers warning
    private String resultFilePath;
    private Runnable runner;

    public static void main(String[] args) throws IOException {
        long startTime = System.nanoTime();

        JSONObject config;
        MatchWrapper game = new MatchWrapper();

        try {
            config = new JSONObject(args[0]);
            game.prepare(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse settings." + e.getMessage());
        }

        System.out.println("Starting...");
        game.run();

        long timeElapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

        System.out.println("Stopping...");
        int exitStatus = game.postrun(timeElapsed);

        System.out.println("Done.");
        System.exit(exitStatus);
    }

    @Override
    public void prepare(JSONObject config) throws IOException {
        JSONObject runnerConfig;
        parseSettings(config);

        if (config.has("match")) {
            runnerConfig = config.getJSONObject("match");
            this.runner = new MatchRunner();
        } else if (config.has("scenario")) {
            runnerConfig = config.getJSONObject("scenario");
            this.runner = new ScenarioRunner();
        } else {
            throw new RuntimeException("Config does not contain either match or scenario");
        }

        this.runner.prepare(runnerConfig);
    }

    /**
     * Sets timebank settings for the bots
     *
     * @param config The JSON string which contains the settings
     */
    private void parseSettings(JSONObject config) {

        JSONObject wrapperConfig = config.getJSONObject("wrapper");

        if (wrapperConfig.has("timebankMax")) {
            MAX_TIME_BANK = wrapperConfig.getLong("timebankMax");
        }

        if (wrapperConfig.has("timePerMove")) {
            TIME_PER_MOVE = wrapperConfig.getLong("timePerMove");
        }

        if (wrapperConfig.has("maxTimeouts")) {
            MAX_TIMEOUTS = wrapperConfig.getInt("maxTimeouts");
        }

        if (wrapperConfig.has("maxMemory")) {
            MAX_MEMORY = wrapperConfig.getLong("maxMemory");
        }

        if (wrapperConfig.has("debug")) {
            DEBUG = wrapperConfig.getBoolean("debug");
        }

        if (wrapperConfig.has("suppressEngineIO")) {
            SUPPRESS_ENGINE_IO = wrapperConfig.getBoolean("suppressEngineIO");
        }

        if (wrapperConfig.has("propagateBotExitCode")) {
            PROPAGATE_BOT_EXIT_CODE = wrapperConfig.getBoolean("propagateBotExitCode");
        }

        this.resultFilePath = wrapperConfig.getString("resultFile");
    }

    @Override
    public void run() throws IOException {
        this.runner.run();
    }

    @Override
    public int postrun(long responseTime) throws IOException {
        int exitStatus = this.runner.postrun(responseTime);

        JSONObject resultSet = ((Reportable) this.runner).getResults();

        System.out.println("Saving game...");
        saveGame(resultSet);

        return exitStatus;
    }

    private void saveGame(JSONObject result) throws IOException {
        System.out.println(String.format("Writing to %s", this.resultFilePath));

        FileWriter writer = new FileWriter(this.resultFilePath);
        writer.write(result.toString());
        writer.close();

        if (DEBUG && result.has("game")) {
            FileWriter gameWriter = new FileWriter("./game-resultfile.json");
            gameWriter.write(result.getString("game"));
            gameWriter.close();
        }

        System.out.println(String.format("Finished writing to %s", this.resultFilePath));
    }
}
