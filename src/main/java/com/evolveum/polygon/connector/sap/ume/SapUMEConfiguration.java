/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.polygon.connector.sap.ume;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.spi.ConfigurationProperty;

import java.util.Arrays;

import static org.identityconnectors.common.StringUtil.isBlank;

public class SapUMEConfiguration extends AbstractConfiguration {

    private static final Log LOG = Log.getLog(SapUMEConfiguration.class);

    // logon credentials
    private String URL;
    private String user;
    private GuardedString password;

    // log debug params
    private Boolean logSPMLRequest = Boolean.FALSE;
    private Boolean logSPMLResponse = Boolean.FALSE;

    //string for full search
    private String fullSearchStringPattern = "abcdefghijklmnopqrstuvwxyz1234567890";

    // ume.admin.addattrs parameter in sap
    // https://archive.sap.com/kmuuid2/50018d40-d370-2910-3ba7-aa83038d257a/How%20to%20Extend%20User%20Details%C2%A0%20(NW7.0).pdf
    private String[] umeAddAttrs;

    // dummy password for password change
    private GuardedString dummyPassword = new GuardedString("5ecretDummyPWD".toCharArray());

    // set initial password
    private Boolean initialPasswordAfterUpdate = Boolean.FALSE;
    private Boolean initialPasswordAfterCreate = Boolean.FALSE;

    // Sets a specified timeout value, in milliseconds, to be used when opening a communications link to the resource referenced by this URLConnection. If the timeout expires before the connection can be established, a java.net.SocketTimeoutException is raised.
    // A timeout of zero is interpreted as an infinite timeout.
    private Integer connectTimeout = Integer.valueOf(0);
    // Sets the read timeout to a specified timeout, in milliseconds. A non-zero value specifies the timeout when reading from Input stream when a connection is established to a resource. If the timeout expires before there is data available for read, a java.net.SocketTimeoutException is raised.
    // A timeout of zero is interpreted as an infinite timeout.
    private Integer readTimeout = Integer.valueOf(0);

    private String enableValidFromTime = "";
    private String enableValidToTime = "9999-12-31 00:00:00";
    private String disableValidFromTime = "";
    private String disableValidToTime = SapUMEDateHelper.CURRENT_VALID_TIME;

    @Override
    public void validate() {
        LOG.info("Validate start");
        if (isBlank(URL)) {
            throw new ConfigurationException("URL is empty");
        } else if (isBlank(user)) {
            throw new ConfigurationException("User is empty");
        } else if (password == null || password.equals(new GuardedString("".toCharArray()))) {
            throw new ConfigurationException("Password is empty");
        } else if (isBlank(fullSearchStringPattern)) {
            throw new ConfigurationException("LogonNameInitialSubstring is empty");
        } else if (dummyPassword == null || dummyPassword.equals(new GuardedString("".toCharArray()))) {
            throw new ConfigurationException("DummyPassword is empty");
        }
        LOG.info("Validate finished");
    }

    @ConfigurationProperty(required = true, order = 1, displayMessageKey = "sap.ume.config.url", helpMessageKey = "sap.ume.config.url.help")
    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    @ConfigurationProperty(required = true, order = 2, displayMessageKey = "sap.ume.config.user", helpMessageKey = "sap.ume.config.user.help")
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @ConfigurationProperty(required = true, order = 3, displayMessageKey = "sap.ume.config.password", helpMessageKey = "sap.ume.config.password.help")
    public GuardedString getPassword() {
        return password;
    }

    public void setPassword(GuardedString password) {
        this.password = password;
    }

    @ConfigurationProperty(order = 9, displayMessageKey = "sap.ume.config.logSPMLRequest", helpMessageKey = "sap.ume.config.logSPMLRequest.help")
    public Boolean getLogSPMLRequest() {
        return this.logSPMLRequest;
    }

    public void setLogSPMLRequest(Boolean logSPMLRequest) {
        this.logSPMLRequest = logSPMLRequest;
    }

    @ConfigurationProperty(order = 10, displayMessageKey = "sap.ume.config.logSPMLResponse", helpMessageKey = "sap.ume.config.logSPMLResponse.help")
    public Boolean getLogSPMLResponse() {
        return logSPMLResponse;
    }

    public void setLogSPMLResponse(Boolean logSPMLResponse) {
        this.logSPMLResponse = logSPMLResponse;
    }

    @ConfigurationProperty(order = 5, displayMessageKey = "sap.ume.config.fullSearchStringPattern", helpMessageKey = "sap.ume.config.fullSearchStringPattern.help")
    public String getFullSearchStringPattern() {
        return this.fullSearchStringPattern;
    }

    public void setFullSearchStringPattern(String fullSearchStringPattern) {
        this.fullSearchStringPattern = fullSearchStringPattern;
    }

    @ConfigurationProperty(order = 4, displayMessageKey = "sap.ume.config.umeAddAttrs", helpMessageKey = "sap.ume.config.umeAddAttrs.help")
    public String[] getUmeAddAttrs() {
        return umeAddAttrs;
    }

