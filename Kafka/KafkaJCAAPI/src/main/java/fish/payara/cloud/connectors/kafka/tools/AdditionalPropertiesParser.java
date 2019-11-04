/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.cloud.connectors.kafka.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Logger;

public class AdditionalPropertiesParser {

    private static final Logger LOG = Logger.getLogger(AdditionalPropertiesParser.class.getName());
    private static final String LIST_SEPARATOR = ",";
    private static final String KEY_VALUE_SEPARATOR = "=";
    private static final char ESCAPE_CHARACTER = '\'';

    private String propertiesString;

    public AdditionalPropertiesParser(String propertiesString) {
        this.propertiesString = propertiesString;
    }


    private static String[] split(String propertiesString, String separator) {
        ArrayList<String> properties = new ArrayList<>();
        boolean insideComplexValue = false;
        StringBuilder part = new StringBuilder();

        for (int i = 0; i < propertiesString.length(); i++) {
            part.append(propertiesString.charAt(i));

            if (propertiesString.charAt(i) == ESCAPE_CHARACTER) {
                insideComplexValue = !insideComplexValue;
            }

            // separator already in part, add to result list
            if (part.substring(part.length() - separator.length()).equals(separator) && !insideComplexValue) {
                properties.add(part.substring(0, part.length() - separator.length()));
                part.setLength(0);
            }
        }

        if (part.length() > 0) {
            properties.add(part.toString());
        }

        return properties.toArray(new String[0]);
    }

    private static String correctSingleQuotes(String val) {
        String cleaned = val.replace("" + ESCAPE_CHARACTER + ESCAPE_CHARACTER, "" + ESCAPE_CHARACTER);

        if (cleaned.charAt(0) == ESCAPE_CHARACTER) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.charAt(cleaned.length() - 1) == ESCAPE_CHARACTER) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }

        return cleaned;
    }


    public static Properties merge(Properties base, Properties addtional){
        Properties properties = new Properties();
        properties.putAll(base);
        if(addtional != null){
            for(String key : addtional.stringPropertyNames()){
                properties.putIfAbsent(key, addtional.getProperty(key));
            }
        }
        return properties;
    }

    public Properties parse() {
        Properties properties = new Properties();
        if (propertiesString != null) {
            String lastKey = null;
            final String[] splittedProperties = split(propertiesString, LIST_SEPARATOR);
            for (String singleKeyValue : splittedProperties) {
                final String[] splittedKeyValue = split(singleKeyValue, KEY_VALUE_SEPARATOR);
                switch (splittedKeyValue.length) {
                    case 2: {
                        final String key = correctSingleQuotes(splittedKeyValue[0].trim());
                        lastKey = key;
                        final String value = correctSingleQuotes(splittedKeyValue[1].trim());
                        final String existingValue = properties.getProperty(key);
                        if (existingValue != null) {
                            properties.setProperty(key, existingValue + LIST_SEPARATOR + value);
                        } else {
                            properties.setProperty(key, value);
                        }
                        break;
                    }
                    case 1: {
                        if (lastKey != null) {
                            final String value = correctSingleQuotes(splittedKeyValue[0].trim());
                            // assume property to be list and use the last key to add to
                            final String existingValue = properties.getProperty(lastKey);
                            if (existingValue != null) {
                                properties.setProperty(lastKey, existingValue + LIST_SEPARATOR + value);
                            } else {
                                properties.setProperty(lastKey, value);
                            }
                        }
                        break;
                    }
                    default:
                        LOG.warning("Found illegal properties " + Arrays.toString(splittedKeyValue));

                }
            }
        }
        return properties;
    }

}
