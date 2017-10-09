#include "Arduino.h"

#include "gyro.h"

Gyroscope* gyro_ptr;

void setup() {
	gyro_ptr =  new Gyroscope;
}


void loop() {
	gyro_ptr->read();
}
