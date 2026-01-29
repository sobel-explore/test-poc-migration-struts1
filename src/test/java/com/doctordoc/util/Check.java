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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for common validation operations.
 * Migrated for Spring Boot compatibility.
 */
public class Check {

    private static final Logger LOG = LoggerFactory.getLogger(Check.class);

    // Email domain pattern for validation
    private static final Pattern EMAIL_DOMAIN_PATTERN = Pattern.compile(
            "@[a-z0-9-]+(\\.[a-z0-9-]+)*\\."
                    + "([a-z]{2}|aero|arpa|asia|biz|com|coop|edu|gov|info|int|jobs|mil|mobi|museum|name|nato|net|"
                    + "org|pro|tel|travel|xxx)$\\b");

    // Year pattern (14th to 22nd century)
    private static final Pattern YEAR_PATTERN = Pattern.compile(
            "13[0-9]{2}|14[0-9]{2}|15[0-9]{2}|16[0-9]{2}|17[0-9]{2}|18[0-9]{2}|19[0-9]{2}|20[0-9]{2}|21[0-9]{2}");

    /**
     * Checks if a string is a valid email address.
     *
     * @param email the email to validate
     * @return true if valid, false otherwise
     */
    public boolean isEmail(final String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        try {
            // Basic format check
            if (!email.contains("@") || email.startsWith("'") || email.endsWith("'")) {
                return false;
            }

            // Check for valid domain
            final Matcher m = EMAIL_DOMAIN_PATTERN.matcher(email.toLowerCase());
            return m.find();

        } catch (final Exception e) {
            LOG.error("isEmail: {} {}", email, e.toString());
            return false;
        }
    }

    /**
     * Ensures the string is not null and has at least the minimum length.
     *
     * @param s the string to check
     * @param l the minimum length
     * @return true if valid, false otherwise
     */
    public boolean isMinLength(final String s, final int l) {
        return s != null && s.length() >= l;
    }

    /**
     * Ensures the string is not null and has exactly the specified length.
     *
     * @param s the string to check
     * @param l the exact length required
     * @return true if valid, false otherwise
     */
    public boolean isExactLength(final String s, final int l) {
        return s != null && s.length() == l;
    }

    /**
     * Checks if the string length is between min and max (inclusive).
     *
     * @param s   the string to check
     * @param min minimum length
     * @param max maximum length
     * @return true if length is within range, false otherwise
     */
    public boolean isLengthInMinMax(final String s, final int min, final int max) {
        return s != null && min <= s.length() && s.length() <= max;
    }

    /**
     * Checks if a string is a valid URL.
     *
     * @param link the URL to validate
     * @return true if valid, false otherwise
     */
    public boolean isUrl(final String link) {
        if (link == null || link.isEmpty()) {
            return false;
        }

        try {
            new URL(link);
            return true;
        } catch (final MalformedURLException e) {
            LOG.info("isUrl: {} {}", link, e.toString());
            return false;
        }
    }

    /**
     * Checks if a string ends with a valid filetype extension.
     *
     * @param fileName  the filename to check
     * @param extension the extension to look for (including dot)
     * @return true if the filename ends with the extension, false otherwise
     */
    public boolean isFiletypeExtension(final String fileName, final String extension) {
        if (fileName == null || extension == null || fileName.length() <= extension.length()) {
            return false;
        }

        try {
            final String fileNameLower = fileName.toLowerCase();
            final String extensionLower = extension.toLowerCase();

            return fileNameLower.contains(extensionLower)
                    && fileNameLower.lastIndexOf(extensionLower) == fileNameLower.length() - extensionLower.length();
        } catch (final Exception e) {
            return false;
        }
    }

    /**
     * Extracts all alphanumeric words from a string.
     *
     * @param s the input string
     * @return list of alphanumeric words
     */
    public List<String> getAlphanumericWordCharacters(final String s) {
        final List<String> words = new ArrayList<>();

        if (s != null) {
            // Remove all non-word characters with spaces
            final String onlyWords = s.replaceAll("[^\\pL\\p{Nd}]", " ");
            final StringTokenizer tokenizer = new StringTokenizer(onlyWords);

            while (tokenizer.hasMoreTokens()) {
                words.add(tokenizer.nextToken());
            }
        }

        return words;
    }

    /**
     * Counts the occurrence of a substring in a string.
     *
     * @param input       the input string
     * @param countString the substring to count
     * @return the count of occurrences
     */
    public int countCharacterInString(final String input, final String countString) {
        if (input == null || countString == null) {
            return 0;
        }
        return input.split("\\Q" + countString + "\\E", -1).length - 1;
    }

    /**
     * Checks if a string contains only numeric digits.
     *
     * @param str the string to check
     * @return true if only contains digits, false otherwise
     */
    public boolean containsOnlyNumbers(final String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Validates an ISSN checksum.
     *
     * @param issn the ISSN to validate (format: XXXX-XXXX)
     * @return true if valid, false otherwise
     */
    public boolean isValidIssn(final String issn) {
        if (issn == null || issn.isEmpty()) {
            return false;
        }

        try {
            if (issn.length() == 9 && issn.substring(4, 5).equals("-")) {
                final int pos8 = Integer.parseInt(issn.substring(0, 1));
                final int pos7 = Integer.parseInt(issn.substring(1, 2));
                final int pos6 = Integer.parseInt(issn.substring(2, 3));
                final int pos5 = Integer.parseInt(issn.substring(3, 4));
                final int pos4 = Integer.parseInt(issn.substring(5, 6));
                final int pos3 = Integer.parseInt(issn.substring(6, 7));
                final int pos2 = Integer.parseInt(issn.substring(7, 8));
                final String pos1 = issn.substring(8);

                final int sum = pos8 * 8 + pos7 * 7 + pos6 * 6 + pos5 * 5 + pos4 * 4 + pos3 * 3 + pos2 * 2;
                final int checksum = 11 - (sum - (sum / 11) * 11);

                String kontrollziffer;
                if (checksum == 10) {
                    kontrollziffer = "X";
                } else if (checksum == 11) {
                    kontrollziffer = "0";
                } else {
                    kontrollziffer = String.valueOf(checksum);
                }

                return pos1.equalsIgnoreCase(kontrollziffer);
            }
        } catch (final Exception e) {
            LOG.error("isValidIssn: {} {}", issn, e.toString());
        }

        return false;
    }

    /**
     * Checks if a string is a valid year (14th to 22nd century).
     *
     * @param year the year string to validate
     * @return true if valid, false otherwise
     */
    public boolean isYear(final String year) {
        if (!isExactLength(year, 4) || !StringUtils.isNumeric(year)) {
            return false;
        }

        try {
            final Matcher m = YEAR_PATTERN.matcher(year);
            return m.find();
        } catch (final Exception e) {
            LOG.error("isYear(String year): {} {}", year, e.toString());
            return false;
        }
    }
}
