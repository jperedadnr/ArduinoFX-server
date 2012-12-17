package com.jpl.embedded.model;

import java.util.Calendar;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author José Pereda Llamas
 * Created on 05-dic-2012 - 18:09:53
 */
@XmlRootElement
public class BeanHT {
    private int id;
    private double temp;
    private double hum;
    private Calendar time;
    
    public BeanHT() {
        id=1;
        temp=0;
        hum=0;
        time=Calendar.getInstance();
    }
    
    public BeanHT(int id, double temp, double hum, Calendar time){
        this.id=id;
        this.temp=temp;
        this.hum=hum;
        this.time=time;
    }

    public double getTemp() {
        return temp;
    }

    public void setTemp(double temp) {
        this.temp = temp;
    }

    public double getHum() {
        return hum;
    }

    public void setHum(double hum) {
        this.hum = hum;
    }

    public Calendar getTime() {
        return time;
    }

    public void setTime(Calendar time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "BeanHT{id=" + id + ", T=" + temp + " ºC, HR=" + hum + " %, time=" + time.getTime().toString() + '}';
    }
}
