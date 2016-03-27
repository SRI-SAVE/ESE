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
package com.sri.tasklearning.lapdogController;

import com.sri.tasklearning.spine.MessageHandler;
import com.sri.tasklearning.spine.MessageHandlerException;
import com.sri.tasklearning.spine.Spine;
import com.sri.tasklearning.spine.SpineConstants;
import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.impl.jms.JmsClient;
import com.sri.tasklearning.spine.impl.jms.JmsSpine;
import com.sri.tasklearning.spine.messages.JmsSpineClosing;
import com.sri.tasklearning.spine.messages.Message;
import com.sri.tasklearning.spine.messages.SystemMessageType;
import com.sri.tasklearning.spine.messages.UserMessageType;
import com.sri.tasklearning.util.LogUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import resources.ResourceLoader;

/**
 * Top-level class for the LAPDOG Mediator. Manages the lifecycle of the
 * service.
 */
public class LapdogClient
        implements MessageHandler {
    private static final Logger _logger = LoggerFactory
            .getLogger(LapdogClient.class);

    boolean _lapdogInitialized;

    private final TypePublicationFacade typeFacade;
    private final LapdogFacade lapdogFacade;
    private final LapdogClientCallServiceImpl service;
    private final Spine spine;

    LapdogClient(TypePublicationFacade typeFacade,
                 LapdogFacade lapdogFacade,
                 Spine spine)
            throws SpineException {
        _lapdogInitialized = false;
        this.spine = spine;
        this.typeFacade = typeFacade;
        this.lapdogFacade = lapdogFacade;
        service = new LapdogClientCallServiceImpl(this, spine);
    }

    void waitForInit() {
        synchronized (this) {
            while (!_lapdogInitialized) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    /**
     * Shut down LAPDOG and the LAPDOG Mediator.
     */
    void shutdown() {
        try {
            spine.unsubscribe(UserMessageType.LEARN_REQUEST);
            spine.unsubscribe(UserMessageType.PROCESS_DEMO);
            spine.unsubscribe(SystemMessageType.SPINE_CLOSING);
        } catch (Exception e) {
            // Ignore.
        }
        lapdogFacade.shutdown();
        typeFacade.shutdown();
        service.shutdown();
        try {
            spine.shutdown(false);
        } catch (Exception e) {
            // Do nothing.
        }
    }

    /**
     * Driver for {@link LapdogClient}.
     *
     * @param args
     *            takes no arguments
     */
    public static void main(final String[] args) {
        LogUtil.configureLogging("ITL_UI_log4j_config", ResourceLoader.class);

        try {
            start();
        } catch (final Exception e) {
            // Controller initialization is not recoverable
            _logger.error("Failed to initialize LAPDOG, shutting down:", e);
            System.exit(1);
        }
    }

    static LapdogClient start()
            throws SpineException {
        Spine spine = new JmsSpine(JmsClient.REMOTE, SpineConstants.LAPDOG_MEDIATOR);
        LapdogFacade lapdogFacade = new LapdogFacade(spine);
        TypePublicationFacade typeFacade = new TypePublicationFacade(
                lapdogFacade);
        LapdogClient lc = new LapdogClient(typeFacade, lapdogFacade, spine);
        LapdogClientCallServiceImpl service = lc.getService();
        spine.subscribe(service, UserMessageType.LEARN_REQUEST);
        spine.subscribe(service, UserMessageType.PROCESS_DEMO);
        spine.subscribe(service, UserMessageType.LEARN_OPTION_REQUEST);
        spine.subscribe(lc, SystemMessageType.SPINE_CLOSING);
        synchronized (lc) {
            lc._lapdogInitialized = true;
            lc.notifyAll();
        }
        return lc;
    }

    LapdogClientCallServiceImpl getService() {
        return service;
    }

    LapdogFacade getLapdogFacade() {
        return lapdogFacade;
    }

    TypePublicationFacade getTypeFacade() {
        return typeFacade;
    }

    @Override
    public String toString() {
        return "LapdogClient";
    }

    public void cancel() {
        lapdogFacade.cancel();
    }

    @Override
    public void handleMessage(Message message)
            throws MessageHandlerException {
        if (message instanceof JmsSpineClosing) {
            JmsSpineClosing close = (JmsSpineClosing) message;
            if (close.getSpineType() == JmsClient.LOCAL) {
                Thread t = new Thread() {
                    @Override
                    public void run() {
                        shutdown();
                    }
                };
                t.setName(getClass() + " shutdown thread");
                t.start();
            }
        } else {
            _logger.warn("Unexpected message ({}): {}", message.getClass(), message);
        }
    }
}
