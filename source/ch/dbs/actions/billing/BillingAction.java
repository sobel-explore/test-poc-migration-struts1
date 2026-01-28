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

package ch.dbs.actions.billing;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataSource;
import javax.mail.internet.InternetAddress;
import javax.mail.util.ByteArrayDataSource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperRunManager;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.util.SimpleFileResolver;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.Auth;
import util.MHelper;
import util.ReadSystemConfigurations;
import ch.dbs.admin.KontoAdmin;
import ch.dbs.entity.Billing;
import ch.dbs.entity.Konto;
import ch.dbs.entity.Text;
import ch.dbs.form.BillingForm;
import ch.dbs.form.ErrorMessage;
import ch.dbs.form.KontoForm;
import ch.dbs.form.Message;
import enums.Result;

/**
 * Handelt Rechnungsinformationen für Konten sowie für Admins
 * 
 * @author Pascal Steiner
 */
public final class BillingAction extends DispatchAction {
    
    private static final Logger LOG = LoggerFactory.getLogger(BillingAction.class);
    //Output Stream für PDF erstellen
    private OutputStream out = null;
    private InputStream reportStream = null;
    private final Map<String, Object> param = new HashMap<String, Object>();
    
    /**
     * Listet die Rechnungen zu einem Konto sortiert nach Offenen Rechnungen,
     * Rechnungsdatum auf
     */
    public ActionForward listBillings(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
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
        
        String forward = Result.FAILURE.getValue();
        final Billing b = new Billing();
        
        try {
            
            // Darf der Benutzer Rechnungen sehen? NUR SEINE EIGENEN... ;-)
            
            // Rechnungen zum Konto auflisten
            final KontoForm kf = (KontoForm) fm;
            
            final BillingForm bf = new BillingForm();
            final Konto k = new Konto(kf.getKid(), b.getConnection());
            bf.setKonto(k);
            bf.setBillings(b.getBillings(k, b.getConnection()));
            
            rq.setAttribute("billingform", bf);
            
            forward = Result.SUCCESS.getValue();
            
        } finally {
            b.close();
        }
        
        return mp.findForward(forward);
    }
    
    /**
     * Admins können Paydate setzen
     */
    public ActionForward setPayDate(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {
        
        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }
        // check access rights
        if (!auth.isAdmin(rq)) {
            return mp.findForward(Result.ERROR_MISSING_RIGHTS.getValue());
        }
        
        String forward = Result.FAILURE.getValue();
        final Billing cn = new Billing();
        
        try {
            
            final BillingForm bf = (BillingForm) fm;
            final Billing b = new Billing(bf.getBillid(), cn.getConnection());
            b.setZahlungseingang(bf.getZahlungseingang());
            b.update(cn.getConnection());
            
            final Message em = new Message("message.setpaydate", b.getKonto().getBibliotheksname() + "\n\n"
                    + b.getRechnungsgrund().getInhalt(), "kontoadmin.do?method=listKontos");
            rq.setAttribute("message", em);
            forward = Result.SUCCESS.getValue();
            
        } finally {
            cn.close();
        }
        
        return mp.findForward(forward);
    }
    
    /**
     * PDF Vorschau für eine Rechnung
     */
    public ActionForward billingPreview(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {
        
        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }
        // check access rights
        if (!auth.isAdmin(rq)) {
            return mp.findForward(Result.ERROR_MISSING_RIGHTS.getValue());
        }
        
        try {
            out = rp.getOutputStream();
            
            final BillingForm bf = (BillingForm) fm;
            pdfPreview(bf, rp);
            
        } catch (final IOException e) {
            LOG.error(e.toString());
        } finally {
            try {
                // Report an den Browser senden
                out.flush();
                out.close();
            } catch (final IOException e) {
                LOG.error("BillingAction.billingPreview: " + e.toString());
            }
        }
        
        return mp.findForward(null);
    }
    
