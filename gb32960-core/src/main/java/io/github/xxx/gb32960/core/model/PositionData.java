package io.github.xxx.gb32960.core.model;

public class PositionData {

    private double longitude;
    private double latitude;
    private double speed;
    private int direction;
    private boolean valid;

    public PositionData() {}

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }

    public int getDirection() { return direction; }
    public void setDirection(int direction) { this.direction = direction; }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    @Override
    public String toString() {
        return "PositionData{lng=" + longitude + ", lat=" + latitude + ", speed=" + speed + "km/h, dir=" + direction + "}";
    }
}
