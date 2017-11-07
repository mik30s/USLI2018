#include <SCServo.h>

SCServo SERVO;

bool cw;

void setup()
{
  Serial3.begin(1000000);
  Serial.begin(9600);
  SERVO.pSerial = &Serial3;
  delay(500);
  SERVO.EnableTorque(1, 1);
  SERVO.wheelMode(1);

  cw = true;


  /*for(int i = 1000; i < 65000; i+= 100){
    SERVO.WritePos(1, 500, i, 1000);
    Serial.println(i);
    delay(3000);
  }*/
}

void loop()
{
  delay(3000);
  cw = !cw;
  resetServo();
}

void resetServo(){
  SERVO.EnableTorque(1, 0);
  delay(500);
  SERVO.EnableTorque(1, 1);
  SERVO.wheelMode(1);
  if(cw){
     SERVO.WritePos(1, 500, 3000, 1000);
  }else{
     SERVO.WritePos(1, 500, 3100, 1000);
  }
}
