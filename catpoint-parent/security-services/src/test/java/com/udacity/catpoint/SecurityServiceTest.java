package com.udacity.catpoint;

import com.udacity.catpoint.data.*;
import com.udacity.catpoint.image.service.ImageServices;
import com.udacity.catpoint.service.SecurityService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Unit tests for security services.
 */
@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest
{
    private Sensor sensor;
    private final String random = UUID.randomUUID().toString();

    @Mock
    private SecurityService securityService;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageServices imageServices;

    private Sensor getNewSensor() {
        return new Sensor(random, SensorType.DOOR);
    }

    private Set<Sensor> getAllSensors(int numSensors, boolean status) {
        Set<Sensor> sensorSet = new HashSet<>();
        int index = 0;

        for (index = 0; index < numSensors; index++) {
            sensorSet.add(new Sensor(random, SensorType.DOOR));
        }
        sensorSet.forEach(sensor -> sensor.setActive(status));

        return sensorSet;
    }

    @BeforeEach
    void initTest() {
        securityService = new SecurityService(securityRepository, imageServices);
        sensor = getNewSensor();
    }

    /**
     * Unit test of req: If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
     *
     * @param armingStatus arming status
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void armedAlarmAndActivatedSensor_AlarmStatusToPending(ArmingStatus armingStatus)
    {
        /* alarm is armed */
        when(securityService.getArmingStatus()).thenReturn(armingStatus);
        /* activate sensor */
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        /* the system must be pushed into pending alarm status. */
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    /**
     * Unit test of req: If alarm is armed and a sensor becomes activated and the system is already pending alarm,
     *                   set the alarm status to alarm.
     *
     * @param armingStatus arming status
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void armedAlarmAndActivatedSensorAndPendingAlarmStatus_AlarmStatusToAlarm(ArmingStatus armingStatus)
    {
        /* alarm is armed */
        when(securityService.getArmingStatus()).thenReturn(armingStatus);
        /* system in pending alarm */
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        /* activate sensor */
        securityService.changeSensorActivationStatus(sensor, true);
        /* the system must be pushed into pending alarm status. */
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }
}
