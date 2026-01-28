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

import java.util.Random;

/**
 *
 * @author Pascal Steiner
 *
 */
public class PasswordGenerator {
    public static final int DEFAULT_PW_LENGTH = 8;

    private static final Random RND = new Random();

    private final transient int passwordLength;

    public PasswordGenerator() {
        this(DEFAULT_PW_LENGTH);
    }

    /**
     * Erstellt eine zufällige Zeichenkette beliebiger Länge
     * @param length
     */
    public PasswordGenerator(final int length) {
        if (length < 1) {
            throw new IllegalArgumentException(
            "could not generate passwords with length smaller than 1");
        }
        passwordLength = length;
    }

    /**
     * Erzeugt eine Zeichenkette
     * @return
     */
    public String getRandomString() {
        final char[] pwd = new char[passwordLength];
        final int max = pwd.length;
        for (int i = 0; i < max; i++) {
            if (RND.nextInt(36) < 10) {
                pwd[i] = (char) (48 + RND.nextInt(10)); // zufällige Zahlenziffer erzeugen
            } else {
                pwd[i] = (char) (97 + RND.nextInt(26)); // zufälligen Kleinbuchstabe erzeugen
            }
        }
        return new String(pwd);
    }
}

