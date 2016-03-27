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
package com.sri.pal.training.core.util;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sri.ai.lumen.atr.learning.impl.ATRDemonstrationImpl;
import com.sri.ai.lumen.syntax.LumenSyntaxError;
import com.sri.pal.ActionStreamEvent;
import com.sri.pal.Learner;
import com.sri.pal.PALException;
import com.sri.pal.training.core.response.TaskResponse;

public class ResponseUtil {
    private static final Logger log = LoggerFactory
            .getLogger(ResponseUtil.class);

    public static List<ActionStreamEvent> getEventStream(TaskResponse response,
                                                         Learner learner) {
        try {
            if (response.getDemonstration() == null) {
                response.setDemonstration(ATRDemonstrationImpl.parse(response
                        .getDemo()));
            }
            List<ActionStreamEvent> eventStream = learner
                    .demoFromAtr(response.getDemonstration().getActions());
            return eventStream;
        } catch (LumenSyntaxError e) {
            log.error("Failed to deserialize demonstration in TaskResponse: "
                    + response.getDemo());
            return null;
        } catch (PALException e) {
            log.error("Failed to convert demonstartion to event stream");
            return null;
        }
    }

    public static void setDemonstration(TaskResponse response,
                                        List<ActionStreamEvent> eventStream,
                                        Learner learner) {
        ActionStreamEvent[] traceArr = eventStream
                .toArray(new ActionStreamEvent[eventStream.size()]);

        try {
            response.setDemonstration(learner.demoToAtr(traceArr));
        } catch (PALException e) {
            log.error("Failed to convert bridge demo to ATRDemonstration", e);
        }
    }
}
