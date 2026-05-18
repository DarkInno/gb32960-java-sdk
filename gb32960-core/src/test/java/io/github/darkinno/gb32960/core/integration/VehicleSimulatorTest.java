package io.github.darkinno.gb32960.core.integration;

import io.github.darkinno.gb32960.core.codec.MessageDecoder;
import io.github.darkinno.gb32960.core.codec.MessageEncoder;
import io.github.darkinno.gb32960.core.constant.CommandFlag;
import io.github.darkinno.gb32960.core.constant.ResponseFlag;
import io.github.darkinno.gb32960.core.model.*;
import io.github.darkinno.gb32960.core.util.TestMessageBuilder;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class VehicleSimulatorTest {

    private static final String VIN = "LSVAM40E7GA000001";

    @Test
    void shouldSimulateFullVehicleLifecycle() {
        List<String> events = new ArrayList<>();

        byte[] loginBytes = TestMessageBuilder.buildVehicleLogin(VIN, 1, "89860000000000000001", 1, 4);
        RawMessage loginRaw = MessageDecoder.decodeRaw(loginBytes);
        VehicleLoginMessage login = (VehicleLoginMessage) MessageDecoder.decode(loginRaw);

        assertEquals(VIN, login.getRaw().getVin());
        assertEquals("89860000000000000001", login.getIccid());
        assertNotNull(login.getCollectTime());
        events.add("LOGIN");

        byte[] loginResp = MessageEncoder.buildResponse(loginRaw, ResponseFlag.SUCCESS, null);
        RawMessage loginRespRaw = MessageDecoder.decodeRaw(loginResp);
        assertEquals(ResponseFlag.SUCCESS, loginRespRaw.getResponseFlag());
        events.add("LOGIN_RESP");

        for (int i = 0; i < 5; i++) {
            byte[] rtBytes = TestMessageBuilder.buildRealtimeData(
                    VIN, 60.0 + i * 5, 85 - i, 121.47 + i * 0.01, 31.23 + i * 0.01);
            RawMessage rtRaw = MessageDecoder.decodeRaw(rtBytes);
            RealtimeDataMessage rt = (RealtimeDataMessage) MessageDecoder.decode(rtRaw);

            assertNotNull(rt.getVehicleData());
            assertNotNull(rt.getPositionData());
            assertEquals(VIN, rt.getRaw().getVin());

            byte[] rtResp = MessageEncoder.buildResponse(rtRaw, ResponseFlag.SUCCESS, null);
            RawMessage rtRespRaw = MessageDecoder.decodeRaw(rtResp);
            assertEquals(ResponseFlag.SUCCESS, rtRespRaw.getResponseFlag());
            events.add("DATA_" + i);
        }

        byte[] hbBytes = TestMessageBuilder.buildHeartbeat(VIN);
        RawMessage hbRaw = MessageDecoder.decodeRaw(hbBytes);
        HeartbeatMessage hb = (HeartbeatMessage) MessageDecoder.decode(hbRaw);
        assertNotNull(hb);
        events.add("HEARTBEAT");

        byte[] hbResp = MessageEncoder.buildResponse(hbRaw, ResponseFlag.SUCCESS, null);
        RawMessage hbRespRaw = MessageDecoder.decodeRaw(hbResp);
        assertEquals(ResponseFlag.SUCCESS, hbRespRaw.getResponseFlag());
        events.add("HEARTBEAT_RESP");

        byte[] logoutBytes = TestMessageBuilder.buildVehicleLogout(VIN, 2);
        RawMessage logoutRaw = MessageDecoder.decodeRaw(logoutBytes);
        VehicleLogoutMessage logout = (VehicleLogoutMessage) MessageDecoder.decode(logoutRaw);
        assertEquals(2, logout.getSerialNumber());
        events.add("LOGOUT");

        byte[] logoutResp = MessageEncoder.buildResponse(logoutRaw, ResponseFlag.SUCCESS, null);
        RawMessage logoutRespRaw = MessageDecoder.decodeRaw(logoutResp);
        assertEquals(ResponseFlag.SUCCESS, logoutRespRaw.getResponseFlag());
        events.add("LOGOUT_RESP");

        assertEquals(
                List.of("LOGIN", "LOGIN_RESP", "DATA_0", "DATA_1", "DATA_2", "DATA_3", "DATA_4",
                        "HEARTBEAT", "HEARTBEAT_RESP", "LOGOUT", "LOGOUT_RESP"),
                events);
    }

    @Test
    void shouldSimulateMultiVehicleConcurrency() throws Exception {
        int vehicleCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch latch = new CountDownLatch(vehicleCount);
        ConcurrentHashMap<String, String> results = new ConcurrentHashMap<>();

        for (int i = 0; i < vehicleCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    String vin = String.format("LSVAM40E7GA%06d", idx);
                    byte[] bytes = TestMessageBuilder.buildVehicleLogin(vin, 1, "89860000000000000001", 1, 4);
                    RawMessage raw = MessageDecoder.decodeRaw(bytes);
                    VehicleLoginMessage msg = (VehicleLoginMessage) MessageDecoder.decode(raw);
                    results.put(vin, msg.getIccid());
                } catch (Exception e) {
                    results.put("ERROR_" + idx, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(vehicleCount, results.size());
        results.forEach((vin, iccid) -> {
            assertTrue(vin.startsWith("LSVAM40E7GA"));
            assertEquals("89860000000000000001", iccid);
        });
    }

    @Test
    void shouldSimulateHighVolumeMessages() {
        int messageCount = 10_000;
        for (int i = 0; i < messageCount; i++) {
            byte[] bytes = TestMessageBuilder.buildRealtimeData(
                    VIN, 50 + (i % 80), 80 - (i % 30),
                    121.47 + (i % 100) * 0.001, 31.23 + (i % 100) * 0.001);
            RawMessage raw = MessageDecoder.decodeRaw(bytes);
            RealtimeDataMessage rt = (RealtimeDataMessage) MessageDecoder.decode(raw);
            assertNotNull(rt);
        }
    }

    @Test
    void shouldHandleBatteryData() {
        byte[] bytes = TestMessageBuilder.buildRealtimeDataFull(
                VIN, 50, 80, 50000, 350, -20,
                121.5, 31.2, 2500, 55);

        RawMessage raw = MessageDecoder.decodeRaw(bytes);
        RealtimeDataMessage rt = (RealtimeDataMessage) MessageDecoder.decode(raw);

        VehicleData vd = rt.getVehicleData();
        assertEquals(80, vd.getSoc());
        assertEquals(350.0, vd.getTotalVoltage(), 0.01);
        assertEquals(-20.0, vd.getTotalCurrent(), 0.01);

        assertTrue(vd.isAcceleratorOn());
        assertFalse(vd.isBrakeOn());
    }

    @Test
    void shouldSimulatePlatformLoginFlow() {
        byte[] loginBytes = TestMessageBuilder.buildPlatformLogin("admin", "secret");
        RawMessage raw = MessageDecoder.decodeRaw(loginBytes);
        PlatformLoginMessage msg = (PlatformLoginMessage) MessageDecoder.decode(raw);

        assertEquals("admin", msg.getUsername());
        assertEquals("secret", msg.getPassword());
        assertEquals(1, msg.getSerialNumber());

        byte[] resp = MessageEncoder.buildResponse(raw, ResponseFlag.SUCCESS, null);
        RawMessage respRaw = MessageDecoder.decodeRaw(resp);
        assertEquals(ResponseFlag.SUCCESS, respRaw.getResponseFlag());
    }

    @Test
    void shouldSimulateTimingCheck() {
        byte[] timingBytes = TestMessageBuilder.buildTimingResponse(VIN);
        RawMessage raw = MessageDecoder.decodeRaw(timingBytes);
        TimingResponseMessage msg = (TimingResponseMessage) MessageDecoder.decode(raw);

        assertNotNull(msg);
        assertNotNull(msg.getCollectTime());
        assertEquals(VIN, raw.getVin());
    }

    @Test
    void shouldVerifyAllInfoTypesInRealtimeData() {
        byte[] bytes = TestMessageBuilder.buildRealtimeDataFull(
                VIN, 60, 75, 10000, 370, -10,
                121.5, 31.2, 2000, 50);

        RawMessage raw = MessageDecoder.decodeRaw(bytes);
        RealtimeDataMessage rt = (RealtimeDataMessage) MessageDecoder.decode(raw);

        assertNotNull(rt.getVehicleData(), "Vehicle data should be present");
        assertNotNull(rt.getPositionData(), "Position data should be present");
        assertNotNull(rt.getExtremumData(), "Extremum data should be present");
        assertNotNull(rt.getAlarmData(), "Alarm data should be present");
        assertFalse(rt.getDriveMotorDataList().isEmpty(), "Drive motor data should be present");
    }
}
