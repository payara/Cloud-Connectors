/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.cloud.connectors.mqtt.example;

import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PUT;
import javax.ws.rs.core.MediaType;

/**
 * REST Web Service
 *
 * @author steve
 */
@Path("events")
@ApplicationScoped
public class EventsResource {

    @Context
    private UriInfo context;
    
    @EJB
    MQTTSendMessage bean;

    /**
     * Creates a new instance of EventsResource
     */
    public EventsResource() {
    }

    /**
     * PUT method for updating or creating an instance of EventsResource
     * @param content representation for the resource
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void putJson(String content) {
        bean.sendMessage(content);
    }
}
