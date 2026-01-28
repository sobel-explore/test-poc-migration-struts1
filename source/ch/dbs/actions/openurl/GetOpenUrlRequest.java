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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.Auth;
import ch.dbs.actions.bestellung.DOI;
import ch.dbs.actions.bestellung.Pubmed;
import ch.dbs.entity.Text;
import ch.dbs.form.ActiveMenusForm;
import ch.dbs.form.ErrorMessage;
import ch.dbs.form.OrderForm;
import ch.dbs.form.UserInfo;
import enums.Result;

/**
 * Methode um OpenUrl-Requests zu empfangen
 * 
 * @author Markus Fischer
 */
public final class GetOpenUrlRequest extends Action {
    
    private static final Logger LOG = LoggerFactory.getLogger(GetOpenUrlRequest.class);
    
    /**
     * empfängt OpenURL-Requests und stellt ein OrderForm her
     */
    public ActionForward execute(final ActionMapping mp, final ActionForm form, final HttpServletRequest rq,
            final HttpServletResponse rp) {
        
        final Auth auth = new Auth();
        String forward = Result.FAILURE.getValue();
        OrderForm of = (OrderForm) form;
        of.setResolver(true); // markiert, dass Angaben bereits aufgelöst wurden
        final ConvertOpenUrl openurlConv = new ConvertOpenUrl();
        final OpenUrl openurl = new OpenUrl();
        final DOI doi = new DOI();
        final Pubmed pubmed = new Pubmed();
        
        final String query = rq.getQueryString();
        
        try {
            
            // es gibt drei mögliche Zugriffsformen, ohne eingeloggt zu sein. Priorisiert wie folgt:
            // 1. Kontokennung
            // 2. IP-basiert
            // 3. Broker-Kennung (z.B. Careum Explorer)
            
            final Text requester = auth.grantAccess(rq);
            
            // Nicht eingeloggt. IP-basiert, Kontokennung oder Brokerkennung
            if (requester != null && requester.getInhalt() != null) {
                
                forward = Result.SUCCESS.getValue();
                
                rq.setAttribute("ip", requester); // Text mit Konto in Request setzen
                of.setBibliothek(requester.getKonto().getBibliotheksname());
                
                if (query != null && (query.contains("sid=Entrez:PubMed&id=pmid:") // Übergabe direkt von Pubmed...
                        || query.contains("sid=google&id=pmid:"))) { // ...oder von Google Scholar
                    String pmid = "";
                    if (query.contains("sid=Entrez:PubMed&id=pmid:")) {
                        pmid = query.substring(query.indexOf("sid=Entrez:PubMed&id=pmid:"));
                        of = pubmed.resolvePmid(pubmed.extractPmid(pmid));
                        of.setRfr_id("Entrez:PubMed");
                    }
                    if (query.contains("sid=google&id=pmid:")) {
                        pmid = query.substring(query.indexOf("sid=google&id=pmid:"));
                        of = pubmed.resolvePmid(pubmed.extractPmid(pmid));
                        of.setRfr_id("Google Scholar");
                    }
                    
                } else { // Übergabe aus Linkresolver
                    if (query != null) {
                        final ContextObject co = openurl.readOpenUrlFromRequest(rq);
                        of = openurlConv.makeOrderform(co);
                        // nur bei Übergabe über OpenURL Rfr_id abfüllen. Nicht bei WorldCat!
                        // Deshalb hier nachträglich...
                        if (co.getRfr_id() != null && !co.getRfr_id().equals("")) {
                            of.setRfr_id(co.getRfr_id());
                        }
                        
                        // Falls Doi vorhanden aber wichtige Angaben fehlen...
                        if (of.getDoi() != null && !of.getDoi().equals("") && of.getMediatype().equals("Artikel")
                                && of.areArticleValuesMissing()) {
                            
                            OrderForm ofDoi = new OrderForm();
                            ofDoi = doi.resolveDoi(doi.extractDoi(of.getDoi()));
                            if ("".equals(of.getIssn()) && !"".equals(ofDoi.getIssn())) {
                                of.setIssn(ofDoi.getIssn());
                            }
                            if ("".equals(of.getZeitschriftentitel()) && !"".equals(ofDoi.getZeitschriftentitel())) {
                                of.setZeitschriftentitel(ofDoi.getZeitschriftentitel());
                            }
                            if ("".equals(of.getJahr()) && !"".equals(ofDoi.getJahr())) {
                                of.setJahr(ofDoi.getJahr());
                            }
                            if ("".equals(of.getJahrgang()) && !"".equals(ofDoi.getJahrgang())) {
                                of.setJahrgang(ofDoi.getJahrgang());
                            }
                            if ("".equals(of.getHeft()) && !"".equals(ofDoi.getHeft())) {
                                of.setHeft(ofDoi.getHeft());
                            }
                            if ("".equals(of.getSeiten()) && !"".equals(ofDoi.getSeiten())) {
                                of.setSeiten(ofDoi.getSeiten());
                            }
                            if ("".equals(of.getAuthor()) && !"".equals(ofDoi.getAuthor())) {
                                of.setAuthor(ofDoi.getAuthor());
                            }
                            
                        }
                        
                    }
                }
                
                if (of.getDeloptions() == null || // Defaultwert deloptions
                        (!"post".equals(of.getDeloptions()) && !"fax to pdf".equals(of.getDeloptions()) && !"urgent"
                                .equals(of.getDeloptions()))) {
                    of.setDeloptions("fax to pdf");
                }
                
                // Cookie auslesen
                final Cookie[] cookies = rq.getCookies();
                
                if (cookies == null) {
                    LOG.debug("Kein Cookie gesetzt!");
                } else {
                    
                    final int max = cookies.length;
                    for (int i = 0; i < max; i++) {
                        
                        if (cookies[i].getName() != null && "doctordoc-bestellform".equals(cookies[i].getName())) {
                            String cookietext = cookies[i].getValue();
                            if (cookietext != null && cookietext.contains("---")) {
                                try {
                                    of.setKundenvorname(cookietext.substring(0, cookietext.indexOf("---")));
                                    cookietext = cookietext.substring(cookietext.indexOf("---") + 3);
                                    of.setKundenname(cookietext.substring(0, cookietext.indexOf("---")));
                                    cookietext = cookietext.substring(cookietext.indexOf("---") + 3);
                                    of.setKundenmail(cookietext);
                                } catch (final Exception e) { //
                                    LOG.error("Fehler beim Cookie auslesen!: " + e.toString());
                                }
                            }
                            
                        }
                    }
                }
                
                final ActiveMenusForm mf = new ActiveMenusForm();
                mf.setActivemenu("bestellform");
                rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);
                
            } else {
                
                // Falls User eingeloggt
                if (auth.isLogin(rq)) {
                    
                    forward = Result.SUCCESS.getValue();
                    final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");
                    
                    // Übergabe direkt von Pubmed...
                    if (query != null
                            && (query.contains("sid=Entrez:PubMed&id=pmid:") || query.contains("sid=google&id=pmid:"))) { // ...oder von Google Scholar
                        String pmid = "";
                        if (query.contains("sid=Entrez:PubMed&id=pmid:")) {
                            pmid = query.substring(query.indexOf("sid=Entrez:PubMed&id=pmid:"));
                            of = pubmed.resolvePmid(pubmed.extractPmid(pmid));
                            of.setRfr_id("Entrez:PubMed");
                        }
                        if (query.contains("sid=google&id=pmid:")) {
                            pmid = query.substring(query.indexOf("sid=google&id=pmid:"));
                            of = pubmed.resolvePmid(pubmed.extractPmid(pmid));
                            of.setRfr_id("Google Scholar");
                        }
                        
                    } else { // Übergabe aus Linkresolver
                        if (query != null) {
                            final ContextObject co = openurl.readOpenUrlFromRequest(rq);
                            of = openurlConv.makeOrderform(co);
                            // nur bei Übergabe über OpenURL Rfr_id abfüllen.
                            // Nicht bei WorldCat! Deshalb hier nachträglich...
                            if (co.getRfr_id() != null && !co.getRfr_id().equals("")) {
                                of.setRfr_id(co.getRfr_id());
                            }
                            
                            // Falls Doi vorhanden aber wichtige Angaben fehlen...
                            if (of.getDoi() != null && !of.getDoi().equals("") && of.getMediatype().equals("Artikel")
                                    && of.areArticleValuesMissing()) {
                                
                                OrderForm ofDoi = new OrderForm();
                                ofDoi = doi.resolveDoi(doi.extractDoi(of.getDoi()));
                                if (of.getIssn().equals("") && !ofDoi.getIssn().equals("")) {
                                    of.setIssn(ofDoi.getIssn());
                                }
                                if (of.getZeitschriftentitel().equals("") && !ofDoi.getZeitschriftentitel().equals("")) {
                                    of.setZeitschriftentitel(ofDoi.getZeitschriftentitel());
                                }
                                if (of.getJahr().equals("") && !ofDoi.getJahr().equals("")) {
                                    of.setJahr(ofDoi.getJahr());
                                }
                                if (of.getJahrgang().equals("") && !ofDoi.getJahrgang().equals("")) {
                                    of.setJahrgang(ofDoi.getJahrgang());
                                }
                                if (of.getHeft().equals("") && !ofDoi.getHeft().equals("")) {
                                    of.setHeft(ofDoi.getHeft());
                                }
                                if (of.getSeiten().equals("") && !ofDoi.getSeiten().equals("")) {
                                    of.setSeiten(ofDoi.getSeiten());
                                }
                                if (of.getAuthor().equals("") && !ofDoi.getAuthor().equals("")) {
                                    of.setAuthor(ofDoi.getAuthor());
                                }
                                
                            }
                            
                        }
                    }
                    
                    if (of.getDeloptions() == null || // Defaultwert deloptions
                            (!of.getDeloptions().equals("post") && !of.getDeloptions().equals("fax to pdf") && !of
                                    .getDeloptions().equals("urgent"))) {
                        of.setDeloptions("fax to pdf");
                    }
                    
                    of.setKundenvorname(ui.getBenutzer().getVorname());
                    of.setKundenname(ui.getBenutzer().getName());
                    of.setKundenmail(ui.getBenutzer().getEmail());
                    
                    final ActiveMenusForm mf = new ActiveMenusForm();
                    mf.setActivemenu("suchenbestellen");
                    rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);
                    
                } else {
                    final ErrorMessage em = new ErrorMessage("error.ip", "login.do");
                    rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
                    final ActiveMenusForm mf = new ActiveMenusForm();
                    mf.setActivemenu("bestellform");
                    rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);
                }
            }
            
        } catch (final Exception e) {
            LOG.error("execute: " + query + "\040" + e.toString());
        }
        
        rq.setAttribute("ofjo", of);
        
        return mp.findForward(forward);
    }
    
}
