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

package ch.dbs.actions.reports;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;

import util.Auth;
import ch.dbs.entity.Text;
import ch.dbs.form.IlvReportForm;
import ch.dbs.form.Message;
import ch.dbs.form.UserInfo;
import enums.Result;
import enums.TextType;

/**
 * Default ILV Mailfields operation
 * 
 * @author Pascal Steiner
 */
public final class ILVMailfields extends DispatchAction {
    
    /**
     * Save default Mailfields subject and mailtext for ILV ordermail with
     * attached PDF
     */
    public ActionForward saveMailFields(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {
        
        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }
        // if activated on system level, access will be restricted to paid only
        if (auth.isPaidOnly(rq)) {
            return mp.findForward(Result.ERROR_PAID_ONLY.getValue());
        }
        
        final Text cn = new Text();
        // Make sure method is only accessible when user is logged in
        String forward = Result.FAILURE.getValue();
        
        try {
            
            final UserInfo ui = (UserInfo) rq.getSession().getAttribute("userinfo");
            final IlvReportForm ilvf = (IlvReportForm) fm;
            
            // Exists already a Mailtext or a subject?
            final Text subject = new Text(cn.getConnection(), TextType.MAIL_SUBJECT, ui.getKonto().getId());
            final Text text = new Text(cn.getConnection(), TextType.MAIL_BODY, ui.getKonto().getId());
            
            subject.setKonto(ui.getKonto());
            if (subject.getInhalt() == null) { // save a new subject for the konto
                subject.setInhalt(ilvf.getSubject());
                subject.setTexttype(TextType.MAIL_SUBJECT);
                subject.saveNewText(cn.getConnection(), subject);
            } else { // update the existing subject for the konto
                subject.setInhalt(ilvf.getSubject());
                subject.updateText(cn.getConnection(), subject);
            }
            
            text.setKonto(ui.getKonto());
            if (text.getInhalt() == null) { // save a new subject for the konto
                text.setInhalt(ilvf.getMailtext());
                text.setTexttype(TextType.MAIL_BODY);
                text.saveNewText(cn.getConnection(), text);
            } else { // update the existing subject for the konto
                text.setInhalt(ilvf.getMailtext());
                text.updateText(cn.getConnection(), text);
            }
            
            forward = Result.SUCCESS.getValue();
            final Message mes = new Message("ilvmail.defaultmailfiedsset", "listkontobestellungen.do?method=overview");
            rq.setAttribute("message", mes);
            
        } finally {
            cn.close();
        }
        
        return mp.findForward(forward);
    }
    
}