    /**
     * Admins können Rechnungen versenden
     */
    public ActionForward sendBill(final ActionMapping mp, final ActionForm fm, final HttpServletRequest rq,
            final HttpServletResponse rp) {
        
        final Auth auth = new Auth();
        // make sure the user is logged in
        if (!auth.isLogin(rq)) {
            return mp.findForward(Result.ERROR_TIMEOUT.getValue());
        }
        // check access rights
        if (!auth.isAdmin(rq)) {
            return mp.findForward(Result.ERROR_MISSING_RIGHTS.getValue());
        }
        
        String forward = Result.FAILURE.getValue();
        final Text cn = new Text();
        final BillingForm bf = (BillingForm) fm;
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            
            // prepare attachment
            DataSource aAttachment = null;
            pdfMailAttachment(bf, rp, baos);
            aAttachment = new ByteArrayDataSource(baos.toByteArray(), "application/pdf");
            
            final Konto k = new Konto(bf.getKontoid(), cn.getConnection());
            
            final InternetAddress[] to = new InternetAddress[1];
            to[0] = new InternetAddress(k.getDbsmail());
            
            // Rechnung speichern
            bf.getBill().save(cn.getConnection());
            
            // define subject
            final String subject = "Rechnung für ihr Konto bei " + ReadSystemConfigurations.getApplicationName();
            
            // Create and send email
            final MHelper mh = new MHelper(to, subject, bf.getBillingtext(), aAttachment, "invoice.pdf");
            mh.setReplyTo(ReadSystemConfigurations.getBillingEmail());
            mh.send();
            
            // Meldung verfassen
            final Message m = new Message("message.sendbill", k.getBibliotheksname() + "\n\n" + bf.getBillingtext(),
                    "listbillings.do?method=listBillings&kid=" + k.getId());
            rq.setAttribute("message", m);
            
            if ("PDF Vorschau".equals(bf.getAction())) {
                forward = "preview";
            } else {
                forward = Result.SUCCESS.getValue();
            }
            
        } catch (final Exception e) {
            LOG.error("sendBilling: " + e.toString());
            final ErrorMessage em = new ErrorMessage("error.sendbilling", e.getMessage(),
                    "BillingAction?method=sendBill");
            rq.setAttribute(Result.ERRORMESSAGE.getValue(), em);
        } finally {
            cn.close();
            try {
                baos.close();
            } catch (final IOException e) {
                LOG.error(e.toString());
            }
        }
        
        return mp.findForward(forward);
    }
    
    private JRMapCollectionDataSource prepareReport(BillingForm bf, final HttpServletResponse rp) {
        
        final Billing b = new Billing();
        JRMapCollectionDataSource ds = null;
        try {
            
            final Konto k = new Konto(bf.getKontoid(), b.getConnection());
            
            // Rechnung parameter abfüllen
            final KontoAdmin ka = new KontoAdmin();
            bf = ka.prepareBillingText(k, b.getConnection(), null, bf);
            final StringBuffer kadress = new StringBuffer();
            kadress.append(k.getBibliotheksname() + "\n" + k.getAdresse() + "\n");
            if (!k.getAdressenzusatz().equals("")) {
                kadress.append(k.getAdressenzusatz() + "\n");
            }
            kadress.append(k.getPLZ() + " " + k.getOrt());
            
            // Parameter abfüllen
            //            final Map<String, Object> param = new HashMap<String, Object>();
            param.put("adress", kadress.toString());
            param.put("billingtext", bf.getBillingtext());
            param.put(JRParameter.REPORT_FILE_RESOLVER, new SimpleFileResolver(new File(this.getServlet()
                    .getServletContext().getRealPath("/reports/"))));
            
            //            final InputStream reportStream = new BufferedInputStream(this.getServlet().getServletContext()
            //                    .getResourceAsStream("/reports/rechnung.jasper"));
            reportStream = new BufferedInputStream(this.getServlet().getServletContext()
                    .getResourceAsStream("/reports/rechnung.jasper"));
            
            // prepare output stream
            rp.setContentType("application/pdf");
            rp.setCharacterEncoding("UTF-8");
            
            final Collection<Map<String, ?>> al = new ArrayList<Map<String, ?>>();
            final HashMap<String, String> hm = new HashMap<String, String>();
            hm.put("Fake", "Daten damit Report nicht leer wird..");
            al.add(hm);
            ds = new JRMapCollectionDataSource(al);
            
            //            try {
            //                JasperRunManager.runReportToPdfStream(reportStream, o, param, ds);
            //            } catch (final JRException e) {
            //                LOG.error(e.toString());
            //            } 
            
        } finally {
            b.close();
        }
        return ds;
        
    }
    
    private void pdfPreview(final BillingForm bf, final HttpServletResponse rp) {
        final JRMapCollectionDataSource ds = prepareReport(bf, rp);
        try {
            JasperRunManager.runReportToPdfStream(reportStream, out, param, ds);
        } catch (final JRException e) {
            LOG.error(e.toString());
        }
        
    }
    
    private void pdfMailAttachment(final BillingForm bf, final HttpServletResponse rp, final ByteArrayOutputStream o) {
        final JRMapCollectionDataSource ds = prepareReport(bf, rp);
        try {
            JasperRunManager.runReportToPdfStream(reportStream, o, param, ds);
        } catch (final JRException e) {
            LOG.error(e.toString());
        }
    }
    
}
