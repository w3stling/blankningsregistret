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
package com.apptastic.blankningsregistret.internal;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.xml.sax.*;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.*;
import java.io.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


public class ExcelFileReader {

    private InputStream file;

    public ExcelFileReader(InputStream file) {
        this.file = file;
    }

    public Iterator<String[]> getIterator() {
        return new ExcelFileIterator(file);
    }

    static class ExcelFileIterator implements Iterator<String[]> {
        private static final String ROW_EVENT = "row";
        private static final String CELL_EVENT = "c";
        private SharedStringsTable sst;
        private XMLEventReader eventReader;
        private boolean nextIsString;
        private String lastContents;
        private List<String> cellCache = new LinkedList<>();
        private String[] nextRow;

        ExcelFileIterator(InputStream is) {
            nextRow = null;

            try {
                OPCPackage pkg = OPCPackage.open(is);
                XSSFReader reader = new XSSFReader(pkg);
                sst = reader.getSharedStringsTable();
                Iterator<InputStream> sheetIterator = reader.getSheetsData();
                InputStream sheetInputStream = sheetIterator.next();
                BufferedInputStream bisSheet = new BufferedInputStream(sheetInputStream);
                InputSource sheetSource = new InputSource(bisSheet);
                XMLInputFactory inputFactory = XMLInputFactory.newInstance();
                eventReader = inputFactory.createXMLEventReader(sheetSource.getByteStream());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public boolean hasNext() {
            if (nextRow == null)
                nextRow = next();

            return nextRow != null;
        }

        @Override
        public String[] next() {
            String[] row = null;

            if (nextRow != null) {
                row = nextRow;
                nextRow = null;
                return row;
            }

            while (eventReader != null && eventReader.hasNext()) {
                XMLEvent event;

                try {
                    event = eventReader.nextEvent();
                }
                catch (XMLStreamException e) {
                    return null;
                }

                if (event.isStartElement()) {
                    StartElement startElement = event.asStartElement();

                    // c => cell
                    if (startElement.getName().getLocalPart().equals(CELL_EVENT)) {
                        Attribute attribute = startElement.getAttributeByName(QName.valueOf("t"));
                        String cellType = (attribute != null) ? attribute.getValue() : "";
                        nextIsString = cellType != null && cellType.equals("s");
                    }
                    // row => row
                    else if (startElement.getName().getLocalPart().equals(ROW_EVENT)) {
                        cellCache.clear();
                    }

                    // Clear contents cache
                    lastContents = "";
                }
                else if (event.isEndElement()) {
                    EndElement endElement = event.asEndElement();

                    // Process the last contents as required.
                    // Do now, as characters() may be called more than once
                    if (nextIsString) {
                        int idx = Integer.parseInt(lastContents.trim());
                        lastContents = new XSSFRichTextString(sst.getEntryAt(idx)).toString();
                        nextIsString = false;
                    }

                    // v => contents of a cell
                    // Output after we've seen the string contents
                    if (endElement.getName().getLocalPart().equals("v"))
                        cellCache.add(lastContents);
                    else if (endElement.getName().getLocalPart().equals(ROW_EVENT)) {
                        row = cellCache.toArray(new String[cellCache.size()]);
                        break;
                    }

                }
                else if (event.isCharacters()) {
                    Characters character = event.asCharacters();
                    lastContents += character.getData();
                }
            }

            return row;
        }
    }
}