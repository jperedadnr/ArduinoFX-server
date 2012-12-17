/*
 * Copyright (c) 2012 Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 */
package com.jpl.embedded.service;

import com.jpl.embedded.model.BeanHT;
import com.jpl.embedded.model.CSensor;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * CStore is a singleton that creates a connection to a database named xbeeBDD using JavaDB
 * stored in /usr/java/jes7.0/samples/dist/run/xbeeBDD
 * 
 * A table 'history' is created the first time, with these fields:
 * id INTEGER, time TIMESTAMP, temp FLOAT, hum FLOAT
 * 
 * On undeploy, the connection is shutdown
 * 
 * On every statement execution, the connection is locked till is fullfilled, 
 * and no more attempts of access are processed.
 * This prevents errors when resolving simultaneous REST web requests
 * 
 * 
 * Modified by José Pereda Llamas
 * On 05-dic-2012 - 18:32:10
 */
public class CStore {

    private final static String DB_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
    private final static String DB_NAME = "xbeeBDD";
    private final static String DB_URL = String.format("jdbc:derby:%s", DB_NAME);
    // Boot password must be at least 8 bytes long.
    private final static String DB_KEY = "jes12345";
    
    private static boolean bddInUse=false;
    public static final int TIMEOUT = 600; // 60 seconds
    
    private static CStore instance;

    private Connection connection;
    private Statement statement;

    public static synchronized CStore getInstance() {
        if (instance == null) {
            instance = new CStore();
        }
        return instance;
    }

    public static boolean isBddInUse() { return bddInUse; }
   
    private CStore() {
        System.out.println("Creating database");
        try {
            Class.forName(DB_DRIVER);
	    connection = DriverManager.getConnection(DB_URL + ";create=true;bootPassword="+DB_KEY, null);
            statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
        } catch (ClassNotFoundException cnfe) {
            System.out.println("Please put derby.jar in the classpath");
            System.exit(1);
        } catch (SQLException sqe) {
            sqe.printStackTrace();
        }
        try {
            statement.execute("CREATE TABLE history(id INTEGER NOT NULL GENERATED "
                    + "ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1), "
                    + "time TIMESTAMP NOT NULL, " 
                    + "temp FLOAT NOT NULL, " 
                    + "hum FLOAT NOT NULL)");
            System.out.println("Database created");
        } catch (SQLException ignored) {
            // ignore if already exist.
            System.out.println("Database was already created");
            // ignored.printStackTrace();
        }

    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        // cleanup 
        try {
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException se) {
            se.printStackTrace();
        }
        // shutdown the database
        try {
            DriverManager.getConnection(DB_URL + ";shutdown=true");
            System.out.println("Database shutdown");
        } catch (SQLException se) {
            if (!se.getSQLState().equals("08006")) {
                se.printStackTrace();
            }
            // otherwise ignore it : this is expected 
        }
    }

    /*
     * stores a beanHT id history table
     */
    public void record(BeanHT bean) {
        String sz="INSERT INTO history (time, temp, hum) VALUES ('"
                + new Timestamp(bean.getTime().getTimeInMillis()) +"', " +
                (float)bean.getTemp() + ", " + (float)bean.getHum()+ ")";
        try{  lock(); } catch(InterruptedException ie) {}
        try {
            statement.execute(sz);
        } catch (SQLException se) {
            se.printStackTrace();
        }
        unlock();
    }
    
    /*
     * get recorded beanHT number id
     */
    public BeanHT get(int id) {
        BeanHT bean = null;
        ResultSet resultset = null;
        try{  lock(); } catch(InterruptedException ie) {}
        try {
            resultset = statement.executeQuery("SELECT * FROM history where id = " + id);
            if (resultset.next()) {
                Calendar cal=Calendar.getInstance();
                cal.setTime(resultset.getTimestamp(2));
                bean = new BeanHT(id, (double)resultset.getFloat(3),
                        (double)resultset.getFloat(4), cal);
            }
        } catch (SQLException se) {
            se.printStackTrace();
        } finally {
            try {
                if (resultset != null) {
                    resultset.close();
                }
            } catch (SQLException ignored) {
            }
        }
        unlock();
        return (bean==null?CSensor.getInstance().getLastBean():bean);
    }

