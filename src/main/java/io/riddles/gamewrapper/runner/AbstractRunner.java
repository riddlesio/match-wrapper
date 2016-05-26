package io.riddles.gamewrapper.runner;

import io.riddles.gamewrapper.io.IOEngine;
import io.riddles.gamewrapper.io.IOPlayer;

import java.io.IOException;

/**
 * Created by niko on 26/05/16.
 */
public class AbstractRunner {

    private Long timebankMax;
    private Long timePerMove;
    private int maxTimeouts;

    public AbstractRunner(Long timebankMax, Long timePerMove, int maxTimeouts) {
        this.timebankMax = timebankMax;
        this.timePerMove = timePerMove;
        this.maxTimeouts = maxTimeouts;
    }

    protected IOPlayer createPlayer(String command, int id) throws IOException {

        IOPlayer player;
        player = new IOPlayer(wrapCommand(command), id, this.timebankMax, this.timePerMove, this.maxTimeouts);
        player.run();

        return player;
    }

    protected IOEngine createEngine(String command) throws IOException {

        IOEngine engine;
        engine = new IOEngine(wrapCommand(command));
        engine.run();

        return engine;
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
}
