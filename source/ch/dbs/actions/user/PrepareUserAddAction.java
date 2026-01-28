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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import util.Auth;
import ch.dbs.entity.AbstractBenutzer;
import ch.dbs.entity.Countries;
import ch.dbs.entity.Konto;
import ch.dbs.entity.Text;
import ch.dbs.form.KontoForm;
import ch.dbs.form.UserForm;
import ch.dbs.form.UserInfo;
import enums.Result;
import enums.TextType;

public final class PrepareUserAddAction extends Action {
    
    public ActionForward execute(final ActionMapping mp, final ActionForm form, final HttpServletRequest rq,
            final HttpServletResponse rp) {
        
        final Auth auth = new Auth();
        // if activated on system level, access will be restricted to paid only
        if (auth.isPaidOnly(rq)) {
            return mp.findForward(Result.ERROR_PAID_ONLY.getValue());
        }
        
        UserForm uf = (UserForm) form;
        final Countries country = new Countries();
        final Konto konto = new Konto();
        final UserForm ufLoginAction = (UserForm) rq.getAttribute("userform"); // get from Login
        if (ufLoginAction != null) {
            uf = ufLoginAction;
        }
        String forward = Result.FAILURE.getValue();
        final Text cn = new Text();
        
        try {
            
            if (auth.isLogin(rq)) {
                // logged in => go directly to modifykontousers.do
                forward = "adduser";
                final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");
                
                final List<Konto> allPossKontos = konto.getAllAllowedKontosAndSelectActive(ui, cn.getConnection());
                final ArrayList<KontoForm> lkf = new ArrayList<KontoForm>();
                
                for (final Konto k : allPossKontos) {
                    final KontoForm kf = new KontoForm();
                    kf.setKonto(k);
                    lkf.add(kf);
                }
                
                final List<Countries> allPossCountries = country.getAllCountries(cn.getConnection());
                final List<Text> categories = cn.getAllKontoText(TextType.USER_CATEGORY, ui.getKonto().getId(),
                        cn.getConnection());
                
                final AbstractBenutzer b = new AbstractBenutzer(uf, cn.getConnection());
                uf.setUser(b);
                
                uf.setAddFromBestellformEmail(true); // triggers the correct header in modifykontousers.jsp
                
                ui.setKontos(allPossKontos);
                ui.setCountries(allPossCountries);
                rq.setAttribute("categories", categories);
                rq.setAttribute("ui", ui);
                
            } else {
                // not logged in => to LoginAction
                forward = Result.LOGIN.getValue();
            }
            
            rq.setAttribute("userform", uf);
            
        } finally {
            cn.close();
        }
        
        return mp.findForward(forward);
    }
    
}
