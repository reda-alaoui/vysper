/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.vysper.xmpp.modules.extension.xep0049_privatedata;

import org.apache.vysper.compliance.SpecCompliant;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.modules.core.base.handler.DefaultIQHandler;
import org.apache.vysper.xmpp.protocol.NamespaceURIs;
import org.apache.vysper.xmpp.protocol.StreamErrorCondition;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.response.ServerErrorResponses;
import org.apache.vysper.xmpp.stanza.IQStanza;
import org.apache.vysper.xmpp.stanza.IQStanzaType;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.apache.vysper.xmpp.stanza.StanzaErrorCondition;
import org.apache.vysper.xmpp.stanza.StanzaErrorType;
import org.apache.vysper.xmpp.xmlfragment.Attribute;
import org.apache.vysper.xmpp.xmlfragment.Renderer;
import org.apache.vysper.xmpp.xmlfragment.XMLElement;

/**
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
@SpecCompliant(spec = "xep-0049", status = SpecCompliant.ComplianceStatus.FINISHED, coverage = SpecCompliant.ComplianceCoverage.COMPLETE)
public class PrivateDataIQHandler extends DefaultIQHandler {

    protected PrivateDataPersistenceManager persistenceManager;

    public void setPersistenceManager(PrivateDataPersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    @Override
    protected boolean verifyNamespace(Stanza stanza) {
        return verifyInnerNamespace(stanza, NamespaceURIs.PRIVATE_DATA);
    }

    @Override
    protected boolean verifyInnerElement(Stanza stanza) {
        return verifyInnerElementWorker(stanza, "query");
    }

    @Override
    protected Stanza handleSet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) {
        // Get From
        Entity to = stanza.getTo();
        Entity from = stanza.getFrom();
        if (from == null) {
            from = sessionContext.getInitiatingEntity();
        }

        // Not null, and not addressed to itself
        if (to != null && !to.getBareJID().equals(sessionContext.getInitiatingEntity().getBareJID())) {
            return ServerErrorResponses.getInstance().getStreamError(StreamErrorCondition.BAD_FORMAT, null, "Private data only modifiable by the owner", null);
        }

        XMLElement queryElement = stanza.getFirstInnerElement();

        // Example 4: http://xmpp.org/extensions/xep-0049.html
        // Query element must have a child element with a non-null namespace
        if (queryElement.getInnerElements().size() != 1) {
            return ServerErrorResponses.getInstance().getStanzaError(StanzaErrorCondition.NOT_ACCEPTABLE, stanza, StanzaErrorType.MODIFY, "query's child element is missing", null, null);
        }
        XMLElement x = queryElement.getFirstInnerElement();
        String ns = x.getAttribute("xmlns").getValue();
        if (ns == null) {
            return ServerErrorResponses.getInstance().getStanzaError(StanzaErrorCondition.NOT_ACCEPTABLE, stanza, StanzaErrorType.MODIFY, "no namespace", null, null);
        }

        // No persistancy Manager
        if (persistenceManager == null) {
            return ServerErrorResponses.getInstance().getStanzaError(StanzaErrorCondition.INTERNAL_SERVER_ERROR, stanza, StanzaErrorType.WAIT, "internal storage inaccessible", null, null);
        }

        String queryKey = getKey(x);
        String queryContent = new Renderer(queryElement).getComplete();
        boolean success = persistenceManager.setPrivateData(from, queryKey, queryContent);

        if (success) {
            return StanzaBuilder.createIQStanza(null, from, IQStanzaType.RESULT, stanza.getID()).getFinalStanza();
        } else {
            return StanzaBuilder.createIQStanza(null, from, IQStanzaType.ERROR, stanza.getID()).getFinalStanza();
        }
    }

    @Override
    protected Stanza handleGet(IQStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) {
        Entity to = stanza.getTo();
        Entity from = stanza.getFrom();
        if (from == null) {
            from = sessionContext.getInitiatingEntity();
        }

        // Not null, and not addressed to itself
        if (to != null && !to.getBareJID().equals(sessionContext.getInitiatingEntity().getBareJID())) {
            return ServerErrorResponses.getInstance().getStreamError(StreamErrorCondition.BAD_FORMAT, null, "can only view your data", null);
        }

        XMLElement queryElement = stanza.getFirstInnerElement();
        XMLElement x = queryElement.getFirstInnerElement();
        if (x == null) {
            return ServerErrorResponses.getInstance().getStanzaError(StanzaErrorCondition.NOT_ACCEPTABLE, stanza, StanzaErrorType.MODIFY, "query's child element missing", null, null);
        }

        // No persistancy Manager
        if (persistenceManager == null) {
            return ServerErrorResponses.getInstance().getStanzaError(StanzaErrorCondition.INTERNAL_SERVER_ERROR, stanza, StanzaErrorType.WAIT, "internal storage inaccessible", null, null);
        }

        String queryKey = getKey(x);
        String privateDataXML = persistenceManager.getPrivateData(from, queryKey);

        StanzaBuilder stanzaBuilder = StanzaBuilder.createIQStanza(stanza.getTo(), stanza.getFrom(), IQStanzaType.RESULT, stanza.getID());
        if (privateDataXML == null) {
            stanzaBuilder.startInnerElement(x.getName());
            for (Attribute a : x.getAttributes()) {
                stanzaBuilder.addAttribute(a);
            }
            stanzaBuilder.endInnerElement();
        } else {
            stanzaBuilder.addText(privateDataXML);
        }
        return stanzaBuilder.getFinalStanza();
    }

    /**
     * Create a property name that is unique for this query. eg this XMLElement:
     * <storage xmlns="storage:bookmarks"> is converted into this string:
     * storage-storage-bookmarks
     */
    private String getKey(XMLElement x) {
        StringBuilder queryKey = new StringBuilder();
        queryKey.append(x.getName());
        queryKey.append("-");
        queryKey.append(x.getAttribute("xmlns").getValue());

        // Some characters are not valid for property names
        for (int i = 0; i < queryKey.length(); i++) {
            char c = queryKey.charAt(i);
            if (c == ' ' || c == ':') {
                queryKey.setCharAt(i, '-');
            }
        }
        return queryKey.toString();
    }

}