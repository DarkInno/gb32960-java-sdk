package io.github.darkinno.gb32960.core.model;

import java.util.ArrayList;
import java.util.List;

public class BatteryVoltageData {

    public static class Subsystem {
        private int subsystemNumber;
        private int cellCount;
        private List<Double> cellVoltages;

        public Subsystem() {
            this.cellVoltages = new ArrayList<>();
        }

        public int getSubsystemNumber() { return subsystemNumber; }
        public void setSubsystemNumber(int subsystemNumber) { this.subsystemNumber = subsystemNumber; }

        public int getCellCount() { return cellCount; }
        public void setCellCount(int cellCount) { this.cellCount = cellCount; }

        public List<Double> getCellVoltages() { return cellVoltages; }
        public void setCellVoltages(List<Double> cellVoltages) { this.cellVoltages = cellVoltages; }

        @Override
        public String toString() {
            return "Subsystem{#" + subsystemNumber + ", cells=" + cellCount + "}";
        }
    }

    private int subsystemCount;
    private List<Subsystem> subsystems;

    public BatteryVoltageData() {
        this.subsystems = new ArrayList<>();
    }

    public int getSubsystemCount() { return subsystemCount; }
    public void setSubsystemCount(int subsystemCount) { this.subsystemCount = subsystemCount; }

    public List<Subsystem> getSubsystems() { return subsystems; }
    public void setSubsystems(List<Subsystem> subsystems) { this.subsystems = subsystems; }

    @Override
    public String toString() {
        return "BatteryVoltageData{subsystems=" + subsystemCount + "}";
    }
}
