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

import com.sri.tasklearning.ui.core.exercise.ExerciseModel;

public final class EditSessionManager {
    private EditSessionManager() {}
        
    private static final List<EditSession> currentSessions = 
            new ArrayList<EditSession>();
    
    private static List<ISessionListener> listeners = 
            new ArrayList<ISessionListener>();
    
    private static EditSession activeSession;
    
    public static EditSession getActiveSession() {
        return activeSession; 
    }
    
    public static List<EditSession> getSessions() {
        return currentSessions;
    }
    
    public static void setActiveSession(EditSession session) {
        EditSession oldSession = activeSession;
        activeSession = session;
        
          	if (session == null)
        		ExerciseModel.setActiveModel(null); 
        	else if (session.getController().getModel() instanceof ExerciseModel)  
        		ExerciseModel.setActiveModel((ExerciseModel) session.getController().getModel());

        for (ISessionListener listen : listeners)
            listen.activeSessionChanged(oldSession, activeSession);
    }
    
    public static void addSession(EditSession session) {
        currentSessions.add(session);
    }
    
    public static EditSession removeSessionByFunctor(String functor) {
        EditSession dead = findSessionByFunctor(functor);
        
        if (dead != null)
            currentSessions.remove(dead);
        
        if (dead == activeSession)
            setActiveSession(null);
        
        return dead; 
    }
    
    public static EditSession removeSessionByName(String name) {
        EditSession dead = findSessionByName(name);
        
        if (dead != null)
            currentSessions.remove(dead);
        
        if (dead == activeSession)
            setActiveSession(null);
        
        return dead; 
    }
    
    public static EditSession findSessionByFunctor(String functor) {
        for (EditSession sess : currentSessions)
            if (functor.equals(sess.getController().getModel().getFunctor()))
                return sess;
        
        return null;
    }
    
    public static EditSession findSessionByName(String name) {
        for (EditSession sess : currentSessions)
            if (name.equals(sess.getController().getModel().getName()))
                return sess;
        
        return null;
    }
    
    public interface ISessionListener {
        public void activeSessionChanged(EditSession oldSession, EditSession newSession);
    }
    
    public static void addSessionListener(ISessionListener listen) {
        listeners.add(listen);
    }
    
    public static void removeSessionListener(ISessionListener listen) {
        listeners.remove(listen);
    }
}