    /*
     * get last beanHT recorded 
     */
    public BeanHT last() {
        BeanHT bean = null;
        ResultSet resultset = null;
        try{  lock(); } catch(InterruptedException ie) {}
        try {
            resultset = statement.executeQuery("SELECT * FROM history ORDER BY id DESC FETCH FIRST ROW ONLY");
            if (resultset.next()) {
                Calendar cal=Calendar.getInstance();
                cal.setTime(resultset.getTimestamp(2));
                bean = new BeanHT(1, (double)resultset.getFloat(3),
                        (double)resultset.getFloat(4), cal);
            }
        } catch (SQLException se) {
            se.printStackTrace();
        } finally {
            try {
                if (resultset != null) {
                    resultset.close();
                }
            } catch (SQLException ignored) {
            }
        }
        unlock();
        return (bean==null?CSensor.getInstance().getLastBean():bean);
    }

    
    /*
     * get number of records in history table
     */
    public int count() {
        int tam=0;
        ResultSet resultset = null;
        try{  lock(); } catch(InterruptedException ie) {}
        try {
            resultset = statement.executeQuery("SELECT count(*) FROM history");
            if (resultset!=null && resultset.next()) {
                tam = resultset.getInt(1);
            }
        } catch (SQLException se) {
            se.printStackTrace();
        } finally {
            try {
                if (resultset != null) {
                    resultset.close();
                }
            } catch (SQLException ignored) {
            }
        }
        unlock();
        return tam;
    }
    
    /*
     * return a list of BeanHT elements
     * @param tam Set maximum number of items to grab from database, default 100, 
     * @param ini Set initial calendar date, in milliseconds from 1970, default 1/1/2012 00:00:00, 
     * @param end Set end calendar date, in milliseconds from 1970, default 1/1/2020 00:00:00
     
     */
    public List<BeanHT> list(int tam, long ini, long end) {
        ResultSet resultset = null;
        
        Calendar calIni=Calendar.getInstance();
        calIni.setTimeInMillis(ini);
        
        Calendar calEnd=Calendar.getInstance();
        calEnd.setTimeInMillis(end);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        ArrayList<BeanHT> list = new ArrayList<>();
        try{  lock(); } catch(InterruptedException ie) {}
        try {
            System.out.println("Reading database");
            resultset = statement.executeQuery("SELECT * FROM history WHERE time>='" + 
                    sdf.format(calIni.getTime()) +"' AND time<='"+
                    sdf.format(calEnd.getTime()) +"'"); 
            
            resultset.last();
            int cont=resultset.getRow();
            resultset.beforeFirst();
            if(tam==0){
                tam=100;
            }            
            int step=cont/tam;
            System.out.println("List Size: "+cont+", Step: "+step);
            cont=0;
            while (resultset.next()) {
                if(cont%step==0){
                    Calendar cal=Calendar.getInstance();
                    cal.setTime(resultset.getTimestamp(2));
                    list.add(new BeanHT(
                            resultset.getInt(1),
                            (double)resultset.getFloat(3),
                            (double)resultset.getFloat(4),
                            cal));
                }
                cont=cont+1;
            }
        } catch (SQLException se) {
            se.printStackTrace();
        } finally {
            try {
                if (resultset != null) {
                    resultset.close();
                }
            } catch (SQLException ignored) {
            }
        }
        unlock();
        System.out.println("Returning list, size: "+list.size());
        return list;
    }
    
    private synchronized void lock() throws InterruptedException{
        while(bddInUse){          
            wait(); //wait strategy - related to notification strategy
        }
        bddInUse = true;
    }

    private synchronized void unlock(){
        bddInUse = false;
        notify(); //notification strategy
    }
    
    public static int waitForDatabase(String rest){
        int cont=0;
        while(isBddInUse()){
            try{
                Thread.sleep(100);
                cont+=1;
            } catch(InterruptedException ie) {}
            if(cont>TIMEOUT){ 
                return TIMEOUT;
            }    
        }
        System.out.println(rest+" time access: "+(cont/10)+" s");
        return cont;
    }
}
