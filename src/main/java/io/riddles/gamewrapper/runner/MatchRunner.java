package io.riddles.gamewrapper.runner;

import io.riddles.gamewrapper.EngineAPI;
import io.riddles.gamewrapper.io.IOEngine;
import io.riddles.gamewrapper.io.IOPlayer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;


/**
 * Created by niko on 26/05/16.
 */
public class MatchRunner extends AbstractRunner implements Runnable, Reportable {

    private EngineAPI api;
    private IOEngine engine;
    private ArrayList<IOPlayer> players; // ArrayList containing player handlers

    public MatchRunner(Long timebankMax, Long timePerMove, int maxTimeouts) {

        super(timebankMax, timePerMove, maxTimeouts);

        engine = null;
        players = new ArrayList<>();
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
     * @param config
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
        api = new EngineAPI(engine, players);
        api.run();
    }

    @Override
    public void postrun() {
        setResults(createResults());

        players.forEach(IOPlayer::finish);

        System.out.println(engine.getStderr());
        engine.finish();
    }

    private JSONObject createResults() {

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
        for (IOPlayer bot : players) {
            System.out.println(bot.getDump());
            System.out.println(bot.getStdout());
            System.out.println(bot.getStderr());
        }
        System.out.println("Engine data:");
        System.out.println(engine.getStdout());
        System.out.println(engine.getStderr());
    }

    private void prepareBot(JSONObject config) {

        if (!config.has("command")) {
            throw new RuntimeException("No command specified for engine");
        }

        String command = config.getString("command");

        try {
            addPlayer(command);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to start engine.");
        }
    }

    private void prepareEngine(JSONObject config) {

        if (!config.has("command")) {
            throw new RuntimeException("No command specified for engine");
        }

        String command = config.getString("command");

        JSONObject engineConfig;
        try {
            engineConfig = config.getJSONObject("configuration");
        } catch (JSONException e) {
            engineConfig = new JSONObject();
        }

        try {
            setEngine(command, engineConfig);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to start engine.");
        }
    }

    /**
     * Creates and starts player (bot) process and adds them
     * to player list
     * @param command Command to start process
     * @throws IOException
     */
    private void addPlayer(String command) throws IOException {
        int id = players.size();
        players.add(createPlayer(command, id));
    }

    /**
     * Creates and starts engine process
     * @param command Command to start process
     * @throws IOException
     */
    private void setEngine(String command, JSONObject engineConfig) throws IOException {
        engine = createEngine(command, engineConfig);
    }
}
