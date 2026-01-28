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

package ch.dbs.actions.openurl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.CodeUrl;
import ch.dbs.actions.bestellung.OrderAction;
import ch.dbs.form.OrderForm;

public class ConvertOpenUrl {
    
    private static final Logger LOG = LoggerFactory.getLogger(ConvertOpenUrl.class);
    
    /**
     * Klasse um OpenURL in "Doctor-Doc" zu übersetzen
     * 
     * @author Markus Fischer
     */
    
    /**
     * Interpretiert aus einem ContextObject ein OrderForm für Doctor-Doc.
     * Relativ komplex und hier mit Vereinfachungen und Anpassungen umgesetzt...
     */
    public OrderForm makeOrderform(final ContextObject co) {
        
        OrderForm of = new OrderForm();
        // OpenURL V1.0 has clear identifiers, so no additional controls for mediatype "Artikel" are necessary
        boolean valFmt = false;
        
        // if we have a SICI, resolve and use it
        if (!co.getRft_sici().equals("")) {
            of = resolveSiciNumber(co.getRft_sici());
        }
        
        // zuerst allfällige Seitenbereiche eruieren => Unterscheidung von Buch / Teilkopie Buch
        
        if (co.getRft_spage() != null && !co.getRft_spage().equals("")) {
            of.setSeiten(co.getRft_spage());
        }
        
        if (co.getRft_epage() != null && !co.getRft_epage().equals("")) {
            // Worldcat repeats the startpage in the endpage
            if (co.getRft_epage().contains("-")) {
                // only use last number
                of.setSeiten(of.getSeiten() + co.getRft_epage().substring(co.getRft_epage().indexOf("-")));
            } else {
                of.setSeiten(of.getSeiten() + "-" + co.getRft_epage());
            }
        }
        
        if (co.getRft_pages() != null && !co.getRft_pages().equals("")) { // überschreibt spage und epage
            of.setSeiten(co.getRft_pages());
        }
        
        // ...nur ausführen, falls Seiten nicht schon gesetzt
        if (co.getRft_tpages() != null && !co.getRft_tpages().equals("")
                && (of.getSeiten() == null || of.getSeiten().equals(""))) {
            of.setSeiten(co.getRft_tpages());
        }
        
        of.setMediatype("Artikel"); // Default-Wert, falls rft_val_fmt, resp. rft.genre nicht analysierbar..
        
        if (co.getRft_val_fmt() != null && !co.getRft_val_fmt().equals("")) {
            if (co.getRft_val_fmt().contains("journal")) {
                of.setMediatype("Artikel");
                valFmt = true;
            }
            if (co.getRft_val_fmt().contains("book")) {
                if ((co.getRft_genre() != null && !co.getRft_genre().equals("") && co.getRft_genre().equalsIgnoreCase(
                        "bookitem"))
                        || !of.getSeiten().equals("")) { // enthält einen Seitenbereich
                    of.setMediatype("Teilkopie Buch");
                } else {
                    of.setMediatype("Buch");
                }
            }
        }
        
        if (co.getRft_genre() != null && !co.getRft_genre().equals("")) { // falls rft_val_fmt fehlt...
            if (co.getRft_genre().toLowerCase().contains("journal")
                    || co.getRft_genre().toLowerCase().contains("article")) {
                of.setMediatype("Artikel");
            }
            if (co.getRft_genre().toLowerCase().contains("book") // wird ggf. von bookitem / chapter überschrieben
                    || co.getRft_genre().toLowerCase().contains("dissertation")) {
                of.setMediatype("Buch");
            }
            if (co.getRft_genre().toLowerCase().contains("bookitem") // hier inoffizieller Identifier
                    || co.getRft_genre().toLowerCase().contains("chapter")) {
                of.setMediatype("Teilkopie Buch");
            }
            // zusätzlich über of Genre mitschicken (es sei denn, es sei überlang...)
            if (co.getRft_genre().length() < 30) {
                of.setGenre(co.getRft_genre());
            }
        }
        
        if (co.getRft_atitle() != null && !co.getRft_atitle().equals("")) {
            if (of.getMediatype().equals("Artikel")) {
                of.setArtikeltitel(removeEndDot(co.getRft_atitle())); // Artikeltitel
            } else {
                if (of.getMediatype().equals("Teilkopie Buch")) {
                    of.setKapitel(removeEndDot(co.getRft_atitle()));
                }
                if (of.getMediatype().equals("Buch")) {
                    of.setBuchtitel(removeEndDot(co.getRft_atitle()));
                }
            }
        }
        
        if (co.getRft_title() != null && !co.getRft_title().equals("")) { // nur OpenURL 0.1
            if (of.getMediatype().equals("Artikel")) {
                of.setZeitschriftentitel(toNormalLetters(removeEndDot(co.getRft_title()))); // Zeitschrift
            } else {
                of.setBuchtitel(toNormalLetters(removeEndDot(co.getRft_title()))); // Buch
            }
        }
        
        if (co.getRft_jtitle() != null && !co.getRft_jtitle().equals("")) { // OpenURL 1.0, überschreibt Rft_title
            if (of.getMediatype().equals("Artikel")) {
                of.setZeitschriftentitel(toNormalLetters(removeEndDot(co.getRft_jtitle()))); // Zeitschrift
            } else {
                of.setBuchtitel(toNormalLetters(removeEndDot(co.getRft_jtitle()))); // Buch [unwahrscheinlich]
            }
        }
        
        if (co.getRft_btitle() != null && !co.getRft_btitle().equals("")) {
            of.setBuchtitel(toNormalLetters(removeEndDot(co.getRft_btitle())));
        }
        
        if (co.getRft_pub() != null && !co.getRft_pub().equals("")) {
            of.setVerlag(co.getRft_pub());
        }
        
        if (co.getRft_publisher() != null && !co.getRft_publisher().equals("")
        // ...nur ausführen, falls Verlag noch nicht gesetzt
                && (of.getVerlag() == null || of.getVerlag().equals(""))) {
            of.setVerlag(co.getRft_publisher());
        }
        
        if (co.getRft_stitle() != null
                && !co.getRft_stitle().equals("")
                && // stitle = Titelabkürzung...
                   // ...nur ausführen, falls Titel noch nicht gesetzt
                (of.getZeitschriftentitel() == null || of.getZeitschriftentitel().equals(""))
                && (of.getBuchtitel() == null || of.getBuchtitel().equals(""))) {
            if (of.getMediatype().equals("Artikel")) {
                of.setZeitschriftentitel(removeEndDot(co.getRft_stitle())); // Zeitschrift
            } else {
                of.setBuchtitel(removeEndDot(co.getRft_stitle())); // Buch [unwahrscheinlich]
            }
        }
        
        if (co.getRft_series() != null && !co.getRft_series().equals("")) { // Serientitel
            if (of.getMediatype().equals("Artikel")) { // unwahrscheinlich
                of.setZeitschriftentitel(of.getZeitschriftentitel() + "\040-\040" + co.getRft_series());
            } else {
                of.setBuchtitel(of.getBuchtitel() + "\040-\040" + co.getRft_series());
            }
            
        }
        
        // if issn or eissn are empty and issn1 or eissn1 are available => set them
        if ((co.getRft_issn() == null || co.getRft_issn().equals(""))
                && (co.getRft_issn1() != null && !co.getRft_issn1().equals(""))) {
            co.setRft_issn(co.getRft_issn1());
        }
        if ((co.getRft_eissn() == null || co.getRft_eissn().equals(""))
                && (co.getRft_eissn1() != null && !co.getRft_eissn1().equals(""))) {
            co.setRft_eissn(co.getRft_eissn1());
        }
        
        if (co.getRft_issn() != null && !co.getRft_issn().equals("")) {
            of.setIssn(normalizeIssn(extractFromSeveralIssns(co.getRft_issn())));
        }
        
        if (co.getRft_eissn() != null && !co.getRft_eissn().equals("")
        // ...nur ausführen, falls P-ISSN nicht schon gesetzt
                && (of.getIssn() == null || of.getIssn().equals(""))) {
            of.setIssn(normalizeIssn(extractFromSeveralIssns(co.getRft_eissn())));
        }
        
        if (co.getRft_isbn() != null && !co.getRft_isbn().equals("")) {
            of.setIsbn(co.getRft_isbn());
        }
        
        if (co.getRft_date() != null && !co.getRft_date().equals("")) {
            final OrderAction orderAction = new OrderAction();
            of.setJahr(orderAction.extractYear(co.getRft_date()));
        }
        
        if (co.getRft_volume() != null && !co.getRft_volume().equals("")) {
            of.setJahrgang(co.getRft_volume());
        }
        
        if (co.getRft_part() != null && !co.getRft_part().equals("")) {
            if (of.getJahrgang() != null && !of.getJahrgang().equals("")) {
                of.setJahrgang(of.getJahrgang() + "\040" + co.getRft_part()); // zum Jahrgang addieren
            } else {
                of.setJahrgang(co.getRft_part());
            }
        }
        
        if (co.getRft_issue() != null && !co.getRft_issue().equals("")) {
            of.setHeft(co.getRft_issue());
        }
        
        if (co.getRft_author() != null && !co.getRft_author().equals("")) {
            of.setAuthor(co.getRft_author());
        }
        
        if (co.getRft_au() != null && !co.getRft_au().equals("")
        // nur ausführen, falls Author nicht schon gesetzt
                && (of.getAuthor() == null || of.getAuthor().equals(""))) {
            of.setAuthor(co.getRft_au());
        }
        
        if (co.getRft_creator() != null && !co.getRft_creator().equals("")
        // inoffizieller Identif. ...nur ausführen, falls Author nicht schon gesetzt
                && (of.getAuthor() == null || of.getAuthor().equals(""))) {
            of.setAuthor(co.getRft_creator());
        }
        
        if (co.getRft_aulast() != null && !co.getRft_aulast().equals("")) {
            final String authorCopy = of.getAuthor(); // bis jetzt bereits vorhandene Einträge kopieren
            // Normalfall (auinit enthält sämtliche Initialen der Vornamen)
            if (co.getRft_auinit() != null && !co.getRft_auinit().equals("")) {
                // falls Vorname vorhanden, hintansetzen...
                of.setAuthor(co.getRft_aulast() + ",\040" + co.getRft_auinit());
            } else { // Spezialfall (Initialen sind aufgeteilt)
                if (co.getRft_aufirst() != null && !co.getRft_aufirst().equals("")) {
                    if (co.getRft_auinitm() != null && !co.getRft_auinitm().equals("")) {
                        // falls Vorname und Mittelinitialen vorhanden, hintansetzen...
                        of.setAuthor(co.getRft_aulast() + ",\040" + co.getRft_aufirst() + "\040" + co.getRft_auinitm());
                    } else {
                        // falls Vorname vorhanden, hintansetzen...
                        of.setAuthor(co.getRft_aulast() + ",\040" + co.getRft_aufirst());
                    }
                } else {
                    of.setAuthor(co.getRft_aulast());
                }
            }
            // Einträge ggf. aneinanderhängen: Vorhandenes und neues Element dürfen nicht identisch sein.
            if (authorCopy != null && !authorCopy.equals("") && !authorCopy.equals(of.getAuthor())) {
                of.setAuthor(of.getAuthor() + "\040;\040" + authorCopy);
            }
        }
        
        if (co.getRft_aucorp() != null && !co.getRft_aucorp().equals("")) {
            if (of.getAuthor() != null && !of.getAuthor().equals("")) {
                of.setAuthor(of.getAuthor() + " ; " + co.getRft_aucorp()); // zum Author addieren
            } else {
                of.setAuthor(co.getRft_aucorp());
            }
        }
        
        if (co.getRft_id() != null) {
            for (final String eachRftID : co.getRft_id()) {
                of = resolveUriScheme(eachRftID, of);
            }
        }
        
        // Kontrollmechanismen
        if (!valFmt && of.getMediatype().equals("Buch") && isBookItem(of)) {
            of.setMediatype("Teilkopie Buch");
        }
        if (!valFmt && of.getMediatype().equals("Artikel") && isBook(of)) {
            of.setMediatype("Buch");
        }
        
        if (!valFmt && of.getMediatype().equals("Artikel") && isBookItem(of)) {
            of.setMediatype("Teilkopie Buch");
            // Nach Umstellen auf Teilkopie sicherstellen dass Kapitel abgefüllt wird
            if (of.getKapitel() == null || of.getKapitel().equals("")) {
                of.setKapitel(co.getRft_atitle());
            }
            // Artikeltitel leeren
            of.setArtikeltitel("");
        }
        // Spezialfall E-Books von Springer
        if (of.getMediatype().equals("Artikel") && isSpringerBook(of) && !of.getDoi().contains("_")) {
            of.setMediatype("Buch");
            // Nach Umstellen auf Buch sicherstellen dass Buchtitel abgefüllt wird
            if (of.getBuchtitel() == null || of.getBuchtitel().equals("")) {
                of.setBuchtitel(co.getRft_atitle());
            }
            // Artikeltitel leeren
            of.setArtikeltitel("");
            if (of.getIsbn() == null || of.getIsbn().equals("")) {
                of.setIsbn(getIsbnSpringerBook(of.getDoi()));
            }
        }
        if (of.getMediatype().equals("Artikel") && isSpringerBook(of) && of.getDoi().contains("_")) {
            of.setMediatype("Teilkopie Buch");
            // Nach Umstellen auf Teilkopie sicherstellen dass Kapitel abgefüllt wird
            if (of.getKapitel() == null || of.getKapitel().equals("")) {
                of.setKapitel(co.getRft_atitle());
            }
            // Nach Umstellen auf Teilkopie sicherstellen dass Buchtitel abgefüllt wird
            if (of.getBuchtitel() == null || of.getBuchtitel().equals("")) {
                of.setBuchtitel(co.getRft_title());
            }
            // Artikeltitel leeren
            of.setArtikeltitel("");
            if (of.getIsbn() == null || of.getIsbn().equals("")) {
                of.setIsbn(getIsbnSpringerBook(of.getDoi()));
            }
        }
        
        if (of.getMediatype().equals("Artikel") && of.getIssn().length() == 0) {
            of.setFlag_noissn(true);
        }
        
        // ggf. Sprache des Artikels setzen
        if (co.getRft_language() != null && !co.getRft_language().equals("")
        // nur Sprachangabe wenn NICHT englisch
                && !co.getRft_language().equalsIgnoreCase("English")) {
            of.setLanguage(co.getRft_language());
        }
        
        return of;
    }
    
