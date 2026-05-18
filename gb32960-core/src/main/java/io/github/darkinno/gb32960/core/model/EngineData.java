package io.github.darkinno.gb32960.core.model;

public class EngineData {

    private int status;
    private int speed;
    private double fuelRate;

    public EngineData() {}

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public int getSpeed() { return speed; }
    public void setSpeed(int speed) { this.speed = speed; }

    public double getFuelRate() { return fuelRate; }
    public void setFuelRate(double fuelRate) { this.fuelRate = fuelRate; }

    @Override
    public String toString() {
        return "EngineData{status=" + status + ", speed=" + speed + "rpm, fuelRate=" + fuelRate + "L/h}";
    }
}
