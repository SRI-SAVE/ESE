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

package com.sri.tasklearning.ui.core.library;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import com.sri.tasklearning.ui.core.Fonts;
import com.sri.tasklearning.ui.core.SelectionManager;
import com.sri.tasklearning.ui.core.Utilities;
import com.sri.tasklearning.ui.core.control.PanelHeader;
import com.sri.tasklearning.ui.core.control.SearchTextBox;
import com.sri.tasklearning.ui.core.control.ToolTipper;

/**
 * The main 'Library' component. Uses the ActionModelAssistant to break
 * actions and procedures in to related groups and displays those in a left
 * pane. The members of each group are displayed on the pane, which is an 
 * instance of {@code LibraryActionList}. 
 */
public class LibraryPanel extends AnchorPane {
    public static final double DEF_WIDTH = 310;   
    protected static final double HEADER_HEIGHT = 28.0;
    
    private final ActionModelAssistant amAssistant = ActionModelAssistant.getInstance();
    private LibraryActionList displayedItemList;
    private final VBox groupList = new VBox();
    private final PanelHeader header;
    private final LibraryInfoPanel infoPanel;
    private final SearchTextBox search = new SearchTextBox(this);
    private final SelectionManager groupSelectionMgr = new SelectionManager(this);
     
	private String proceduresListTitle = ""; 
	private String applicationsListTitle = "";

	public LibraryPanel() {
		this("Procedures", "Applications"); 
	}
	    
    protected LibraryPanel(String proceduresListTitle, String applicationsListTitle) {
    	
    	this.proceduresListTitle = proceduresListTitle;
    	this.applicationsListTitle = applicationsListTitle;
    	
        this.setPrefWidth(DEF_WIDTH);      
                
        Label title = new Label("Library");
        title.setFont(Fonts.STANDARD);       
        
        header = new PanelHeader(title, search);
                     
        getStyleClass().add("library-panel");
        groupList.setStyle("-fx-border-color: transparent; -fx-border-width: 0;");
                
        LibraryRowGroup dflt = populateGroupList();
        
        groupList.setMaxWidth(DEF_WIDTH / 2);
                
        infoPanel = new LibraryInfoPanel();
        
        displayedItemList = new LibraryActionList(infoPanel, false, null);
        displayedItemList.setPrefHeight(180);        
        
        AnchorPane.setLeftAnchor(header, 0.0);
        AnchorPane.setRightAnchor(header, 0.0);
        
        AnchorPane.setLeftAnchor(groupList, 0.0);
        AnchorPane.setBottomAnchor(groupList, LibraryInfoPanel.DEF_HEIGHT);
        AnchorPane.setTopAnchor(groupList, PanelHeader.DEF_HEIGHT);
        
        AnchorPane.setRightAnchor(displayedItemList, 0.0);
        AnchorPane.setBottomAnchor(displayedItemList, LibraryInfoPanel.DEF_HEIGHT);
        AnchorPane.setTopAnchor(displayedItemList, PanelHeader.DEF_HEIGHT);
        
        AnchorPane.setLeftAnchor(infoPanel, 0.0);
        AnchorPane.setRightAnchor(infoPanel, 0.0);
        AnchorPane.setBottomAnchor(infoPanel, 0.0);
        
        this.getChildren().addAll(displayedItemList, infoPanel, groupList, header);
        
        layoutChildren();
        
        groupSelectionMgr.selectOnly(dflt);
        displayedItemList.showActions(null, false, false);
    }
    
    @Override
    protected void layoutChildren() {
        double widest_group = 0;
        for (Node node : groupList.getChildren()) {
            double width = ((LibraryRowGroup)node).computeWidth();
            if (width > widest_group)
                widest_group = width; 
        }
        
        for (Node node : groupList.getChildren())
            ((LibraryRowGroup)node).setPrefWidth(widest_group);
        
        groupList.setPrefWidth(widest_group);
        AnchorPane.setLeftAnchor(displayedItemList, Math.min(widest_group, DEF_WIDTH / 2));   
        super.layoutChildren();     
    }
    
    private LibraryRowGroup populateGroupList() {
    	
        LibraryRowGroup procs = new LibraryRowGroup(proceduresListTitle, Utilities.getImage("gear-x2.png"));
        procs.setOnMousePressed(createGroupEventHandler(procs, Namespace.BUILTIN, false, false));
        LibraryRowGroup apps = new LibraryRowGroup(applicationsListTitle, Utilities.getImage("library.png"));
        apps.setOnMousePressed(createGroupEventHandler(apps, null, false, false));
        groupList.getChildren().addAll(procs, apps);
        
        // populate namespaces and prepare spacing considerations
        for(Namespace namespace : amAssistant.getNamespaces()) {
            if (amAssistant.getActions(namespace).size() <= 0 || 
                namespace == Namespace.BUILTIN) {
                continue;
            }
            
            LibraryRowGroup lrg = new LibraryRowGroup(namespace.getName(), Utilities.getImage(namespace.getIcon()));
            lrg.setOnMousePressed(createGroupEventHandler(lrg, namespace, false, false));
            lrg.setLeftIndent(16);

            groupList.getChildren().add(lrg);
        }
        
        LibraryRowGroup tools = new LibraryRowGroup("Toolkit", Utilities.getImage("toolbox.png"));
        tools.setOnMousePressed(createGroupEventHandler(tools, null, false, true));
        groupList.getChildren().add(tools);
        
        LibraryRowGroup used = new LibraryRowGroup("Recently Used", Utilities.getImage("recent.png"));
        used.setOnMousePressed(createGroupEventHandler(used, null, true, false)); 
        groupList.getChildren().add(used);       
        
        return apps; 
    }   
    
    private EventHandler<MouseEvent> createGroupEventHandler(
            final LibraryRowGroup group, final Namespace ns,
            final boolean recent, final boolean toolkit) {
        return new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                ToolTipper.hideTooltip();
                groupSelectionMgr.selectOnly(group);

                search.setText("");
                
                displayedItemList.requestFocus();
                displayedItemList.showActions(ns, recent, toolkit);
                displayedItemList.scrollToTop();

                groupList.requestLayout();
                groupList.layout();
            }
        };
    }  
    
    public void filterActions(String text) {
        displayedItemList.filterDisplayedActions(text);
    }
    
    public void refresh() {
        displayedItemList.refresh(); 
    }
}
