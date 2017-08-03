/*******************************************************************************
 * ============LICENSE_START==================================================
 * * org.onap.dmaap
 * * ===========================================================================
 * * Copyright © 2017 AT&T Intellectual Property. All rights reserved.
 * * ===========================================================================
 * * Licensed under the Apache License, Version 2.0 (the "License");
 * * you may not use this file except in compliance with the License.
 * * You may obtain a copy of the License at
 * * 
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 * * 
 *  * Unless required by applicable law or agreed to in writing, software
 * * distributed under the License is distributed on an "AS IS" BASIS,
 * * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * * See the License for the specific language governing permissions and
 * * limitations under the License.
 * * ============LICENSE_END====================================================
 * *
 * * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 * *
 ******************************************************************************/

package datarouter.provisioning;

import static org.junit.Assert.fail;

import java.util.Iterator;

import org.junit.Test;

import com.att.research.datarouter.provisioning.utils.RLEBitSet;

public class testRLEBitSet {
	@Test
	public void testBasicConstructor() {
		RLEBitSet bs = new RLEBitSet();
		if (!bs.isEmpty())
			fail("bit set not empty");
	}
	@Test
	public void testStringConstructor() {
		RLEBitSet bs = new RLEBitSet("1-10");
		if (bs.isEmpty())
			fail("bit set is empty");
		if (!bs.toString().equals("1-10"))
			fail("bad value");
		bs = new RLEBitSet("69,70,71");
		if (bs.isEmpty())
			fail("bit set is empty");
		if (!bs.toString().equals("69-71"))
			fail("bad value");
		bs = new RLEBitSet("555 444    443  442");
		if (!bs.toString().equals("442-444,555"))
			fail("bad value");
	}
	@Test
	public void testLength() {
		RLEBitSet bs = new RLEBitSet();
		if (bs.length() != 0)
			fail("testLength fail "+bs + " " + bs.length());
		bs = new RLEBitSet("1-10");
		if (bs.length() != 11)
			fail("testLength fail "+bs + " " + bs.length());
		bs = new RLEBitSet("1-20,100000000-100000005");
		if (bs.length() != 100000006)
			fail("testLength fail "+bs + " " + bs.length());
	}
	@Test
	public void testGet() {
		RLEBitSet bs = new RLEBitSet("1-10");
		if (!bs.get(5))
			fail("get");
		if (bs.get(69))
			fail("get");
	}
	@Test
	public void testSetOneBit() {
		RLEBitSet bs = new RLEBitSet();
		for (int i = 12; i < 200; i++)
			bs.set(i);
		bs.set(690);
		for (int i = 305; i < 309; i++)
			bs.set(i);
		bs.set(304);
		if (!bs.toString().equals("12-199,304-308,690"))
			fail("testSetOneBit fail "+bs);
	}
	@Test
	public void testSetString() {
		RLEBitSet bs = new RLEBitSet();
		bs.set("1-100");
		if (!bs.toString().equals("1-100"))
			fail("testSetString fail "+bs);
	}
	@Test
	public void testSetRange() {
		RLEBitSet bs = new RLEBitSet();
		bs.set(50,60);
		if (!bs.toString().equals("50-59"))
			fail("testSetRange fail "+bs);
	}
	@Test
	public void testClearOneBit() {
		RLEBitSet bs = new RLEBitSet("1-10");
		bs.clear(5);
		if (!bs.toString().equals("1-4,6-10"))
			fail("testClearOneBit fail");
		bs = new RLEBitSet("1-10");
		bs.clear(11);
		if (!bs.toString().equals("1-10"))
			fail("testClearOneBit fail "+bs);
	}
	@Test
	public void testClearRangeLeft() {
		RLEBitSet bs = new RLEBitSet("100-200");
		bs.clear(40,50);
		if (!bs.toString().equals("100-200"))
			fail("testClearRangeLeft fail "+bs);
	}
	@Test
	public void testClearRangeRight() {
		RLEBitSet bs = new RLEBitSet("100-200");
		bs.clear(400,500);
		if (!bs.toString().equals("100-200"))
			fail("testClearRangeRight fail "+bs);
	}
	@Test
	public void testClearRangeMiddle() {
		RLEBitSet bs = new RLEBitSet("100-200");
		bs.clear(120,130);
		if (!bs.toString().equals("100-119,130-200"))
			fail("testClearRangeRight fail "+bs);
	}
	@Test
	public void testClearRangeIntersect() {
		RLEBitSet bs = new RLEBitSet("100-200");
		bs.clear(100,200);
		if (!bs.toString().equals("200"))
			fail("testClearRangeIntersect fail "+bs);
	}
	@Test
	public void testClearOverlapLeft() {
		RLEBitSet bs = new RLEBitSet("100-200");
		bs.clear(50,150);
		if (!bs.toString().equals("150-200"))
			fail("testClearOverlapLeft fail "+bs);
	}
	@Test
	public void testClearOverlapRight() {
		RLEBitSet bs = new RLEBitSet("100-200");
		bs.clear(150,250);
		if (!bs.toString().equals("100-149"))
			fail("testClearOverlapRight fail "+bs);
	}
	@Test
	public void testClearOverlapAll() {
		RLEBitSet bs = new RLEBitSet("100-200");
		bs.clear(50,250);
		if (!bs.toString().equals(""))
			fail("testClearOverlapAll fail "+bs);
	}
	@Test
	public void testAnd() {
		RLEBitSet bs = new RLEBitSet("100-200");
		RLEBitSet b2 = new RLEBitSet("150-400");
		bs.and(b2);
		if (!bs.toString().equals("150-200"))
			fail("testAnd fail "+bs);
		bs = new RLEBitSet("100-200");
		b2 = new RLEBitSet("1500-4000");
		bs.and(b2);
		if (!bs.isEmpty())
			fail("testAnd fail "+bs);
	}
	@Test
	public void testAndNot() {
		RLEBitSet bs = new RLEBitSet("100-200");
		RLEBitSet b2 = new RLEBitSet("150-159");
		bs.andNot(b2);
		if (!bs.toString().equals("100-149,160-200"))
			fail("testAndNot fail "+bs);
	}
	@Test
	public void testIsEmpty() {
		RLEBitSet bs = new RLEBitSet("");
		if (!bs.isEmpty())
			fail("testIsEmpty fail "+bs);
		bs.set(1);
		if (bs.isEmpty())
			fail("testIsEmpty fail "+bs);
	}
	@Test
	public void testCardinality() {
		RLEBitSet bs = new RLEBitSet("1-120,10000000-10000005");
		if (bs.cardinality() != 126)
			fail("testCardinality fail 1");
	}
	@Test
	public void testIterator() {
		RLEBitSet bs = new RLEBitSet("1,5,10-12");
		Iterator<Long[]> i = bs.getRangeIterator();
		if (!i.hasNext())
			fail("iterator fail 1");
		Long[] ll = i.next();
		if (ll == null || ll[0] != 1 || ll[1] != 1)
			fail("iterator fail 2");

		if (!i.hasNext())
			fail("iterator fail 3");
		ll = i.next();
		if (ll == null || ll[0] != 5 || ll[1] != 5)
			fail("iterator fail 4");

		if (!i.hasNext())
			fail("iterator fail 5");
		ll = i.next();
		if (ll == null || ll[0] != 10 || ll[1] != 12)
			fail("iterator fail 6");

		if (i.hasNext())
			fail("iterator fail 7");
	}
	@Test
	public void testClone() {
		RLEBitSet bs1 = new RLEBitSet("1,5,10-12");
		RLEBitSet bs2 = (RLEBitSet) bs1.clone();
		if (!bs1.toString().equals(bs2.toString()))
			fail("clone");
	}
}
