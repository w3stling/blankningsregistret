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

import com.github.miachm.sods.Range;
import com.github.miachm.sods.Sheet;
import com.github.miachm.sods.SpreadSheet;

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
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
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
    private static final String URL_ACTIVE_FORMAT = "https://www.fi.se/sv/vara-register/blankningsregistret/GetAktuellFile/?_=%1$s";
    private static final String URL_HISTORICAL_FORMAT = "https://www.fi.se/sv/vara-register/blankningsregistret/GetHistFile/?_=%1$s";
    private static final String HTTP_USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11";
    private static final int INDEX_POSITION_HOLDER = 0;
    private static final int INDEX_ISSUER = 1;
    private static final int INDEX_ISIN = 2;
    private static final int INDEX_POSITION = 3;
    private static final int INDEX_POSITION_DATE = 4;
    private static final int INDEX_COMMENT = 5;
    private DateTimeFormatter dateFormat;


    /**
     * Default constructor.
     */
    public Blankningsregistret() {
        dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }

    /**
     * Search both active and historical net positions.
     *
     * @return stream of net short positions
     */
    public Stream<NetShortPosition> search() {
        try {
            return getStream(System.currentTimeMillis(), true, true);
        } catch (Exception e) {
            return Stream.empty();
        }
    }

    /**
     * Search active net positions.
     *
     * @return stream of net short positions
     */
    public Stream<NetShortPosition> searchActivePositions() {
        try {
            return getStream(System.currentTimeMillis(), true, false);
        } catch (Exception e) {
            return Stream.empty();
        }
    }

    /**
     * Search historical net positions.
     *
     * @return stream of net short positions
     */
    public Stream<NetShortPosition> searchHistoricalPositions() {
        try {
            return getStream(System.currentTimeMillis(), false, true);
        } catch (Exception e) {
            return Stream.empty();
        }
    }

    @SuppressWarnings("squid:S1181")
    private Stream<NetShortPosition> getStream(long timestamp, boolean activePositions, boolean historicalPositions) throws IOException {
        try {
            String searchDateString = String.valueOf(timestamp);
            var urlHistorical = String.format(URL_HISTORICAL_FORMAT, String.valueOf(searchDateString));
            var urlActive = String.format(URL_ACTIVE_FORMAT, searchDateString);

            if (!activePositions && historicalPositions) {
                var resultHistorical = sendAsyncRequest(urlHistorical).thenApply(processResponse());
                return resultHistorical.get(45, TimeUnit.SECONDS).sorted(Comparator.comparing(NetShortPosition::getPositionDate).reversed());
            } else if (activePositions && !historicalPositions) {
                var resultActive = sendAsyncRequest(urlActive).thenApply(processResponse());
                return resultActive.get(45, TimeUnit.SECONDS).sorted(Comparator.comparing(NetShortPosition::getPositionDate).reversed());
            }

            var resultHistorical = sendAsyncRequest(urlHistorical).thenApply(processResponse());
            var resultActive = sendAsyncRequest(urlActive).thenApply(processResponse());
            return Stream.concat(resultActive.get(45, TimeUnit.SECONDS), resultHistorical.get(45, TimeUnit.SECONDS))
                         .sorted(Comparator.comparing(NetShortPosition::getPositionDate).reversed());
        } catch (CompletionException e) {
            try {
                throw e.getCause();
            } catch (IOException e2) {
                throw e2;
            } catch(Throwable e2) {
                throw new AssertionError(e2);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IOException(e);
        }
    }

    private Stream<NetShortPosition> parsResponse(InputStream is) {
        if (is != null) {
            try {
                var spread = new SpreadSheet(is);
                var sheet = spread.getSheet(0);
                Iterator<NetShortPosition> rowIterator = new SheetRowIterator(sheet);
                return StreamSupport.stream(Spliterators.spliteratorUnknownSize(rowIterator, Spliterator.ORDERED), false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return Stream.empty();
    }

    private LocalDate toLocalDate(String date) {
        if (date.length() == 10)
            return LocalDate.parse(date, dateFormat);

        LocalDate epoch = LocalDate.of(1899, 12, 30);
        epoch = epoch.plusDays(Integer.parseInt(date));

        return epoch;
    }

    private NetShortPosition createNetShortPosition(String[] row) {
        if (row[0].length() == 43 && row[0].startsWith("Innehavare"))
            return null;

        var comment = (row.length == 6) ? row[INDEX_COMMENT].trim() : null;
        comment = (comment == null || comment.isEmpty()) ? null : comment;
        double position;
        LocalDate positionDate;

        try {
            positionDate = toLocalDate(row[INDEX_POSITION_DATE].trim());
            position = toPosition(row[INDEX_POSITION]);
        }
        catch (NumberFormatException e) {
            Logger logger = Logger.getLogger(Blankningsregistret.class.getName());

            if (logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING, "Failed to parse net short position. ", e);

            return null;
        }

        String positionHolder = toText(row[INDEX_POSITION_HOLDER]);
        String issuer = toText(row[INDEX_ISSUER]);
        String isin = toText(row[INDEX_ISIN]);
        comment = toText(comment);
        boolean isSignificantPosition = checkSignificantPosition(row[INDEX_POSITION]);

        return new NetShortPosition(positionHolder, issuer, isin, position, positionDate, comment, isSignificantPosition);
    }

    private static String toText(String text) {
        if (text == null) {
            return null;
        }

        if (text.length() > 0 && text.charAt(text.length() -1) == 160) {
            text = text.substring(0, text.length()-1);
        }

        return text.trim();
    }

    private boolean checkSignificantPosition(String text) {
        return text.trim().charAt(0) != '<';
    }

    private double toPosition(String text) {
        text = text.trim();

        if (text.charAt(0) == '<')
            text = text.substring(1);

        return Double.parseDouble(text.replace(',', '.'));
    }

    private CompletableFuture<HttpResponse<InputStream>> sendAsyncRequest(String url) {
        var request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Accept-Encoding", "gzip")
                .header("User-Agent", HTTP_USER_AGENT)
                .GET()
                .build();

        var httpClient = createHttpClient();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream());
    }

    private Function<HttpResponse<InputStream>, Stream<NetShortPosition>> processResponse() {
        return response -> {
            try {
                if (response.statusCode() >= 400 && response.statusCode() < 600) {
                    throw new IOException("Response http status code: " + response.statusCode());
                }

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

    private HttpClient createHttpClient() {
        HttpClient httpClient;

        try {
            SSLContext context = SSLContext.getInstance("TLSv1.3");
            context.init(null, null, null);

            httpClient = HttpClient.newBuilder()
                    .sslContext(context)
                    .connectTimeout(Duration.ofSeconds(20))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(20))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
        }

        return httpClient;
    }

    class SheetRowIterator implements Iterator<NetShortPosition> {
        private Range range;
        private int numberOfRows;
        private int row;
        private boolean skippRows;
        private NetShortPosition next;

        SheetRowIterator(Sheet sheet) {
            this.range = sheet.getDataRange();
            numberOfRows = range.getLastRow();
            row = 0;
            skippRows = true;
        }

        @Override
        public boolean hasNext() {
            if (skippRows) {
                findHeader();
                skippRows = false;
            }

            do {
                next = getNextNetShortPosition();
                ++row;
            } while (row < numberOfRows && next == null);

            return next != null;
        }

        @Override
        public NetShortPosition next() {
           return next;
        }

        private NetShortPosition getNextNetShortPosition() {
            String positionHolder = getString(range, row, 0);
            String issuer = getString(range, row, 1);
            String isin = getString(range, row, 2);
            String positionText = getString(range, row, 3);
            String positionDateText = getString(range, row, 4);
            String comment = getString(range, row, 5);

            if (positionDateText == null || isin == null || isin.length() != 12) {
                return null;
            }

            LocalDate positionDate;
            double position;
            try {
                positionDate = toLocalDate(positionDateText);
                position = toPosition(positionText);
            }
            catch (NumberFormatException e) {
                Logger logger = Logger.getLogger(Blankningsregistret.class.getName());

                if (logger.isLoggable(Level.WARNING))
                    logger.log(Level.WARNING, "Failed to parse net short position. ", e);

                return null;
            }

            boolean isSignificantPosition = checkSignificantPosition(positionText);
            return new NetShortPosition(positionHolder, issuer, isin, position, positionDate, comment, isSignificantPosition);
        }

        private boolean findHeader() {
            while (row < numberOfRows) {
                String positionHolder = getString(range, row, 0);
                String issuer = getString(range, row, 1);
                String isin = getString(range, row, 2);
                String position = getString(range, row, 3);
                String positionDate = getString(range, row, 4);
                ++row;

                if (positionHolder != null && positionHolder.startsWith("Innehavare av positionen") &&
                    issuer != null && issuer.startsWith("Namn på emittent") &&
                    "ISIN".equalsIgnoreCase(isin) &&
                    position != null && position.startsWith("Position i procent") &&
                    positionDate != null && positionDate.startsWith("Datum för positionen")) {

                    return true;
                }
            }

            return false;
        }

        String getString(Range range, int row, int column) {
            Range r = range.getCell(row, column);
            if (r == null) {
                return null;
            }
            Object value = r.getValue();
            if (value instanceof Double) {
                return value.toString();
            } else if (value instanceof LocalDateTime) {
                return ((LocalDateTime)value).toLocalDate().toString();
            }
            return toText((String) r.getValue());
        }
    }
}
