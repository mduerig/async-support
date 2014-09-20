Overview
========

This is the demo code for the presentation "OSGi Asynchronous Services: more than RPC"
I gave at [adaptTo() 2014](http://www.adaptto.org/2014/) in Berlin.

It consists of an implementation of the OSGi Promises API based on the Akka libraries
and a small demo application as shown in the presentation.

Getting started
===============

The application requires Java 8 and Maven 3.1. Use 

    mvn clean install
    
to build the demo application and all its dependencies. This creates a runnable
jar `trade-1.0-SNAPSHOT-jar-with-dependencies.jar` in the `trade/target` directory.