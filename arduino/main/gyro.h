#pragma once

#define RadioSerial Serial1
#define ServoSerial Serial3 
#define DebugSerial Serial
#ifdef DEBUG
  #define DEBUG(text) Serial.println(text); 
#else
  #define DEBUG(text)
#endif

#define INTERRUPT_PIN 43 // use pin 2 on Arduino Uno & most boards
#include "Arduino.h"
#include "I2Cdev.h"
#include "MPU6050.h"
// Arduino Wire library is required if I2Cdev I2CDEV_ARDUINO_WIRE implementation
// is used in I2Cdev.h
#if I2CDEV_IMPLEMENTATION == I2CDEV_ARDUINO_WIRE
    #include "Wire.h"
#endif

class Gyroscope 
{
public:
  struct Values {
    bool isNone;
    int ax, ay, az;
    int gx, gy, gz;
  };

  Values values;
  MPU6050 gyroDevice;
  
  Gyroscope() {
    values = Values{0};
    // join I2C bus (I2Cdev library doesn't do this automatically)
    #if I2CDEV_IMPLEMENTATION == I2CDEV_ARDUINO_WIRE
      Wire.begin();
    #elif I2CDEV_IMPLEMENTATION == I2CDEV_BUILTIN_FASTWIRE
      Fastwire::setup(400, true);
    #endif
     
    gyroDevice.initialize();
    DebugSerial.println("Gyro initialized.");
    if (!(values.isNone = gyroDevice.testConnection())) {
      Serial.println("Failed to initialize gyroscope.");
    } else{
     
    }
  }

  const Values& read() {
      gyroDevice.getMotion6 (
        &values.ax,
        &values.ay,
        &values.az,
        &values.gx,
        &values.gy,
        &values.gz
      );

      return values;
  }
};
