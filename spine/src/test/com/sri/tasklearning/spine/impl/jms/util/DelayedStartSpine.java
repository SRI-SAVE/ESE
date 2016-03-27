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
package com.sri.tasklearning.spine.impl.jms.util;

import java.net.URI;

import javax.jms.JMSException;
import javax.jms.Session;

import com.sri.tasklearning.spine.SpineException;
import com.sri.tasklearning.spine.impl.jms.JmsClient;
import com.sri.tasklearning.spine.impl.jms.JmsExceptionListener;
import com.sri.tasklearning.spine.impl.jms.JmsSpine;
import com.sri.tasklearning.spine.impl.jms.StateManager;
import com.sri.tasklearning.spine.impl.jms.SubscriptionManager;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerFactory;

/**
 * This spine instance simulates the LOCAL Spine instance which starts the MessageBroker.
 * It is used to simulate a delay beween starting the MessageBroker, which allows the REMOTE
 * spines to start, and registering for REGISTER messages which must be intercepted
 * to allow the REMOTE spines to continue construction.
 */
public class DelayedStartSpine extends JmsSpine {

    public static void main(String[] args) throws Throwable {
        DelayedStartSpine delayedStartSpine = null;
        try {
            delayedStartSpine = new DelayedStartSpine();
        }
        catch (SpineException e) {
            System.out.println("Couldn't start shell, failing test.");
            System.exit(-1);
        }
        int attempts = 0;
        while (delayedStartSpine.getSpineInstanceCount() == 0 && attempts++ < 10) {
            Thread.sleep(1000);
            System.out.println("Waiting for Lumen to Register.");
        }
        if (delayedStartSpine.getSpineInstanceCount() == 0) {
            System.out.println("Lumen never registered, failing test.");
            delayedStartSpine.finalize();
            System.exit(-1);
        }
        attempts = 0;
        while (delayedStartSpine.getSpineInstanceCount() != 2 && attempts++ < 10) {
            Thread.sleep(100);
        }
        if (delayedStartSpine.getSpineInstanceCount() != 2) {
            System.out.println("Lumen & Shell did not both register, failing test.");
            delayedStartSpine.finalize();
            System.exit(-1);
        }

        // This is to allow the final subscription messages to be sent by the lumen
        // spine - this could do with some serious mocking out to avoid the sleeps
        System.out.println("About to sleep to allow final registration to carry on.");
        for (int i = 0; i < 1000; i++) {
            Thread.sleep(5);
        }
        System.out.println("Sleep done, successfully completed test.");
        delayedStartSpine.finalize();
        System.exit(1);
    }

    public DelayedStartSpine() throws SpineException {
        // Get configured parameters
        int port = getBrokerPort();

        // REMOTE or LOCAL
        this.clientType = JmsClient.LOCAL;
        // The Id of the client (Lumen, Lapdog etc), used to create UID's
        this.clientId = "Shell";
        // The index used to create uniquer UID's for the client
        transactionUidIndex = 0;
        // This is used to verify that at least 1 client is subscribed to a topic before sending

        // Create a ConnectionFactory
        ActiveMQConnectionFactory connectionFactory;
        try {
            System.out.println("Starting local Jms Server for client: " + clientId);
            // configure the embedded broker in the PAL JVM, there can only be one instance
            // of this type of JmsSpine.
            broker = BrokerFactory.createBroker(
                    new URI("broker:(tcp://127.0.0.1:" + port + ")?" +
                            "persistent=false&" +
                            "useJmx=false&" +
                            "maximumConnections=10"));
            broker.start();
            // For high speed comms, the in vm transport protocol is used for
            // messages sent inside the JVM
            connectionFactory = new ActiveMQConnectionFactory(broker.getVmConnectorURI());
            // Create a Connection
            connection = connectionFactory.createConnection();
            connection.setClientID(clientId);
            connection.start();
            connection.setExceptionListener(new JmsExceptionListener(this));

            // Create a Session
            boolean transacted = false;
            session = connection.createSession(transacted, Session.AUTO_ACKNOWLEDGE);

            // Create the topics that the system messages will pass through
            createTopics();

            System.out.println("About to sleep to ensure we miss the REGISTER message");
            Thread.sleep(5000);
            System.out.println("Sleep done lets see if the message is still there.");

            // Manage the subscription synchronization between JmsSpine instances
            stateManager = new StateManager(this);
            subscriptionManager = new SubscriptionManager(this);
        }
        catch (JMSException e) {
            // This exception is encountered when the LOCAL JmsSpine is not up and running and
            // a REMOTE instance tries to connect. The exception is being suppressed in favor
            // of an informative message as this is expected behavior at times - there is a loop
            // in the PALBridge code that tries to connect until it succeeds.
            System.out.println("Unable to connect to Jms Spine, please try again");
            throw new SpineException(e);
        } catch (Exception e) {
            System.out.println("Exception thrown when configuring embedded broker" + e);
            throw new SpineException(e);
        }

    }

    public DelayedStartSpine(JmsClient clientType, String clientId) throws SpineException {
        super(clientType, clientId);
    }
}
