#pragma once

#include "Arduino.h"
#include <Wire.h>
#include <tuple>

class Gyroscope 
{
private:
    constexpr static uint8_t address =0x68;

public:
    Gyroscope() {
        Wire.begin();
        Wire.beginTransmission(address);
        Wire.write(0x6B);  // PWR_MGMT_1 register
        Wire.write(0);     // set to zero (wakes up the MPU-6050)
        Wire.endTransmission(true);
        Serial.begin(9600);       
    }

    std::tuple<float, float, float, float, float>
    read() {
        Wire.beginTransmission(MPU_addr);
        Wire.write(0x3B);  // starting with register 0x3B (ACCEL_XOUT_H)
        Wire.endTransmission(false);
        Wire.requestFrom(MPU_addr,14,true);  // request a total of 14 registers
        int AcX=Wire.read()<<8|Wire.read();  // 0x3B (ACCEL_XOUT_H) & 0x3C (ACCEL_XOUT_L)    
        int AcY=Wire.read()<<8|Wire.read();  // 0x3D (ACCEL_YOUT_H) & 0x3E (ACCEL_YOUT_L)
        int AcZ=Wire.read()<<8|Wire.read();  // 0x3F (ACCEL_ZOUT_H) & 0x40 (ACCEL_ZOUT_L)
        int Tmp=Wire.read()<<8|Wire.read();  // 0x41 (TEMP_OUT_H)   & 0x42 (TEMP_OUT_L)
        int GyX=Wire.read()<<8|Wire.read();  // 0x43 (GYRO_XOUT_H) & 0x44 (GYRO_XOUT_L)
        int GyY=Wire.read()<<8|Wire.read();  // 0x45 (GYRO_YOUT_H) & 0x46 (GYRO_YOUT_L)
        int GyZ=Wire.read()<<8|Wire.read();  // 0x47 (GYRO_ZOUT_H) & 0x48 (GYRO_ZOUT_L)
        Serial.print("AcX = "); Serial.print(AcX);
        Serial.print(" | AcY = "); Serial.print(AcY);
        Serial.print(" | AcZ = "); Serial.print(AcZ);
        Serial.print(" | Tmp = "); Serial.print(Tmp/340.00+36.53);  //equation for temperature in degrees C from datasheet
        Serial.print(" | GyX = "); Serial.print(GyX);
        Serial.print(" | GyY = "); Serial.print(GyY);
        Serial.print(" | GyZ = "); Serial.println(GyZ);
        delay(333);
        return {ax, ay,az, gx,gy,gz};
    }
};