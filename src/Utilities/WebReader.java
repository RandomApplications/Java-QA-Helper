/*
 *
 * MIT License
 *
 * Copyright (c) 2018 PCs for People (based on CommandReader.java)
 * Copyright (c) 2025 Free Geek
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
package Utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * This class reads commands determined by constructor and returns all lines or line containing a given string
 *
 * @author Based on CommandReader.java by Stefani Monson (of PCs for People) & Pico Mitchell (of Free Geek)
 */
public class WebReader {

    private final boolean debugLogging = false;
    private HttpURLConnection webConnection;
    private BufferedReader webReader;

    public WebReader(String url) {
        initWebReader(url, null, null, null, null, null, null, 0, 0);
    }

    public WebReader(String url, String username, String password, String jsonBody, String requestMethod) {
        initWebReader(url, username, password, null, jsonBody, null, requestMethod, 0, 0);
    }

    public WebReader(String url, String bearerToken, String jsonBody, String requestMethod) {
        initWebReader(url, null, null, bearerToken, jsonBody, null, requestMethod, 0, 0);
    }

    public WebReader(String url, String bearerToken, String jsonBody) {
        initWebReader(url, null, null, bearerToken, jsonBody, null, null, 0, 0);
    }

    public WebReader(String url, String bearerTokenOrJsonBody) {
        if (bearerTokenOrJsonBody.matches("^[a-zA-Z0-9]{8}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{4}-[a-zA-Z0-9]{12}$")) {
            initWebReader(url, null, null, bearerTokenOrJsonBody, null, null, null, 0, 0);
        } else {
            initWebReader(url, null, null, null, bearerTokenOrJsonBody, null, null, 0, 0);
        }
    }

    public WebReader(String url, String username, String password, HashMap<String, String> formFields) {
        initWebReader(url, username, password, null, null, formFields, null, 0, 0);
    }

    public WebReader(String url, String bearerToken, HashMap<String, String> formFields) {
        initWebReader(url, null, null, bearerToken, null, formFields, null, 0, 0);
    }

    public WebReader(String url, HashMap<String, String> formFields) {
        initWebReader(url, null, null, null, null, formFields, null, 0, 0);
    }

    public WebReader(String url, HashMap<String, String> formFields, int readTimeoutSeconds) {
        initWebReader(url, null, null, null, null, formFields, null, 0, readTimeoutSeconds);
    }

    private void initWebReader(String url, String username, String password, String bearerToken, String jsonBody, HashMap<String, String> formFields, String requestMethod) {
        initWebReader(url, username, password, bearerToken, jsonBody, formFields, requestMethod, 0, 0);
    }

    private void initWebReader(String url, String username, String password, String bearerToken, String jsonBody, HashMap<String, String> formFields, String requestMethod, int readTimeoutSeconds) {
        initWebReader(url, username, password, bearerToken, jsonBody, formFields, requestMethod, 0, readTimeoutSeconds);
    }

    private void initWebReader(String url, String username, String password, String bearerToken, String jsonBody, HashMap<String, String> formFields, String requestMethod, int connectTimeoutSeconds, int readTimeoutSeconds) {
        try {
            // NOTE: Cannot use newer HttpClient because all existing Linux and Windows installs of QA Helper use a jlink JRE which doesn't include "java.net.http" module,
            // and would be a bigger hassle to automate updating the jlink JRE than use the older HttpURLConnection.

            webConnection = (HttpURLConnection) URI.create(url).toURL().openConnection();

            if (connectTimeoutSeconds <= 0) {
                connectTimeoutSeconds = 5;
            }

            webConnection.setConnectTimeout(connectTimeoutSeconds * 1000);

            if (readTimeoutSeconds <= 0) {
                readTimeoutSeconds = 10;
            }

            webConnection.setReadTimeout(readTimeoutSeconds * 1000);

            if ((bearerToken != null) && !bearerToken.isEmpty()) {
                webConnection.setRequestProperty("Authorization", "Bearer " + bearerToken);
            } else if ((username != null) && !username.isEmpty() && (password != null) && !password.isEmpty()) {
                webConnection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes()));
            }

