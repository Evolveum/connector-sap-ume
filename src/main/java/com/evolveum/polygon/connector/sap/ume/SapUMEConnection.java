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

import com.evolveum.polygon.connector.sap.ume.operation.SapUMEAbstractOperation;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.SecurityUtil;
import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.openspml.message.*;
import org.openspml.util.SpmlException;

import java.io.*;
import java.net.*;
import java.util.Base64;
import java.util.Iterator;

import static org.identityconnectors.common.StringUtil.isBlank;

public class SapUMEConnection {

    private static final Log LOG = Log.getLog(SapUMEConnection.class);
    private static final String HIDDEN_PASSWORD = "********";

    private SapUMEConfiguration configuration;

    public SapUMEConnection(SapUMEConfiguration configuration) {
        this.configuration = configuration;
    }

    public void dispose() {
        LOG.info("Dispose start");
        this.configuration = null;
        LOG.info("Dispose finished");
    }

    public SpmlResponse connect(SpmlRequest spmlRequest, String logOperation) {
        LOG.info("Connect start");

        logRequest(spmlRequest, logOperation);
        String request = spmlRequest.toXml();
        String response = "";
        SpmlResponse spmlResponse = null;

        StringBuffer sb = new StringBuffer();
        sb.append("<?xml version='1.0' encoding='UTF-8'?> \n");
        sb.append("<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"> \n");
        sb.append("<SOAP-ENV:Header/> \n");
        sb.append("<SOAP-ENV:Body> \n");
        sb.append(request).append("</SOAP-ENV:Body> \n");
        sb.append("</SOAP-ENV:Envelope> \n");
        String sSOAPRequest = sb.toString();

        HttpURLConnection httpURLConnection = null;

        try {
            String ur1 = this.configuration.getURL();
            final String user = this.configuration.getUser();
            final GuardedString password = this.configuration.getPassword();

            if (isBlank(ur1)) {
                throw new InvalidCredentialException("URL must not be empty");
            }

            if (isBlank(user) || password == null || password.equals(new GuardedString("".toCharArray()))) {
                throw new InvalidCredentialException("User and Password must not be empty");
            }

            URL url = new URL(ur1);
            httpURLConnection = (HttpURLConnection) url.openConnection();

            String basicAuth = "Basic " + new String(Base64.getEncoder().encode((this.configuration.getUser() + ":" + SecurityUtil.decrypt(this.configuration.getPassword())).getBytes()));
            httpURLConnection.setRequestProperty ("Authorization", basicAuth);

            byte[] xmlBytes = sSOAPRequest.getBytes();
            httpURLConnection.setRequestProperty("Content-Length", String.valueOf(xmlBytes.length));
            httpURLConnection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
            httpURLConnection.setRequestProperty("SOAPAction", "POST");
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setDoInput(true);

            Integer connectTimeout = this.configuration.getConnectTimeout();
            Integer readTimeout = this.configuration.getReadTimeout();
            if (connectTimeout == null) {
                connectTimeout = Integer.valueOf(0);
            }
            if (readTimeout == null) {
                readTimeout = Integer.valueOf(0);
            }
            httpURLConnection.setConnectTimeout(connectTimeout);
            httpURLConnection.setReadTimeout(readTimeout);

            OutputStream out = httpURLConnection.getOutputStream();
            out.write(xmlBytes);
            out.close();
            out = null;

            BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            in = null;

            if (content.length() == 0) {
                response = null;
            } else {
                response = content.toString();
            }
            spmlResponse = SpmlResponse.parseResponse(response);

            httpURLConnection.disconnect();
            httpURLConnection = null;

            logResponse(spmlResponse, logOperation);

            if (spmlResponse.isFailure()) {
                LOG.error("SPML RESPONSE is failure: {0}", spmlResponse.getErrorMessage());
                spmlResponse.throwErrors();
                throw new ConnectorException(spmlResponse.getErrorMessage());
            }
        } catch (ProtocolException pException) {
            try {
                if (httpURLConnection != null) {
                    int responseCode = httpURLConnection.getResponseCode();
                    if (responseCode == 401) {
                        throw new InvalidCredentialException("Connection error " + responseCode + " (" + httpURLConnection.getResponseMessage() + ")", pException);
                    }
                }
            } catch (IOException iOException) {
                throw new ConnectorIOException(iOException);
            }
            throw new ConnectorIOException(pException);
        } catch (SocketTimeoutException stException) {
            throw new OperationTimeoutException(stException);
        } catch (IOException iOException) {
            throw new ConnectorIOException(iOException);
        } catch (SpmlException spmlException) {
            if (spmlException.getMessage().contains("already exists")) {
                throw new AlreadyExistsException(spmlException);
            } else if (spmlException.getMessage().contains("is not allowed")) {
                throw new PermissionDeniedException(spmlException);
            } else if (spmlException.getMessage().contains("doesn't exist")) {
                throw new UnknownUidException(spmlException);
            } throw new ConnectorException(spmlException);
        } catch (Exception exception) {
            throw new ConnectorException(exception);
        }

        LOG.info("Connect finished");
        return spmlResponse;
    }

    public void test() {
        LOG.info("Test start");
        SchemaRequest sr = new SchemaRequest();
        sr.setSchemaIdentifier(SapUMESchema.SCHEMA_ID);
        try {
            SpmlResponse response = connect(sr, SapUMEAbstractOperation.LOG_OPERATION_TEST);
            LOG.info("Backend ume schema: {0}", response.toXml());
            response = null;
        } catch (Exception exception) {
            LOG.error("Exception in connection : {0}", exception.getMessage());
            throw new ConnectionFailedException(exception);
        }
        sr = null;
        LOG.info("Test finished");
    }

    public void logRequest(SpmlRequest request, String logOperation) {
        SpmlRequest logRequest = null;
        String identifier = request.getIdentifierString();
        if (!isBlank(identifier)) {
            identifier = "id=" + identifier + ": ";
        } else {
            identifier = "";
        }

        if (configuration.getLogSPMLRequest()) {
            if (request instanceof ModifyRequest) {
                logRequest = removePasswordElementFromModifyRequest((ModifyRequest) request);
            } else if (request instanceof AddRequest) {
                logRequest = removePasswordElementFromAddRequest((AddRequest) request);
            } else {
                logRequest = request;
            }
            String log = null;
            if (logRequest != null) {
                log = logRequest.toXml();
            }
            LOG.info("SPML REQUEST: operation={0}: {1}{2}", logOperation, identifier, log);
        }
    }

    public void logResponse(SpmlResponse response, String logOperation) {
        if (configuration.getLogSPMLResponse()) {
            String log = null;
            if (response != null) {
                log = response.toXml();
            }
            LOG.info("SPML RESPONSE: operation={0}: {1}", logOperation, log);
        }
    }

    private static ModifyRequest removePasswordElementFromModifyRequest(ModifyRequest modifyRequest) {
        ModifyRequest newModifyReq = new ModifyRequest();
        if (!(modifyRequest == null || modifyRequest.getModifications() == null)) {
            Iterator<Modification> modyfs = modifyRequest.getModifications().iterator();
            while (modyfs.hasNext()) {
                Modification modyf = modyfs.next();
                String modyfName = modyf.getName();
                Object modyfValue = modyf.getValue();
                String modyfOperation = modyf.getOperation();
                if ((modyfName.equals(SapUMESchema.ATTRIBUTE_PASSWORD) || modyfName.equals(SapUMESchema.ATTRIBUTE_OLDPASSWORD) || (modyfName.equals(OperationalAttributes.PASSWORD_NAME)))) {
                    modyfValue = HIDDEN_PASSWORD;
                }
                Modification newModyf = new Modification(modyfName, modyfValue);
                newModyf.setOperation(modyfOperation);
                newModifyReq.addModification(newModyf);
            }
            return newModifyReq;
        } else {
            return modifyRequest;
        }
    }

    private static AddRequest removePasswordElementFromAddRequest(AddRequest addRequest) {
        AddRequest newAddReq = new AddRequest();
        if (!(addRequest == null || addRequest.getAttributes() == null)) {
            Iterator adds = addRequest.getAttributes().iterator();
            while (adds.hasNext()) {
                org.openspml.message.Attribute add = (org.openspml.message.Attribute) adds.next();
                String addName = add.getName();
                Object addValue = add.getValue();
                if ((addName.equals(SapUMESchema.ATTRIBUTE_PASSWORD) || addName.equals(SapUMESchema.ATTRIBUTE_OLDPASSWORD) || (addName.equals(OperationalAttributes.PASSWORD_NAME)))) {
                    addValue = HIDDEN_PASSWORD;
                }
                newAddReq.setAttribute(addName, addValue);
            }
            return newAddReq;
        } else {
            return addRequest;
        }
    }
}