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

package ch.dbs.admin;

import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import javax.mail.internet.InternetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.MHelper;
import util.ReadSystemConfigurations;
import ch.dbs.entity.Billing;
import ch.dbs.entity.Konto;
import ch.dbs.entity.Text;
import ch.dbs.form.BillingForm;
import enums.TextType;

/**
 * Erledigt administrative Tätigkeiten zu Abonomenten
 * 
 * @author Pascal Steiner
 */
public final class KontoAdmin {

    private static final int TWO_WEEKS = 14;
    private static final int TEN_DAYS = 14;

    private static final Logger LOG = LoggerFactory.getLogger(KontoAdmin.class);

    /**
     * <p>Automatische Rechnungsstellung sowie Zahlungserinnerungen an Konten
     * vor Konto-Ablaufdatum:</p> 2 Monate vor Ablauf wird die Rechnung erstellt
     * und versendet<br> 1 Monat vor Ablauf wird eine Erinnerung inklusive
     * Rechnungsinformationen versendet<br> 10 Tage vor Ablauf wird eine Warnung
     * inklusive Rechnungsinformationen versendet<br><br>
     * 
     * @author Pascal Steiner
     */
    public void autoBillExpdate() {

        // Datumsformatierung vorbereiten
        final SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd"); // Datumsformatierung für Vergleich
        final SimpleDateFormat expdateformater = new SimpleDateFormat("dd.MM.yyyy"); // Datumsformatierung für Mailtext
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone(ReadSystemConfigurations.getSystemTimezone()));

        // Delta 2 Monate zu aktuellem Monat ausrechnen
        cal.add(Calendar.MONTH, +2);
        final String expDateMysql = formater.format(cal.getTime()); // Datum fuer MYSQL vergleich
        cal.add(Calendar.DAY_OF_MONTH, -TWO_WEEKS);
        final String lastPayDate2Month = expdateformater.format(cal.getTime()); //Letzte Zahlungsmöglichkeit

        // Delta 1 Monat ausrechnen
        cal = new GregorianCalendar(TimeZone.getTimeZone(ReadSystemConfigurations.getSystemTimezone()));
        cal.add(Calendar.MONTH, +1);
        final String expMysql1Month = formater.format(cal.getTime()); // Datum fuer MYSQL vergleich
        cal.add(Calendar.DAY_OF_MONTH, -TWO_WEEKS);
        final String lastPayDate1Month = expdateformater.format(cal.getTime()); // Letzte Zahlungsmöglichkeit

        // 10 Tage vor Kontoablauf ausrechnen (Hinweis, dass es zu Unterbruch kommen kann)
        cal = new GregorianCalendar(TimeZone.getTimeZone(ReadSystemConfigurations.getSystemTimezone()));
        cal.add(Calendar.DAY_OF_MONTH, +TEN_DAYS);
        final String warnDateMysql = formater.format(cal.getTime()); // Datum fuer MYSQL vergleich
        cal.add(Calendar.DAY_OF_MONTH, -TWO_WEEKS);
        final String warnLastPayDate = expdateformater.format(cal.getTime()); // Letzte Zahlungsmöglichkeit vorbei am

        // Abgelaufenen Kontos deaktivieren !!CronJob Muss ca 10:30 laufen
        // (cal.add(Calendar.DAY_OF_MONTH, -1); geht nicht, sonst wird eine neue Rechnung erstellt!!
        cal = new GregorianCalendar(TimeZone.getTimeZone(ReadSystemConfigurations.getSystemTimezone()));
        //      cal.add(Calendar.DAY_OF_MONTH, -1);
        final String exp = formater.format(cal.getTime()); // Datum fuer MYSQL vergleich

        // Datenbankverbindung aufbauen
        final Text cn = new Text();

