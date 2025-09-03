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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

/**
 * This class parses a SOAP file into usable data.
 *
 * @author Stefani Monson (of PCs for People) & Pico Mitchell (of Free Geek)
 */
public class SOAPParse {

    DocumentBuilderFactory factory;
    DocumentBuilder builder;

    public SOAPParse() throws Exception {
        factory = DocumentBuilderFactory.newInstance();
        builder = factory.newDocumentBuilder();
    }

    public String parseXML(String xml, String key) throws Exception {
        return parseXML(xml, key, 0);
    }

    public String parseXML(String xml, String key, int index) throws Exception {
        NodeList nodeList = builder.parse(new InputSource(new StringReader(xml))).getDocumentElement().getElementsByTagName(key);

        String returnValue = "";
        if (nodeList != null && nodeList.getLength() > 0) {
            NodeList sublist = nodeList.item(index).getChildNodes();
            if (sublist != null & sublist.getLength() > 0) {
                returnValue = sublist.item(0).getNodeValue();
            }

        }

        return returnValue;
    }

    public ArrayList<HashMap<String, String>> parseDataset(String xml) {
        /**
         * ********************************************************************
         * This method converts an XML file that contains a DataSet with several responses to a set of elements, like "Status History" the I use as keys or "headers" in a HashMap for each XML response in the set.
         *
         * The layout is as follows: Array[0] = HashMap1 HashMap1[Header1] = value 1 for Entry 1 HashMap1[Header2] = value 2 for Entry 1 Array[1] = HashMap2 HashMap2[Header1] = value 1 for Entry 2 HashMap2[Header2]
         *
         * Each interaction of the array contains one entire ROW of data for use in a table.
         */
        String[] xmlLines = xml.split("><");

        ArrayList<String> columns = new ArrayList<>();
        ArrayList<HashMap<String, String>> parsedData = new ArrayList<>();

        boolean collectingHeader = true;
        int collectingRowIndex = -1; // Start with "-1" so that the first row increment is 0 for the first actual index.
        for (String thisXMLLine : xmlLines) {
            if (collectingHeader) {
                if (thisXMLLine.startsWith("xs:element") && thisXMLLine.contains("type=\"xs:")) {
                    columns.add(thisXMLLine.substring(thisXMLLine.indexOf("\"") + 1, thisXMLLine.indexOf("\" type=")));
                } else if (thisXMLLine.equals("/xs:sequence")) {
                    collectingHeader = false;
                }
            } else {
                if (thisXMLLine.contains(":rowOrder=\"")) {
                    collectingRowIndex++;
                    parsedData.add(new HashMap<>());
                }

                if (collectingRowIndex >= 0) {
                    for (String thisColumn : columns) {
                        if (thisXMLLine.startsWith(thisColumn + ">") && thisXMLLine.endsWith("</" + thisColumn)) {
                            parsedData.get(collectingRowIndex).put(thisColumn, thisXMLLine.substring(thisXMLLine.indexOf(">") + 1, thisXMLLine.lastIndexOf("</")));
                        }
                    }
                }
            }
        }

        return parsedData;
    }
}
