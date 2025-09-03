/*
 *
 * MIT License
 *
 * Copyright (c) 2018 PCs for People
 * Copyright (c) 2019-2024 Free Geek
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package SOAP;

import java.io.StringWriter;
import jakarta.xml.soap.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import Utilities.PrivateStrings;
import java.util.concurrent.TimeUnit;

/**
 * This class prepares, sends and receives SOAP files.
 *
 * @author Stefani Monson (of PCs for People) & Pico Mitchell (of Free Geek)
 */
// REQUIRED LIBRARIES:
//  jakarta.activation: https://maven-badges.sml.io/maven-central/com.sun.activation/jakarta.activation
//  jakarta.xml.soap-api: https://maven-badges.sml.io/maven-central/jakarta.xml.soap/jakarta.xml.soap-api
//  saaj-impl: https://maven-badges.sml.io/maven-central/com.sun.xml.messaging.saaj/saaj-impl
//  stax-ex: https://maven-badges.sml.io/maven-central/org.jvnet.staxex/stax-ex
public class SOAPSend {

    private SOAPConnectionFactory soapConnectionFactory;
    private SOAPConnection soapConnection;
    private SOAPEnvelope envelope;
    private MessageFactory messageFactory;
    private SOAPMessage soapMessage;
    private SOAPPart soapPart;
    private SOAPBody soapBody;
    private SOAPElement soapBodyElem;
    private PrivateStrings privateStrings = new PrivateStrings();
    private String serviceURL = privateStrings.getPCsCRMServiceURL();
    private String serverURI = privateStrings.getPCsCRMServerURI();
    private MimeHeaders mimeHeaders;

    public SOAPSend(boolean testMode) {
        if (testMode) {
            serviceURL = privateStrings.getPCsCRMServiceURL(testMode);
        }

        try {
            soapConnectionFactory = SOAPConnectionFactory.newInstance();
            soapConnection = soapConnectionFactory.createConnection();
            messageFactory = MessageFactory.newInstance();
            soapMessage = messageFactory.createMessage();
            soapPart = soapMessage.getSOAPPart();
            envelope = soapPart.getEnvelope();
            envelope.addNamespaceDeclaration("xsd", serverURI);
            soapBody = envelope.getBody();
        } catch (UnsupportedOperationException | SOAPException soapInitException) {
            System.out.println("soapInitException: " + soapInitException);
        }

    }

    public String buildAndSendSOAP(String endpoint, String key, String value) {
        return buildAndSendSOAP(endpoint, new String[]{key}, new String[]{value});
    }

    public String buildAndSendSOAP(String endpoint, String keys[], String values[]) {
        //System.out.println("SOAPSend buildAndSendSOAP: " + endpoint); // DEBUG

        int maxSoapAttempts = 4;
        for (int soapAttemptCount = 1; soapAttemptCount <= maxSoapAttempts; soapAttemptCount++) {
            // After adding "sendErrorEmail" functionality, it seems that sporadic SOAP failures are one of the most common issues.
            // To help workaround this issue, try to send SOAP commands up to "maxSoapAttempts" times if there is an exception
            // (waiting a moment between attempts), and only return an error if the final attempt fails.

            try {
                soapBodyElem = soapBody.addChildElement(endpoint, "", serverURI);

                for (int i = 0; i < keys.length; i++) {
                    SOAPElement thisSoapElement = soapBodyElem.addChildElement(keys[i]);
                    thisSoapElement.addTextNode(values[i]);
                }

                mimeHeaders = soapMessage.getMimeHeaders();
                mimeHeaders.addHeader("SOAPAction", serverURI + endpoint);
                soapMessage.saveChanges();

                String soapResponse = parseSoapResponse(soapConnection.call(soapMessage, serviceURL));

                soapConnection.close();

                if (!soapResponse.startsWith("<?xml")) {
                    throw new Exception("SOAP RESPONSE NOT XML: " + soapResponse);
                }

                return soapResponse;
            } catch (Exception soapBuildAndSendException) {
                System.out.println("soapBuildAndSendException (ATTEMPT " + soapAttemptCount + " OF " + maxSoapAttempts + "): " + soapBuildAndSendException);

                if (soapAttemptCount < maxSoapAttempts) {
                    try {
                        TimeUnit.SECONDS.sleep(soapAttemptCount);
                    } catch (InterruptedException sleepException) {
                        // Ignore sleepException
                    }
                } else {
                    return soapBuildAndSendException.toString();
                }
            }
        }

        return "UNKNOWN buildAndSendSOAP ERROR";
    }

    public String parseSoapResponse(SOAPMessage soapResponse) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            Source sourceContent = soapResponse.getSOAPPart().getContent();

            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            transformer.transform(sourceContent, result);

            return writer.toString();
        } catch (SOAPException | TransformerException soapParseResponseException) {
            System.out.println("soapParseResponseException: " + soapParseResponseException);

            return soapParseResponseException.toString();
        }
    }

}
