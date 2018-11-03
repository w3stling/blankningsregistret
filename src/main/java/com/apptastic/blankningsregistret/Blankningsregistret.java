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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;


/**
 * Class for searching published net short position in the short selling registry (swedish Blankningsregistret) publish by Financial Supervisory Authority (Finansinspektionen).
 */
public class Blankningsregistret {
    private static final String URL_ACTIVE_FORMAT = "https://www.fi.se/contentassets/71a61417bb4c49c0a4a3a2582ea8af6c/aktuella_positioner_%1$s.xlsx";
    private static final String URL_HISTORICAL_FORMAT = "https://www.fi.se/contentassets/71a61417bb4c49c0a4a3a2582ea8af6c/historiska_positioner_%1$s.xlsx";
    private static final String HTTP_USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11";
    private static final int INDEX_POSITION_HOLDER = 0;
    private static final int INDEX_ISSUER = 1;
    private static final int INDEX_ISIN = 2;
    private static final int INDEX_POSITION = 3;
    private static final int INDEX_POSITION_DATE = 4;
    private static final int INDEX_COMMENT = 5;
    private final HttpClient httpClient;
    private SimpleDateFormat dateFormat;


    /**
     * Default constructor.
     */
    public Blankningsregistret() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    }

    /**
     * Searches for published net search positions from today's date.
     *
     * If the positions has not been yet been published for today then try the previous day.
     * Maximum 30 day back from today's date.
     * @return stream of net short positions
     */
    public Stream<NetShortPosition> search() {
        var searchDate = Calendar.getInstance(TimeZone.getTimeZone("Europe/Stockholm"));
        return search(searchDate.getTime(), 30);
    }

    /**
     * Searches for published net search positions from the given date.
     *
     * If the positions has not yet been been published for the given date then try the previous day
     * up until the given max number of previous days.
     * @param date date to search from
     * @param maxPreviousDays search max previous days back from the given date
     * @return stream of net short positions
     */
    public Stream<NetShortPosition> search(Date date, int maxPreviousDays) {
        var searchDate = Calendar.getInstance(TimeZone.getTimeZone("Europe/Stockholm"));
        searchDate.setTime(date);

        for (var i = 0; i < maxPreviousDays + 1; ++i) {
            try {
                var searchDateString = dateFormat.format(searchDate.getTime());
                var active = getStream(URL_ACTIVE_FORMAT, searchDateString);
                var historical = getStream(URL_HISTORICAL_FORMAT, searchDateString);

                return Stream.concat(active, historical).sorted(Comparator.comparing(NetShortPosition::getPositionDate).reversed());
            }
            catch (IOException e) {
                var logger = Logger.getLogger("com.apptastic.blankningsregistret");

                if (logger.isLoggable(Level.FINER))
                    logger.log(Level.FINER, "Failed to parse file. ", e);
            }

            searchDate.add(Calendar.DAY_OF_YEAR, -1);
        }

        return Stream.empty();
    }

    private Stream<NetShortPosition> getStream(String urlFormat, String searchDateString) throws IOException {
        var url = String.format(urlFormat, searchDateString);
        var is = sendRequest(url);

        if (is != null) {
            var reader = new ExcelFileReader(is);
            var rowIterator = reader.getIterator();
            var targetStream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(rowIterator, Spliterator.ORDERED), false);

            return targetStream.filter(r -> r.length == 5 || r.length == 6)
                    .map(this::createNetShortPosition)
                    .filter(Objects::nonNull);
        }

        return Stream.empty();
    }

    private String toDate(String date) {
        if (date.length() == 10)
            return date;

        var epoch = new GregorianCalendar(1899,11,30);
        epoch.add(Calendar.DAY_OF_YEAR, Integer.valueOf(date));

        return dateFormat.format(epoch.getTime());
    }


    private NetShortPosition createNetShortPosition(String[] row) {
        if (row[0].length() == 43 && row[0].startsWith("Innehavare"))
            return null;

        var comment = (row.length == 6) ? row[INDEX_COMMENT].trim() : null;
        comment = (comment == null || comment.isEmpty()) ? null : comment;
        double position;
        String positionDate;

        try {
            positionDate = toDate(row[INDEX_POSITION_DATE].trim());
            position = toPosition(row[INDEX_POSITION]);
        }
        catch (NumberFormatException e) {
            Logger logger = Logger.getLogger("com.apptastic.blankningsregistret");

            if (logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING, "Failed to parse net short position. ", e);

            return null;
        }

        return new NetShortPosition(null, row[INDEX_POSITION_HOLDER].trim(), row[INDEX_ISSUER].trim(), row[INDEX_ISIN].trim(), position, positionDate, comment);
    }

    private double toPosition(String text) {
        text = text.trim();

        if (text.charAt(0) == '<')
            text = text.substring(1);

        return Double.parseDouble(text.replace(',', '.'));
    }

    /**
     * Internal method for sending the http request.
     * @param url URL to send the request
     * @return The response for the request
     * @throws IOException exception
     */
    protected InputStream sendRequest(String url) throws IOException {
        var req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept-Encoding", "gzip")
                .header("User-Agent", HTTP_USER_AGENT)
                .GET()
                .build();

        try {
            var resp = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());

            if (resp.statusCode() == 404)
                throw new IOException("404 - Not Found");

            var inputStream = resp.body();

            if (Optional.of("gzip").equals(resp.headers().firstValue("Content-Encoding")))
                inputStream = new GZIPInputStream(inputStream);

            return new BufferedInputStream(inputStream);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }
}