    /**
     * Stellt aus einem Ordeform ein ContextObject her.
     */
    public ContextObject makeContextObject(final OrderForm of, final String encoding) {
        
        final ContextObject co = new ContextObject();
        final CodeUrl codeUrl = new CodeUrl();
        
        if (of.getMediatype().equals("Artikel")) {
            co.setRft_val_fmt(codeUrl.encode("info:ofi/fmt:kev:mtx:journal", encoding));
        }
        if (of.getMediatype().contains("Buch")) {
            co.setRft_val_fmt(codeUrl.encode("info:ofi/fmt:kev:mtx:book", encoding));
        }
        
        // Aus Kompatibilität mit OpenURl 01.1 momentan ausserhalb dieser Methode zu implementieren
        // co.setRfr_id(UrlCode.encode("DRDOC:doctor-doc"));
        
        if (of.getMediatype().equals("Artikel")) {
            co.setRft_genre(codeUrl.encode("article", encoding));
        }
        if (of.getMediatype().equals("Buch")) {
            co.setRft_genre(codeUrl.encode("book", encoding));
        }
        if (of.getMediatype().equals("Teilkopie Buch")) {
            co.setRft_genre(codeUrl.encode("bookitem", encoding));
        }
        
        co.setRft_atitle(codeUrl.encode(of.getArtikeltitel(), encoding));
        co.setRft_btitle(codeUrl.encode(of.getBuchtitel(), encoding));
        co.setRft_jtitle(codeUrl.encode(of.getZeitschriftentitel(), encoding));
        // fragwürdig, da vermutlich nicht immer im korrekten Format...
        co.setRft_pub(codeUrl.encode(of.getVerlag(), encoding));
        co.setRft_issn(codeUrl.encode(of.getIssn(), encoding));
        co.setRft_isbn(codeUrl.encode(of.getIsbn(), encoding));
        co.setRft_date(codeUrl.encode(of.getJahr(), encoding));
        co.setRft_volume(codeUrl.encode(of.getJahrgang(), encoding));
        co.setRft_issue(codeUrl.encode(of.getHeft(), encoding));
        co.setRft_pages(codeUrl.encode(of.getSeiten(), encoding));
        co.setRft_spage(codeUrl.encode(extractStartPage(of.getSeiten()), encoding));
        co.setRft_author(codeUrl.encode(of.getAuthor(), encoding));
        
        // enthält uri-infos wie pmid, doi, lccn etc.: // http://www.info-uri.info/registry/
        // TODO: private List rft_id;
        
        return co;
    }
    
