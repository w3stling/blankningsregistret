/*
 * MIT License
 *
 * Copyright (c) 2018, Apptastic Software
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.apptastic.blankningsregistret;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;


public class NetShortPositionTest {

    private NetShortPosition defaultNetShortPosition() {
        return new NetShortPosition("2018-05-11", "JPMorgan Asset Management (UK) Ltd", "RaySearch Laboratories AB",
                "SE0000135485", 0.43, "2018-05-09", "Fallit under 0,5%");
    }

    @Test
    public void testEquals() {
        NetShortPosition position1 = defaultNetShortPosition();

        assertTrue(position1.equals(position1));
        assertFalse(position1.equals("Position"));

        {
            NetShortPosition position2 = defaultNetShortPosition();
            assertEquals(position1, position2);
        }
        {
            NetShortPosition position2 = defaultNetShortPosition();
            position2.setPublicationDate("2018-05-12");
            assertNotEquals(position1, position2);
        }
        {
            NetShortPosition position2 = defaultNetShortPosition();
            position2.setPositionHolder("JPMorgan Asset Management");
            assertNotEquals(position1, position2);
        }
        {
            NetShortPosition position2 = defaultNetShortPosition();
            position2.setIssuer("RaySearch Laboratories");
            assertNotEquals(position1, position2);
        }
        {
            NetShortPosition position2 = defaultNetShortPosition();
            position2.setIsin("SE0000135486");
            assertNotEquals(position1, position2);
        }
        {
            NetShortPosition position2 = defaultNetShortPosition();
            position2.setPositionInPercent(1.23);
            assertNotEquals(position1, position2);
        }
        {
            NetShortPosition position2 = defaultNetShortPosition();
            position2.setPositionDate("2018-05-10");
            assertNotEquals(position1, position2);
        }
        {
            NetShortPosition position2 = defaultNetShortPosition();
            position2.setComment("");
            assertNotEquals(position1, position2);
        }
    }

    @Test
    public void testSort() {
        NetShortPosition position1 = defaultNetShortPosition();
        position1.setPositionDate("2018-05-10");

        NetShortPosition position2 = defaultNetShortPosition();
        position2.setPositionDate("2018-05-09");

        NetShortPosition position3 = defaultNetShortPosition();
        position3.setPositionDate("2018-05-11");

        List<NetShortPosition> positions = Arrays.asList(position1, position2, position3);
        Collections.sort(positions);

        assertEquals("2018-05-09", positions.get(0).getPositionDate());
        assertEquals("2018-05-10", positions.get(1).getPositionDate());
        assertEquals("2018-05-11", positions.get(2).getPositionDate());
    }

    @Test
    public void testHashMap() {
        Map<NetShortPosition, String> transactions = new HashMap<>();

        NetShortPosition position1 = defaultNetShortPosition();
        position1.setPositionDate("2018-05-09");
        transactions.put(position1, "Position1");

        NetShortPosition position2 = defaultNetShortPosition();
        position2.setPositionDate("2018-05-10");
        transactions.put(position2, "Position2");

        NetShortPosition position3 = defaultNetShortPosition();
        position3.setPositionDate("2018-05-11");
        transactions.put(position3, "Position3");

        assertEquals("Position1", transactions.get(position1));
        assertEquals("Position2", transactions.get(position2));
        assertEquals("Position3", transactions.get(position3));
    }

    @Test
    public void testTreeMap() {
        Map<NetShortPosition, String> transactions = new TreeMap<>();

        NetShortPosition position1 = defaultNetShortPosition();
        position1.setPositionDate("2018-05-09");
        transactions.put(position1, "Position1");

        NetShortPosition position2 = defaultNetShortPosition();
        position2.setPositionDate("2018-05-10");
        transactions.put(position2, "Position2");

        NetShortPosition position3 = defaultNetShortPosition();
        position3.setPositionDate("2018-05-11");
        transactions.put(position3, "Position3");

        assertEquals("Position1", transactions.get(position1));
        assertEquals("Position2", transactions.get(position2));
        assertEquals("Position3", transactions.get(position3));
    }

    @Test
    public void basic() {
        NetShortPosition position = new NetShortPosition();
        position.setPositionDate("2018-05-11");

        assertEquals("2018-05-11", position.getPositionDate());
    }
}
