#define DEBUG 0
#include "Arduino.h"
#include "gyro.h"
#include "SoftwareSerial.h"
#include "ArduinoJson.h"
#include "SCServo.h"

using HandlerCallbackType = void(*)();
#define RadioSerial Serial1
#define ServoSerial Serial3 
#define DebugSerial Serial

#ifdef DEBUG
  #define DEBUG(text) DebugSerial.println(text); 
#else
  #define DEBUG(text)
#endif

/**
 * Represents the state the rover is in
 * See data flow diagram in PDR final
 */
struct RoverState 
{
private:  
  bool isLocked;
  bool isDriving;
  SCServo servoCtrl;
  Gyroscope* scope;
  long gravityVectorZ;
  uint16_t servoOneTiming;
  uint16_t servoTwoTiming;

  enum ServoID : char 
  {
    WHEEL_SERVO_ONE  = 1,
    WHEEL_SERVO_TWO  = 2, 
    LOCK_SERVO       = 3,
    DEPLOYMENT_SERVO = 4
  };
  
  /**
   * Creates a rover state
   */
  RoverState():
    isLocked(true), isDriving(false)
  {
    DEBUG("Enter state constructor");
    servoCtrl.pSerial = &ServoSerial;
    servoCtrl.EnableTorque(ServoID::WHEEL_SERVO_ONE, 1);
    servoCtrl.EnableTorque(ServoID::WHEEL_SERVO_TWO, 1);
    servoCtrl.EnableTorque(ServoID::LOCK_SERVO, 1);
    servoCtrl.EnableTorque(ServoID::DEPLOYMENT_SERVO, 1);
    servoCtrl.wheelMode(ServoID::WHEEL_SERVO_ONE);
    servoCtrl.wheelMode(ServoID::WHEEL_SERVO_TWO);
    servoCtrl.WriteSpe(ServoID::WHEEL_SERVO_ONE, -2);
    scope = new Gyroscope();
      scope->read();
    this->gravityVectorZ = scope->values.az;
    DEBUG("Exit state constructor");
  }
public:
  /**
   * Creates a new rover state to be used.0
.
* Adheres to the singleton pattern.
   * We should have only one rover state.
   */
  static RoverState* getInstance() {
    static RoverState* instance;
    if (instance == nullptr) {
      instance = new RoverState;
    }
    return instance;
  }
  
  /**
   * @brief Moves the rover in one direction
   */
  void drive() 
  {
    //static bool cw = false;
    //delay(3000);
    //cw = !cw;
    if (this->gravityVectorZ  < 0) {
      this->servoOneTiming = 2100;
      this->servoTwoTiming = 2000;
    } else {
      this->servoOneTiming = 2000;
      this->servoTwoTiming = 2100;  
    }
    // FLIP TIME VALUES TO MOVE IN REVERSE DIRECTION
    //if (!isLocked || !isDriving) 
    {
      //servoCtrl.WritePos(ServoID::WHEEL_SERVO_ONE, 1000, 2000, 0);
      //servoCtrl.WritePos(ServoID::WHEEL_SERVO_TWO, 1000, 2100, 0);
      servoCtrl.WritePos(ServoID::WHEEL_SERVO_ONE, 1000, this->servoOneTiming);
      servoCtrl.WritePos(ServoID::WHEEL_SERVO_TWO, 1000, this->servoTwoTiming);
      isDriving = true;
    }
  }
  
  /**
   * @brief Unlocks the rover from
   * Docking bay.
   */
  void unlock() 
  {
    // orient rover using gyro readings.
    scope->read();
    this->gravityVectorZ = scope->values.az;

    if (this->gravityVectorZ  < 0) {
      this->servoOneTiming = 2100;
      this->servoTwoTiming = 2000;
    } else {
      this->servoOneTiming = 2000;
      this->servoTwoTiming = 2100;  
    }
    
    // move unlocking servo
    isLocked = false;
  }

  /**
   * @brief Deploys the solar panels
   * of the rover
   */
  void deploy() {
    
  }

  /**
   * Sends data to the base station via on board radio.
   */
  void report() {
    scope->read();
    this->gravityVectorZ = scope->values.az;
    String jsonStr = String("{")
       + "ax:"+scope->values.ax + ","
       + "ay:"+scope->values.ay + ","
       + "az:"+scope->values.az + ","
       + "gx:"+scope->values.gx + ","
       + "gy:"+scope->values.gy + ","
       + "gz:"+scope->values.gz
       +"}";
    RadioSerial.println(jsonStr);
    DEBUG(jsonStr);
  }
};

/**
 * Manages the state of the rover.
 */
RoverState* state = nullptr;

/**
 * Command codes
 * DE = Deploy Rover, 1
 * DA = Rover Data, 2
 * DS = Deploy Solar Panels, 3
 * see radioComm function for full usage.
 */
enum CommandCode { DE = 1, DA = 2, DS = 3 };

/**
 * Communicates with base station and rover.
 * Will continously send data to base station for monitoring
 * Will process commands for controlling the rover.
 * No commands for manual control.
 * cmd codes
 * DE = Deploy Rover, 1
 * DA = Rover Data, 2
 * see enum abover for definition.
 */
void radioComm() 
{
  DEBUG("Radio comm called.");
  DynamicJsonBuffer jsonBuffer;
  if (RadioSerial.available()) 
  {
    const String jsonStr(RadioSerial.readStringUntil('\n'));
    JsonObject& object = jsonBuffer.parseObject(jsonStr);
    String cmd = object[String("cmd")];

    if (cmd.toInt() == CommandCode::DE) {
      // deploy rover
      state->unlock();
    } else if (cmd.toInt() == CommandCode::DA) {
      // send data to base station
      state->report();
    } else if (cmd.toInt() == CommandCode::DS) {
      // deploy solar panels
      state->deploy();
    }
  }
}

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

/**
 * Microcontroller setup called once by 
 * onboard firmware
 */
void setup() 
{
  // Serial for debugging
  DebugSerial.begin(9600);
  // Serial connection to onboard radio
  RadioSerial.begin(57500);
  // Serial connection to Servo TTL board
  ServoSerial.begin(1000000);
  
  state = RoverState::getInstance();
  DEBUG("Rover state set!");
}

/**
 * Microcontroller remains in this loop until 
 * power is cut.
 */
void loop() 
{
   // process base station commands every second
   millisHandler(200, radioComm);
   // move rover 
   state->drive();
   state->report();
}







