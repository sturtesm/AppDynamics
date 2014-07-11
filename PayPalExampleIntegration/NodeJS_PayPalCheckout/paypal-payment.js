'use strict'; 

/**
require("appdynamics").profile({
  controllerHostName: '127.0.0.1',
  controllerPort: 8090,
  accountName: '', //Required for a controller running in multi-tenant mode.
  accountAccessKey: '', //Required for a controller running in multi-tenant mode.
  applicationName: 'PayPal ECommerce Store',
  tierName: 'NodeJS Web Tier',
  nodeName: 'nodejs-osxltsturt', 
  debug: true //Debug is optional; defaults to false.
 });
*/

var Client = require('node-rest-client').Client;
var http = require('http');

var client = new Client();



var paymentArgs ={
        path:{"bearer":"bearer"}, // path substitution var
        headers:{"Accept": "text/plain", "Accept-Language": "en_US", "Content-Type": "text/plain" }
};

// registering remote methods
client.registerMethod("authTokenMethod", "http://127.0.0.1:8080/jaxrs-sample/v1/paypal/auth", "GET");
client.registerMethod("paymentMethod", "http://127.0.0.1:8080/jaxrs-sample/v1/paypal/payment/${bearer}", "GET");

function process_payment(cb) {
  console.log("Called process_payment...");

  client.methods.authTokenMethod(function(data, response) {
     paymentArgs.path.bearer=data;

     console.log("Generated Auth Token: " + data);

     client.methods.paymentMethod(paymentArgs, function(data, response) {
        console.log("Processed Payment: " + data);
        cb(data, response); 
     });
  });
}

// Configure our HTTP server to respond to all requests by processing a payment with PayPal
var server = http.createServer(function (serverRequest, serverResponse) {

   console.log("Got Server Request: " + serverRequest);

   process_payment(function(data, response) {

      console.log("Processed Payment, Response=>" + data);

      if (data.state = "authorized") {
         serverResponse.writeHead(200, {"Content-Type": "text/plain"});

         var responseMsg = "" + data;

         serverResponse.end(responseMsg);
      }
      else {
         serverResponse.writeHead(500, {"Content-Type": "text/plain"});
         serverResponse.end("Error Processing Payment: " + data.state);
      } 
   });
});

var server2 = http.createServer(function (serverRequest, serverResponse) {
  serverResponse.writeHead(200, {"Content-Type": "text/plain"});
  serverResponse.end("Hello World!!!");
});

// Listen on port 8000, IP defaults to 127.0.0.1
server.listen(8000);
server2.listen(8001);

// Put a friendly message on the terminal
console.log("Server running at http://127.0.0.1:8000/");
