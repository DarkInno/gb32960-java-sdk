package io.github.darkinno.gb32960.core.model;

public class ExtremumData {

    private int maxBatteryVoltageSubsystem;
    private int maxBatteryVoltageCell;
    private double maxBatteryVoltage;
    private int minBatteryVoltageSubsystem;
    private int minBatteryVoltageCell;
    private double minBatteryVoltage;
    private int maxBatteryTemperatureSubsystem;
    private int maxBatteryTemperatureProbe;
    private int maxBatteryTemperature;
    private int minBatteryTemperatureSubsystem;
    private int minBatteryTemperatureProbe;
    private int minBatteryTemperature;

    public ExtremumData() {}

    public int getMaxBatteryVoltageSubsystem() { return maxBatteryVoltageSubsystem; }
    public void setMaxBatteryVoltageSubsystem(int maxBatteryVoltageSubsystem) { this.maxBatteryVoltageSubsystem = maxBatteryVoltageSubsystem; }

    public int getMaxBatteryVoltageCell() { return maxBatteryVoltageCell; }
    public void setMaxBatteryVoltageCell(int maxBatteryVoltageCell) { this.maxBatteryVoltageCell = maxBatteryVoltageCell; }

    public double getMaxBatteryVoltage() { return maxBatteryVoltage; }
    public void setMaxBatteryVoltage(double maxBatteryVoltage) { this.maxBatteryVoltage = maxBatteryVoltage; }

    public int getMinBatteryVoltageSubsystem() { return minBatteryVoltageSubsystem; }
    public void setMinBatteryVoltageSubsystem(int minBatteryVoltageSubsystem) { this.minBatteryVoltageSubsystem = minBatteryVoltageSubsystem; }

    public int getMinBatteryVoltageCell() { return minBatteryVoltageCell; }
    public void setMinBatteryVoltageCell(int minBatteryVoltageCell) { this.minBatteryVoltageCell = minBatteryVoltageCell; }

    public double getMinBatteryVoltage() { return minBatteryVoltage; }
    public void setMinBatteryVoltage(double minBatteryVoltage) { this.minBatteryVoltage = minBatteryVoltage; }

    public int getMaxBatteryTemperatureSubsystem() { return maxBatteryTemperatureSubsystem; }
    public void setMaxBatteryTemperatureSubsystem(int maxBatteryTemperatureSubsystem) { this.maxBatteryTemperatureSubsystem = maxBatteryTemperatureSubsystem; }

    public int getMaxBatteryTemperatureProbe() { return maxBatteryTemperatureProbe; }
    public void setMaxBatteryTemperatureProbe(int maxBatteryTemperatureProbe) { this.maxBatteryTemperatureProbe = maxBatteryTemperatureProbe; }

    public int getMaxBatteryTemperature() { return maxBatteryTemperature; }
    public void setMaxBatteryTemperature(int maxBatteryTemperature) { this.maxBatteryTemperature = maxBatteryTemperature; }

    public int getMinBatteryTemperatureSubsystem() { return minBatteryTemperatureSubsystem; }
    public void setMinBatteryTemperatureSubsystem(int minBatteryTemperatureSubsystem) { this.minBatteryTemperatureSubsystem = minBatteryTemperatureSubsystem; }

    public int getMinBatteryTemperatureProbe() { return minBatteryTemperatureProbe; }
    public void setMinBatteryTemperatureProbe(int minBatteryTemperatureProbe) { this.minBatteryTemperatureProbe = minBatteryTemperatureProbe; }

    public int getMinBatteryTemperature() { return minBatteryTemperature; }
    public void setMinBatteryTemperature(int minBatteryTemperature) { this.minBatteryTemperature = minBatteryTemperature; }

    @Override
    public String toString() {
        return "ExtremumData{maxVoltage=" + maxBatteryVoltage + "mV, minVoltage=" + minBatteryVoltage + "mV, maxTemp=" + maxBatteryTemperature + ", minTemp=" + minBatteryTemperature + "}";
    }
}
