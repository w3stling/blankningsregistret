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

import java.util.Objects;
import java.util.Optional;


/**
 * Class represent the net short position.
 */
public class NetShortPosition implements Comparable<NetShortPosition> {
    private String positionHolder;
    private String issuer;
    private String isin;
    private double positionInPercent;
    private String positionDate;
    private String comment;

    /**
     * Default constructor
     */
    public NetShortPosition() {

    }

    /**
     * Constructor
     *
     * @param positionHolder position holder name
     * @param issuer issuer name
     * @param isin ISIN number
     * @param positionInPercent position in percent
     * @param positionDate position date
     * @param comment comment
     */
    public NetShortPosition(String positionHolder, String issuer, String isin, double positionInPercent,
                            String positionDate, String comment) {

        this.positionHolder = positionHolder;
        this.issuer = issuer;
        this.isin = isin;
        this.positionInPercent = positionInPercent;
        this.positionDate = positionDate;
        this.comment = comment;
    }

    /**
     * Copy constructor
     *
     * @param o net short position
     */
    public NetShortPosition(NetShortPosition o) {
        this.positionHolder = o.positionHolder;
        this.issuer = o.issuer;
        this.isin = o.isin;
        this.positionInPercent = o.positionInPercent;
        this.positionDate = o.positionDate;
        this.comment = o.comment;
    }

    /**
     * Name of the position holder. For example Goldman Sachs International.
     * @return position holder name
     */
    public String getPositionHolder() {
        return positionHolder;
    }

    /**
     * Name of the position holder. For example Goldman Sachs International.
     * @param positionHolder position holder name
     */
    public void setPositionHolder(String positionHolder) {
        this.positionHolder = positionHolder;
    }

    /**
     * Get issuer for this position. For example Alfa Laval AB.
     * @return issuer
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * Set issuer for this position. For example Alfa Laval AB.
     * @param issuer issuer
     */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    /**
     * Get ISIN for this position. For example SE0000695876.
     * @return ISIN number
     */
    public String getIsin() {
        return isin;
    }

    /**
     * Set ISIN for this position. For example SE0000695876.
     * @param isin ISIN number
     */
    public void setIsin(String isin) {
        this.isin = isin;
    }

    /**
     * Get the net position in percent.
     * @return position in percent
     */
    public double getPositionInPercent() {
        return positionInPercent;
    }

    /**
     * Set the net position in percent.
     * @param positionInPercent position in percent
     */
    public void setPositionInPercent(double positionInPercent) {
        this.positionInPercent = positionInPercent;
    }

    /**
     * Get date for this position in format YYYY-DD-MM.
     * @return date
     */
    public String getPositionDate() {
        return positionDate;
    }

    /**
     * Set date for this position in format YYYY-DD-MM.
     * @param positionDate date
     */
    public void setPositionDate(String positionDate) {
        this.positionDate = positionDate;
    }

    /**
     * Get comment for this net short position.
     * @return comment
     */
    public Optional<String> getComment() {
        return Optional.ofNullable(comment);
    }

    /**
     * Set comment for this net short position.
     * @param comment comment
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof NetShortPosition)) return false;

        NetShortPosition that = (NetShortPosition) o;
        return Double.compare(that.getPositionInPercent(), getPositionInPercent()) == 0 &&
                Objects.equals(getPositionHolder(), that.getPositionHolder()) &&
                Objects.equals(getIssuer(), that.getIssuer()) &&
                Objects.equals(getIsin(), that.getIsin()) &&
                Objects.equals(getPositionDate(), that.getPositionDate()) &&
                Objects.equals(getComment(), that.getComment());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPositionHolder(), getIssuer(), getIsin(), getPositionInPercent(),
                getPositionDate(), getComment());
    }

    @Override
    public int compareTo(NetShortPosition o) {
        return positionDate.compareTo(o.positionDate);
    }
}
