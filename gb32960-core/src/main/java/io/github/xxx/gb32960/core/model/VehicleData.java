package io.github.xxx.gb32960.core.model;

public class VehicleData {

    private int status;
    private int chargeStatus;
    private int runMode;
    private double speed;
    private double odometer;
    private double totalVoltage;
    private double totalCurrent;
    private int soc;
    private int dcDcStatus;
    private int gearPosition;
    private double insulationResistance;
    private int acceleratorPedal;
    private int brakePedal;

    public VehicleData() {}

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public boolean isAcceleratorOn() { return (status & 0x01) != 0; }
    public boolean isBrakeOn() { return (status & 0x02) != 0; }
    public boolean isCharging() { return (status & 0x04) != 0; }
    public boolean isAcCharging() { return (status & 0x08) != 0; }
    public boolean isDcCharging() { return (status & 0x10) != 0; }

    public int getChargeStatus() { return chargeStatus; }
    public void setChargeStatus(int chargeStatus) { this.chargeStatus = chargeStatus; }

    public int getRunMode() { return runMode; }
    public void setRunMode(int runMode) { this.runMode = runMode; }

    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }

    public double getOdometer() { return odometer; }
    public void setOdometer(double odometer) { this.odometer = odometer; }

    public double getTotalVoltage() { return totalVoltage; }
    public void setTotalVoltage(double totalVoltage) { this.totalVoltage = totalVoltage; }

    public double getTotalCurrent() { return totalCurrent; }
    public void setTotalCurrent(double totalCurrent) { this.totalCurrent = totalCurrent; }

    public int getSoc() { return soc; }
    public void setSoc(int soc) { this.soc = soc; }

    public int getDcDcStatus() { return dcDcStatus; }
    public void setDcDcStatus(int dcDcStatus) { this.dcDcStatus = dcDcStatus; }

    public int getGearPosition() { return gearPosition; }
    public void setGearPosition(int gearPosition) { this.gearPosition = gearPosition; }

    public double getInsulationResistance() { return insulationResistance; }
    public void setInsulationResistance(double insulationResistance) { this.insulationResistance = insulationResistance; }

    public int getAcceleratorPedal() { return acceleratorPedal; }
    public void setAcceleratorPedal(int acceleratorPedal) { this.acceleratorPedal = acceleratorPedal; }

    public int getBrakePedal() { return brakePedal; }
    public void setBrakePedal(int brakePedal) { this.brakePedal = brakePedal; }

    @Override
    public String toString() {
        return "VehicleData{speed=" + speed + "km/h, odometer=" + odometer + "km, soc=" + soc + "%, voltage=" + totalVoltage + "V}";
    }
}
