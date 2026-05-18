package io.github.darkinno.gb32960.core.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RealtimeDataMessage {

    private LocalDateTime collectTime;
    private RawMessage raw;

    private VehicleData vehicleData;
    private List<DriveMotorData> driveMotorDataList;
    private FuelCellData fuelCellData;
    private EngineData engineData;
    private PositionData positionData;
    private ExtremumData extremumData;
    private AlarmData alarmData;
    private List<BatteryVoltageData> batteryVoltageDataList;
    private List<BatteryTemperatureData> batteryTemperatureDataList;

    public RealtimeDataMessage() {
        this.driveMotorDataList = new ArrayList<>();
        this.batteryVoltageDataList = new ArrayList<>();
        this.batteryTemperatureDataList = new ArrayList<>();
    }

    public LocalDateTime getCollectTime() { return collectTime; }
    public void setCollectTime(LocalDateTime collectTime) { this.collectTime = collectTime; }

    public RawMessage getRaw() { return raw; }
    public void setRaw(RawMessage raw) { this.raw = raw; }

    public VehicleData getVehicleData() { return vehicleData; }
    public void setVehicleData(VehicleData vehicleData) { this.vehicleData = vehicleData; }

    public List<DriveMotorData> getDriveMotorDataList() { return driveMotorDataList; }
    public void setDriveMotorDataList(List<DriveMotorData> driveMotorDataList) { this.driveMotorDataList = driveMotorDataList; }

    public FuelCellData getFuelCellData() { return fuelCellData; }
    public void setFuelCellData(FuelCellData fuelCellData) { this.fuelCellData = fuelCellData; }

    public EngineData getEngineData() { return engineData; }
    public void setEngineData(EngineData engineData) { this.engineData = engineData; }

    public PositionData getPositionData() { return positionData; }
    public void setPositionData(PositionData positionData) { this.positionData = positionData; }

    public ExtremumData getExtremumData() { return extremumData; }
    public void setExtremumData(ExtremumData extremumData) { this.extremumData = extremumData; }

    public AlarmData getAlarmData() { return alarmData; }
    public void setAlarmData(AlarmData alarmData) { this.alarmData = alarmData; }

    public List<BatteryVoltageData> getBatteryVoltageDataList() { return batteryVoltageDataList; }
    public void setBatteryVoltageDataList(List<BatteryVoltageData> batteryVoltageDataList) { this.batteryVoltageDataList = batteryVoltageDataList; }

    public List<BatteryTemperatureData> getBatteryTemperatureDataList() { return batteryTemperatureDataList; }
    public void setBatteryTemperatureDataList(List<BatteryTemperatureData> batteryTemperatureDataList) { this.batteryTemperatureDataList = batteryTemperatureDataList; }

    @Override
    public String toString() {
        return "RealtimeDataMessage{" +
                "vin='" + (raw != null ? raw.getVin() : "null") + '\'' +
                ", time=" + collectTime +
                ", hasVehicle=" + (vehicleData != null) +
                ", motors=" + driveMotorDataList.size() +
                ", position=" + (positionData != null) +
                '}';
    }
}
