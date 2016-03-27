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
package com.sri.tasklearning.spine.impl.jms;

import javax.jms.ExceptionListener;
import javax.jms.JMSException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used by JMS to route exceptions to for handling
 */
public class JmsExceptionListener implements ExceptionListener {
    private static final Logger log = LoggerFactory.getLogger(JmsSpine.class);

    private final JmsSpine client;
    private final String clientId;
    private final JmsClient clientType;

    public JmsExceptionListener(JmsSpine client) {
        this.client = client;
        this.clientId = client.getClientId();
        this.clientType = client.getClientType();
    }

    public void onException(JMSException e) {
        log.warn("A " + clientType
                + " JMS Spine exception occurred in instance " + clientId, e);
        try {
            client.shutdown(false);
        } catch (Exception e1) {
            log.warn("Exception while shutting down " + client, e1);
        }
    }
}
