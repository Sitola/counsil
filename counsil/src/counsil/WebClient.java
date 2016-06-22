/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package counsil;

import java.io.IOException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.BasicConfigurator;

/**
 *
 * @author xminarik
 */
public class WebClient {
    
    public String getRoomList(String ip, int port) throws IOException {
        return getFromURL(ip, port, "/roomList");
    }
    
    public String getRoom(String ip, int port, String name) throws IOException {
        return getFromURL(ip, port, "/room/" + name);
    }
    
    public String getFromURL(String ip, int port, String URL) throws IOException {
        String outString = "";
        
        Integer portInt = port;
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(2 * 1000).build();
        
        CloseableHttpClient httpclient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
        BasicConfigurator.configure();
        
        try {
            HttpGet httpget = new HttpGet("http://" + ip + ":" + portInt.toString() + URL);

            // Create a custom response handler
            ResponseHandler<String> responseHandler = (HttpResponse response) -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            };
            String responseBody = httpclient.execute(httpget, responseHandler);
            outString = responseBody;
        } finally {
            httpclient.close();
        }
        
        return outString;
    }
    
    /*public static void main(String[] args) {
        WebClient a = new WebClient();
        String roomList = "";
        String room = "";
        try{
            roomList = a.getRoomList("147.251.54.45", 8080);
            room = a.getRoom("147.251.54.45", 8080, "room1");
        }catch(IOException ex){
            
        }
        System.out.println(roomList);
        System.out.println(room);
    }*/
}
        