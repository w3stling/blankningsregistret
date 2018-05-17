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

import java.util.List;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;


public class BlankningsregistretTest {

    @Test
    public void search() {
        Blankningsregistret br = new Blankningsregistret();

        List<NetShortPosition> positions = br.search().collect(toList());

        assertTrue(positions.size() > 2000);

        Pattern datePattern = Pattern.compile("20\\d{2}-\\d{2}-\\d{2}");

        for (NetShortPosition position : positions) {
            assertEquals(10, position.getPublicationDate().length());
            assertTrue(datePattern.matcher(position.getPublicationDate()).find());
            assertTrue(position.getPositionHolder().length() > 3);
            assertTrue(position.getIssuer().length() > 3);
            assertEquals(12, position.getIsin().length());
            assertTrue(position.getPosition() >= 0.0);
            assertEquals(10, position.getPositionDate().length());
            assertTrue(datePattern.matcher(position.getPositionDate()).find());
            assertTrue(position.getComment().isEmpty() || position.getComment().length() > 3);
        }
    }
}
