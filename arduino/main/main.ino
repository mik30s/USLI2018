#include "Arduino.h"

#include "gyro.h"
#include "math.h"

Gyroscope* gyro_ptr;

bool isGyroReady() {
  uint8_t i = 0;
  const uint8_t MAX_READS = 100; 

  delay(10000);
  Gyroscope::Values values[MAX_READS];
  while (i < MAX_READS) 
  {
    values[i] = gyro_ptr->read();
    Serial.println(//String(values.ax) + " ax, "
                 //+ String(values.ay) + " ay, "
                 //+ String(values.az) + " az, "
                  String(values[i].az) + " az, "
                 //+ String(values.gy) + " gy, "
                 //+ String(values.gz) + " gz"
                 );
    delay(250);
    i++;
  }
  // sum and find averages
  float sum = 0;
  for (uint8_t i = 0; i < MAX_READS-1; ++i) {
     auto delta = double(values[i+1].az) - double(values[i].az);
     Serial.println("Delta: "+ String(delta));
     sum += delta;
     //Serial.println(" sum: " + String(i) + String(sum));
  }
 double avg = sum / (MAX_READS - 1) ;
  Serial.println("averge: "+ String(avg) + " last az: "+ String(gyro_ptr->read().az));
  return true;
}

void setup() {
  
  //pinMode(INTERRUPT_PIN, INPUT);
  Serial.begin(9600);
  Serial.println("Setting up gyro...");
  gyro_ptr =  new Gyroscope;
  Serial.println("Done! setting up gyro...");
  // attach new pinchange interrupt
  //attachPCINT(digitalPinToPCINT(2), readGyro, CHANGE);
  
  bool isReady = isGyroReady();
}

void loop() {
  
}



