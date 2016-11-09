int incomingByte = 0;
boolean green = true;
boolean red = true;
boolean yellow = true;
char test[15]="Hello Arduino";

void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);
  pinMode(LED_BUILTIN, OUTPUT);
  pinMode(4, OUTPUT);//GREEN LED 0x02
  pinMode(5, OUTPUT);//YELLOW LED 0x03
  pinMode(6, OUTPUT);//RED LED 0x01
}

void loop() {
  // put your main code here, to run repeatedly:
  
  if (Serial.available() > 0){
    digitalWrite(LED_BUILTIN, LOW);
    incomingByte = Serial.read();
    if (incomingByte == 0x01){
      green = false;
      red = true;
      yellow = false;
    }
    if (incomingByte == 0x02){
      green = true;
      red = false;
      yellow = false;
    }
    if (incomingByte == 0x03){
      green = false;
      red = false;
      yellow = true;
    }
    if (incomingByte == 0x04){
      green = true;
      red = true;
      yellow = true;
    }
    if (incomingByte == 0x05){
      green = false;
      red = false;
      yellow = false;
    }
    if (incomingByte == 0x06){
      digitalWrite(LED_BUILTIN, HIGH);
      Serial.print(test);
    }
  }
  if (green){
    digitalWrite(4, HIGH);
  } else {
    digitalWrite(4, LOW);
  }
  if (yellow){
    digitalWrite(5, HIGH);
  } else {
    digitalWrite(5, LOW);
  }
  if (red){
    digitalWrite(6, HIGH);
  } else {
    digitalWrite(6, LOW);
  }
}
