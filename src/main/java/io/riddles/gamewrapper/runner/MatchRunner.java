package io.riddles.gamewrapper.runner;

import io.riddles.gamewrapper.EngineAPI;
import io.riddles.gamewrapper.io.IOEngine;
import io.riddles.gamewrapper.io.IOPlayer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by niko on 26/05/16.
 */
public class MatchRunner extends AbstractRunner implements Runnable {

    private EngineAPI api;
    private IOEngine engine;
    private ArrayList<IOPlayer> players; // ArrayList containing player handlers
    private JSONObject resultSet;

    public MatchRunner(Long timebankMax, Long timePerMove, int maxTimeouts) {

        super(timebankMax, timePerMove, maxTimeouts);

        engine = null;
        players = new ArrayList<>();
    }

    @Override
    public void prepare(JSONObject config) {


    }

    @Override
    public void run() throws IOException {
        api = new EngineAPI(engine, players);
        api.run();
    }

    @Override
    public void postrun() {
        resultSet = createResultSet();

        players.forEach(IOPlayer::finish);

        System.out.println(engine.getStderr());
        engine.finish();
    }

    @Override
    public JSONObject getResultSet() {

        return resultSet;
    }

    private JSONObject createResultSet() {

        JSONObject output = new JSONObject();
        JSONArray players = new JSONArray();

        String details = api.askGameDetails();
        String playedGame = api.askPlayedGame();

        for (IOPlayer player : this.players) {

            String log    = player.getDump();
            String errors = player.getStderr();

            JSONObject playerOutput = new JSONObject();
            playerOutput.put("log", log);
            playerOutput.put("errors", errors);

            players.put(playerOutput);
        }

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

    /**
     * Creates and starts player (bot) process and adds them
     * to player list
     * @param command Command to start process
     * @throws IOException
     */
    private void addPlayer(String command) throws IOException {
        int id = this.players.size();
        this.players.add(createPlayer(command, id));
    }



    /**
     * Creates and starts engine process
     * @param command Command to start process
     * @throws IOException
     */
    private void setEngine(String command) throws IOException {
        this.engine = createEngine(command);
    }
}
