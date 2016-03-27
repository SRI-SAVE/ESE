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

/**
 * Implements undo stack functionality including undo, redo and clear.
 * Undoable operations are defined by the {@link IUndoable} interface. 
 * Components interested in undo stack mutations can implement 
 * {@link IUndoWatcher}. 
 */

public final class UndoManager {
    private List<IUndoable> undoStack = new ArrayList<IUndoable>();
    private List<IUndoable> redoStack = new ArrayList<IUndoable>();
    private List<IUndoWatcher> watchers = new ArrayList<IUndoWatcher>();
    private boolean undoing = false;   
       
    public boolean canUndo() {
        return undoStack.size() > 0;
    }
    
    public boolean canRedo() {
        return redoStack.size() > 0;
    }

    public void pushUndo(IUndoable action) {
        pushUndo(action, true);
    }

    private void pushUndo(IUndoable action, boolean clearRedoStack) {
        if (action != null && !undoing) {
            if (clearRedoStack) 
                redoStack = new ArrayList<IUndoable>();
            undoStack.add(0, action);
            for (IUndoWatcher w : watchers) {
                w.onUndoChanged(peekUndo());
            }
        }
    }

    private void pushRedo(IUndoable action) {
        if (action != null) {
            redoStack.add(0, action);
            for (IUndoWatcher w : watchers) 
                w.onUndoChanged(peekUndo());
        }
    }

    public void undo() {
        if (canUndo()) {
            undoing = true;
            final IUndoable action = undoStack.get(0);
            undoStack.remove(0);
            action.undo();
            pushRedo(action);
            undoing = false;
        }
    }

    public void redo() {
        if (canRedo()) {
            undoing = true;
            final IUndoable action = redoStack.get(0);
            redoStack.remove(0);
            action.redo();
            undoing = false;
            pushUndo(action, false);
        }
    }

    public IUndoable peekUndo() {
        if (undoStack.size() > 0)
            return undoStack.get(0);
        return null;
    }

    public IUndoable peekRedo() {
        if (redoStack.size() > 0)
            return redoStack.get(0);
        return null; 
    }

    public void reset() {
        redoStack = new ArrayList<IUndoable>();
        undoStack = new ArrayList<IUndoable>();
        
        for (IUndoWatcher w : watchers) 
            w.onUndoCleared();
    }

    public void registerWatcher(IUndoWatcher watcher) {
        watchers.add(watcher);
    }

    public void unregisterWatcher(IUndoWatcher watcher) {
        watchers.remove((Object) watcher);
    }
}
