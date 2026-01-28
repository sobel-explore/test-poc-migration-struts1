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

package ch.dbs.cronjobs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;

import ch.dbs.admin.KontoAdmin;

public final class KontoCron extends DispatchAction {
    
    /**
     * Automatische Rechnungsstellung sowie Zahlungserinnerungen an Konten vor
     * Konto-Ablaufdatum: 2 Monate vor Ablauf wird die Rechnung erstellt und
     * versendet 1 Monat vor Ablauf wird eine Erinnerung inklusive
     * Rechnungsinformationen versendet 10 Tage vor Ablauf wird eine Warnung
     * inklusive Rechnungsinformationen versendet
     */
    public void autoBilling(final ActionMapping mp, final ActionForm form, final HttpServletRequest rq,
            final HttpServletResponse rp) {
        
        final KontoAdmin k = new KontoAdmin();
        k.autoBillExpdate();
        
    }
    
}
