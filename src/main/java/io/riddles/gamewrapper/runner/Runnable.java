package io.riddles.gamewrapper.runner;

import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by niko on 26/05/16.
 */
public interface Runnable {

    void prepare(JSONObject config) throws IOException;
    void run() throws IOException;
    void postrun();
    JSONObject getResultSet();
}
