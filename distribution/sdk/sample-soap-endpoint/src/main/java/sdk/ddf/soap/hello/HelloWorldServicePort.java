/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package sdk.ddf.soap.hello;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.7-b01-
 * Generated source version: 2.1
 */
@WebService(name = "HelloWorldServicePort", targetNamespace = "http://ddf.sdk/soap/hello")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@XmlSeeAlso({ObjectFactory.class})
public interface HelloWorldServicePort {

    /**
     * @param parameters
     * @return returns sdk.ddf.soap.hello.HelloWorldResponse
     */
    @WebMethod(action = "helloWorld")
    @WebResult(name = "helloWorldResponse", targetNamespace = "http://ddf.sdk/soap/hello", partName = "parameters")
    public HelloWorldResponse helloWorldOp(
            @WebParam(name = "helloWorld", targetNamespace = "http://ddf.sdk/soap/hello", partName = "parameters") HelloWorld parameters);

}