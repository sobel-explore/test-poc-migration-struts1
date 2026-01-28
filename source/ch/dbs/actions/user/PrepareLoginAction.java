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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import util.Auth;
import ch.dbs.form.LoginForm;
import ch.dbs.form.OrderForm;
import enums.Result;

public final class PrepareLoginAction extends Action {

    public ActionForward execute(final ActionMapping mp, final ActionForm form, final HttpServletRequest rq,
            final HttpServletResponse rp) {

        final LoginForm lf = (LoginForm) form;
        final OrderForm pageForm = new OrderForm(lf);
        String forward = Result.FAILURE.getValue();
        final Auth auth = new Auth();

        if (auth.isLogin(rq)) {
            // bereits eingeloggt => direkt zu den Verfügbarkeits-Prüfungen
            forward = "checkavailability";
            pageForm.setResolver(true); // verhindet, dass beim Sprachwechsel Bestellangaben verloren gehen
            rq.setAttribute("orderform", pageForm);

        } else {
            // nicht eingeloggt => zu LoginAction
            forward = Result.SUCCESS.getValue();
            rq.setAttribute("loginform", lf);
            pageForm.setResolver(true); // verhindet, dass beim Sprachwechsel Bestellangaben verloren gehen
            rq.setAttribute("orderform", pageForm);
        }

        return mp.findForward(forward);

    }

}
