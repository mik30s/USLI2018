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
    LOCK_SERVO       = 4,
    DEPLOYMENT_SERVO = 3
  };

  SCServo servoCtrl;
  Gyroscope* scope;
  long gravityVectorZ;
  bool isLocked, isDriving, isDeployed, allowOrientation;
  long driveSpeed;

private:
  /**
   * Creates a rover state
   */
  RoverState()
  :isDriving(false), isLocked(false), driveSpeed(1)
  {
    servoCtrl.pSerial = &ServoSerial;
    servoCtrl.EnableTorque(ServoID::WHEEL_SERVO_TWO, 1);
    servoCtrl.EnableTorque(ServoID::LOCK_SERVO, 1);
    servoCtrl.EnableTorque(ServoID::DEPLOYMENT_SERVO, 1);
    servoCtrl.wheelMode(ServoID::WHEEL_SERVO_ONE);
    servoCtrl.wheelMode(ServoID::WHEEL_SERVO_TWO);
    delay(500);
    servoCtrl.EnableTorque(ServoID::WHEEL_SERVO_ONE, 1);
    scope = new Gyroscope();
    gravityVectorZ = scope->read().az;
  }
    
public:  
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
  
  /**
   * @brief Drives the rover.
   */
  void drive() {
    static long v = 127;
   
    this->gravityVectorZ = scope->read().az;
   
    if (allowOrientation) {
      // move rover 
      if (this->gravityVectorZ < 0) {
        v = -127;
      } else {
        v = 127;
      }
    } else {
        v = -127;
    }

    if (isDriving) { 
      this->servoCtrl.WriteSpe(this->ServoID::WHEEL_SERVO_ONE, v * driveSpeed);
      this->servoCtrl.WriteSpe(this->ServoID::WHEEL_SERVO_TWO, -1*(v *driveSpeed));
    } else {
      this->servoCtrl.WriteSpe(this->ServoID::WHEEL_SERVO_ONE, 0);  
       this->servoCtrl.WriteSpe(this->ServoID::WHEEL_SERVO_TWO, 0); 
    }
    //delay(1000);
  }

  /**
   * @brief Unlocks the rover from
   * Docking bay.
   */
  void unlock() 
  {
    // move unlocking servo
    if (isLocked) {
      servoCtrl.WritePos(ServoID::LOCK_SERVO,300,500);
    } else {
      servoCtrl.WritePos(ServoID::LOCK_SERVO,0,500);
    }
  }

  /**
   * @brief Sends data to the base station via on board radio.
   */
  void report() {
    DebugSerial.println("reporting data...");
    scope->read();
    int solVoltage = (float(analogRead(SolarVoltage)) / 1023) * 5;
    int batVoltage = (float(analogRead(BatteryVoltage)) / 1023) * 7.4;
    this->gravityVectorZ = scope->values.az;
    const String jsonStr = String("{")
       + "ax:\""+ String(scope->values.ax) + "\","      
       + "ay:\""+ String(scope->values.ay) + "\","
       + "az:\""+ String(scope->values.az) + "\","
       + "gx:\""+ String(scope->values.gx) + "\","
       + "gy:\""+ String(scope->values.gy) + "\","
       + "gz:\""+ String(scope->values.gz) + "\","
       + "bv:\""+ String(batVoltage)       + "\","
       + "sv:\""+ String(solVoltage)       + "\"}";
    char lenStrBuf[3];
    RadioSerial.println(jsonStr);
    
    DebugSerial.println(jsonStr);
  }

  void deploy(){
     // move unlocking servo
    if(isDeployed == false){
      servoCtrl.WritePos(ServoID::DEPLOYMENT_SERVO,500,3000);
      delay(1000);
    } else {
      servoCtrl.WritePos(ServoID::DEPLOYMENT_SERVO,0,3000);
      delay(1000);
    }
  }

  void reportOrientation() {
    DebugSerial.println("reporting data...");
    RadioSerial.println(String(this->gravityVectorZ) + "\n");
  }
};

/**
 * Command codes
 * DE = Deploy Rover, 1
 * DA = Rover Data, 2
 * DS = Deploy Solar Panels, 3
 * see radioComm function for full usage.
 */
enum CommandCode : char { DE = 1, DA = 2, DS = 3, DR = 4};

// An instance of the rover state object.
RoverState* state = nullptr;
bool sendData = true;

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
void radioControlComm() 
{
  DynamicJsonBuffer jsonBuffer;
  const String jsonStr(RadioSerial.readStringUntil('\n'));
  JsonObject& object = jsonBuffer.parseObject(jsonStr);
  String cmd = object[String("cmd")];
  bool az = object[String("az")];
  String ad = object[String("ad")];
  state->allowOrientation = az;
  sendData = bool(ad.toInt());

  DebugSerial.println("allow orientation: " + String(az));
  
  if (cmd.toInt() == CommandCode::DE) 
  {
    // unlock rover
    String aux = object[String("aux")];
    DebugSerial.println("cmd: " + cmd + " aux: " + aux);
    state->isLocked = bool(aux.toInt());
    state->unlock();
  } 
  else if (cmd.toInt() == CommandCode::DS) 
  {
    String aux = object[String("aux")];
    DebugSerial.println("cmd: " + cmd + " aux: " + aux);
    state->isDeployed= bool(aux.toInt());
    state->deploy();
  } 
  else if (cmd.toInt() == CommandCode::DR) 
  {
    // deploy rover
    String aux = object[String("aux")];
    DebugSerial.println("cmd: " + cmd + " aux: " + aux);
    state->isDriving = bool(aux.toInt());
    DebugSerial.println("isDriving: "+String((int)state->isDriving));
    String dSpeed = object[String("speed")];
    state->driveSpeed = dSpeed.toInt();
    DebugSerial.println("driving: "+String(state->driveSpeed));
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


void setup() {
  // Serial for debugging
  DebugSerial.begin(9600);
  // Serial connection to onboard radio
  RadioSerial.begin(57500);
  // Serial connection to Servo TTL board
  ServoSerial.begin(1000000);
 
  state = RoverState::getInstance();
  // lock the servo
  //state->servoCtrl.WritePos(state->ServoID::DEPLOYMENT_SERVO, 500,1000);  
}

void loop() 
{
  // process base station commands every second
  millisHandler(200, radioControlComm);
  millisHandler(700, []{
      state->report();
  });
  
  // drive rover.
  state->drive();
}