    /**
     * Stellt aus einem Orderform einen "Doctor-Doc-String" für interne
     * Get-Methoden her.
     */
    public String makeGetMethodString(final OrderForm of) {
        
        final StringBuffer sb = new StringBuffer(800);
        final CodeUrl codeUrl = new CodeUrl();
        sb.append("mediatype="); // Default "Artikel" (s.a. OrderForm)
        sb.append(codeUrl.encode(of.getMediatype(), "UTF-8"));
        if (of.getIssn() != null && !of.getIssn().equals("")) {
            sb.append("&issn=");
            sb.append(codeUrl.encode(of.getIssn(), "UTF-8"));
        }
        if (of.getJahr() != null && !of.getJahr().equals("")) {
            sb.append("&jahr=");
            sb.append(codeUrl.encode(of.getJahr(), "UTF-8"));
        }
        if (of.getJahrgang() != null && !of.getJahrgang().equals("")) {
            sb.append("&jahrgang=");
            sb.append(codeUrl.encode(of.getJahrgang(), "UTF-8"));
        } // könnte Leerschläge und Interpunktion enthalten
        if (of.getHeft() != null && !of.getHeft().equals("")) {
            sb.append("&heft=");
            sb.append(codeUrl.encode(of.getHeft(), "UTF-8"));
        } // Leerschläge etc.
        if (of.getSeiten() != null && !of.getSeiten().equals("")) {
            sb.append("&seiten=");
            sb.append(codeUrl.encode(of.getSeiten(), "UTF-8"));
        } // Leerschläge etc.
        if (of.getIsbn() != null && !of.getIsbn().equals("")) {
            sb.append("&isbn=");
            sb.append(of.getIsbn());
        }
        if (of.getArtikeltitel() != null && !of.getArtikeltitel().equals("")) {
            sb.append("&artikeltitel=");
            sb.append(codeUrl.encode(of.getArtikeltitel(), "UTF-8"));
        }
        if (of.getZeitschriftentitel() != null && !of.getZeitschriftentitel().equals("")) {
            sb.append("&zeitschriftentitel=");
            sb.append(codeUrl.encode(of.getZeitschriftentitel(), "UTF-8"));
        }
        if (of.getAuthor() != null && !of.getAuthor().equals("")) {
            sb.append("&author=");
            sb.append(codeUrl.encode(of.getAuthor(), "UTF-8"));
        }
        if (of.getKapitel() != null && !of.getKapitel().equals("")) {
            sb.append("&kapitel=");
            sb.append(codeUrl.encode(of.getKapitel(), "UTF-8"));
        }
        if (of.getBuchtitel() != null && !of.getBuchtitel().equals("")) {
            sb.append("&buchtitel=");
            sb.append(codeUrl.encode(of.getBuchtitel(), "UTF-8"));
        }
        if (of.getVerlag() != null && !of.getVerlag().equals("")) {
            sb.append("&verlag=");
            sb.append(codeUrl.encode(of.getVerlag(), "UTF-8"));
        }
        if (of.getRfr_id() != null && !of.getRfr_id().equals("")) {
            sb.append("&rfr_id=");
            sb.append(codeUrl.encode(of.getRfr_id(), "UTF-8"));
        }
        if (of.getForuser() != null && !of.getForuser().equals("0")) {
            // falls in bestellform.sendOrder bereits ein User anhand der Email gefunden wurde
            sb.append("&foruser=");
            sb.append(codeUrl.encode(of.getForuser(), "UTF-8"));
        }
        if (of.getGenre() != null && !of.getGenre().equals("")) {
            sb.append("&genre=");
            sb.append(codeUrl.encode(of.getGenre(), "UTF-8"));
        }
        if (of.getPmid() != null && !of.getPmid().equals("")) {
            sb.append("&pmid=");
            sb.append(codeUrl.encode(of.getPmid(), "UTF-8"));
        }
        if (of.getDoi() != null && !of.getDoi().equals("")) {
            sb.append("&doi=");
            sb.append(codeUrl.encode(of.getDoi(), "UTF-8"));
        }
        if (of.getSici() != null && !of.getSici().equals("")) {
            sb.append("&sici=");
            sb.append(codeUrl.encode(of.getSici(), "UTF-8"));
        } // kann Sonderzeichen enthalten
        if (of.getZdbid() != null && !of.getZdbid().equals("")) {
            sb.append("&zdbid=");
            sb.append(codeUrl.encode(of.getZdbid(), "UTF-8"));
        }
        if (of.getLccn() != null && !of.getLccn().equals("")) {
            // http://www.info-uri.info/registry/
            sb.append("&lccn=");
            sb.append(codeUrl.encode(of.getLccn(), "UTF-8"));
        }
        
        return sb.toString();
    }
    
