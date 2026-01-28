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

package ch.dbs.actions.illformat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.dbs.entity.Text;
import ch.dbs.form.IllForm;
import enums.Result;

/**
 * Methode um Illformat-Requests zu empfangen
 * 
 * @author Markus Fischer
 */
public final class GetIllFormatRequest extends Action {
    
    private static final Logger LOG = LoggerFactory.getLogger(GetIllFormatRequest.class);
    
    /**
     * empfängt Ill-Requests und stellt ein IllForm her
     */
    public ActionForward execute(final ActionMapping mp, final ActionForm form, final HttpServletRequest rq,
            final HttpServletResponse rp) {
        
        final Text cn = new Text();
        String forward = Result.FAILURE.getValue();
        final IllHandler illHandler = new IllHandler();
        
        try {
            
            forward = Result.SUCCESS.getValue();
            
            final IllForm illform = illHandler.readIllRequest(rq);
            final String returnvalue = illHandler.updateOrderState(illform, cn.getConnection());
            
            if ("OK".equals(returnvalue)) {
                illform.setStatus("OK");
            } else {
                illform.setStatus("ERROR");
                illform.setComment(returnvalue);
            }
            
            rq.setAttribute("illform", illform);
            
        } catch (final Exception e) {
            LOG.error("getIllFormatRequest - execute: " + e.toString());
        } finally {
            cn.close();
        }
        
        return mp.findForward(forward);
    }
    
}
