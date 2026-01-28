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

package ch.dbs.action.statistik;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;

import util.Auth;
import util.Check;
import ch.dbs.entity.Bestellungen;
import ch.dbs.form.ActiveMenusForm;
import ch.dbs.form.OverviewForm;
import ch.dbs.form.UserInfo;
import ch.dbs.statistik.Statistik;
import enums.Result;

/**
 * Class for retrieving statistics of the orders during a given period.
 */
public final class OrderStatistik extends DispatchAction {
    
    private static final int DEFAULT_PERIOD = 4; // default period in months for select
    
    /**
     * Gets all statistics for an account of a specified period.
     */
    public ActionForward kontoOrders(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {
        
        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }
        // check access rights
        if (!auth.isBibliothekar(rq) && !auth.isAdmin(rq)) {
            return mp.findForward(Result.ERROR_MISSING_RIGHTS.getValue());
        }
        // if activated on system level, access will be restricted to paid only
        if (auth.isPaidOnly(rq)) {
            return mp.findForward(Result.ERROR_PAID_ONLY.getValue());
        }
        
        final OverviewForm of = (OverviewForm) fm; // contains the desired time period
        String forward = Result.FAILURE.getValue();
        final Bestellungen b = new Bestellungen();
        
        try {
            
            forward = Result.SUCCESS.getValue();
            final Statistik st = new Statistik();
            final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");
            
            final Check check = new Check();
            
            // Set the default time period in select. Starting from the beginning of the actual year.
            if (of.getYfrom() == null || of.getYto() == null || of.getMfrom() == null || of.getMto() == null
                    || of.getDfrom() == null || of.getDto() == null) {
                
                final Calendar calTo = Calendar.getInstance();
                calTo.setTimeZone(TimeZone.getTimeZone(ui.getKonto().getTimezone()));
                
                of.setYfrom(new SimpleDateFormat("yyyy").format(calTo.getTime()));
                of.setMfrom("01");
                of.setDfrom("01");
                of.setFromdate(of.getYfrom() + "-" + of.getMfrom() + "-" + of.getDfrom() + "00:00:00");
                
                of.setYto(new SimpleDateFormat("yyyy").format(calTo.getTime()));
                of.setMto(new SimpleDateFormat("MM").format(calTo.getTime()));
                of.setDto(new SimpleDateFormat("dd").format(calTo.getTime()));
                of.setTodate(of.getYto() + "-" + of.getMto() + "-" + of.getDto() + "23:59:59");
            }
            
            check.checkDateRegion(of, DEFAULT_PERIOD, ui.getKonto().getTimezone());
            
            rq.setAttribute("overviewform", of);
            
            // ***************** Statistics ******************
            
            final Long kid = ui.getKonto().getId();
            
            st.setKontoordersstat(b.countOrdersPerKonto(kid, of.getFromdate(), of.getTodate(), b.getConnection()));
            st.setLieferantstat(b.countLieferantPerKonto(kid, of.getFromdate(), of.getTodate(), b.getConnection()));
            st.setGratis_kosten(b.countGratisKostenpflichtigPerKonto(kid, of.getFromdate(), of.getTodate(),
                    b.getConnection()));
            st.setSum_gratis_kosten(b.sumGratisKostenpflichtigPerKonto(kid, of.getFromdate(), of.getTodate(),
                    b.getConnection()));
            st.setLieferartstat(b.countLieferartPerKonto(kid, of.getFromdate(), of.getTodate(), b.getConnection()));
            st.setMediatype(b.countMediatypePerKonto(kid, of.getFromdate(), of.getTodate(), b.getConnection()));
            st.setFileformatstat(b.countFileformatPerKonto(kid, of.getFromdate(), of.getTodate(), b.getConnection()));
            st.setPrioritystat(b.countPriorityPerKonto(kid, of.getFromdate(), of.getTodate(), b.getConnection()));
            st.setZeitschriftstat(b.countISSNPerKonto(kid, of.getFromdate(), of.getTodate(), b.getConnection()));
            st.setTotuserwithordersstat(b.countRowsUID(kid, of.getFromdate(), of.getTodate(), b.getConnection()));
            st.setJahrstat(b.countOrderYears(kid, of.getFromdate(), of.getTodate(), b.getConnection()));
            st.setGenderstat(b.countGenderPerKonto(kid, of.getFromdate(), of.getTodate(), b.getConnection()));
            st.setInstitutionstat(b.countInstPerKonto(kid, of.getFromdate(), of.getTodate(), b.getConnection()));
            st.setAbteilungstat(b.countAbteilungPerKonto(kid, of.getFromdate(), of.getTodate(), b.getConnection()));
            st.setCategorystat(b.countCategoriesPerKonto(kid, of.getFromdate(), of.getTodate(), b.getConnection()));
            st.setOrtstat(b.countPLZPerKonto(kid, of.getFromdate(), of.getTodate(), b.getConnection()));
            st.setLandstat(b.countLandPerKonto(kid, of.getFromdate(), of.getTodate(), b.getConnection()));
            
            rq.setAttribute("statistics", st);
            
            // Navigation: tab statistics
            final ActiveMenusForm mf = new ActiveMenusForm();
            mf.setActivemenu("stats");
            rq.setAttribute(Result.ACTIVEMENUS.getValue(), mf);
            
        } finally {
            b.close();
        }
        
        return mp.findForward(forward);
    }
    
}
