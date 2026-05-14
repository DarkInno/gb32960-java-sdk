package io.github.xxx.gb32960.core.model;

import java.util.ArrayList;
import java.util.List;

public class DriveMotorData {

    public static class MotorInfo {
        private int serial;
        private int status;
        private int controllerTemperature;
        private int speed;
        private double torque;
        private int motorTemperature;
        private double controllerInputVoltage;
        private double dcBusCurrent;

        public MotorInfo() {}

        public int getSerial() { return serial; }
        public void setSerial(int serial) { this.serial = serial; }

        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }

        public int getControllerTemperature() { return controllerTemperature; }
        public void setControllerTemperature(int controllerTemperature) { this.controllerTemperature = controllerTemperature; }

        public int getSpeed() { return speed; }
        public void setSpeed(int speed) { this.speed = speed; }

        public double getTorque() { return torque; }
        public void setTorque(double torque) { this.torque = torque; }

        public int getMotorTemperature() { return motorTemperature; }
        public void setMotorTemperature(int motorTemperature) { this.motorTemperature = motorTemperature; }

        public double getControllerInputVoltage() { return controllerInputVoltage; }
        public void setControllerInputVoltage(double controllerInputVoltage) { this.controllerInputVoltage = controllerInputVoltage; }

        public double getDcBusCurrent() { return dcBusCurrent; }
        public void setDcBusCurrent(double dcBusCurrent) { this.dcBusCurrent = dcBusCurrent; }

        @Override
        public String toString() {
            return "MotorInfo{#" + serial + ", speed=" + speed + "rpm, temp=" + motorTemperature + "}";
        }
    }

    private int motorCount;
    private List<MotorInfo> motors;

    public DriveMotorData() {
        this.motors = new ArrayList<>();
    }

    public int getMotorCount() { return motorCount; }
    public void setMotorCount(int motorCount) { this.motorCount = motorCount; }

    public List<MotorInfo> getMotors() { return motors; }
    public void setMotors(List<MotorInfo> motors) { this.motors = motors; }

    @Override
    public String toString() {
        return "DriveMotorData{count=" + motorCount + ", motors=" + motors.size() + "}";
    }
}
