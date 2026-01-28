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

package ch.dbs.actions.user;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.Auth;
import util.Encrypt;
import util.ReadSystemConfigurations;
import ch.dbs.entity.AbstractBenutzer;
import ch.dbs.entity.Benutzer;
import ch.dbs.entity.Bibliothekar;
import ch.dbs.entity.Text;
import ch.dbs.form.ActiveMenusForm;
import ch.dbs.form.ErrorMessage;
import ch.dbs.form.LoginForm;
import ch.dbs.form.OrderForm;
import ch.dbs.form.UserForm;
import ch.dbs.form.UserInfo;
import ch.dbs.login.Gtc;
import enums.Result;

/**
 * LoginAction checkt die eingegebenen Logininformationen auf ihre Gültigkeit
 * und erstellt bei Erfolg (User existiert in der DB) das Bean userinfo
 * {@link ch.dbs.form.UserInfo}
 * 
 * @author Pascal Steiner
 */
public final class LoginAction extends Action {
    
    private static final Logger LOG = LoggerFactory.getLogger(LoginAction.class);
    
    public ActionForward execute(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {
        final LoginForm lf = (LoginForm) fm;
        final OrderForm pageForm = new OrderForm(lf); // falls Artikelangaben aus Linkresolver vorhanden
        
        String forward = Result.FAILURE.getValue();
        final Text cn = new Text();
        final Auth auth = new Auth();
        
        // User mittels Logininfos aus der DB heraussuchen
        final Encrypt e = new Encrypt();
        AbstractBenutzer u = new AbstractBenutzer();
        
        try {
            
            final List<UserInfo> uil = u.login(lf.getEmail(), e.makeSHA(lf.getPassword()), cn.getConnection());
            
            // Wurde nur ein User mit den Logininfos gefunden, Bean userinfo erstellen
            if (uil.size() == 1) {
                
                u = uil.get(0).getBenutzer();
                // Last-Login Datum beim Benutzer hinterlegen
                u.updateLastuse(u, uil.get(0).getKontos().get(0).getTimezone(), cn.getConnection());
                
                //           Prüfung, ob GTC (General Terms and Conditions) in der aktuellen Version akzeptiert wurden
                final Gtc gtc = new Gtc();
                final Text t = gtc.getCurrentGtc(cn.getConnection()); // Text der aktuellen Version
                if (ReadSystemConfigurations.isGTC() && (u.getGtc() == null || !u.getGtc().equals(t.getInhalt()))) {
                    forward = "gtc"; // falls ein User die GTC noch nicht akzeptiert hat
                } else {
                    forward = Result.SUCCESS.getValue();
                }
                
                // Veraenderter Benutzer wieder in UserInfo und Session speichern.
                // Ab hier keine Veränderungen mehr am UserInfo!
                if ((u.getClass().isInstance(new Bibliothekar()) || u.getClass().isInstance(new Benutzer()))
                // Damit die Auswahl bei nur einem konto nicht dargestellt wird (Gilt nicht für Admins)
                        && uil.get(0).getKontos().size() == 1) {
                    uil.get(0).setKontoanz(1);
                }
                uil.get(0).setBenutzer(u);
                rq.getSession().setAttribute("userinfo", uil.get(0)); // userinfo in Session schreiben
                
                if (auth.isAdmin(rq)) {
                    forward = Result.SUCCESS.getValue();
                } // Admin braucht keine GTC-Prüfung
                
                // Check ob sich der Benutzer an mehr als an 1 Konto anmelden kann und Werte in UserInfo schreiben
                if (!"gtc".equals(forward)) {
                    if (uil.get(0).getKontos().size() == 1) {
                        uil.get(0).setKonto(uil.get(0).getKontos().get(0));
                        forward = Result.SUCCESS.getValue();
                    } else { // Sind mehr als ein Konto im UserInfo, muss das Konto vom Benutzer ausgewählt werden
                        forward = "konto";
                    }
                }
                
            }
            
            // Benutzerauswahl falls mehrere berechtigte Benutzer mit denselben Loginangaben gefunden wurden
            if (uil.size() > 1) {
                forward = "chooseuser";
                lf.setUserinfolist(uil);
                rq.getSession().setAttribute("authuserlist", lf);
            }
            
            // Keine gültgen Logininformationen
            if (uil.isEmpty()) {
                LOG.info("Wrong Login-Credentials!: " + lf.getEmail());
                final ErrorMessage em = new ErrorMessage();
                if (!lf.isResolver()) {
                    em.setError("error.username_pw");
                    em.setLink("login.do");
                } else {
                    em.setError("error.username_pw_linkresolver");
                    em.setLink("login.do");
                }
                rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
            }
            
            // Angaben aus Linkresolver
            if (lf.isResolver()) {
                // hier kommen auch Artikelangaben aus der Übergabe des Linkresolvers mit...
                rq.setAttribute("orderform", pageForm); // Übergabe in jedem Fall
                // Die Bestellberechtigung wird in der Methode prepare geprüft!
                if (forward.equals(Result.SUCCESS.getValue()) && auth.isBenutzer(rq)) {
                    forward = "order";
                }
                // Bibliothekar oder Admin auf Checkavailability
                if (forward.equals(Result.SUCCESS.getValue()) && !auth.isBenutzer(rq)) {
                    forward = "checkavailability";
                }
            }
            // Übernahme von Userangaben aus Link aus Bestellform-Email
            if (lf.getKundenemail() != null && !lf.getKundenemail().equals("")) {
                final UserForm uf = new UserForm(lf);
                rq.setAttribute("userform", uf);
                // Bibliothekar oder Admin auf Checkavailability
                if (forward.equals(Result.SUCCESS.getValue()) && !auth.isBenutzer(rq)) {
                    forward = "adduser";
                }
            }
            
            final ActiveMenusForm mf = new ActiveMenusForm();
            mf.setActivemenu("suchenbestellen");
            rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);
            
        } finally {
            cn.close();
        }
        
        return mp.findForward(forward);
    }
    
}
