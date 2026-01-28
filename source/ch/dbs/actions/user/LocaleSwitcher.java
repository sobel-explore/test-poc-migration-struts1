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

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import util.Auth;


public final class LocaleSwitcher extends Action {

    /**
     * Enables the user to switch manually the Locale
     *
     * @author Markus Fischer
     */

    public ActionForward execute(final ActionMapping mp, final ActionForm form,
            final HttpServletRequest rq, final HttpServletResponse rp) {

        String forward = "loggedout";
        final Auth auth = new Auth();

        String loc = rq.getParameter("request_locale");

        if (loc == null || loc.equals("")) { loc = "en"; } // Setting  a standard value

        rq.getSession().setAttribute("org.apache.struts.action.LOCALE", new Locale(loc));

        if (auth.isLogin(rq)) { // User is logged in
            forward = "loggedin";
        }

        return mp.findForward(forward);
    }



}
