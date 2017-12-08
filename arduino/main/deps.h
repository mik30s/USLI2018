const String extendHex(const String& value) {
  char digits[4] = {'0','0','0','0'};
  short len = (short)value.length();
  short startPos = 4 - len;
  for (int i = 0; i < len; i++) {
     digits[startPos++] = value[i];
  }
  return String(digits);
}
