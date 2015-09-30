You can use XML Spy to open the *.wsdl file.  Then, with the wsdl opened, you can go to menu 

SOAP->Create New SOAP Request 

to have XML Spy create a new xml request which is valid against the wsdl.  Enter your values into
the argument list (these will match the arg list of the method you're invoking in JBoss).  You can then 
send the message to jboss by using menu item

SOAP->Send Request To Server

How does XML Spy know where to send it?  The wsdl has all the destination info:

	<service name="BlastWSBeanService">
		<port name="BlastWSBeanPort" binding="tns:BlastWSBinding">
			<soap:address location="http://erato.jcvi.org:8080/compute-compute/Blast"/>
		</port>
	</service>

The wsdl can be obtained from JBoss by navigating to http://erato.jcvi.org:8080/compute-compute/Blast?wsdl (in my case),
where Blast is the name of the web service and ?wsdl is calling for that info.  You can also to go your JBoss deployment
and grab the wsdl it generates when it deploys the web services.  An example would be this

server/default/data/wsdl/compute.ear/compute.jar/

and you'll see the wsdl files inside.