    /**
     * Holt den Inhalt aus einem URI-Schema und legt ihn ins OrderForm
     */
    private OrderForm resolveUriScheme(final String input, final OrderForm of) {
        
        // http://www.info-uri.info/registry/
        
        try {
            
            if (input.contains("info:doi/")) {
                of.setDoi(input.substring(input.indexOf("info:doi/")));
            }
            if (input.contains("info:pmid/")) {
                of.setPmid(input.substring(input.indexOf("info:pmid/")));
            }
            if (input.contains("info:sici/")) {
                of.setSici(input.substring(input.indexOf("info:sici/")));
            }
            if (input.contains("info:lccn/")) {
                of.setLccn(input.substring(input.indexOf("info:lccn/")));
            }
            
        } catch (final Exception e) {
            LOG.error("resolveUriScheme: " + e.toString());
        }
        
        return of;
    }
    
    /**
     * Löst eine SICI-Number auf und legt die Angaben ins OrderForm
     */
    private OrderForm resolveSiciNumber(final String sici) {
        
        final OrderForm of = new OrderForm();
        
        // http://en.wikipedia.org/wiki/SICI
        // ANSI/NISO Z39.56
        // 0002-8231(199412)45:10<737:TIODIM>2.3.TX;2-M => Abstract from Lynch, Clifford A.
        // "The Integrity of Digital Information; Mechanics and Definitional Issues." JASIS 45:10 (Dec. 1994) p. 737-44
        
        try {
            
            if (sici.contains("(")) {
                final int start = sici.indexOf('(');
                String tmp = "";
                if (start > 0) {
                    of.setIssn(normalizeIssn(sici.substring(0, start)));
                } // ISSN-Nummer steht am Anfang
                if (sici.substring(start).contains(")")) {
                    tmp = sici.substring(start, sici.indexOf(')'));
                }
                // Jahreszahl sind die ersten vier Stellen in der Klammer
                if (tmp.length() > 4) {
                    of.setJahr(tmp.substring(1, 5));
                }
            }
            if (sici.contains(")") && sici.contains("<") && (sici.indexOf(')') < sici.indexOf('<'))) {
                final String segment = sici.substring(sici.indexOf(')') + 1, sici.indexOf('<')); // enthält Jg:Heft
                if (segment.contains(":")) {
                    of.setJahrgang(segment.substring(0, segment.indexOf(':')));
                    of.setHeft(segment.substring(segment.indexOf(':') + 1));
                }
            }
            if (sici.contains("<") && sici.substring(sici.indexOf('<')).contains(":")) {
                // Seiten nach < bis :
                of.setSeiten(sici.substring(sici.indexOf('<') + 1, sici.indexOf(':', sici.indexOf('<'))));
            }
            
        } catch (final Exception e) {
            LOG.error("resolveSiciNumber: " + sici + "\040" + e.toString());
        }
        
        return of;
    }
    
