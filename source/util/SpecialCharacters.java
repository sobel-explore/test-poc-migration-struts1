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

package util;

/**
 * Chunk method for removing differtent encodings from web sites.
 * @author Markus Fischer
 *
 */
public class SpecialCharacters {

    /**
     * Removes HTML entities and partially UTF8 URLencoding from links embedded in web sites.
     *
     * @param String input
     * @return String output, returns null on null input
     */
    public String replace(final String input) {
        String output = input;

        // http://www.utf8-zeichentabelle.de/unicode-utf8-table.pl?start=768&number=128

        if (input != null) {

            if (output.contains("%CC")) { // URL in HTML UTF-8 encoded
                // for the moment, we cannot use a standard decoding method, because of a conflict in OpenURL.readOpenUrlFromString

                // COMBINING GRAVE ACCENT
                output = output.replaceAll("a%CC%80", "à");
                output = output.replaceAll("e%CC%80", "è");
                output = output.replaceAll("i%CC%80", "ì");
                output = output.replaceAll("o%CC%80", "ò");
                output = output.replaceAll("u%CC%80", "ù");
                output = output.replaceAll("A%CC%80", "À");
                output = output.replaceAll("E%CC%80", "È");
                output = output.replaceAll("I%CC%80", "Ì");
                output = output.replaceAll("O%CC%80", "Ò");
                output = output.replaceAll("U%CC%80", "Ù");
                // COMBINING ACUTE ACCENT
                output = output.replaceAll("a%CC%81", "á");
                output = output.replaceAll("e%CC%81", "é");
                output = output.replaceAll("i%CC%81", "í");
                output = output.replaceAll("o%CC%81", "ó");
                output = output.replaceAll("u%CC%81", "ú");
                output = output.replaceAll("A%CC%81", "Á");
                output = output.replaceAll("E%CC%81", "É");
                output = output.replaceAll("I%CC%81", "Í");
                output = output.replaceAll("O%CC%81", "Ó");
                output = output.replaceAll("U%CC%81", "Ú");
                // COMBINING CIRCUMFLEX ACCENT
                output = output.replaceAll("a%CC%82", "â");
                output = output.replaceAll("e%CC%82", "ê");
                output = output.replaceAll("i%CC%82", "î");
                output = output.replaceAll("o%CC%82", "ô");
                output = output.replaceAll("u%CC%82", "û");
                output = output.replaceAll("A%CC%82", "Â");
                output = output.replaceAll("E%CC%82", "Ê");
                output = output.replaceAll("I%CC%82", "Î");
                output = output.replaceAll("O%CC%82", "Ô");
                output = output.replaceAll("U%CC%82", "Û");
                // COMBINING TILDE
                output = output.replaceAll("a%CC%83", "ã");
                output = output.replaceAll("n%CC%83", "ñ");
                output = output.replaceAll("o%CC%83", "õ");
                output = output.replaceAll("A%CC%83", "Ã");
                output = output.replaceAll("N%CC%83", "Ñ");
                output = output.replaceAll("O%CC%83", "Õ");
                // COMBINING DIAERESIS
                output = output.replaceAll("a%CC%88", "ä");
                output = output.replaceAll("e%CC%88", "ë");
                output = output.replaceAll("i%CC%88", "ï");
                output = output.replaceAll("o%CC%88", "ö");
                output = output.replaceAll("u%CC%88", "ü");
                output = output.replaceAll("A%CC%88", "Ä");
                output = output.replaceAll("E%CC%88", "Ë");
                output = output.replaceAll("I%CC%88", "Ï");
                output = output.replaceAll("O%CC%88", "Ö");
                output = output.replaceAll("U%CC%88", "Ü");
                // COMBINING CEDILLA
                output = output.replaceAll("c%CC%A7", "ç");
                output = output.replaceAll("C%CC%A7", "Ç");
            }

            // escape all HTML entities
            output = org.apache.commons.lang.StringEscapeUtils.unescapeHtml(output);

        }

        return output;

    }

}
