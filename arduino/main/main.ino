#include "Arduino.h"
#include "gyro.h"
#include "ArduinoJson.h"
#include "SCServo.h"
#include "deps.h"

using HandlerCallbackType = void(*)();
#define RadioSerial Serial1
#define ServoSerial Serial3 
#define DebugSerial Serial
#define SolarVoltage A15
#define BatteryVoltage A14

struct RoverState 
{
public:
  enum ServoID : char 
  {
    WHEEL_SERVO_ONE  = 1,
    WHEEL_SERVO_TWO  = 2, 
    LOCK_SERVO       = 3,
    DEPLOYMENT_SERVO = 4
  };

  SCServo servoCtrl;
  Gyroscope* scope;
  long gravityVectorZ;

private:
  /**
   * Creates a rover state
   */
  RoverState()
  {
    servoCtrl.pSerial = &ServoSerial;
    servoCtrl.EnableTorque(ServoID::WHEEL_SERVO_TWO, 1);
    servoCtrl.EnableTorque(ServoID::LOCK_SERVO, 1);
    servoCtrl.EnableTorque(ServoID::DEPLOYMENT_SERVO, 1);
    servoCtrl.wheelMode(ServoID::WHEEL_SERVO_ONE);
    delay(500);
    servoCtrl.EnableTorque(ServoID::WHEEL_SERVO_ONE, 1);
    scope = new Gyroscope();
    gravityVectorZ = scope->read().az;
  }
    
public:  
  void drive() {
    servoCtrl.WriteSpe(ServoID::WHEEL_SERVO_ONE, -511);
    delay(2000);
    servoCtrl.WriteSpe(ServoID::WHEEL_SERVO_ONE, 511);
    delay(2000);
  }

  /**
   * Creates a new rover state to be used.
   * Adheres to the singleton pattern.
   * We should have only one rover state.`
   */
  static RoverState* getInstance() {
    static RoverState* instance;
    if (instance == nullptr) {
      instance = new RoverState;
    }
    return instance;
  }
};

/**
 * Calls a function after a certain time (in seconds)
 * has passed.
 */
void millisHandler(uint64_t duration, HandlerCallbackType callback) 
{
  static uint64_t tEnd = 0, tStart = 0;
  tStart = millis();
  if (tStart - tEnd > duration) 
  {
    tEnd = tStart;
    callback();
  }
}

RoverState* state = nullptr;

void setup() {
  // Serial for debugging
  DebugSerial.begin(9600);
  // Serial connection to onboard radio
  RadioSerial.begin(57500);
  // Serial connection to Servo TTL board
  ServoSerial.begin(1000000);
  
  state = RoverState::getInstance();
}

void loop() {
   // process base station commands every second
   // move rover 
   state->drive();
}





