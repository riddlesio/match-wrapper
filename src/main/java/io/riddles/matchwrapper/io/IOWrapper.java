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

package io.riddles.matchwrapper.io;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;;

/**
 * IOWrapper class
 * 
 * A wrapper that handles communication between a process and
 * the game wrapper
 * 
 * @author Sid Mijnders <sid@riddles.io>, Jim van Eeden <jim@starapple.nl>
 */
public abstract class IOWrapper implements Runnable {

    private Process process;
    private OutputStreamWriter inputStream;
    private InputStreamGobbler outputGobbler;
    private InputStreamGobbler errorGobbler;
    protected long pid = -1;
    protected boolean finished;
    protected boolean errored;
    protected long timebank;
    protected int exitStatus;
    
    public String response;
    public ConcurrentLinkedQueue<String> inputQueue;
    
    public IOWrapper(Process process) {
        this.inputStream = new OutputStreamWriter(process.getOutputStream());
        this.outputGobbler = new InputStreamGobbler(process.getInputStream(), this, "output");
        this.errorGobbler = new InputStreamGobbler(process.getErrorStream(), this, "error");
        this.process = process;
        this.errored = false;
        this.finished = false;

        setPid();
    }

    private synchronized void setPid() {
        try {
            Field field = this.process.getClass().getDeclaredField("pid");
            field.setAccessible(true);
            this.pid = field.getLong(this.process);
            field.setAccessible(false);
        }
        catch (Exception ignored) {}
    }

    public abstract boolean send(String line);

    public abstract String ask(String line) throws IOException;
    
    /**
     * Sends a line to the process
     * @param line Output line
     * @return True if write was successful, false otherwise
     */
    public boolean write(String line) {
        if (this.finished) return false;

        try {
            this.inputStream.write(line + "\n");
            this.inputStream.flush();
        } catch(IOException e) {
            System.err.println("Writing to inputstream failed.");
            finish();
            return false;
        }

        return true;
    }

    /**
     * Waits until process returns a response and returns it. Only the
     * first response after the request is processed, others are ignored.
     * @param timeout Time before timeout
     * @return Process's response
     */
    public String getResponse(long timeout) {
        long timeStart = System.nanoTime();
        String response;

        while (this.response == null) {
            long timeElapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - timeStart);
            
            if (timeElapsed >= timeout) {
                return handleResponseTimeout(timeout);
            }

            try { Thread.sleep(2); } catch (InterruptedException ignored) {}
        }
        
        if (this.inputQueue != null) {
            this.inputQueue.remove(this.response);
        }

        response = this.response;
        this.response = null;
        
        return response;
    }

    /**
     * Reponse when there is a timeout
     * @param timeout Time before timeout
     * @return Response
     */
    protected String handleResponseTimeout(long timeout) {
        return "";
    }
    
    /**
     * Ends the process and it's communication
     */
    public int finish() {
        if (this.finished) {
            return this.exitStatus;
        }

        // stop io streams
        try { this.inputStream.close(); } catch (IOException ignored) {}
        this.outputGobbler.finish();
        this.errorGobbler.finish();
        
        // end the process
        this.process.destroy();
        try { this.process.waitFor(); } catch (InterruptedException ignored) {}

        this.finished = true;
        this.exitStatus = this.errored ? 1 : 0;

        return this.exitStatus;
    }
    
    /**
     * @return The process
     */
    public Process getProcess() {
        return this.process;
    }
    
    /**
     * @return The complete stdOut of the process
     */
    public String getStdout() {
        return this.outputGobbler.getData();
    }
    
    /**
     * @return The complete stdErr from the process
     */
    public String getStderr() {
        return this.errorGobbler.getData();
    }

    /**
     * Start the communication with the process
     */
    @Override
    public void run() {
        this.outputGobbler.start();
        this.errorGobbler.start();
    }

    public void setTimebank(long timebank) {
        this.timebank = timebank;
    }
}
