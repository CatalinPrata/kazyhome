/**
 * Smart relay with switch button.
 * 
 * The implementation does the following:
 * - connects to wifi through WPS when is powered on
 * - creates a web server that enables switching off/on a GPIO pin and uses a basic authentication mechanism
 * - handles a button press that switches on/off a GPIO pin
 * - the button press handling also takes into consideration some charges that the relay 
 * is giving to the board to avoid false button presses. This should be removed and handled at the hardware level
 *
 *  Created on: 01.05.2018 by Catalin Prata
 */

#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <ESP8266WebServer.h>
#include <ESP8266mDNS.h>
#include <WiFiUdp.h>
#include <ArduinoOTA.h>

// uncomment this to be able to see the logs
//#define SERIAL_LOG

// set some basic credentials for the web server calls, change them before ussage!!
const char* www_password = "you_got_ME_69";
const char* www_username = "smart_1";

ESP8266WebServer server(80);

// led/relay GPIO number
const int led = 5;
// button input GPIO number
const int button = 4;
boolean ledStatus = false;
long last_pressed = 0;
int lastButtonState = 0;

void setup(void){
  // Prepare the hardware components
  pinMode(led, OUTPUT);
  pinMode(button, INPUT);

  // make sure the LED will be turned off
  digitalWrite(led, LOW);

  // Set the serial data rate (make sure you sync this with the serial monitor in order to be able to see the logs)
  Serial.begin(115200);

  String hostname = "kazy";
  WiFi.hostname(hostname);
  // needed for mDNS
  WiFi.mode(WIFI_STA);
  delay(1000);
  
  WiFi.begin("","");

  printLog("START.....");
        
  if (WiFi.status() != WL_CONNECTED){

      // if there is no connection, try WPS
      WiFi.beginWPSConfig();
      // make sure the wps connection is done
      delay(6000);
      
      if (WiFi.status() == WL_CONNECTED) {
        printLog("---- Connected! ----");
      } else {
        printLog("---- Could not connect to wifi ----");
      }
  } else {
    printLog("Connection already established.");
  }

  // register to the desired server routes
  server.on("/", handleRoot);
  server.on("/status", handleCheckStatus);
  server.onNotFound(handleNotFound);

  printLog(WiFi.localIP().toString());
  printLog(WiFi.SSID());
  printLog("mac addr:" + WiFi.macAddress());

  server.begin();

  // start the OTA and mDNS
  ArduinoOTA.setHostname((const char *)hostname.c_str());
  ArduinoOTA.begin();
}

void loop(void){
  // handle OTA
  ArduinoOTA.handle();
  // handle Server requests
  server.handleClient();

  handleButtonPress();
}

void printLog(String message){
  #ifdef SERIAL_LOG
  Serial.println(message);
  #endif
}

/*
 * Handles button press taking into consideration also some spikes comming from a reelay 
 * (this should be fixed hardware wise though..)
 */
void handleButtonPress(void){
  int buttonState = digitalRead(button);
  if (buttonState != lastButtonState) {
    if (buttonState == HIGH) {
      ledStatus = !ledStatus;
      if (ledStatus){
        digitalWrite(led, HIGH);
      } else {
        digitalWrite(led, LOW);
      }
    } 
  }
  lastButtonState = buttonState;
       
  delay(400);
}

// ----- WEB SERVER CALLBACKS -----

/**
 * Check status route implementation, simply returns the current led status as JSON
 */
void handleCheckStatus() {

  if(!server.authenticate(www_username, www_password)){
      return server.requestAuthentication();
  }

  if (ledStatus){
    server.send(200, "application/json", "{\"pin_status\":\"on\"}");  
  } else{
    server.send(200, "application/json", "{\"pin_status\":\"off\"}");
  }
  
}

/**
 * Handles a GET with "action" param with "on/off" values and returns the current led status as JSON
 */
void handleRoot() {

  if(!server.authenticate(www_username, www_password)){
      return server.requestAuthentication();
  }

  String action = server.arg("action");

  if (action.equals("on")) {
    digitalWrite(led, HIGH);
    ledStatus = true;
  } else if (action.equals("off")){
    digitalWrite(led, LOW);
    ledStatus = false;
  }
  
  server.send(200, "application/json", "{\"pin_status\":\"" + action + "\"}");
}

void handleNotFound(){
  String message = "File Not Found\n\n";
  message += "URI: ";
  message += server.uri();
  message += "\nMethod: ";
  message += (server.method() == HTTP_GET)?"GET":"POST";
  message += "\nArguments: ";
  message += server.args();
  message += "\n";
  for (uint8_t i=0; i<server.args(); i++){
    message += " " + server.argName(i) + ": " + server.arg(i) + "\n";
  }
  server.send(404, "text/plain", message);
}
