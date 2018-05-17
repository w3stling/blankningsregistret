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

public class NetShortPosition implements Comparable<NetShortPosition> {
    private String publicationDate;
    private String positionHolder;
    private String issuer;
    private String isin;
    private double position;
    private String positionDate;
    private String comment;

    public NetShortPosition() {

    }

    public NetShortPosition(String publicationDate, String positionHolder, String issuer, String isin,
                            double position, String positionDate, String comment) {

        this.publicationDate = publicationDate;
        this.positionHolder = positionHolder;
        this.issuer = issuer;
        this.isin = isin;
        this.position = position;
        this.positionDate = positionDate;
        this.comment = comment;
    }

    public String getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(String publicationDate) {
        this.publicationDate = publicationDate;
    }

    public String getPositionHolder() {
        return positionHolder;
    }

    public void setPositionHolder(String positionHolder) {
        this.positionHolder = positionHolder;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getIsin() {
        return isin;
    }

    public void setIsin(String isin) {
        this.isin = isin;
    }

    public double getPosition() {
        return position;
    }

    public void setPosition(double position) {
        this.position = position;
    }

    public String getPositionDate() {
        return positionDate;
    }

    public void setPositionDate(String positionDate) {
        this.positionDate = positionDate;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof NetShortPosition)) return false;

        NetShortPosition that = (NetShortPosition) o;
        return Double.compare(that.getPosition(), getPosition()) == 0 &&
                Objects.equals(getPublicationDate(), that.getPublicationDate()) &&
                Objects.equals(getPositionHolder(), that.getPositionHolder()) &&
                Objects.equals(getIssuer(), that.getIssuer()) &&
                Objects.equals(getIsin(), that.getIsin()) &&
                Objects.equals(getPositionDate(), that.getPositionDate()) &&
                Objects.equals(getComment(), that.getComment());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPublicationDate(), getPositionHolder(), getIssuer(), getIsin(), getPosition(),
                getPositionDate(), getComment());
    }

    @Override
    public int compareTo(NetShortPosition o) {
        return positionDate.compareTo(o.positionDate);
    }
}
