package com.jpl.embedded;

import com.jpl.embedded.model.BeanHT;
import com.jpl.embedded.model.CSensor;
import com.jpl.embedded.service.CStore;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * This JAX-RS web service handles HTTP GET request. 
 * Each request waits for operations over database to finish
 * 
 * It supports retrieving the following services:
 * 
 *  1    http://<IP>:<PORT>/embedded         <-- Last BeanHT read in text format
 *  2    http://<IP>:<PORT>/embedded/last    <-- Last BeanHT read in json format
 *  3    http://<IP>:<PORT>/embedded/count   <-- Number of records in database, in text format
 *  4    http://<IP>:<PORT>/embedded/list?tam=100&ini=1325286000603&fin=1577746800603
 *               <-- List of BeanHT stored in database, in json format, queue params:
 *                   max tam items, default 100, 
 *                   between ini milliseconds from 1970, default 1/1/2012, 
 *                   and end milliseconds from 1970, default 1/1/2020
 *  5    http://<IP>:<PORT>/embedded/{id}    <-- BeanHT stored number id, in json format
 * 
 * More services can be added.
 * 
 * All the services accesing to database are required to wait till it isn't in use, with 
 * a timeout of 60 seconds.
 * 
 * Note the first REST request takes around 80 seconds to be responded.
 *
 * @author José Pereda Llamas
 * Created on 14-dic-2012 - 15:23:16
 */

@Path("/") 
public class EmbeddedREST {

    private CStore store=CStore.getInstance();
    
    @GET
    @Produces({MediaType.TEXT_PLAIN, MediaType.TEXT_HTML})
    public String getText() {
        if(CStore.waitForDatabase("Text")==CStore.TIMEOUT){ 
            return "Device was blocked for more than "+(CStore.TIMEOUT/10)+" seconds";
        } 
        return store.last().toString();
    }
    
    @GET
    @Path("last")
    @Produces({MediaType.APPLICATION_JSON})
    public BeanHT getLast() {
        if(CStore.waitForDatabase("Last")==CStore.TIMEOUT){ 
            return CSensor.getInstance().getLastBean();
        }    
        return store.last();
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public BeanHT getItem(@PathParam("id") int id) {
        if(CStore.waitForDatabase("Id")==CStore.TIMEOUT){ 
            return CSensor.getInstance().getLastBean();
        }
        return store.get(id);
    }
    
    @GET
    @Path("count")
    @Produces(MediaType.TEXT_PLAIN)
    public String getCount() {
        if(CStore.waitForDatabase("Count")==CStore.TIMEOUT){ 
            return "Device was blocked for more than "+(CStore.TIMEOUT/10)+" seconds";
        }
        return String.valueOf(store.count());
    }

    /** 
     * Get a JSONArray of BeanHT elements from database, with the following limits:
     * @param tam Set maximum number of items to grab from database, default 100, 
     * @param ini Set initial calendar date, in milliseconds from 1970, default 1/1/2012 00:00:00, 
     * @param end Set end calendar date, in milliseconds from 1970, default 1/1/2020 00:00:00
     */
    @GET
    @Path("list")
    @Produces({MediaType.APPLICATION_JSON})
    public List<BeanHT> getList(@QueryParam("tam") @DefaultValue("100") int tam,
                                @QueryParam("ini") @DefaultValue("1325286000603") long ini,
                                @QueryParam("end") @DefaultValue("1577746800603") long end) {
        if(CStore.waitForDatabase("List")==CStore.TIMEOUT){ 
            return new ArrayList<BeanHT>();
        }
        return store.list(tam,ini,end);
    }

}