    /**
     * Prüft, ob Bestellobjekt logischerweise ein Buch sein muss
     */
    private boolean isBook(final OrderForm of) {
        
        boolean check = false;
        
        try {
            
            if (of.getIsbn() != null && !of.getIsbn().equals("")
                    && (of.getArtikeltitel() == null || of.getArtikeltitel().equals(""))
                    // ISBN vorhanden, aber kein Kapitel (hier noch Artikeltitel) oder spezifische Seitenangaben
                    && (of.getSeiten() == null || of.getSeiten().equals(""))) {
                check = true;
            }
            
            if (of.getBuchtitel() != null && !of.getBuchtitel().equals("")
                    && (of.getArtikeltitel() == null || of.getArtikeltitel().equals(""))
                    // Buchtitel vorhanden, aber kein Kapitel (hier noch Artikeltitel) oder spezifische Seitenangaben
                    && (of.getSeiten() == null || of.getSeiten().equals(""))) {
                check = true;
            }
            
        } catch (final Exception e) {
            LOG.error("isBook: " + e.toString());
        }
        
        return check;
    }
    
    /**
     * Prüft, ob das Bestellobjekt logischerweise eine Teilkopie Buch sein muss
     */
    private boolean isBookItem(final OrderForm of) {
        
        boolean check = false;
        
        try {
            
            if (of.getIsbn() != null && !of.getIsbn().equals("")
                    && ((of.getKapitel() != null && !of.getKapitel().equals(""))
                    // ISBN vorhanden und Kapitel oder Artikeltitel oder spezifische Angaben vorhanden
                    || (of.getSeiten() != null && !of.getSeiten().equals("")))) {
                check = true;
            }
            
            if (of.getBuchtitel() != null
                    && !of.getBuchtitel().equals("")
                    && ((of.getKapitel() != null && !of.getKapitel().equals(""))
                            || (of.getArtikeltitel() != null && !of.getArtikeltitel().equals(""))
                    // Buchtitel vorhanden und Kapitel oder Artikeltitel oder spezifische Angaben vorhanden
                    || (of.getSeiten() != null && !of.getSeiten().equals("")))) {
                check = true;
            }
            
        } catch (final Exception e) {
            LOG.error("isBookItem: " + e.toString());
        }
        
        return check;
    }
    
