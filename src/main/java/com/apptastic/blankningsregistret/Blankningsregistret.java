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
import org.apache.poi.openxml4j.util.ZipSecureFile;

import javax.net.ssl.SSLContext;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
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
    private HttpClient httpClient;
    private DateTimeFormatter dateFormat;


    /**
     * Default constructor.
     */
    public Blankningsregistret() {
        ZipSecureFile.setMinInflateRatio(0.0070);
        try {
            SSLContext context = SSLContext.getInstance("TLSv1.3");
            context.init(null, null, null);

            httpClient = HttpClient.newBuilder()
                    .sslContext(context)
                    .connectTimeout(Duration.ofSeconds(15))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
        }

        dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }

    /**
     * Searches for published net search positions from today's date.
     *
     * If the positions has not been yet been published for today then try the previous day.
     * Maximum 30 day back from today's date if no position is found for that date.
     * @return stream of net short positions
     */
    public Stream<NetShortPosition> search() {
        var searchDate = LocalDate.now(ZoneId.of("Europe/Stockholm"));
        return search(searchDate, 30);
    }

    /**
     * Searches for published net search positions from the given date.
     *
     * If the positions has not yet been been published for the given date then try the previous day
     * up until the given max number of previous days.
     * @param date date to search from
     * @param maxPreviousDays search max previous days back from the given date if no position for the given date if found.
     * @return stream of net short positions
     *
     * @deprecated Use LocalDate class instead of Date class
     */
    @SuppressWarnings("squid:S1133")
    @Deprecated(since="2.1.0")
    public Stream<NetShortPosition> search(Date date, int maxPreviousDays) {
        LocalDate searchDate = LocalDate.ofInstant(date.toInstant(), ZoneId.of("Europe/Stockholm"));

        for (var i = 0; i < maxPreviousDays + 1; ++i) {
            try {
                var searchDateString = dateFormat.format(searchDate);
                return getStream(searchDateString);
            }
            catch (IOException e) {
                var logger = Logger.getLogger(Blankningsregistret.class.getName());

                if (logger.isLoggable(Level.FINER))
                    logger.log(Level.FINER, "Failed to parse file. ", e);
            }

            searchDate = searchDate.minusDays(1);
        }

        return Stream.empty();
    }

    public Stream<NetShortPosition> search(LocalDate searchDate, int maxPreviousDays) {
        for (var i = 0; i < maxPreviousDays + 1; ++i) {
            try {
                var searchDateString = dateFormat.format(searchDate);
                return getStream(searchDateString);
            }
            catch (IOException e) {
                var logger = Logger.getLogger(Blankningsregistret.class.getName());

                if (logger.isLoggable(Level.FINER))
                    logger.log(Level.FINER, "Failed to parse file. ", e);
            }

            searchDate = searchDate.minusDays(1);
        }

        return Stream.empty();
    }

    @SuppressWarnings("squid:S1181")
    private Stream<NetShortPosition> getStream(String searchDateString) throws IOException {
        var urlHistorical = String.format(URL_HISTORICAL_FORMAT, searchDateString);
        var resultHistorical = sendAsyncRequest(urlHistorical).thenApply(processResponse());

        var urlActive = String.format(URL_ACTIVE_FORMAT, searchDateString);
        var resultActive = sendAsyncRequest(urlActive).thenApply(processResponse());

        try {
            return Stream.concat(resultActive.join(), resultHistorical.join())
                         .sorted(Comparator.comparing(NetShortPosition::getPositionDate).reversed());
        } catch (CompletionException e) {
            try {
                throw e.getCause();
            } catch (IOException e2) {
                throw e2;
            } catch(Throwable e2) {
                throw new AssertionError(e2);
            }
        }
    }

    private Stream<NetShortPosition> parsResponse(InputStream is) {
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

        LocalDate epoch = LocalDate.of(1899, 11, 30);
        epoch = epoch.plusDays(Integer.parseInt(date));

        return dateFormat.format(epoch);
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
            Logger logger = Logger.getLogger(Blankningsregistret.class.getName());

            if (logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING, "Failed to parse net short position. ", e);

            return null;
        }

        return new NetShortPosition(row[INDEX_POSITION_HOLDER].trim(), row[INDEX_ISSUER].trim(), row[INDEX_ISIN].trim(), position, positionDate, comment);
    }

    private double toPosition(String text) {
        text = text.trim();

        if (text.charAt(0) == '<')
            text = text.substring(1);

        return Double.parseDouble(text.replace(',', '.'));
    }

    private CompletableFuture<HttpResponse<InputStream>> sendAsyncRequest(String url) {
        var request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept-Encoding", "gzip")
                .header("User-Agent", HTTP_USER_AGENT)
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream());
    }

    private Function<HttpResponse<InputStream>, Stream<NetShortPosition>> processResponse() {
        return response -> {
            try {
                if (response.statusCode() == 404)
                    throw new IOException("404 - Not Found");

                var inputStream = response.body();

                if (Optional.of("gzip").equals(response.headers().firstValue("Content-Encoding")))
                    inputStream = new GZIPInputStream(inputStream);

                inputStream = new BufferedInputStream(inputStream);
                return parsResponse(inputStream);

            } catch (IOException e) {
                throw new CompletionException(e);
            }
        };
    }
}
