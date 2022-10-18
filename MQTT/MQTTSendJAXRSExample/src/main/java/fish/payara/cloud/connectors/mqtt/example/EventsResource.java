/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.cloud.connectors.mqtt.example;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.core.MediaType;

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
