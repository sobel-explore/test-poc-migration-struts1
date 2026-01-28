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

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.validator.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.dbs.entity.Text;
import ch.dbs.form.OverviewForm;

public class Check {

    private static final Logger LOG = LoggerFactory.getLogger(Check.class);

    /**
     * Checks if a string is a valid email address.
     * 
     * @param email
     * @return
     */
    public boolean isEmail(final String email) {
        boolean check = false;
        try {
            final InternetAddress ia = new InternetAddress();
            ia.setAddress(email);
            try {
                ia.validate(); // throws an error for invalid addresses and null input, but does not catch all errors
                // we are using apache commons email validator
                final EmailValidator validator = EmailValidator.getInstance();
                check = validator.isValid(email);

                // EmailValidator is agnostic about valid domain names...
                // ...we do an additional check
                final Pattern p = Pattern
                        .compile("@[a-z0-9-]+(\\.[a-z0-9-]+)*\\"
                                + ".([a-z]{2}|aero|arpa|asia|biz|com|coop|edu|gov|info|int|jobs|mil|mobi|museum|name|nato|net|"
                                + "org|pro|tel|travel|xxx)$\\b");
                // we need to match against lower case for the above regex
                final Matcher m = p.matcher(email.toLowerCase());
                if (!m.find()) {
                    // reset to false, if we do not have a match
                    check = false;
                }

            } catch (final AddressException e1) {
                LOG.info("isEmail: " + email + " " + e1.toString());
            }
        } catch (final Exception e) {
            LOG.error("isEmail: " + email + " " + e.toString());
        }

        return check;
    }

    /**
     * Stellt sicher, dass der String nicht null ist und einen Wert der
     * Mindestlänge l hat
     * 
     * @param s
     * @param l
     * @return
     */
    public boolean isMinLength(final String s, final int l) {
        boolean check = false;
        if (s != null && s.length() >= l) {
            check = true;
        }
        return check;
    }

    /**
     * Stellt sicher, dass der String nicht null ist und einen Wert der genauen
     * Länge l hat
     * 
     * @param s
     * @param l
     * @return
     */
    public boolean isExactLength(final String s, final int l) {
        boolean check = false;
        if (s != null && s.length() == l) {
            check = true;
        }
        return check;
    }

    /**
     * Ueeberprueft, ob die Länge des Strings sich zwischen einem Minimum und
     * einem Maximum befindet<br> min <= s <= max<p>
     * 
     * @param s
     * @param min
     * @param max
     * @return
     */
    public boolean isLengthInMinMax(final String s, final int min, final int max) {
        boolean check = false;
        if (s != null && (min <= s.length()) && (s.length() <= max)) {
            check = true;
        }
        return check;
    }