            String bodyString = "";
            if ((formFields != null) && !formFields.isEmpty()) {
                StringBuilder formFieldParametersBuilder = new StringBuilder();
                formFields.forEach((thisKey, thisValue) -> {
                    try {
                        String thisURLEncodedKey = URLEncoder.encode(thisKey, StandardCharsets.UTF_8.toString());
                        String thisURLEncodedValue = URLEncoder.encode(thisValue, StandardCharsets.UTF_8.toString());

                        if (formFieldParametersBuilder.length() != 0) { // StringBuilder.isEmpty() is not available on Java 11!
                            formFieldParametersBuilder.append('&');
                        }

                        formFieldParametersBuilder.append(thisURLEncodedKey);
                        formFieldParametersBuilder.append('=');
                        formFieldParametersBuilder.append(thisURLEncodedValue);
                    } catch (UnsupportedEncodingException encodeValueException) {
                    }
                });

                bodyString = formFieldParametersBuilder.toString();
            } else if (jsonBody != null) {
                jsonBody = jsonBody.trim();

                if (jsonBody.startsWith("{")) {
                    bodyString = jsonBody;
                }
            }

            if (!bodyString.isEmpty()) {
                if (debugLogging) {
                    System.out.println("WebReader bodyString: " + bodyString);
                }

                webConnection.setDoOutput(true);
                webConnection.setRequestProperty("Content-Type", (bodyString.startsWith("{") ? "application/json" : "application/x-www-form-urlencoded"));

                if ((requestMethod != null) && requestMethod.toUpperCase().equals("PUT")) {
                    webConnection.setRequestMethod("PUT");
                } else {
                    webConnection.setRequestMethod("POST");
                }

                byte[] bodyStringBytes = bodyString.getBytes(StandardCharsets.UTF_8.toString());
                webConnection.setRequestProperty("Content-Length", String.valueOf(bodyStringBytes.length));

                OutputStream httpConnectionOutputStream = webConnection.getOutputStream();
                httpConnectionOutputStream.write(bodyStringBytes);
                httpConnectionOutputStream.flush();
            } else if (requestMethod != null) {
                switch (requestMethod.toUpperCase()) {
                    case "PUT":
                        webConnection.setRequestMethod("PUT");
                        break;
                    case "POST":
                        webConnection.setRequestMethod("POST");
                        break;
                    default:
                        webConnection.setRequestMethod("GET");
                        break;
                }
            }

            if (debugLogging) {
                System.out.println("WebReader baseURL: " + (url.contains("?") ? url.substring(0, url.indexOf("?")) : url));
            }

