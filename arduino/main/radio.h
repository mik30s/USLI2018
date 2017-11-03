#pragma once

#include "Arduino.h"

class Radio {
public:
	enum AT_CMD {
		AT1
	};

	Radio(){}

	void send(AT_CMD cmd) {
	}
};