1.How to compile my code
My code was written by Java and I selected 6666 as my port number.
The way to compile my code is simply typing

javac *.java
java ProxyServer

these two commands in command line with CADE lab Linux machine to compile and run my program.

2.How to test my code
I used "Telnet" to test my program on the command line. I type 

telnet localhost 6666

to connect my server by using Telnet as a client when my server is listening. Then I type

GET http://www.google.com/ HTTP/1.0

and

GET http://www.cs.utah.edu/~kobus/simple.html HTTP/1.0

and 

localhost:6666/http://www.cs.utah.edu/~kobus/simple.html (in Firefox)

to test basic function.  
For the exception test, since I have some different cases to handle different error, I type:

GEE http://www.google.com/ HTTP/1.0

GET ftp://www.google.com/ HTTP/1.0

PUT http://www.google.com/ HTTP/1.0
 
GET http://www.google.com

to test different exception handler.

Qixiang Chao
1/24/2017 
