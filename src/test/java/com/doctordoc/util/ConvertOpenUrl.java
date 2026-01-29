//  Copyright (C) 2005 - 2010  Markus Fischer, Pascal Steiner
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; version 2 of the License.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
//
//  Contact: info@doctor-doc.com

package com.doctordoc.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for OpenURL conversion operations.
 * Simplified version for testing purposes.
 * Migrated for Spring Boot compatibility.
 */
public class ConvertOpenUrl {

    private static final Logger LOG = LoggerFactory.getLogger(ConvertOpenUrl.class);

    /**
     * Extracts the start page from a page range string.
     *
     * @param input the page range (e.g., "60-4" or "S60-4")
     * @return the start page
     */
    public String extractStartPage(final String input) {
        String result = "";
        if (input != null) {
            try {
                // Use word characters, since we see numberings like e1254-56
                final Pattern z = Pattern.compile("\\w+");
                final Matcher w = z.matcher(input);

                if (w.find()) { // only extract first number
                    result = input.substring(w.start(), w.end());
                }
            } catch (final Exception e) {
                LOG.error("extractStartPage: {} {}", input, e.toString());
            }
        }

        return result;
    }

    /**
     * Normalizes an ISSN without hyphen to standard format.
     *
     * @param input the ISSN without hyphen
     * @return the normalized ISSN with hyphen
     */
    public String normalizeIssn(String input) {
        try {
            if (input != null && input.length() == 8 && !input.contains("-")) {
                input = input.substring(0, 4) + "-" + input.substring(4, 8);
            }
        } catch (final Exception e) {
            LOG.error("normalizeIssn: {} {}", input, e.toString());
        }

        return input;
    }

    /**
     * Gets an ISSN out of a String with several ISSNs.
     * e.g., Refworks sends multiple ISSNs in the param: issn=1234-1234; 2345-2345
     */
    private String extractFromSeveralIssns(String input) {
        try {
            // Must have at least the length of two ISSNs plus a separation character
            if (input != null && input.length() > 19 && (input.contains(";") || input.contains(","))) {
                // Separation character is comma or semicolon
                if (input.contains(";")) {
                    if (input.indexOf(';') > 8) { // must have an ISSN number before the separation character
                        input = input.substring(0, input.indexOf(';')).trim(); // take the first ISSN
                    }
                } else if (input.contains(",") && input.indexOf(',') > 8) {
                    // must have an ISSN number before the separation character
                    input = input.substring(0, input.indexOf(',')).trim(); // take the first ISSN
                }
            }
        } catch (final Exception e) {
            LOG.error("extractFromSeveralIssns: {} {}", input, e.toString());
        }

        return input;
    }

    /**
     * Converts uppercase string to normal case (first letter uppercase, rest lowercase).
     */
    private String toNormalLetters(String input) {
        try {
            if (input != null && !input.isEmpty() && !checkLowercase(input)) {
                input = input.toLowerCase();
                input = input.substring(0, 1).toUpperCase() + input.substring(1);
            }
        } catch (final Exception e) {
            LOG.error("toNormalLetters: {} {}", input, e.toString());
        }

        return input;
    }

    /**
     * Tests if a string contains lowercase letters.
     */
    private boolean checkLowercase(final String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        try {
            final Pattern z = Pattern.compile("\\p{Lower}");
            final Matcher w = z.matcher(input);
            return w.find();
        } catch (final Exception e) {
            LOG.error("testLowercase: {} {}", input, e.toString());
            return false;
        }
    }
}
