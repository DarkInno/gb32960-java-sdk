package io.github.darkinno.gb32960.core.model;

import java.util.ArrayList;
import java.util.List;

public class AlarmData {

    private int maxAlarmLevel;
    private long generalAlarmFlags;
    private List<Long> generalAlarmCodes;
    private long batteryAlarmFlags;
    private List<Long> batteryAlarmCodes;
    private long driveMotorAlarmFlags;
    private List<Long> driveMotorAlarmCodes;
    private long engineAlarmFlags;
    private List<Long> engineAlarmCodes;
    private long otherAlarmFlags;

    public AlarmData() {
        this.generalAlarmCodes = new ArrayList<>();
        this.batteryAlarmCodes = new ArrayList<>();
        this.driveMotorAlarmCodes = new ArrayList<>();
        this.engineAlarmCodes = new ArrayList<>();
    }

    public int getMaxAlarmLevel() { return maxAlarmLevel; }
    public void setMaxAlarmLevel(int maxAlarmLevel) { this.maxAlarmLevel = maxAlarmLevel; }

    public long getGeneralAlarmFlags() { return generalAlarmFlags; }
    public void setGeneralAlarmFlags(long generalAlarmFlags) { this.generalAlarmFlags = generalAlarmFlags; }

    public List<Long> getGeneralAlarmCodes() { return generalAlarmCodes; }
    public void setGeneralAlarmCodes(List<Long> generalAlarmCodes) { this.generalAlarmCodes = generalAlarmCodes; }

    public long getBatteryAlarmFlags() { return batteryAlarmFlags; }
    public void setBatteryAlarmFlags(long batteryAlarmFlags) { this.batteryAlarmFlags = batteryAlarmFlags; }

    public List<Long> getBatteryAlarmCodes() { return batteryAlarmCodes; }
    public void setBatteryAlarmCodes(List<Long> batteryAlarmCodes) { this.batteryAlarmCodes = batteryAlarmCodes; }

    public long getDriveMotorAlarmFlags() { return driveMotorAlarmFlags; }
    public void setDriveMotorAlarmFlags(long driveMotorAlarmFlags) { this.driveMotorAlarmFlags = driveMotorAlarmFlags; }

    public List<Long> getDriveMotorAlarmCodes() { return driveMotorAlarmCodes; }
    public void setDriveMotorAlarmCodes(List<Long> driveMotorAlarmCodes) { this.driveMotorAlarmCodes = driveMotorAlarmCodes; }

    public long getEngineAlarmFlags() { return engineAlarmFlags; }
    public void setEngineAlarmFlags(long engineAlarmFlags) { this.engineAlarmFlags = engineAlarmFlags; }

    public List<Long> getEngineAlarmCodes() { return engineAlarmCodes; }
    public void setEngineAlarmCodes(List<Long> engineAlarmCodes) { this.engineAlarmCodes = engineAlarmCodes; }

    public long getOtherAlarmFlags() { return otherAlarmFlags; }
    public void setOtherAlarmFlags(long otherAlarmFlags) { this.otherAlarmFlags = otherAlarmFlags; }

    @Override
    public String toString() {
        return "AlarmData{maxLevel=" + maxAlarmLevel + ", generalFlags=" + generalAlarmFlags + "}";
    }
}
