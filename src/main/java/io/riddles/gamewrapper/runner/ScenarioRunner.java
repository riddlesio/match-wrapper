package io.riddles.gamewrapper.runner;

import io.riddles.gamewrapper.io.IOPlayer;
import io.riddles.gamewrapper.io.IOWrapper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Created by niko on 26/05/16.
 */
public class ScenarioRunner extends AbstractRunner implements Runnable, Reportable {

    private IOWrapper subject;
    private String subjectType;
    private JSONArray scenario;
    private Long timeout;

    public ScenarioRunner(Long timebankMax, Long timePerMove, int maxTimeouts) {

        super(timebankMax, timePerMove, maxTimeouts);
        timeout = 2000L;
    }

    @Override
    public void prepare(JSONObject config) throws IOException {

        JSONObject subjectConfig = config.getJSONObject("subject");
        String subjectCommand = subjectConfig.getString("command");
        scenario = config.getJSONArray("scenario");
        subjectType = subjectConfig.getString("type");

        switch (subjectType) {

            case "bot":
                subject = createPlayer(subjectCommand, 0);
                return;
            case "engine":
                subject = createEngine(subjectCommand);
                return;
        }

        throw new RuntimeException("Scenario should contain a subject with type bot or engine");
    }

    public void run() {

        JSONObject result;
        int scenarioSize = scenario.length();

        try {
            for (int i = 0; i < scenarioSize; i++) {
                String action = scenario.getString(i);

                if (i + 1 < scenarioSize) {
                    subject.write(action);
                } else {
                    String response = subject.ask(action, timeout);

                    if (response.isEmpty()) {
                        throw new IOException("Response timed out (2000ms)");
                    }
                }
            }

            result = createSuccessResult();
            setResults(result);

        } catch (IOException exception) {

            result = createErrorResult(exception);
            setResults(result);
        }
    }

    @Override
    public void postrun() {

    }

    private JSONObject createSuccessResult() {

        return createResult("ok");
    }

    private JSONObject createErrorResult(Exception exception) {

        JSONObject error = new JSONObject();
        error.put("message", exception.getMessage());

        JSONObject result = createResult("error");
        result.put("error", error);

        return result;
    }

    private JSONObject createResult(String status) {

        String errors = subject.getStderr();

        JSONObject subjectResult = new JSONObject();
        subjectResult.put("errors", errors);

        if (Objects.equals(subjectType, "bot")) {
            String dump = ((IOPlayer) subject).getDump();
            subjectResult.put("log", dump);
        }

        JSONObject result = new JSONObject();
        result.put("status", status);
        result.put("subject", subjectResult);

        return result;
    }
}