    /**
     * Prüft Datumseingaben und stellt ggf. einen gültigen Datumsbereich von x
     * Wochen zusammen
     * 
     * @param of
     */
    public void checkDateRegion(final OverviewForm of, final int defaultPeriod, final String defaultTimezone) {

        //Längen Sicherstellen
        if (this.isExactLength(of.getYfrom(), 4) && this.isExactLength(of.getYto(), 4)
                && this.isLengthInMinMax(of.getMfrom(), 1, 2) && this.isLengthInMinMax(of.getMto(), 1, 2)
                && this.isLengthInMinMax(of.getDfrom(), 1, 2) && this.isLengthInMinMax(of.getDto(), 1, 2)) {

            try {
                final Calendar calTo = Calendar.getInstance();
                final Calendar calFrom = Calendar.getInstance();
                calTo.set(Integer.parseInt(of.getYto()), Integer.parseInt(of.getMto()) - 1,
                        Integer.parseInt(of.getDto()), 0, 0, 0);
                calFrom.set(Integer.parseInt(of.getYfrom()), Integer.parseInt(of.getMfrom()) - 1,
                        Integer.parseInt(of.getDfrom()), 0, 0, 0);
                final long time = calTo.getTime().getTime() - calFrom.getTime().getTime();

                if (time < 0) { // Überprüfung ob eine negativer Zeitbereicg vorliegt...
                    of.setYfrom(null); // zurückstellen, damit im nächsten Schritt die Default-Werte zum Zug kommen...
                } else {
                    //          System.out.println("Kalender wieder in of setzen, wegen Überläufen (31.2.2008)");
                    of.setYfrom(new SimpleDateFormat("yyyy").format(calFrom.getTime()));
                    of.setMfrom(new SimpleDateFormat("MM").format(calFrom.getTime()));
                    of.setDfrom(new SimpleDateFormat("dd").format(calFrom.getTime()));
                    of.setFromdate(of.getYfrom() + "-" + of.getMfrom() + "-" + of.getDfrom() + " 00:00:00");

                    of.setYto(new SimpleDateFormat("yyyy").format(calTo.getTime()));
                    of.setMto(new SimpleDateFormat("MM").format(calTo.getTime()));
                    of.setDto(new SimpleDateFormat("dd").format(calTo.getTime()));
                    of.setTodate(of.getYto() + "-" + of.getMto() + "-" + of.getDto() + " 24:00:00");
                }

            } catch (final NumberFormatException e) {
                LOG.error("checkDateRegion: " + e.toString());
                of.setYfrom(null);
            }

        } else { //Benutzerangaben falsche anzahl Zeichen
            // zurückstellen, damit im nächsten Schritt die Default-Werte zum Zug kommen
            of.setYfrom(null);
        }

        // Defaultwerte bei bedarf Setzen
        if (of.getYfrom() == null || of.getYto() == null || of.getMfrom() == null || of.getMto() == null
                || of.getDfrom() == null || of.getDto() == null) {

            final Calendar calTo = Calendar.getInstance();
            calTo.setTimeZone(TimeZone.getTimeZone(defaultTimezone));
            final Calendar calFrom = Calendar.getInstance();
            calFrom.setTimeZone(TimeZone.getTimeZone(defaultTimezone));
            calFrom.add(Calendar.MONTH, -defaultPeriod); // bessere Verständlichkeit wenn Monatsbereiche vorliegen...

            of.setYfrom(new SimpleDateFormat("yyyy").format(calFrom.getTime()));
            of.setMfrom(new SimpleDateFormat("MM").format(calFrom.getTime()));
            of.setDfrom(new SimpleDateFormat("dd").format(calFrom.getTime()));
            of.setFromdate(of.getYfrom() + "-" + of.getMfrom() + "-" + of.getDfrom() + " 00:00:00");

            of.setYto(new SimpleDateFormat("yyyy").format(calTo.getTime()));
            of.setMto(new SimpleDateFormat("MM").format(calTo.getTime()));
            of.setDto(new SimpleDateFormat("dd").format(calTo.getTime()));
            of.setTodate(of.getYto() + "-" + of.getMto() + "-" + of.getDto() + " 24:00:00");
        }
    }

    /**
     * Ueberprueft Sortierkriterium (asc/desc), sowie ob nach gültigem Feld
     * Sortiert wird<br> Sollte dies nicht der Fall sein weird automatisch
     * Sortierung auf asc ausser bei Sortierung nach<br> orderdate nach desc <p>
     * 
     * @param of OverviewForm
     */
    public void checkSortOrderValues(final OverviewForm of) {

        //Check, damit nur gültige Sortierkriterien daherkommen (asc/desc)
        if (of.getSort() == null) {
            of.setSort("orderdate");
        }
        // nach dem Login Default-Sortierung nach asc ausser bei date nach desc
        if ((of.getSortorder() == null) || ((!of.getSortorder().equals("asc")) && (!of.getSortorder().equals("desc")))) {
            if (of.getSort().equals("orderdate")) {
                of.setSortorder("desc");
            } else {
                of.setSortorder("asc");
            }
        }
    }