    /**
     * Prüft, ob das Bestellobjekt ein Springer-Buch ist
     */
    private boolean isSpringerBook(final OrderForm of) {
        
        boolean check = false;
        
        try {
            
            if (of.getIssn() == null || "".equals(of.getIssn())) { // treat Springer-Books with ISSN as article / journal
            
                if (of.getDoi().contains("10.1007/3-") || of.getDoi().contains("10.1007/978-3-") || // deutsch / spanisch
                        of.getDoi().contains("10.1007/1-") || of.getDoi().contains("10.1007/978-1-") || // englisch
                        of.getDoi().contains("10.1007/0-") || of.getDoi().contains("10.1007/978-0-") || // englisch
                        of.getDoi().contains("10.1007/2-") || of.getDoi().contains("10.1007/978-2-") || // französisch
                        of.getDoi().contains("10.1007/90-") || of.getDoi().contains("10.1007/978-90-") || // niederländisch
                        of.getDoi().contains("10.1007/88-") || of.getDoi().contains("10.1007/978-88-")) { // ital. / port.
                    check = true;
                }
            }
            
        } catch (final Exception e) {
            LOG.error("isSpringerBook:" + e.toString());
        }
        
        return check;
    }
    
    /**
     * Holt aus einer doi von Springer die ISBN
     */
    private String getIsbnSpringerBook(final String doi) {
        
        String isbn = "";
        
        try {
            
            if (doi.contains("_")) {
                isbn = doi.substring(doi.indexOf("10.1007/") + 8, doi.indexOf('_'));
            } else {
                isbn = doi.substring(doi.indexOf("10.1007/") + 8);
            }
            
        } catch (final Exception e) {
            LOG.error("getIsbnSpringerBook: " + doi + "\040" + e.toString());
        }
        
        return isbn;
    }
    
