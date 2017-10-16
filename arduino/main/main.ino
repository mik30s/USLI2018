#include "Arduino.h"

#include "gyro.h"
//#include "PinChangeInterrupt.h"

Gyroscope* gyro_ptr;

void setup() {
  //pinMode(INTERRUPT_PIN, INPUT);
  Serial.begin(9600);
  Serial.println("Setting up gyro...");
	gyro_ptr =  new Gyroscope;
  Serial.println("Done! setting up gyro...");
  // attach new pinchange interrupt
  //attachPCINT(digitalPinToPCINT(2), readGyro, CHANGE);
}

void readGyro() {
  Gyroscope::Values values = gyro_ptr->read();
  Serial.println(String(values.ax) + " ax, "
                 + String(values.ay) + " ay, "
                 + String(values.az) + " az, "
                 + String(values.gx) + " gx, "
                 + String(values.gy) + " gy, "
                 + String(values.gz) + " gz");
}


void loop() {
  readGyro();
}