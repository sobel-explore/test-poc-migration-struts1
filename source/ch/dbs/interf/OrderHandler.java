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

package ch.dbs.interf;

import java.sql.Date;

import ch.dbs.entity.AbstractBenutzer;
import ch.dbs.entity.Konto;

/**
 * Interface zum Handeln der OrderForm/Positionen
 * @author Pascal Steiner
 */
public interface OrderHandler {

    AbstractBenutzer getBenutzer();
    Konto getKonto();
    String getPriority();
    String getDeloptions();
    String getFileformat();
    Date getOrderdate();
    String getMediatype();
    String getAutor();
    String getZeitschrift_verlag();
    String getHeft();
    String getJahrgang();
    String getJahr();
    String getTitel();
    String getKapitel();
    String getSeiten();
    String getWaehrung();
    String getPreis();

}
