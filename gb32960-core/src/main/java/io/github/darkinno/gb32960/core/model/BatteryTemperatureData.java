package io.github.darkinno.gb32960.core.model;

import java.util.ArrayList;
import java.util.List;

public class BatteryTemperatureData {

    public static class Subsystem {
        private int subsystemNumber;
        private int probeCount;
        private List<Integer> probeTemperatures;

        public Subsystem() {
            this.probeTemperatures = new ArrayList<>();
        }

        public int getSubsystemNumber() { return subsystemNumber; }
        public void setSubsystemNumber(int subsystemNumber) { this.subsystemNumber = subsystemNumber; }

        public int getProbeCount() { return probeCount; }
        public void setProbeCount(int probeCount) { this.probeCount = probeCount; }

        public List<Integer> getProbeTemperatures() { return probeTemperatures; }
        public void setProbeTemperatures(List<Integer> probeTemperatures) { this.probeTemperatures = probeTemperatures; }

        @Override
        public String toString() {
            return "Subsystem{#" + subsystemNumber + ", probes=" + probeCount + "}";
        }
    }

    private int subsystemCount;
    private List<Subsystem> subsystems;

    public BatteryTemperatureData() {
        this.subsystems = new ArrayList<>();
    }

    public int getSubsystemCount() { return subsystemCount; }
    public void setSubsystemCount(int subsystemCount) { this.subsystemCount = subsystemCount; }

    public List<Subsystem> getSubsystems() { return subsystems; }
    public void setSubsystems(List<Subsystem> subsystems) { this.subsystems = subsystems; }

    @Override
    public String toString() {
        return "BatteryTemperatureData{subsystems=" + subsystemCount + "}";
    }
}
