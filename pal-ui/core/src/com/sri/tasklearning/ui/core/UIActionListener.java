/*
 * Copyright 2016 SRI International
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sri.tasklearning.ui.core;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.pal.ActionStreamEvent;
import com.sri.pal.GestureEnd;
import com.sri.pal.GestureStart;
import com.sri.pal.GlobalActionListener;
import com.sri.pal.PALException;
import com.sri.pal.ActionStreamEvent.Status;

public abstract class UIActionListener {
    private final SimpleBooleanProperty listening = new SimpleBooleanProperty(true);
    private List<ActionStreamEvent> stream = new ArrayList<ActionStreamEvent>();
    private GestureStart gStart = null;
    
    private static final Logger log = LoggerFactory
            .getLogger(UIActionListener.class);

    private static boolean initialized = false;
    private static final List<UIActionListener> listeners = new ArrayList<UIActionListener>();

    public UIActionListener(boolean listening) {
        if (!initialized)
            init();

        this.listening.setValue(listening);

        listeners.add(this); 
    }
    
    private static void init() {
        BackendFacade.getInstance().addActionListener(new GlobalActionListener() {
            @Override
            public void actionStarted(final ActionStreamEvent act) {
                if (act.getCaller() == null) {
                    Runnable run = new Runnable() {
                        public void run() {
                            for (UIActionListener listener : listeners) {
                                if (listener.listening.getValue()) {
                                    listener.onActionStarted(act);
                                }
                            }
                        }
                    };
                    Platform.runLater(run);
                    act.waitUntilFinished();

                    // If an action invocation failed, don't listen to it
                    if (act.getStatus() != com.sri.pal.ActionStreamEvent.Status.ENDED)
                        return;

                    run = new Runnable() {
                        public void run() {
                            for (UIActionListener listener : listeners) {
                                if (listener.listening.getValue()) {
                                    listener.onActionFinished(act);
                                }
                            }
                        }
                    };
                    Platform.runLater(run);
                }
            }
        });
    }
    
    public BooleanProperty listeningProperty() {
        return listening; 
    }
    
    public void setListening(boolean listening) {
        this.listening.setValue(listening);
    }
    
    public boolean isListening() {
        return listening.getValue();
    }
    
    public void defean() {
        listeners.remove(this);
    }

    public List<ActionStreamEvent> takeStream() {
        for (ActionStreamEvent act : new ArrayList<ActionStreamEvent>(stream))
            if (act.getStatus() == Status.FAILED)
                stream.remove(act);
        List<ActionStreamEvent> taken = stream;
        stream = new ArrayList<ActionStreamEvent>();
        return taken;
    }
    
    public void addSynthesizedActions(List<ActionStreamEvent> acts) {
        for (ActionStreamEvent act : acts) {
            onActionStarted(act);
            onActionFinished(act); 
        }
    }

    public void onActionStarted(ActionStreamEvent act) {
        stream.add(act);
    }

    public void onActionFinished(ActionStreamEvent act) {
        if (act instanceof GestureStart) {
            gStart = (GestureStart) act;
            return;
        } else if (act instanceof GestureEnd) {
            //Collections.sort(stream, ActionStreamEvent.COMPARATOR);
            GestureEnd gEnd = (GestureEnd) act;
            int startIdx = stream.indexOf(gStart);
            int endIdx = stream.indexOf(gEnd);
            List<ActionStreamEvent> chunk = stream.subList(startIdx, endIdx + 1);
            List<ActionStreamEvent> recognized = null;
            try {
                recognized = BackendFacade
                        .getInstance()
                        .getBridge()
                        .getLearner()
                        .recognizeIdiom(chunk.toArray(new ActionStreamEvent[0]));
            } catch (PALException e) {
                log.error("Error during idiom recognition", e);
                recognized = chunk;
            }

            if (recognized.get(0).getDefinition() != null) {
                List<ActionStreamEvent> temp = new ArrayList<ActionStreamEvent>();
                temp.addAll(stream.subList(0, startIdx));
                temp.addAll(recognized);
                temp.addAll(stream.subList(endIdx + 1, stream.size()));
                stream = temp; 
                // Visualize the idiom as a plain action
                visualizeAction(recognized.get(0));
            } else {
                for (ActionStreamEvent evt : chunk) {
                    if (evt.getDefinition() != null)
                        visualizeAction(evt);
                }
            }
            gStart = null;
            return;
        }

        if (gStart == null)
            visualizeAction(act);
    }
    
    public abstract void visualizeAction(ActionStreamEvent act);
}