            int maxWebReaderAttempts = 3;
            for (int webReaderAttemptCount = 1; webReaderAttemptCount <= maxWebReaderAttempts; webReaderAttemptCount++) {
                try {
                    try {
                        int responseCode = webConnection.getResponseCode();
                        if (debugLogging) {
                            System.out.println("WebReader responseCode: " + responseCode);
                        }

                        webReader = new BufferedReader(new InputStreamReader(webConnection.getInputStream()));
                        break;
                    } catch (NullPointerException | IOException readInputStreamException) {
                        if (debugLogging) {
                            System.out.println("WebReader readInputStreamException: " + readInputStreamException);
                        }

                        webReader = new BufferedReader(new InputStreamReader(webConnection.getErrorStream()));
                        break;
                    }
                } catch (Exception webReaderAttemptException) {
                    if (debugLogging) {
                        System.out.println("webReaderAttemptException (ATTEMPT " + webReaderAttemptCount + " OF " + maxWebReaderAttempts + "): " + webReaderAttemptException);
                    }

                    if (webReaderAttemptCount < maxWebReaderAttempts) {
                        try {
                            TimeUnit.SECONDS.sleep(webReaderAttemptCount);
                        } catch (InterruptedException sleepException) {
                            // Ignore sleepException
                        }
                    } else {
                        throw webReaderAttemptException;
                    }
                }
            }
        } catch (Exception webReaderException) {
            webReader = null;
            // Ignore Error

            //JOptionPane.showMessageDialog(null, "<html><b>Failed To Load URL</b><br/><br/>" + url + "</html>", "QA Helper  —  Web Reader Error", JOptionPane.ERROR_MESSAGE);
            System.out.println("webReaderException: " + webReaderException);
        }
    }

    public String getFirstOutputLine() {
        return getFirstOutputLineContaining("");
    }

    public String getFirstOutputLineContaining(String match) {
        if (webReader == null) {
            return "";
        }

        boolean getFirstLine = match.isEmpty();

        String outputLine = "";

        try {
            String thisLine;
            while ((thisLine = webReader.readLine()) != null) {
                if (getFirstLine || thisLine.contains(match)) {
                    outputLine = thisLine;
                    break;
                }
            }

            webReader.close();
        } catch (IOException readLineException) {

        }

        webConnection.disconnect();

        return outputLine;
    }

    public String getFirstOutputLineNotContaining(String match) {
        if (webReader == null) {
            return "";
        }

        boolean getFirstNonEmptyLine = match.isEmpty();

        String outputLine = "";

        try {
            String thisLine;
            while ((thisLine = webReader.readLine()) != null) {
                if ((getFirstNonEmptyLine && !thisLine.isEmpty()) || !thisLine.contains(match)) {
                    outputLine = thisLine;
                    break;
                }
            }

            webReader.close();
        } catch (IOException readLineException) {

        }

        webConnection.disconnect();

        return outputLine;
    }

    public String[] getOutputLines() {
        return getOutputLinesContaining(new String[]{""});
    }

    public String getOutputLinesAsString() {
        return String.join("\n", getOutputLines());
    }

    public String[] getOutputLinesContaining(String match) {
        return getOutputLinesContaining(new String[]{match});
    }

    public String[] getOutputLinesContaining(String[] matches) {
        if ((webReader == null) || (matches.length == 0)) {
            return new String[0];
        }

        ArrayList<String> outputLines = new ArrayList<>();

        boolean matchAllLines = false;
        for (String thisMatch : matches) {
            if (thisMatch.isEmpty()) {
                matchAllLines = true;
                break;
            }
        }

        try {
            String thisLine;
            while ((thisLine = webReader.readLine()) != null) {
                if (matchAllLines) {
                    outputLines.add(thisLine);
                } else {
                    for (String thisMatch : matches) {
                        if (thisLine.contains(thisMatch)) {
                            outputLines.add(thisLine);
                            break;
                        }
                    }
                }
            }

            webReader.close();
        } catch (IOException readLineException) {

        }

        webConnection.disconnect();

        return outputLines.toArray(String[]::new);
    }

    public String[] getOutputLinesNotContaining(String match) {
        return getOutputLinesNotContaining(new String[]{match});
    }

    public String[] getOutputLinesNotContaining(String[] matches) {
        if ((webReader == null) || (matches.length == 0)) {
            return new String[0];
        }

        ArrayList<String> outputLines = new ArrayList<>();

        boolean matchNonEmptyLines = false;
        for (String thisMatch : matches) {
            if (thisMatch.isEmpty()) {
                matchNonEmptyLines = true;
                break;
            }
        }

        try {
            String thisLine;
            while ((thisLine = webReader.readLine()) != null) {
                if (!matchNonEmptyLines || (matchNonEmptyLines && !thisLine.isEmpty())) {
                    boolean lineContainsMatch = false;

                    for (String thisMatch : matches) {
                        if (!thisMatch.isEmpty() && thisLine.contains(thisMatch)) {
                            lineContainsMatch = true;
                            break;
                        }
                    }

                    if (!lineContainsMatch) {
                        outputLines.add(thisLine);
                    }
                }
            }

            webReader.close();
        } catch (IOException readLineException) {

        }

        webConnection.disconnect();

        return outputLines.toArray(String[]::new);
    }
}
