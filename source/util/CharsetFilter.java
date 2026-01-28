//  Copyright (C) 2012  Markus Fischer
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

package util;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class CharsetFilter implements Filter {
    private String encoding;
    
    public void init(final FilterConfig config) throws ServletException {
        encoding = config.getInitParameter("requestEncoding");
        
        if (encoding == null) {
            encoding = "UTF-8";
        }
    }
    
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain next)
            throws IOException, ServletException {
        
        // Respect the client-specified character encoding
        // (see HTTP specification section 3.4.1)
        if (null == request.getCharacterEncoding()) {
            request.setCharacterEncoding(encoding);
        }
        
        /**
         * Set the default response content type and encoding
         */
       String uri = ((javax.servlet.http.HttpServletRequest) request).getRequestURI();
    if (uri.endsWith(".do") || uri.endsWith(".jsp")) {
        response.setContentType("text/html; charset=UTF-8");
    }
        response.setCharacterEncoding("UTF-8");
        
        next.doFilter(request, response);
    }
    
    public void destroy() {
    }
}
