ArduinoFX-server
================

Embedded Server side for ArduinoFX project

1. Basic Hardware Setup

Arduino Uno with RHT03 temperature and relative humidity sensor and a XBee antenna plugged in an Arduino shield. 

The measures are sent wirelessly to a second XBee antenna, plugged via a XBee Explorer to a Raspberry Pi USB port.

In the Raspberry Pi, with soft-float Debian wheezy distro, we have installed Java Embedded Suite 7.0. 

Please, read full description here:

http://jperedadnr.blogspot.com.es/2012/12/arduinofx-javafx-gui-for-home.html

2. Embedded Server

This code allows you to create the embedded server in the Raspberry Pi

It will perform the following main tasks: 

-Back-end task: it will repeatedly read the serial port to get the measures from the Arduino and store them in a database.
    
-Web services: it will respond to web requests returning the current values measured.

-RESTful web services: it will respond to REST requests returning lists in json format with the data measured between two dates.

Please read here:

http://jperedadnr.blogspot.com.es/2012/12/arduinofx-javafx-gui-for-home_17.html

for full explanation on the embedded server installation. 

These tasks are intended for a JavaFX based client (https://github.com/jperedadnr/ArduinoFX-client)

Comments are really welcome.

José Pereda - http://jperedadnr.blogspot.com.es