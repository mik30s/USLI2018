#include "Arduino.h"
#include "gyro.h"
#include "SoftwareSerial.h"
#include "ArduinoJson.h"
#include "SCServo.h"
#include "deps.h"

using HandlerCallbackType = void(*)();
#define RadioSerial Serial1
#define ServoSerial Serial3 
#define DebugSerial Serial
#define SolarVoltage A15
#define BatteryVoltage A14


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
public:  
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
    DEPLOYMENT_SERVO = 3,
    LOCK_SERVO       = 4
  };

private:
  /**
   * Creates a rover state
   */
  RoverState():
    isLocked(true), isDriving(false)
  {
    //DEBUG("Enter state constructor");
    servoCtrl.pSerial = &ServoSerial;
    servoCtrl.EnableTorque(ServoID::WHEEL_SERVO_TWO, 1);
    servoCtrl.EnableTorque(ServoID::LOCK_SERVO, 1);
    servoCtrl.EnableTorque(ServoID::DEPLOYMENT_SERVO, 1);
    servoCtrl.wheelMode(ServoID::WHEEL_SERVO_ONE);
    delay(500);
    servoCtrl.EnableTorque(ServoID::WHEEL_SERVO_ONE, 1);
    //servoCtrl.wheelMode(ServoID::WHEEL_SERVO_TWO);
    //servoCtrl.WriteSpe(ServoID::WHEEL_SERVO_ONE, 2000);
    scope = new Gyroscope();
    scope->read();
    this->gravityVectorZ = scope->values.az;
    //DEBUG("Exit state constructor");
  }
public:
  /**
   * Creates a new rover state to be used.
.
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
  
  /**
   * @brief Moves the rover in one direction
   */
  void drive() 
  {
    if (isDriving == true) {   
      //DEBUG("Is driving")
      servoCtrl.WriteSpe(1, this->servoOneTiming);
    } else {
      //DEBUG("Is not driving")
      servoCtrl.WriteSpe(1, 0);  
    }
  }
  
  /**
   * @brief Unlocks the rover from
   * Docking bay.
   */
  void unlock(bool lock) 
  {
    // orient rover using gyro readings.
    scope->read();
    this->gravityVectorZ = scope->values.az;
    if (this->gravityVectorZ  < 0) {
      this->servoOneTiming = 1023;
      this->servoTwoTiming = 1023;
    } else {
      this->servoOneTiming = -1023;
      this->servoTwoTiming = -1023;  
    }
    delay(500);
    
    // move unlocking servo
    if(lock == false){
      servoCtrl.WritePos(ServoID::LOCK_SERVO,350,1000);
      delay(1000);
    } else {
      servoCtrl.WritePos(ServoID::LOCK_SERVO,0,1000);
      delay(1000);
    }
    isLocked = lock;
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
    int solVoltage = (float(analogRead(SolarVoltage)) / 1023) * 5;
    int batVoltage = (float(analogRead(BatteryVoltage)) / 1023) * 7.4;
    this->gravityVectorZ = scope->values.az;
    const String jsonStr = String("{")
       + "ax:"+ extendHex(String(scope->values.ax, HEX)) + ","
       + "ay:"+ extendHex(String(scope->values.ay, HEX)) + ","
       + "az:"+ extendHex(String(scope->values.az, HEX)) + ","
       + "gx:"+ extendHex(String(scope->values.gx, HEX)) + ","
       + "gy:"+ extendHex(String(scope->values.gy, HEX)) + ","
       + "gz:"+ extendHex(String(scope->values.gz, HEX)) + ","
       + "bv:"+ extendHex(String(batVoltage, HEX))       + ","
       + "sv:"+ extendHex(String(solVoltage, HEX))       + "}";
    char lenStrBuf[3];
    RadioSerial.println(jsonStr + itoa(jsonStr.length(), lenStrBuf, 16));
    
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
enum CommandCode : char { DE = 1, DA = 2, DS = 3, DR = 4};

/**
 * Communicates with base station and rover.
 * Will continously send data to base station for monitoring
 * Will process commands for controlling the rover.
 * No commands for manual control.
 * cmd codes
 * DE = Unlock Rover, 1
 * DA = Rover Data, 2
 * DS = deploy solar panels, 3
 * see enum abover for definition.
 */
void radioComm() 
{
  DEBUG("Radio comm called.");
  DynamicJsonBuffer jsonBuffer;
//  if (RadioSerial.available()) 
//  {
    const String jsonStr(RadioSerial.readStringUntil('\n'));
    JsonObject& object = jsonBuffer.parseObject(jsonStr);
    String cmd = object[String("cmd")];
    
    if (cmd.toInt() == CommandCode::DE) {
      // unlock rover
      String aux = object[String("aux")];
      DEBUG("cmd: " + cmd)
      DEBUG("aux: " + aux)
      // if 1 then unlock the rover
      // else lock it back in
      if(aux.toInt() == 1){
        state->unlock(true);
      } else {
        // lock the rover in
        state->unlock(false);
      }
    } else if (cmd.toInt() == CommandCode::DA) {
      // send data to base station
      state->report();
    } else if (cmd.toInt() == CommandCode::DS) {
      // deploy solar panels
      state->deploy();
    } else if (cmd.toInt() == CommandCode::DR) {
      // deploy rover
      String aux = object[String("aux")];
      DEBUG("cmd: " + cmd)
      DEBUG("aux: " + aux)
      // if 1 then drive the rover
      // else stop
      if(aux.toInt() == 1){
        state->isDriving = true;
      } else if(aux.toInt() == 0){
        // lock the rover in
        state->isDriving = false;
      }
    }
//  } else {
//    DEBUG("No radio comm.")
//  }
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
    // orient rover using gyro readings.
    state->scope->read();
    state->gravityVectorZ = state->scope->values.az;
    if (state->gravityVectorZ  < 0) {
      state->servoOneTiming = 1023;
      state->servoTwoTiming = 1023;
    } else {
      state->servoOneTiming = -1023;
      state->servoTwoTiming = -1023;  
    }
   // process base station commands every second
   millisHandler(200, radioComm);
   // move rover 
   state->drive();
   //state->report();
}