    /**
     * gets an ISSN out of a String with several ISSNs e.g. Refworks is sending
     * multiple ISSN in the param: issn=1234-1234; 2345-2345
     */
    private String extractFromSeveralIssns(String input) {
        
        try {
            // must have at least the lenght of two ISSNs plus a separation character
            if (input != null && input.length() > 19 && (input.contains(";") || input.contains(","))) { // Separation character is comma or semicolon
                if (input.contains(";")) {
                    if (input.indexOf(';') > 8) { // must have a ISSN number before the separtion character
                        input = input.substring(0, input.indexOf(';')).trim(); // take the first ISSN
                    }
                } else if (input.contains(",") && input.indexOf(',') > 8) {
                    // must have a ISSN number before the separtion character
                    input = input.substring(0, input.indexOf(',')).trim(); // take the first ISSN
                }
            }
            
        } catch (final Exception e) {
            LOG.error("extractFromSeveralIssns:\040" + input + "\040" + e.toString());
        }
        
        return input;
    }
    
    /**
     * Bringt eine ISSN ohne Bindestrich in die "normale Form"
     */
    public String normalizeIssn(String input) {
        
        try {
            if (input != null && input.length() == 8 && !input.contains("-")) {
                input = input.substring(0, 4) + "-" + input.substring(4, 8);
            }
            
        } catch (final Exception e) {
            LOG.error("normalizeIssn:\040" + input + "\040" + e.toString());
        }
        
        return input;
    }
    