        try {

            //    Alle Kontos holen
            final List<Konto> expKontos = new Konto().getAllKontos(cn.getConnection());

            // Kontos abarbeiten, ob eine Rechnung erstelt und versendet werden muss
            for (final Konto k : expKontos) {
                if (k.getKontotyp() > 0 && k.getExpdate() != null) {

                    //           neue Rechnung erstellen, wenn das Abo in 2 Monaten ablauuft
                    if (k.getExpdate().toString().equals(expDateMysql)) {
                        this.newKontoAboBill(k, cn.getConnection(), lastPayDate2Month);
                    }

                    // Zahlungserinnerung versenden wenn Abo in 1 Monat ablauuft
                    if (k.getExpdate().toString().equals(expMysql1Month)) {
                        this.sendBillingReminder(k, cn.getConnection(), lastPayDate1Month);
                    }

                    // Zahlungserinnerung versenden, wenn Abo in 10 Tagen ablauft
                    if (k.getExpdate().toString().equals(warnDateMysql)) {
                        this.sendBillingWarning(k, cn.getConnection(), warnLastPayDate);
                    }

                    // Bibliothekskonto deaktivieren, wenn Abo ohne Zahlungseingang abläuft
                    if (k.getExpdate().toString().equals(exp)) {

                        // Meldung an Kunden und Team Doctor-Doc senden
                        this.sendExpireMessage(k, cn.getConnection());

                        //             Faxserver deaktivieren
                        k.setFaxno(null);
                        k.update(cn.getConnection());
                    }
                }
            }
        } catch (final Exception e) {
            LOG.error(e.toString());
            final MHelper mh = new MHelper(e, "Fehler beim Versenden von Rechnungen!");
            mh.sendError();
        } finally {
            cn.close();
        }
    }

    /**
     * Rechnung erstellen und an Kunden versenden
     * 
     * @param k
     * @param cn
     * @param lastpaydate
     */
    private void newKontoAboBill(final Konto k, final Connection cn, final String lastpaydate) throws Exception {
        // Rechnung erstellen
        Billing b = null;
        //TODO: Nullpointer exception bei Kontotyp 1! noch beheben!
        //    if (k.getKontotyp() == 1)  { b = new Billing(k, new Text(cn, "1 Jahr "
        // + ReadSystemConfigurations.getApplicationName() + " Enhanced") , Double.valueOf("120"), "CHF"); }
        if (k.getKontotyp() == 2) {
            b = new Billing(k, new Text(cn, TextType.BILLING_REASON, "1 Jahr Doctor-Doc Enhanced plus Fax to Mail"),
                    Double.valueOf("210"), "CHF");
        }
        if (k.getKontotyp() == 3) {
            b = new Billing(k, new Text(cn, TextType.BILLING_REASON, "3 Monate Doctor-Doc Enhanced plus Fax to Mail"),
                    Double.valueOf("90"), "CHF");
        }

        if (b != null) {
            b.save(cn);
            final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
            // Mailtext vorbereiten
            BillingForm bf = new BillingForm();
            bf.setManuelltext("Ihr Faxserver auf doctor-doc.com läuft am " + sdf.format(k.getExpdate()) + " ab.");
            bf.setBillingtext("\n\nUm weiterhin optimal von " + ReadSystemConfigurations.getApplicationName()
                    + " profitieren zu können, muss der Betrag von " + b.getBetrag() + " " + b.getWaehrung()
                    + " bis spätestens am " + lastpaydate + " auf dem Konto des " + "Vereins "
                    + ReadSystemConfigurations.getApplicationName() + " eintreffen.");
            bf = this.prepareBillingText(k, cn, lastpaydate, bf);

            // Mail senden
            final InternetAddress[] to = new InternetAddress[1];
            if (b.getId() != null) {
                to[0] = new InternetAddress(k.getBibliotheksmail());
                final String subject = "RECHNUNG: Erneuerung Ihres Faxservers bei "
                        + ReadSystemConfigurations.getApplicationName() + ". Ablaufdatum: "
                        + sdf.format(k.getExpdate());
                final MHelper mh = new MHelper(to, subject, bf.getBillingtext());
                mh.send();
            } else { // Hinweis senden falls keine Rechnung erstellt werden konnte
                to[0] = new InternetAddress(ReadSystemConfigurations.getErrorEmail());
                final String subject = "WARNUNG: Folgende Rechnung konnte nicht erstellt werden, Kunde "
                        + k.getBibliotheksname()
                        + " ist nicht benachrichtigt worden! Es wurde keine Rechnung gespeichert!";
                final MHelper mh = new MHelper(to, subject, bf.getBillingtext());
                mh.send();
            }
        }
    }

    /**
     * Rechnungstext vorbereiten / korrigieren
     * 
     * @param k
     * @param cn
     * @param BillingForm
     * @param lastpaydate
     */
    public BillingForm prepareBillingText(final Konto k, final Connection cn, final String lastpaydate, BillingForm bf) {

        if (bf == null) {
            bf = new BillingForm();
        }

        // Rechnungsobjekt vorbereiten, noch nicht speichern
        // da dies erst geschieht wenn die Rechnung wirklich versendet wird.
        Billing b = new Billing();
        b = b.getLastBilling(k, cn);
        if (b == null) {
            if (k.getKontotyp() == 0 || k.getKontotyp() == 1) {
                b = new Billing(k, new Text(cn, TextType.BILLING_REASON, "Dienstleistung"), Double.valueOf("0"), "CHF");
            }
            if (k.getKontotyp() == 2) {
                b = new Billing(k,
                        new Text(cn, TextType.BILLING_REASON, "1 Jahr Doctor-Doc Enhanced plus Fax to Mail"),
                        Double.valueOf("210"), "CHF");
            }
            if (k.getKontotyp() == 3) {
                b = new Billing(k, new Text(cn, TextType.BILLING_REASON,
                        "3 Monate Doctor-Doc Enhanced plus Fax to Mail"), Double.valueOf("90"), "CHF");
            }
            if (k.getKontotyp() == 4) {
                b = new Billing(k, new Text(cn, TextType.BILLING_REASON, "1 Jahr Doctor-Doc Premium"),
                        Double.valueOf("400"), "CHF");
            }
        }

        //Betrag aendern
        if (bf.getBetrag() != 0.0) {
            b.setBetrag(bf.getBetrag());
        }

        //Waehrung aendern
        if (bf.getWaehrung() != null && !"".equals(bf.getWaehrung())) {
            b.setWaehrung(bf.getWaehrung());
        }

        //    Rechnungsdatum aendern
        if (bf.getRechnungsdatum() != null && !"".equals(bf.getWaehrung())) {
            b.setRechnungsdatum(bf.getRechnungsdatum());
        }

        // Rechnungsgrund aendern
        if (bf.getRechnungsgrundid() != null && bf.getRechnungsgrundid() != 0) {
            b.setRechnungsgrund(new Text(cn, bf.getRechnungsgrundid(), TextType.BILLING_REASON));
        }

        bf.setBill(b);

        //Rechnungseinleitung vorbereiten
        final StringBuffer t = new StringBuffer(500);
        final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        t.append("RECHNUNG\n\nSehr geehrte Damen und Herren");

        //Optionaler Rechnungstext ergänzen (nur für Mail, wird nicht gespeichert
        if (bf.getManuelltext() != null) {
            if (!bf.getManuelltext().equals("")) {
                t.append("\n\n");
                t.append(bf.getManuelltext());
            } else {
                // Bestehende Kunden auf das Ablaufdatum des Kontos hinweisen, nur wenn
                // kein manueller Rechnungstext ergänzt werden soll
                if (k.getKontotyp() != 0 && k.getExpdate() != null) {
                    t.append("\n\nIhr Faxserver auf doctor-doc.com läuft am ");
                    t.append(sdf.format(k.getExpdate()));
                    t.append(" ab.\n\nBitte  überweisen Sie den Betrag innert 30 Tagen und mit "
                            + "Angabe der Rechnungsnummer.\n");
                }
            }
        }

        t.append("\n\nRechnungsinformationen:\nBetrag: ");
        t.append(b.getBetrag());
        t.append(' ');
        t.append(b.getWaehrung());
        t.append("\nRechnungsnummer: ");
        t.append(b.getRechnungsnummer());
        t.append("\nRechnungsdatum: ");
        t.append(sdf.format(b.getRechnungsdatum()));
        t.append("\nRechnungsgrund: ");
        t.append(b.getRechnungsgrund().getInhalt());

        //    Zahlungsinformationen hinzufügen
        t.append(getBillingInfos());

        // Hinweis Zahlungskreuzung einschieben
        if (bf.getBillingtext() != null && !bf.getBillingtext().equals("")) {
            t.append("\n\n");
            t.append(bf.getBillingtext());
        }
        t.append("\n\nFreundliche Grüsse:\n\nIhr Team ");
        t.append(ReadSystemConfigurations.getApplicationName());

        final Text txt = new Text();
        bf.setRechnungsgrundliste(txt.getText(TextType.BILLING_REASON, cn));

        bf.setBillingtext(t.toString());

        return bf;
    }

    /**
     * Zahlungserinnerung versenden
     * 
     * @param k
     * @param cn
     * @param lastpaydate
     */
    private void sendBillingReminder(final Konto k, final Connection cn, final String lastpaydate) throws Exception {

        // Offene Rechnung heraussuchen
        Billing b = new Billing();
        b = b.getLastBilling(k, cn);

        //       Mailtext vorbereiten
        final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        BillingForm bf = new BillingForm();

        final InternetAddress[] to = new InternetAddress[1];

        if (b != null) {
            to[0] = new InternetAddress(k.getBibliotheksmail());
            bf.setManuelltext("Ihr Konto auf doctor-doc.com läuft am " + sdf.format(k.getExpdate()) + " ab.");
            bf.setBillingtext("\n\nUm weiterhin vom Faxserver auf " + ReadSystemConfigurations.getApplicationName()
                    + " profitieren zu können, muss der Betrag von " + b.getBetrag() + " " + b.getWaehrung()
                    + " bis spätestens am " + lastpaydate + " auf dem Konto des Vereins "
                    + ReadSystemConfigurations.getApplicationName() + " eintreffen."
                    + "\n\nSollte sich ihre Zahlung mit dieser Mail gekreuzt haben, betrachten Sie dieses Mail "
                    + "bitte als gegenstandslos."); // Hinweis Zahlungskreuzung, laspaydate
            bf = this.prepareBillingText(k, cn, lastpaydate, bf);

            final String subject = "Hinweis: Ihr Faxserver auf doctor-doc.com laeuft am " + sdf.format(k.getExpdate())
                    + " ab";

            // Rechnung versenden
            final MHelper mh = new MHelper(to, subject, bf.getBillingtext());
            mh.send();
        } else { // Hinweis versenden wenn keine offene Zahlung gefunden wurde
            to[0] = new InternetAddress(ReadSystemConfigurations.getErrorEmail());
            final String subject = "ZAHLUNGSERINNERUNG 30 Tage vor Ablauf: von Kunde " + k.getBibliotheksname()
                    + " musste nicht versendet werden.";
            final String text = "Keine offene Rechnung gefunden, alles bezahlt!\n\nBitte Kontoablaufdatum noch richtig setzen, "
                    + "das ist vermutlich beim Zahlungseingang vergessen gegangen... ";
            final MHelper mh = new MHelper(to, subject, text);
            mh.send();
        }
    }

    /**
     * Warntext versenden
     * 
     * @param k
     * @param cn
     * @param expdate
     * @param lastpaydate
     */
    private void sendBillingWarning(final Konto k, final Connection cn, final String lastpaydate) throws Exception {

        //       Offene Rechnung heraussuchen
        Billing b = new Billing();
        b = b.getLastBilling(k, cn);

        //      Mailtext vorbereiten
        BillingForm bf = new BillingForm();

        final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

        final InternetAddress[] to = new InternetAddress[1];

        if (b != null) {
            to[0] = new InternetAddress(k.getBibliotheksmail());
            bf.setManuelltext("Wir konnten leider keinen rechtzeitigen Zahlungseingang ihres ausstehenden Betrages bei "
                    + "uns verzeichnen. Wir weisen Sie darauf hin, dass wir aus administrativen Gründen nicht "
                    + "gewährleisten können, dass Ihnen ihr Faxserver unterbrechungsfrei zur Verfügung steht. "
                    + "Sobald der offene Betrag bei uns eingegangen ist, reaktivieren wir Ihnen Ihr, in der "
                    + "Zwischenzeit ggf. deaktivierter, Faxserver auf doctor-doc.com wieder");
            bf.setBillingtext("Sollte sich ihre Zahlung mit dieser Mail gekreuzt haben, betrachten Sie dieses Mail "
                    + "bitte als gegenstandslos."); // Hinweis Zahlungskreuzung
            bf = this.prepareBillingText(k, cn, lastpaydate, bf);

            final String subject = "Hinweis: Ihr Faxserver auf doctor-doc.com laeuft ab am "
                    + sdf.format(k.getExpdate());

            //       Rechnung versenden
            final MHelper mh = new MHelper(to, subject, bf.getBillingtext());
            mh.send();
        } else { // Hinweis versenden wenn keine offene Zahlung gefunden wurde
            to[0] = new InternetAddress(ReadSystemConfigurations.getErrorEmail());
            final String subject = "LETZTE ZAHLUNGSERINNERUNG: von Kunde " + k.getBibliotheksname()
                    + " musste nicht versendet werden.!";
            final String text = "Keine offene Rechnung gefunden, alles bezahlt!\n\nBitte Kontoablaufdatum noch richtig setzen, "
                    + "das ist vermutlich beim Zahlungseingang vergessen gegangen... ";
            final MHelper mh = new MHelper(to, subject, text);
            mh.send();
        }
    }

    /**
     * Versendet die Meldung dass das Bibliothekskonto deaktiviert wurde
     * inklusive nochmals Zahlungsdetails und dass das Konto bei Zahlungseingang
     * wieder reaktiviert wird.
     * 
     * @param Konto
     */
    private void sendExpireMessage(final Konto k, final Connection cn) throws Exception {

        //       Offene Rechnung heraussuchen
        Billing b = new Billing();
        b = b.getLastBilling(k, cn);

        // Hinweismeldung vorbereiten
        BillingForm bf = new BillingForm();
        if (b != null) {
            bf.setManuelltext("Der Faxserver ihres Bibliothekskonto wurde automatisch deaktiviert, da kein "
                    + "rechtzeitiger Zahlungseingang der unten stehenden Rechnung verzeichnet werden konnte. "
                    + "Sobald die Rechnung beglichen wird, können wir ihren Faxserver wieder freischalten "
                    + "(Keine Nummergarantie).");
            bf = this.prepareBillingText(k, cn, null, bf);
            //      Eigentlich sollte man nie ohne offene Rechnung hier landen, ansonsten ist vermutlich
            // das Expire Date des Kontos falsch gesetzt
        } else {
            bf.setBillingtext("Sehr geehrte Damen und Herren\n\n"
                    + "Leider mussten wir aufgrund eines internen Fehlers in "
                    + ReadSystemConfigurations.getApplicationName() + " ihren Faxserver deaktivieren. "
                    + "Wir bemühen uns, diesen Fehler schnellstmöglich zu beheben um den Faxserver wieder frei "
                    + "geben zu können.\n\n" + "Freundliche Grüsse:\n\n" + "Ihr Team "
                    + ReadSystemConfigurations.getApplicationName());
        }

        // Hinweis an Kunden versenden
        final InternetAddress[] to = new InternetAddress[2];
        to[0] = new InternetAddress(k.getBibliotheksmail()); // customer
        to[1] = new InternetAddress(ReadSystemConfigurations.getBillingEmail()); // billing department
        final MHelper mh = new MHelper(to, "Hinweis: Ihr Faxserver auf doctor-doc.com wurde deaktiviert.",
                bf.getBillingtext());
        mh.send();

    }

    private String getBillingInfos() {

        final StringBuffer t = new StringBuffer(430);
        t.append("\n\n*******************************************************\n" + "Zahlungsinformationen:" + "\n");
        t.append(ReadSystemConfigurations.getApplicationName());
        t.append("\nKonto Nr.:      16 9.350.8040.02\n" + "IBAN:           CH62 0630 0016 9350 8040 2\n"
                + "Bank:           Valiant Bank, Postfach, 3001 Bern\n" + "Bankenclearing: 6300\n"
                + "Postcheckkonto: 30-38112-0\n" + "Swift-Code:     VABECH22\n\n" + "Name und Adresse:\n"
                + "Pascal Steiner\n" + "Pfanne 4\n" + "5032 Aaru Rohr (CH)\n"
                + "*******************************************************");

        return t.toString();
    }
}
