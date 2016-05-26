package io.riddles.gamewrapper.runner;

import io.riddles.gamewrapper.io.IOWrapper;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

/**
 * Created by niko on 26/05/16.
 */
public class ScenarioRunner extends AbstractRunner implements Runnable {

    private IOWrapper subject;
    private List<String> scenario;
    private Long timeout;

    public ScenarioRunner(Long timebankMax, Long timePerMove, int maxTimeouts) {

        super(timebankMax, timePerMove, maxTimeouts);
    }

    @Override
    public void prepare(JSONObject config) throws IOException {

        JSONObject subjectConfig = config.getJSONObject("subject");
        String subjectType = subjectConfig.getString("type");
        String subjectCommand = subjectConfig.getString("command");

        switch (subjectType) {

            case "bot":
                subject = createPlayer(subjectCommand, 0);
                break;
            case "engine":
                subject = createEngine(subjectCommand);
                break;
            default:
                throw new RuntimeException("Scenario should contain a subject with type bot or engine");
        }
    }

    public void run() throws IOException {

        int scenarioSize = scenario.size();

        for(int i = 0; i < scenarioSize; i++) {
            String action = scenario.get(i);

            if (i + 1 < scenarioSize) {
                subject.write(action);
            } else {
                String response = subject.ask(action, timeout);
            }
        }
    }

    @Override
    public void postrun() {

    }

    @Override
    public JSONObject getResultSet() {
        return null;
    }
}
