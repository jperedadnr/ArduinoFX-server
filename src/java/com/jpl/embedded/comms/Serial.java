package com.jpl.embedded.comms;

import com.jpl.embedded.model.BeanHT;
import com.jpl.embedded.model.CSensor;
import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

/**
 * Serial connect with RXTXcomm library to specified serial port
 * starting two threads to read and write on it.
 * 
 * SerialReader thread reads some bytes from serial port, and then
 * tries to get the string sent from arduino with the format "{00.0,00.0}\n"
 * creating and storing a BeanHT(T,H,Time)
 * It doesn't block Raspberry Pi's CPU
 * 
 * On undeploy, the serial port is closed
 * 
 * @author José Pereda Llamas
 * Created on 04-dic-2012 - 19:32:10
 */

public class Serial {
    
    private CommPort m_commPort;
    private SerialPort m_serialPort;
    
    public void connect( String portName ) throws Exception {
        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier( portName );
        if( portIdentifier.isCurrentlyOwned() ) {
            System.out.println( "Error: Port is currently in use" );
        } else {
            int timeout = 2000;
            m_commPort = portIdentifier.open( this.getClass().getName(), timeout );

            if( m_commPort instanceof SerialPort ) {
                m_serialPort = ( SerialPort )m_commPort;
                m_serialPort.setSerialPortParams( 9600, SerialPort.DATABITS_8,
                                                SerialPort.STOPBITS_1, SerialPort.PARITY_NONE );

                InputStream in = m_serialPort.getInputStream();
                OutputStream out = m_serialPort.getOutputStream();

                ( new Thread( new SerialReader( in ) ) ).start();
                ( new Thread( new SerialWriter( out ) ) ).start();

            } else {
                System.out.println( "Not a serial port" );
            }
        }
    }
    
    public void disconnect(){
        if(m_serialPort!=null){
            m_serialPort.removeEventListener();
            m_serialPort.close();
        }
        if(m_commPort!=null){
            m_commPort.close();
            System.out.println("Port closed");
        }
    }
    
    
    private static class SerialReader implements Runnable {
 
        InputStream in;
        
        public SerialReader( InputStream in ) {
            this.in = in;
        }
 
        @Override
        public void run() {
            String sz="";
            byte[] buffer = new byte[ 1024 ];
            int len = -1;
            try {
              while( ( len = this.in.read( buffer ) ) > -1 ) {
                if(len>0){
                    // string of space-separated hexadecimal numbers                        
                    for(int i=0; i<len; i++){
                        sz=sz.concat(" ").concat(dec2hexStr(buffer[i]));                    
                    }
                    
                    if(sz.length()>6){
                        if(sz.trim().endsWith("0D 0A")){
                            // readable string "T,H"
                            String[] sz2=hex2ReadStr(sz).split("\\,");
                            BeanHT bean=new BeanHT();
                            try {
                                bean.setTemp(Double.parseDouble(sz2[0]));
                                bean.setHum(Double.parseDouble(sz2[1]));
                            } catch (NumberFormatException exception) {}
                            
                            Calendar cal=Calendar.getInstance();
                            bean.setTime(cal);
                            
                            // set last bean to this last measure
                            CSensor.getInstance().setLastBean(bean);
                            //System.out.println(bean);          
                            
                            // reset sz string, and continue reading
                            sz="";
                            try{
                                Thread.sleep(1000);
                            } catch(InterruptedException ie) {}
                        }
                    }
                }
              }
            } catch( IOException e ) {
              e.printStackTrace();
            }
        }
    }
    private static class SerialWriter implements Runnable {

        OutputStream out;

        public SerialWriter( OutputStream out ) {
            this.out = out;
        }

          @Override
        public void run() {
            try {
                int c = 0;
                while( ( c = System.in.read() ) > -1 ) {
                    this.out.write( c );
                }
            } catch( IOException e ) {
                e.printStackTrace();
            }
        }
    }
    
    /*
     * byte (0xA8) to String ("A8")
     */
    public static String dec2hexStr(byte b){
        return Integer.toString((b&0xff)+0x100,16).substring(1).toUpperCase();
    }
    
    
    public static String hex2String(String s) {
        int decimal = Integer.parseInt(s, 16);        
         if(decimal==0){
            return "0";
        }
       return String.valueOf((char)decimal);
    }
    
    /*
     * Convert string of space-separated hexadecimal numbers into a readable string
     * decoding all the digit bytes plus '.' and ','
     */
    public static String hex2ReadStr(String hexSz){
        String out="";
        if(hexSz.trim().equals("")){
            return out;
        }
        String[] bytes = (hexSz.trim()).split("\\s");   
        for(int i=0; i<bytes.length; i++){
            char theChar=(char) Byte.decode("0x"+bytes[i]).byteValue();
            if(Character.isLetterOrDigit(theChar)){
                out=out.concat(hex2String(bytes[i]));
            } else if(theChar=='.' || theChar==','){
                out=out.concat(hex2String(bytes[i]));                
            }
        }
        return out;
    }
}
