/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.openhim.mediator.normalization;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openhim.mediator.engine.messages.FinishRequest;
import org.openhim.mediator.engine.messages.SimpleMediatorRequest;
import org.openhim.mediator.messages.ParsedRegistryStoredQuery;
import scala.concurrent.duration.Duration;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ParseRegistryStoredQueryActorTest {

    static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }

    @Test
    public void testParseValidRequest() throws Exception {
        InputStream testAdhocRequestIn = getClass().getClassLoader().getResourceAsStream("adhocQueryRequest_wSOAP.xml");
        final String testAdhocRequest = IOUtils.toString(testAdhocRequestIn);

        new JavaTestKit(system) {{
            ActorRef actor = system.actorOf(Props.create(ParseRegistryStoredQueryActor.class));

            actor.tell(new SimpleMediatorRequest<>(getRef(), getRef(), testAdhocRequest), getRef());

            ParsedRegistryStoredQuery result = expectMsgClass(Duration.create(60, TimeUnit.SECONDS), ParsedRegistryStoredQuery.class);
            assertEquals("1234567890", result.getPatientId().getIdentifier());
            assertEquals("TestID", result.getPatientId().getAssigningAuthority().getAssigningAuthority());
            assertEquals("1.2.3", result.getPatientId().getAssigningAuthority().getAssigningAuthorityId());
            assertEquals("urn:uuid:44657c95-9a4a-420e-bb62-93a773e8a5f1", result.getMessageId());
        }};
    }

    @Test
    public void testParseInvalidRequest() throws Exception {
        final String testAdhocRequest = "a bad message";

        new JavaTestKit(system) {{
            ActorRef actor = system.actorOf(Props.create(ParseRegistryStoredQueryActor.class));

            actor.tell(new SimpleMediatorRequest<String>(getRef(), getRef(), testAdhocRequest), getRef());

            FinishRequest result = expectMsgClass(Duration.create(60, TimeUnit.SECONDS), FinishRequest.class);
            assertEquals(new Integer(400), result.getResponseStatus());
        }};
    }
}