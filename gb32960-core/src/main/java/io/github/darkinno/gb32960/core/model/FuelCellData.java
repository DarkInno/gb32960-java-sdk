package io.github.darkinno.gb32960.core.model;

import java.util.ArrayList;
import java.util.List;

public class FuelCellData {

    private double voltage;
    private double current;
    private double fuelConsumptionRate;
    private int probeCount;
    private List<Integer> probeTemperatures;
    private double hydrogenMaxPressure;
    private double hydrogenPressure;
    private int hydrogenMaxTemperature;
    private int hydrogenTemperature;

    public FuelCellData() {
        this.probeTemperatures = new ArrayList<>();
    }

    public double getVoltage() { return voltage; }
    public void setVoltage(double voltage) { this.voltage = voltage; }

    public double getCurrent() { return current; }
    public void setCurrent(double current) { this.current = current; }

    public double getFuelConsumptionRate() { return fuelConsumptionRate; }
    public void setFuelConsumptionRate(double fuelConsumptionRate) { this.fuelConsumptionRate = fuelConsumptionRate; }

    public int getProbeCount() { return probeCount; }
    public void setProbeCount(int probeCount) { this.probeCount = probeCount; }

    public List<Integer> getProbeTemperatures() { return probeTemperatures; }
    public void setProbeTemperatures(List<Integer> probeTemperatures) { this.probeTemperatures = probeTemperatures; }

    public double getHydrogenMaxPressure() { return hydrogenMaxPressure; }
    public void setHydrogenMaxPressure(double hydrogenMaxPressure) { this.hydrogenMaxPressure = hydrogenMaxPressure; }

    public double getHydrogenPressure() { return hydrogenPressure; }
    public void setHydrogenPressure(double hydrogenPressure) { this.hydrogenPressure = hydrogenPressure; }

    public int getHydrogenMaxTemperature() { return hydrogenMaxTemperature; }
    public void setHydrogenMaxTemperature(int hydrogenMaxTemperature) { this.hydrogenMaxTemperature = hydrogenMaxTemperature; }

    public int getHydrogenTemperature() { return hydrogenTemperature; }
    public void setHydrogenTemperature(int hydrogenTemperature) { this.hydrogenTemperature = hydrogenTemperature; }

    @Override
    public String toString() {
        return "FuelCellData{voltage=" + voltage + "V, current=" + current + "A, temp=" + hydrogenTemperature + "}";
    }
}