    /**
     * Validierung der Filterkriterien anhand erlaubter Bestellstati.<br> Wenn
     * ungueltig auf null setzen. Benötigt, dass die kontoabhängigen Stati
     * vorgängig mittels of.setStati abgelegt wurden <p>
     * 
     * @param of OverviewForm
     */
    public void checkFilterCriteriasAgainstAllTextsFromTexttypPlusKontoTexts(final OverviewForm of) {
        if (of.getFilter() != null) { // grundsätzlich muss ein zu prüfendes Filterkriterium vorhanden sein
            boolean validCriteria = false;
            // Die kontoabhängigen Stati müssen vorgängig mittels of.setStati abgelegt werden
            if (of.getStatitexts().size() > 0) {
                for (final Text status : of.getStatitexts()) {
                    if (of.getFilter().equals(status.getInhalt()) || of.getFilter().equals("offen")) {
                        validCriteria = true;
                    }
                }
            }
            if (!validCriteria) {
                of.setFilter(null);
            } // ungültige Filterkriterien löschen
        }
    }

    /**
     * Sortierkriterium Bestellungen ueberpruefen auf Gueltigkeit, sonst
     * ersetzen mit orderdate<p>
     * 
     * @param of OverviewForm
     */
    public void checkOrdersSortCriterias(final OverviewForm of) {
        if (of.getSort().equals("mail") || of.getSort().equals("artikeltitel") || of.getSort().equals("zeitschrift")
                || of.getSort().equals("inhalt") || of.getSort().equals("orderdate")
                || of.getSort().equals("statedate") || of.getSort().equals("state")
                || of.getSort().equals("deloptions") || of.getSort().equals("name") || of.getSort().equals("konto")
                || of.getSort().equals("bestellquelle") || of.getSort().equals("mediatype")) {

            if (of.getSort().equals("konto")) {
                of.setSort("b.KID");
            }

        } else {
            of.setSort("orderdate");
        }
    }

    /**
     * This method checks if a String is a valid URL
     */
    public boolean isUrl(final String link) {

        boolean check = true;

        try {
            final URL url = new URL(link);
            System.out.println("Gültige URL: " + url); // unterdrückt Warnung, dass url nie gebraucht wird
        } catch (final MalformedURLException e) {
            LOG.info("isUrl: " + link + "\040" + e.toString());
            check = false;
        }

        return check;
    }

    /**
     * This method checks if a String is a valid URL and from a specific
     * filetype
     */
    public boolean isUrlAndFiletype(final String link, final String[] filetypes) {

        boolean check = false;

        try {
            final String extension = link.substring(link.lastIndexOf('.') + 1); // may throw exception

            // check for specified filetypes
            for (final String filetype : filetypes) {
                if (extension.equalsIgnoreCase(filetype)) {
                    check = true;
                }
            }

            // link mustn't be longer than 254 charactres
            if (link.length() > 254) {
                check = false;
            }

            // needs to be an URL after all!
            if (check) {
                check = isUrl(link);
            }

        } catch (final Exception e) {
            check = false; // if we got here, the check failed
        }

        return check;
    }

    /**
     * This method checks if a String ends with a valid filetype extension
     * 
     * @param String path
     * @return boolean check
     */
    public boolean isFiletypeExtension(final String fileName, final String extension) {

        boolean check = false;

        // filename must be longer than the filetype extension plus leading dot
        if (fileName != null && extension != null && fileName.length() > extension.length()) {
            // make case insensitive
            final String fileNameLower = fileName.toLowerCase();
            final String extensionLower = extension.toLowerCase();
            try {
                if (fileNameLower.contains(extensionLower) // must contain extension
                        // extension must be placed at the end
                        && fileNameLower.lastIndexOf(extensionLower) == fileNameLower.length()
                        - extensionLower.length()) {
                    check = true;
                }

            } catch (final Exception e) {
                check = false; // if we got here, the check failed
            }

        }

        return check;
    }

