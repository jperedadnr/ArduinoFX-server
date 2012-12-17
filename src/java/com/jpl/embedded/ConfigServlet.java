package com.jpl.embedded;

import com.jpl.embedded.model.BeanHT;
import com.jpl.embedded.model.CSensor;
import com.jpl.embedded.service.CStore;
import com.jpl.embedded.service.XBee;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * SERVLET http://<IP>:<PORT>/embedded/ArduinoOnline
 * 
 * It's loaded on deploy so:
 * - /usr/lib/jni is added to path
 * - a database connection is established.
 * - a thread is started to read serial port 
 * - a scheduled task is started to store values in database 
 * 
 * It responds to http://<IP>:<PORT>/embedded/ArduinoOnline request,
 * returning the last values in html format, autorefreshing every 30 seconds
 * 
 * Closes the serial port and end the task on undeploying
 * 
 * Note the first Get request takes around 18 seconds to be responded.
 * 
 * @author José Pereda Llamas
 * Created on 14-dic-2012 - 15:26:09
 */

@WebServlet(urlPatterns={"/ArduinoOnline"}, loadOnStartup=0)
public class ConfigServlet extends HttpServlet {
    
    /*
     * Add /usr/lib/jni to JVM path for dynamic libraries at first run
     * 
     * Another option would be editing /usr/java/jes7.0/samples/dist/run/config.sh
     * and adding it as a VM argument:
     * 
     * JAVA_COMMAND="$JES_HOME/jre/bin/java -Xmx64m -Djava.library.path='/usr/lib/jni'"
     */
    static {
        
        System.out.println("loading jni");
        
        System.setProperty( "java.library.path", "/usr/lib/jni" );
 
        /*
         * BUT changing the system property after the application has been started
         * doesn’t have any effect, since the property is evaluated very early and cached.
         * 
         * Work Around 
         * http://blog.cedarsoft.com/2010/11/setting-java-library-path-programmatically/
         * 
         * Classloader static field 'sys_paths' contains the paths. 
         * If that field is set to null, it is initialized automatically, and this will
         * result into the reevaluation of the library path when loadLibrary() is called.         * 
         */
        
        Field fieldSysPath = null;
        try {
            fieldSysPath = ClassLoader.class.getDeclaredField( "sys_paths" );
        } catch (NoSuchFieldException ex) {
            System.out.println("Error No Such Field: "+ ex.getMessage());
        } catch (SecurityException ex) {
            System.out.println("Error Security: "+ ex.getMessage());
        }
        fieldSysPath.setAccessible( true );
        try {
            fieldSysPath.set( null, null );
        } catch (IllegalArgumentException ex) {
            System.out.println("Error Illegal Argument: "+ ex.getMessage());;
        } catch (IllegalAccessException ex) {
            System.out.println("Error Illegal Access: "+ ex.getMessage());
        }
        
        System.out.println("jni loaded");
    }
    
    private XBee xbee;
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
        /*
         * Initialize database. If it doesn't exist it will be created.
         */
        CStore.getInstance();
        
        /*
         * Initialize Serial communication, starts reading port and storing measures
         * to database
         */
        xbee=new XBee();
        
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        System.out.println("HTTP Request: "+request.getRemoteAddr());
        
        response.setContentType("text/html;charset=UTF-8");
        
        PrintWriter out = response.getWriter();
        try {
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet ArduinoOnline</title>");  
            out.println("<META HTTP-EQUIV='refresh' content='30'>");
            out.println("</head>");
            out.println("<body>");
            out.append("<br><b>Embedded Server in Raspberry Pi</b>:</br>");
                    
            out.append("<br>Reading data from remote sensor...</br>");
            
            BeanHT bean=CSensor.getInstance().getLastBean();
            
            out.append("<br>Temperature: "+bean.getTemp()+"ºC</br>");
            out.append("<br>Relative Humidity: "+bean.getHum()+"%</br>");
            out.append("<br>Date: "+bean.getTime().getTime().toString()+"</br>");
            
            out.append("<br>&nbsp;</br><br><b>Raspberrry Pi - Arduino - XBee - Java Embedded Suite 7.0</b></br>");
           
            out.println("</body>");
            out.println("</html>");             
        } finally {            
            out.close();
        }
    }
    
    /*
     * Destroys the servlet: 
     * - close serial port
     * - stop and destroy scheduled task
     */
    @Override
    public void destroy() {
        xbee.disconnect();
    }
   
}
