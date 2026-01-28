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

package ch.dbs.login;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.dbs.entity.AbstractBenutzer;
import ch.dbs.entity.Konto;
import ch.dbs.entity.Text;
import enums.TextType;

/**
 * @author Markus Fischer
 */
public class Gtc {
    
    private static final Logger LOG = LoggerFactory.getLogger(Gtc.class);
    
    /**
     * Checkt, ob der Benutzer die neueste Version der GTC abgesegnet hat
     * 
     * @param AbstractBenutzer b
     * @param Connection cn Datenbankverbindung
     * @return boolean
     */
    public boolean isAccepted(final AbstractBenutzer b, final Connection cn) {
        
        boolean isAccepted = false;
        
        // Aktuellste GTC aus Datenbank holen
        final Text gtc = getCurrentGtc(cn);
        
        // Hat der Benutzer die aktuellste GTC hinterlegt?
        if (b.getGtc() != null && b.getGtc().equals(gtc.getInhalt())) {
            isAccepted = true;
        }
        
        return isAccepted;
    }
    
    /**
     * Holt die aktuellste GTC aus der Datenbank
     * 
     * @param Connection cn
     * @return Text GTC
     */
    public Text getCurrentGtc(final Connection cn) {
        
        Text gtc = null;
        
        //Aktuellste GTC aus Datenbank holen
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = cn.prepareStatement("SELECT * FROM `text` WHERE `TYID`=? ORDER BY TID ASC");
            pstmt.setString(1, "6");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                gtc = new Text();
                gtc.setId(rs.getLong("TID"));
                gtc.setKonto(new Konto(rs.getLong("KID"), cn));
                gtc.setTexttype(TextType.GTC);
                gtc.setInhalt(rs.getString("inhalt"));
            }
            
        } catch (final Exception e) {
            LOG.error("getCurrentGtc(Connection cn): " + e.toString());
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (final SQLException e) {
                    LOG.error(e.toString());
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (final SQLException e) {
                    LOG.error(e.toString());
                }
            }
        }
        return gtc;
    }
    
}
