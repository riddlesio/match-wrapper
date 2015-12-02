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

package io.riddles.gamewrapper.io;

import java.io.IOException;
import java.util.ArrayList;

/**
 * IOEngine class
 * 
 * Extends IOWrapper class by adding stuff specifically
 * for engine processes
 * 
 * @author Sid Mijnders <sid@riddles.io>, Jim van Eeden <jim@starapple.nl>
 */
public class IOEngine extends IOWrapper {
	
	private final long TIMEOUT = 2000; // 2 seconds

    public IOEngine(Process process) {
        super(process);
    }

    /**
     * Send line to engine
     * @param line Line to send
     * @return True if write was successful, false otherwise
     */
    public boolean send(String message) throws IOException {
    	return write(message);
    }
    
    /**
     * Send line to engine and waits for response
     * @param line Line to output
     * @param timeout Time before timeout
     * @return Engine's response
     * @throws IOException
     */
    public String ask(String message) throws IOException {
        return super.ask(message, this.TIMEOUT);
    }
    
    /**
     * Waits until engine returns a response and returns it
     * @return Engine's response, returns and empty string when there is no response
     */
    public String getResponse() {
    	return super.getResponse(this.TIMEOUT);
    }
    
    /**
     * Shuts down the engine
     */
    public void finish() {
    	super.finish();
    	System.out.println("Engine shut down.");
    }
    
    /**
     * Handles engine response time out
     * @param timeout Time before timeout
     * @return Empty string
     */
    protected String handleResponseTimeout(long timeout) {

        // retry message again with longer timeout?
        System.err.println("Engine took too long!");
        return "";
    }
    
    /**
     * Sends the bot IDs to the engine
     * @param bots All the bots for this game
     * @return False if write failed, true otherwise
     */
    public boolean sendPlayers(ArrayList<IOPlayer> bots) {
    	StringBuilder message = new StringBuilder();
    	message.append("bot_ids ");
    	String connector = "";
    	for(int i=0; i < bots.size(); i++) {
    		message.append(String.format("%s%d", connector, i));
    		connector = ",";
    	}
    	return write(message.toString());
    }
}