    public void setUmeAddAttrs(String[] umeAddAttrs) {
        this.umeAddAttrs = umeAddAttrs;
    }

    @ConfigurationProperty(order = 6, displayMessageKey = "sap.ume.config.dummyPassword", helpMessageKey = "sap.ume.config.dummyPassword.help")
    public GuardedString getDummyPassword() {
        return dummyPassword;
    }

    public void setDummyPassword(GuardedString dummyPassword) {
        this.dummyPassword = dummyPassword;
    }

    @ConfigurationProperty(order = 8, displayMessageKey = "sap.ume.config.initialPasswordAfterUpdate", helpMessageKey = "sap.ume.config.initialPasswordAfterUpdate.help")
    public Boolean getInitialPasswordAfterUpdate() {
        return initialPasswordAfterUpdate;
    }

    public void setInitialPasswordAfterUpdate(Boolean initialPasswordAfterUpdate) {
        this.initialPasswordAfterUpdate = initialPasswordAfterUpdate;
    }

    @ConfigurationProperty(order = 7, displayMessageKey = "sap.ume.config.initialPasswordAfterCreate", helpMessageKey = "sap.ume.config.initialPasswordAfterCreate.help")
    public Boolean getInitialPasswordAfterCreate() {
        return initialPasswordAfterCreate;
    }

    public void setInitialPasswordAfterCreate(Boolean initialPasswordAfterCreate) {
        this.initialPasswordAfterCreate = initialPasswordAfterCreate;
    }

    @ConfigurationProperty(order = 11, displayMessageKey = "sap.ume.config.connectTimeout", helpMessageKey = "sap.ume.config.connectTimeout.help")
    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    @ConfigurationProperty(order = 12, displayMessageKey = "sap.ume.config.readTimeout", helpMessageKey = "sap.ume.config.readTimeout.help")
    public Integer getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
    }

    @ConfigurationProperty(order = 13, displayMessageKey = "sap.ume.config.enable.validFromTime", helpMessageKey = "sap.ume.config.enable.validFromTime.help")
    public String getEnableValidFromTime() {
        return enableValidFromTime;
    }

    public void setEnableValidFromTime(String enableValidFromTime) {
        this.enableValidFromTime = enableValidFromTime;
    }

    @ConfigurationProperty(order = 14, displayMessageKey = "sap.ume.config.enable.validToTime", helpMessageKey = "sap.ume.config.enable.validToTime.help")
    public String getEnableValidToTime() {
        return enableValidToTime;
    }

    public void setEnableValidToTime(String enableValidToTime) {
        this.enableValidToTime = enableValidToTime;
    }

    @ConfigurationProperty(order = 15, displayMessageKey = "sap.ume.config.disable.validFromTime", helpMessageKey = "sap.ume.config.disable.validFromTime.help")
    public String getDisableValidFromTime() {
        return disableValidFromTime;
    }

    public void setDisableValidFromTime(String disableValidFromTime) {
        this.disableValidFromTime = disableValidFromTime;
    }

    @ConfigurationProperty(order = 16, displayMessageKey = "sap.ume.config.disable.validToTime", helpMessageKey = "sap.ume.config.disable.validToTime.help")
    public String getDisableValidToTime() {
        return disableValidToTime;
    }

    public void setDisableValidToTime(String disableValidToTime) {
        this.disableValidToTime = disableValidToTime;
    }

    private String printPassword(GuardedString pwd) {
        if (pwd == null) {
            return "<null>";
        } else if (pwd.equals(new GuardedString("".toCharArray()))) {
            return "<empty>";
        } else {
            return "********";
        }
    }

    @Override
    public String toString() {
        return "SapUMEConfiguration{" +
                "URL='" + URL + '\'' +
                ", user='" + user + '\'' +
                ", password='" + printPassword(password) + '\'' +
                ", logSPMLRequest='" + logSPMLRequest + '\'' +
                ", logSPMLResponse='" + logSPMLResponse + '\'' +
                ", fullSearchStringPattern='" + fullSearchStringPattern + '\'' +
                ", umeAddAttrs='" + Arrays.deepToString(umeAddAttrs) + '\'' +
                ", dummyPassword='" + printPassword(dummyPassword) + '\'' +
                ", initialPasswordAfterUpdate='" + initialPasswordAfterUpdate + '\'' +
                ", initialPasswordAfterCreate='" + initialPasswordAfterCreate + '\'' +
                ", connectTimeout='" + connectTimeout + '\'' +
                ", readTimeout='" + readTimeout + '\'' +
                ", enableValidFromTime='" + enableValidFromTime + '\'' +
                ", enableValidToTime='" + enableValidToTime + '\'' +
                ", disableValidFromTime='" + disableValidFromTime + '\'' +
                ", disableValidToTime='" + disableValidToTime + '\'' +
                '}';
    }
}