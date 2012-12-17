package com.jpl.embedded.service;

import com.jpl.embedded.comms.Serial;
import com.jpl.embedded.model.BeanHT;
import com.jpl.embedded.model.CSensor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * XBee open serial port on deploying. A XBee antenna must be plugged in the USB port
 * of the Raspberry Pi via XBee Explorer (https://www.sparkfun.com/products/8687)
 * 
 * The serial port reads measures, which are sent by the Arduino every 10 seconds
 * 
 * A scheduled task is launched to store last measures read every 30 seconds
 * 
 * On undeploy, close the port and stop the task
 * 
 * @author José Pereda Llamas
 * Created on 05-dic-2012 - 18:32:10
 */
public class XBee {
    
    private Serial serial;
    
    private long CICLO_EVENTOS = 30000; // miliseconds 
    private ScheduledFuture<?> scheduleAtFixedRate;
    private ScheduledExecutorService scheduler;
    
    public XBee(){
        
        /*
         * 1. Start connection with serial port and start reading T,H
         * These values are printed from Arduino each 10 seconds
         */
        serial=new Serial();
        try {
            System.out.println("Connecting to serial port...");
            serial.connect( "/dev/ttyUSB0" );
        } catch( Exception e ) {
            e.printStackTrace();
            return;
        }
        
        /*
         * 2. Start scheduler to record in database each 30 seconds the last values of T,H
         */
        System.out.println("Starting cicle events");
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduleAtFixedRate = scheduler.scheduleAtFixedRate(new Runnable(){

            @Override public void run() {
               /*
                * get last beanHT read 
                */
               BeanHT bean=CSensor.getInstance().getLastBean();
               System.out.println(bean);
               if(CStore.waitForDatabase("Servlet XBee")!=CStore.TIMEOUT){ 
                    /*
                    * store last beanHT
                    */
                   CStore.getInstance().record(bean);
                }
            }            
        }, CICLO_EVENTOS, CICLO_EVENTOS, TimeUnit.MILLISECONDS);
    
    }
    
    public void disconnect(){
        System.out.println("Closing serial port");
        serial.disconnect();

        System.out.println("Stopping scheduler");
        scheduleAtFixedRate.cancel(true);
        scheduler.shutdown();
    }
    
}
