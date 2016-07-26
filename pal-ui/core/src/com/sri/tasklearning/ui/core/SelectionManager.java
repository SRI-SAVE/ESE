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

import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

/**
 * Provides selection management for {@link ISelectable} items. Supports single
 * and multi-selection and can be disabled when necessary. 
 */
public class SelectionManager {
	
    private List<ISelectable> selectedItems = new ArrayList<ISelectable>();
    private List<ISelectable> unselectableItems = new ArrayList<ISelectable>();
    private boolean disable = false;
    private boolean multiSelecting = false; 
    private Node owner;
    private SelectionEventCallback onItemMousePressed;
    private SelectionEventCallback onItemMousePressedOneShot;
    private SimpleIntegerProperty numSelected = new SimpleIntegerProperty(0);
	private boolean commandInvokationDisabled = false;   

    public boolean isCommandInvokationDisabled() {
		return commandInvokationDisabled;
	}

	public void setCommandInvokationDisabled(boolean commandInvokationDisabled) {
		this.commandInvokationDisabled = commandInvokationDisabled;
	}

	public SelectionManager(Node argOwner) {
        this.owner = argOwner;                
    }

    public void setSelection(ISelectable selectable, boolean isSelected) {
    	
        if (disable)
            return;
        
        if (selectable.isSelected() != isSelected && !unselectableItems.contains(selectable)) {
            selectable.setSelected(isSelected);
                        
            if (isSelected) {
                for (int i = 0; i <= selectedItems.size(); i++)
                    if (i == selectedItems.size() || selectedItems.get(i).getIndex() > selectable.getIndex()) {
                        selectedItems.add(i, selectable);
                        break;
                    }                                 
            }
            else
                selectedItems.remove(selectable);
        }
        numSelected.setValue(selectedItems.size());
        
        if (! commandInvokationDisabled  ) {
        	if (isSelected) {
        		if (onItemMousePressed != null)
        			onItemMousePressed.invoke(selectable); 
        
        		if (onItemMousePressedOneShot != null) {
        			onItemMousePressedOneShot.invoke(selectable);
        			onItemMousePressedOneShot = null; 
        		}
        	}
        }
        
    }
    
    public void selectOnly(ISelectable sel) {
        if (sel != null) {
            selectNone();
            setSelection(sel, true);
        }
        numSelected.setValue(1);
    }
    
    public ISelectable getTopSelection() {
        if (selectedItems.size() > 0)
            return selectedItems.get(0);
        
        return null;
    }
    
    public void selectNone() {
        Object[] copy = selectedItems.toArray();
        for (int i = 0; i < copy.length; i++){
            ISelectable sel = (ISelectable)copy[i];
            setSelection(sel, false);
        }
        numSelected.setValue(0);
        owner.requestFocus();
    }
    
    public void selectAll(List<ISelectable> selections) {
        for (ISelectable sel : selections)
            setSelection(sel, true);
        
        numSelected.setValue(selectedItems.size());
    }
    
    public SimpleIntegerProperty numSelectedProperty() {
        return numSelected;
    }
    
    public List<ISelectable> getSelectedItems() {
        return selectedItems;
    }
    
    public void setMultiSelect(MouseEvent me) {        
        multiSelecting = me.isShiftDown() || me.isControlDown();
    }
    
    public void setMultiSelect(KeyEvent ke) {
        multiSelecting = ke.isShiftDown() || ke.isControlDown();
    }    
    
    public boolean isDisable() {
        return disable;
    }

    public void setDisable(boolean disable) {
        this.disable = disable;
    }
    
    public boolean isMultiSelecting() {
        return multiSelecting;
    }
    
    public void registerSelectionEventCallback(SelectionEventCallback callback) {
        this.onItemMousePressed = callback;
    }
    
    public void registerOneShotSelectionEventCallback(SelectionEventCallback callback) {
        this.onItemMousePressedOneShot = callback;
    }
    
    public void clearSelectionEventCallback() {
        this.onItemMousePressed = null; 
    }        
}
