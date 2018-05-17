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

import com.apptastic.blankningsregistret.internal.ExcelFileReader;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;


public class Blankningsregistret {
    private static final String URL_FORMAT = "https://www.fi.se/contentassets/71a61417bb4c49c0a4a3a2582ea8af6c/korta_positioner_%1$s.xlsx";
    private static final int INDEX_PUBLICATION_DATE = 0;
    private static final int INDEX_POSITION_HOLDER = 1;
    private static final int INDEX_ISSUER = 2;
    private static final int INDEX_ISIN = 3;
    private static final int INDEX_POSITION = 4;
    private static final int INDEX_POSITION_DATE = 5;
    private static final int INDEX_COMMENT = 6;
    private SimpleDateFormat dateFormat;


    public Blankningsregistret() {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    }

    public Stream<NetShortPosition> search() {
        Calendar searchDate = Calendar.getInstance(TimeZone.getTimeZone("Europe/Stockholm"));

        return search(searchDate.getTime(), 30);
    }

    public Stream<NetShortPosition> search(Date date, int maxPreviousDays) {
        Calendar searchDate = Calendar.getInstance(TimeZone.getTimeZone("Europe/Stockholm"));
        searchDate.setTime(date);

        for (int i = 0; i < maxPreviousDays + 1; ++i) {
            try {
                String searchDateString = dateFormat.format(searchDate.getTime());
                String url = String.format(URL_FORMAT, searchDateString);
                InputStream is = sendRequest(url);

                if (is != null) {
                    ExcelFileReader reader = new ExcelFileReader(is);
                    Iterator<String[]> rowIterator = reader.getIterator();
                    Stream<String[]> targetStream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(rowIterator, Spliterator.ORDERED), false);

                    return targetStream.filter(r -> r.length == 6 || r.length == 7)
                            .map(this::createNetShortPosition)
                            .filter(Objects::nonNull);
                }
            }
            catch (IOException e) {
                Logger logger = Logger.getLogger("com.apptastic.blankningsregistret");

                if (logger.isLoggable(Level.FINER))
                    logger.log(Level.FINER, "Failed to parse file. ", e);
            }

            searchDate.add(Calendar.DAY_OF_YEAR, -1);
        }

        return Stream.empty();
    }


    private String toDate(String date) {
        if (date.length() == 10)
            return date;

        Calendar epoch = new GregorianCalendar(1899,11,30);
        epoch.add(Calendar.DAY_OF_YEAR, Integer.valueOf(date));

        return dateFormat.format(epoch.getTime());
    }


    private NetShortPosition createNetShortPosition(String[] row) {
        String comment = (row.length == 7) ? row[INDEX_COMMENT].trim() : "";
        double position;
        String publicationDate;
        String positionDate;

        try {
            publicationDate = toDate(row[INDEX_PUBLICATION_DATE].trim());
            positionDate = toDate(row[INDEX_POSITION_DATE].trim());
            position = Double.parseDouble(row[INDEX_POSITION].replace(',', '.').trim());
        }
        catch (NumberFormatException e) {
            Logger logger = Logger.getLogger("com.apptastic.blankningsregistret");

            if (logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING, "Failed to parse net short position. ", e);

            return null;
        }

        return new NetShortPosition(publicationDate, row[INDEX_POSITION_HOLDER].trim(), row[INDEX_ISSUER].trim(), row[INDEX_ISIN].trim(), position, positionDate, comment);
    }


    /**
     * Internal method for sending the http request.
     * @param url URL to send the request
     * @return The response for the request
     * @throws IOException exception
     */
    protected InputStream sendRequest(String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("Accept-Encoding", "gzip");
        InputStream inputStream = connection.getInputStream();

        if ("gzip".equals(connection.getContentEncoding()))
            inputStream = new GZIPInputStream(inputStream);

        return inputStream;
    }
}