    /**
     * Extrahiert aus einem String alle Wörter und Zahlen.
     * 
     * @param s
     * @return List words
     */
    public List<String> getAlphanumericWordCharacters(final String s) {

        final List<String> words = new ArrayList<String>();

        if (s != null) {

            // remove all non word characters with spaces
            final String onlyWords = s.replaceAll("[^\\pL\\p{Nd}]", " ");

            final StringTokenizer tokenizer = new StringTokenizer(onlyWords);

            while (tokenizer.hasMoreTokens()) {
                words.add(tokenizer.nextToken());
            }

        }

        return words;
    }

    /**
     * Counts the occurence of a character in a string
     */
    public int countCharacterInString(final String input, final String countString) {
        return input.split("\\Q" + countString + "\\E", -1).length - 1;
    }

    /**
     * This method checks if a String contains only numbers
     */
    public boolean containsOnlyNumbers(final String str) {

        //It can't contain only numbers if it's null or empty...
        if (str == null || str.length() == 0) {
            return false;
        }

        final int max = str.length();
        for (int i = 0; i < max; i++) {

            //If we find a non-digit character we return false.
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if an ISSN is valid
     * 
     * @param String issn
     * @return boolean check
     */
    public boolean isValidIssn(final String issn) {

        boolean check = false;
        String kontrollziffer = "";

        try {

            if (issn.length() == 9 && issn.substring(4, 5).equals("-")) {
                final int pos8 = Integer.valueOf(issn.substring(0, 1));
                final int pos7 = Integer.valueOf(issn.substring(1, 2));
                final int pos6 = Integer.valueOf(issn.substring(2, 3));
                final int pos5 = Integer.valueOf(issn.substring(3, 4));
                final int pos4 = Integer.valueOf(issn.substring(5, 6));
                final int pos3 = Integer.valueOf(issn.substring(6, 7));
                final int pos2 = Integer.valueOf(issn.substring(7, 8));
                final String pos1 = issn.substring(8);

                final int sum = pos8 * 8 + pos7 * 7 + pos6 * 6 + pos5 * 5 + pos4 * 4 + pos3 * 3 + pos2 * 2;
                final int checksum = 11 - (sum - (sum / 11) * 11);
                if (checksum == 10 || checksum == 11) {
                    if (checksum == 10) {
                        kontrollziffer = "X";
                    }
                    if (checksum == 11) {
                        kontrollziffer = "0";
                    }
                } else {
                    kontrollziffer = String.valueOf(checksum);
                }
                if (pos1.equalsIgnoreCase(kontrollziffer)) {
                    check = true;
                } else {
                    LOG.error("invalid checksum: " + issn);
                }
            } else {
                if (issn.length() > 0) {
                    LOG.error("invalid checksum: " + issn);
                }
            }
        } catch (final Exception e) {
            LOG.error("isValidIssn: " + issn + "\040" + e.toString());
        }

        return check;
    }

    /**
     * Checks if a String is a valid year. Works from 14th century till 22th
     * century. This sould be usable for some time...
     * 
     * @param String year
     * @return boolean check
     */
    public boolean isYear(final String year) {

        boolean check = false;

        // check if length = 4 and string contains only digits
        if (isExactLength(year, 4) && org.apache.commons.lang.StringUtils.isNumeric(year)) {
            // Search pattern works from 14th century till 22th century. This sould be usable for some time...
            final Pattern p = Pattern
                    .compile("13[0-9]{2}|14[0-9]{2}|15[0-9]{2}|16[0-9]{2}|17[0-9]{2}|18[0-9]{2}|19[0-9]{2}|20[0-9]{2}|21[0-9]{2}");
            final Matcher m = p.matcher(year);
            try {
                if (m.find()) {
                    check = true;
                }
            } catch (final Exception e) {
                LOG.error("isYear(String year): " + year + "\040" + e.toString());
            }
        }

        return check;
    }

}
