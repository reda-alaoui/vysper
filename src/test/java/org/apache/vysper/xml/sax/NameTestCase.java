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
package org.apache.vysper.xml.sax;

import junit.framework.TestCase;

import org.apache.vysper.xml.sax.impl.XMLParser;



/**
 * @author The Apache MINA Project (dev@mina.apache.org)
 */
public class NameTestCase extends TestCase {

	public void testValidName() {
		assertTrue(XMLParser.NAME_PATTERN.matcher("abc").find());
		assertTrue(XMLParser.NAME_PATTERN.matcher("_abc").find());
		assertTrue(XMLParser.NAME_PATTERN.matcher(":abc").find());
		assertTrue(XMLParser.NAME_PATTERN.matcher("Aabc").find());
		assertTrue(XMLParser.NAME_PATTERN.matcher("\u00C8abc").find());
		assertTrue(XMLParser.NAME_PATTERN.matcher("\u00C8abc").find());
		assertFalse(XMLParser.NAME_PATTERN.matcher("3abc").find());
		assertFalse(XMLParser.NAME_PATTERN.matcher("\u2001abc").find());
		assertTrue(XMLParser.NAME_PATTERN.matcher("a3bc").find());
		assertFalse(XMLParser.NAME_PATTERN.matcher("-abc").find());
		assertTrue(XMLParser.NAME_PATTERN.matcher("ab-c").find());
		
	}

}