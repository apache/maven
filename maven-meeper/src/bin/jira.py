#!/usr/bin/python

## Note: codehaus has a JIRA version which doesn't support xmlrpc
##       so we can just scrape the screen or use the jelly interface
##       on the codehaus box.

import sys
import xmlrpclib

class JiraXmlRpcClient:
    
    def __init__(self):

        self.supplier = xmlrpclib.Server("http://jira.codehaus.org/rpc/xmlrpc")

    def transmitMessages(self):

        token = self.supplier.jira1.login( "jason", "" )

        #content = self.supplier.jira1.getIssueTypes( token )
        
        return None

##########################################################
# Main loop
##########################################################

try:
    JiraXmlRpcClient().transmitMessages()

except Exception, e:
    print "Error:", e

