/*
 * Copyright 2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ws.transport.http;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.transport.TransportInputStream;
import org.springframework.ws.transport.TransportOutputStream;
import org.springframework.ws.transport.WebServiceMessageReceiver;
import org.springframework.ws.transport.support.WebServiceMessageReceiverObjectSupport;

/**
 * Adapter to use the {@link WebServiceMessageReceiver} interface with the generic {@link
 * org.springframework.web.servlet.DispatcherServlet}. Requires a {@link org.springframework.ws.WebServiceMessageFactory}
 * which is used to convert the incoming <code>HttpServletRequest</code> into a <code>WebServiceMessage</code>, and
 * passes that context to the mapped <code>WebServiceMessageReceiver</code>. If a response is created, that is sent via
 * the <code>HttpServletResponse</code>.
 * <p/>
 * Note that the <code>MessageDispatcher</code> implements the <code>WebServiceMessageReceiver</code> interface,
 * enabling this adapter to function as a gateway to further message handling logic.
 *
 * @author Arjen Poutsma
 * @see #setMessageFactory(org.springframework.ws.WebServiceMessageFactory)
 * @see org.springframework.ws.transport.WebServiceMessageReceiver
 * @see org.springframework.ws.WebServiceMessageFactory
 * @see org.springframework.ws.server.MessageDispatcher
 */
public class WebServiceMessageReceiverHandlerAdapter extends WebServiceMessageReceiverObjectSupport
        implements HandlerAdapter {

    public long getLastModified(HttpServletRequest request, Object handler) {
        return -1L;
    }

    public ModelAndView handle(HttpServletRequest httpServletRequest,
                               HttpServletResponse httpServletResponse,
                               Object handler) throws Exception {
        if ("POST".equals(httpServletRequest.getMethod())) {
            TransportInputStream tis = new HttpServletTransportInputStream(httpServletRequest);
            TransportOutputStream tos = new HttpServletTransportOutputStream(httpServletResponse);
            handle(tis, tos, (WebServiceMessageReceiver) handler);
            return null;
        }
        else {
            throw new ServletException("Request method '" + httpServletRequest.getMethod() + "' not supported");
        }
    }

    public boolean supports(Object handler) {
        return handler instanceof WebServiceMessageReceiver;
    }

    /**
     * Sets the response code to 204, No Content.
     */
    protected void handleNoResponse(TransportInputStream tis, TransportOutputStream tos) {
        HttpServletResponse httpServletResponse = ((HttpServletTransportOutputStream) tos).getHttpServletResponse();
        httpServletResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    /**
     * Sets the response code to 200, OK, for normal responses. Set the code to 500, Internal Server Error, in case of a
     * SOAP Fault,
     */
    protected void handleResponse(TransportInputStream tis, TransportOutputStream tos, WebServiceMessage response)
            throws Exception {
        HttpServletResponse httpServletResponse = ((HttpServletTransportOutputStream) tos).getHttpServletResponse();
        if (response.hasFault()) {
            httpServletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        else {
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        }
        response.writeTo(tos);
    }

    /**
     * Sets the response code to 404, Not Found.
     */
    protected void handleNoEndpointFound(TransportInputStream tis, TransportOutputStream tos) {
        HttpServletResponse httpServletResponse = ((HttpServletTransportOutputStream) tos).getHttpServletResponse();
        httpServletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
}