    /**
     * Entfernt Punkt am Ende eines Strings
     */
    private String removeEndDot(String input) {
        
        try {
            
            if (input != null && !input.equals("") && input.endsWith(".")) {
                
                final int l = input.length();
                // falls letztes Zeichen "." => eliminieren
                if (input.lastIndexOf('.') == l - 1) {
                    input = input.substring(0, input.lastIndexOf('.'));
                }
                
            }
            
        } catch (final Exception e) {
            LOG.error("removeEndDot: " + input + "\040" + e.toString());
        }
        
        return input;
    }
    
    public String extractStartPage(final String input) {
        String result = "";
        if (input != null) {
            try {
//                final Pattern z = Pattern.compile("\\d+");
            	// use word characters, since we see numberings like e1254-56
                final Pattern z = Pattern.compile("\\w+");
                final Matcher w = z.matcher(input);
                
                if (w.find()) { // only extract first number
                    result = input.substring(w.start(), w.end());
                }
            } catch (final Exception e) {
                LOG.error("extractStartPage: " + input + "\040" + e.toString());
            }
        }
        
        return result;
    }
    
    /**
     * Setzt einen String aus lauter GROSSBUCHSTABEN in die englische
     * Schreibeweise: 1. Buchstabe gross, alles andere klein. Z.B. aus METHODS
     * IN ENZYMOLOGY => wird Methods in enzymology
     */
    private String toNormalLetters(String input) {
        
        try {
            
            if (input != null && input.length() != 0 && !checkLowercase(input)) {
                
                input = input.toLowerCase();
                input = input.substring(0, 1).toUpperCase() + input.substring(1);
                
            }
            
        } catch (final Exception e) {
            LOG.error("toNormalLetters: " + input + "\040" + e.toString());
        }
        
        return input;
    }
    
    /**
     * Testet ob ein String Kleinbuchstaben enthält
     */
    private boolean checkLowercase(final String input) {
        
        boolean check = false;
        
        if (input != null && input.length() != 0) {
            
            try {
                
                final Pattern z = Pattern.compile("\\p{Lower}");
                final Matcher w = z.matcher(input);
                
                if (w.find()) { // Testen, ob String Kleinbuchstaben enthält...
                    check = true;
                }
                
            } catch (final Exception e) {
                LOG.error("testLowercase: " + input + "\040" + e.toString());
            }
        }
        
        return check;
    }
    
}
