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

package com.sri.tasklearning.novo.factory;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.util.Callback;

import com.sri.tasklearning.novo.Novo;
import com.sri.tasklearning.novo.thing.Assembly;
import com.sri.tasklearning.novo.thing.SizeEnum;
import com.sri.tasklearning.novo.thing.Thing;

public class ThingSocket extends HBox {
    private static final List<ThingSocket> sockets = new ArrayList<ThingSocket>();
    private final boolean acceptAssemblies;
    private final Callback<ThingSocket,Object> onChanged;
    private Thing thing;
    
    public ThingSocket(boolean acceptAssemblies, Callback<ThingSocket,Object> onChanged) {        
        super();
        
        this.acceptAssemblies = acceptAssemblies; 
        this.onChanged = onChanged;
        
        setStyle("-fx-background-color: white; -fx-border-color: darkgray; -fx-border-width: 1;");
        setAlignment(Pos.CENTER);        
        
        // TODO this wont' work for assemblies unless we scale them, which maybe we should. 
        double size = SizeEnum.LARGE.pixels() + 20;
        this.setPrefSize(size, size);
        this.setMaxWidth(Region.USE_PREF_SIZE);
        this.setMaxHeight(Region.USE_PREF_SIZE);
        
        sockets.add(this);
    }
    
    public boolean dropThing(Thing thing) {
        if (!acceptAssemblies && thing instanceof Assembly)
            return false;
        
        if (this.thing != null)
            if (thing.isUsed())
                Novo.getInstance().getUsedPartsViewer().addThing(this.thing); 
            else
                Novo.getInstance().getUnusedPartsViewer().addThing(this.thing);
        
        this.thing = thing;
        
        thing.getNode().setOpacity(1);
        this.getChildren().clear();
        this.getChildren().add(thing.getNode());
        
        onChanged.call(this);
        
        return true;
    }
    
    public Thing getThing() {
        return thing;
    }
    
    public static List<ThingSocket> getAllSockets() {
        return sockets; 
    }
    
    public void clear() {
        this.getChildren().clear();
        
        thing = null; 
        onChanged.call(this);
    }
}
