package io.riddles.gamewrapper.runner;

import io.riddles.gamewrapper.io.IOEngine;
import io.riddles.gamewrapper.io.IOPlayer;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by niko on 26/05/16.
 */
public class AbstractRunner implements Reportable {

    private Long timebankMax;
    private Long timePerMove;
    private int maxTimeouts;
    private JSONObject results;

    public AbstractRunner(Long timebankMax, Long timePerMove, int maxTimeouts) {
        this.timebankMax = timebankMax;
        this.timePerMove = timePerMove;
        this.maxTimeouts = maxTimeouts;

        results = new JSONObject();
    }

    protected IOPlayer createPlayer(String command, int id) throws IOException {

        IOPlayer player;
        player = new IOPlayer(wrapCommand(command), id, this.timebankMax, this.timePerMove, this.maxTimeouts);
        player.run();

        return player;
    }

    protected IOEngine createEngine(String command, JSONObject engineConfig) throws IOException {

        IOEngine engine;
        engine = new IOEngine(wrapCommand(command), engineConfig);
        engine.run();

        return engine;
    }

    protected void setResults(JSONObject value) {
        results = value;
    }

    /**
     * Execute command string as a process
     * @param command Command to start process
     * @return The started processs
     * @throws IOException
     */
    private Process wrapCommand(String command) throws IOException {
        System.out.println("executing: " + command);
        return Runtime.getRuntime().exec(command);
    }

    @Override
    public JSONObject getResults() {
        return results;
    }
}
