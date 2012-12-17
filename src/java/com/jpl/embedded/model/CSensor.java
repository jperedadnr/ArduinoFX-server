package com.jpl.embedded.model;

/**
 *
 * @author José Pereda Llamas
 * Created on 05-dic-2012 - 18:22:09
 */
public class CSensor {

    private static CSensor instance;
    private BeanHT lastBean;
    
    private CSensor(){
        lastBean=new BeanHT();
    }
    public static synchronized CSensor getInstance() {
        if (instance == null) {
            instance = new CSensor();
        }
        return instance;
    }
    
    public void reset(){
        lastBean.setHum(0);
        lastBean.setTemp(0);
    }
    public void setLastBean(BeanHT bean){
        lastBean.setHum(bean.getHum());
        lastBean.setTemp(bean.getTemp());
        lastBean.setTime(bean.getTime());
    }
    
    public BeanHT getLastBean(){
        return lastBean;
    } 
    
}
