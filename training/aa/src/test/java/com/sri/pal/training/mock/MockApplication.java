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

// $Id: MockApplication.java 7401 2016-03-25 20:18:20Z Chris Jones (E24486) $
package com.sri.pal.training.mock;

import java.net.URL;

import com.sri.pal.ActionModel;
import com.sri.pal.Bridge;
import com.sri.pal.PALException;
import com.sri.pal.common.SimpleTypeName;

public class MockApplication {
    public static final String NS = "mockapp";
    public static final String VERS = "1.0";
    public static final SimpleTypeName LATLON = new SimpleTypeName("latlon",
            VERS, NS);
    public static final SimpleTypeName GEO_BOX_PRED = new SimpleTypeName(
            "isInGeoBox", VERS, NS);

    private final Bridge bridge;
    private final ActionModel am;
    private final MockAppExecutor executor;

    public MockApplication()
            throws PALException {
        bridge = Bridge.newInstance(this.getClass().getSimpleName());
        am = bridge.getActionModel();
        URL url = this.getClass().getResource("MockActionModel.xml");
        am.load(url, NS);
        executor = new MockAppExecutor(bridge);
        am.registerExecutor(GEO_BOX_PRED, executor);
    }

    public void shutdown()
            throws PALException {
        bridge.disconnect();
    }
}
