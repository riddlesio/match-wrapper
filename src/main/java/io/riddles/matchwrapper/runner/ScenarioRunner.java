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

import io.riddles.matchwrapper.io.IOPlayer;
import io.riddles.matchwrapper.io.IOWrapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

/**
 * ScenarioRunner class
 *
 * Feeds a given scenario to a subject (typically a bot)
 * If the bot responds, status "ok" is stored. This is used for
 * the input test on Riddles.io.
 *
 * @author Niko van Meurs <niko@riddles.io>, Jim van Eeden <jim@riddles.io>
 */
public class ScenarioRunner extends AbstractRunner implements Runnable, Reportable {

    private IOWrapper subject;
    private String subjectType;
    private JSONArray scenario;

    public ScenarioRunner() {
        super();
    }

    @Override
    public void prepare(JSONObject config) throws IOException {
        this.scenario = config.getJSONArray("scenario");
        JSONObject subjectConfig = config.getJSONObject("subject");

        String subjectCommand = null;
        String[] subjectCommandParts = null;
        if (commandIsString(subjectConfig)) {
            subjectCommand = subjectConfig.getString("command");
        } else {
            subjectCommandParts = jsonArrayToStringArray(subjectConfig.getJSONArray("command"));
        }

        this.subjectType = subjectConfig.getString("type");

        switch (this.subjectType) {
            case "bot":
                if (subjectCommand != null) {
                    this.subject = createPlayer(subjectCommand, 0);
                } else {
                    this.subject = createPlayer(subjectCommandParts, 0);
                }
                return;
            case "engine":
                JSONObject engineConfig = new JSONObject();
                try {
                    engineConfig = subjectConfig.getJSONObject("configuration");
                } catch (JSONException ignored) {}

                if (subjectCommand != null) {
                    this.subject = createEngine(subjectCommand, engineConfig);
                } else {
                    this.subject = createEngine(subjectCommandParts, engineConfig);
                }

                return;
        }

        throw new RuntimeException("Scenario should contain a subject with type bot or engine");
    }

    public void run() {
        JSONObject result;
        int scenarioSize = this.scenario.length();
        long timeout = getScenarioTimeout();

        try {
            for (int i = 0; i < scenarioSize; i++) {
                String line = this.scenario.getString(i);

                if (line.length() <= 0) continue;

                String[] split = line.split(" ");
                if (!split[0].equals("action")) {
                    this.subject.send(line);
                } else {
                    String action = removeTimeFromAction(line);

                    this.subject.setTimebank(timeout);
                    String response = this.subject.ask(action);

                    if (response.isEmpty()) {
                        throw new IOException(String.format("Response timed out (%dms)", timeout));
                    }
                }
            }

            result = createSuccessResult();
        } catch (IOException exception) {
            result = createErrorResult(exception);
        }

        setResults(result);
    }

    @Override
    public int postrun(long timeElapsed) {
        return 0;
    }

    private long getScenarioTimeout() {
        try {
            for (int i = 0; i < this.scenario.length(); i++) {
                String action = this.scenario.getString(i);
                String[] split = action.split(" ");

                if (split[1].equals("timebank")) {
                    return Integer.parseInt(split[2]);
                }
            }
        } catch(Exception ignored) {}

        System.err.println("Failed to read timebank from scenario");
        return 2000L;
    }

    // Time gets added by the line 'settings timebank [milliseconds]'
    // in the IOPlayer/IOEngine, so removed here
    private String removeTimeFromAction(String action) {
        String[] split = action.split(" ");

        if (split.length != 3) {
            return action;
        }

        try {
            Integer.parseInt(split[2]);
        } catch (Exception ignored) {
            return action;
        }

        return String.format("%s %s", split[0], split[1]);
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

        String errors = this.subject.getStderr();

        JSONObject subjectResult = new JSONObject();
        subjectResult.put("errors", errors);

        if (Objects.equals(this.subjectType, "bot")) {
            String dump = ((IOPlayer) this.subject).getDump();
            subjectResult.put("log", dump);
        }

        JSONObject result = new JSONObject();
        result.put("status", status);
        result.put("subject", subjectResult);

        return result;
    }
}
