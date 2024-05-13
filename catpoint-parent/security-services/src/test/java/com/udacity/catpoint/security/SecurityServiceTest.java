package com.udacity.catpoint.security;

import java.awt.image.BufferedImage;

import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import com.udacity.catpoint.image.service.ImageServices;
import com.udacity.catpoint.security.service.SecurityService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
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
    public Sensor sensor;
    public String random = UUID.randomUUID().toString();

    public SecurityService securityService;

    @Mock
    public SecurityRepository securityRepository;

    @Mock
    public ImageServices imageServices;

    public StatusListener statusListener;

    public Sensor getNewSensor() {
        return new Sensor(random, SensorType.WINDOW);
    }

    public Set<Sensor> setupSensors(int numSensors, boolean status) {
        Set<Sensor> sensorSet = new HashSet<>();

        for (int index = 0; index < numSensors; index++) {
            sensorSet.add(getNewSensor());
        }

        if(status){
            sensorSet.forEach(sensor -> sensor.setActive(true));
        }

        return sensorSet;
    }

    @BeforeEach
    public void initTest() {
        securityService = new SecurityService(securityRepository, imageServices);
        sensor = getNewSensor();
    }

    /**
     * Unit test of req 1: If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
     *
     * @param armingStatus arming status
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void armedAlarmAndActivatedSensor_AlarmStatusToPending(ArmingStatus armingStatus)
    {
        /* alarm is armed */
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        /* activate a sensor */
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        /* the system must be pushed into pending alarm status. */
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    /**
     * Unit test of req 2: If alarm is armed and a sensor becomes activated and the system is already pending alarm,
     *                   set the alarm status to alarm.
     *
     * @param armingStatus arming status
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void armedAlarmAndActivatedSensorAndPendingAlarmStatus_AlarmStatusToAlarm(ArmingStatus armingStatus)
    {
        Set<Sensor> allSensors = setupSensors(2, false);
        /* get particular sensor in the set */
        sensor = allSensors.iterator().next();
        /* alarm is armed */
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        /* system in pending alarm */
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        /* activate a sensor */
        securityService.changeSensorActivationStatus(sensor, true);
        /* the system must be pushed into PENDING ALARM status. */
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    /**
     * Unit test of req 3: If pending alarm and all sensors are inactive, return to no alarm state.
     */
    @Test
    public void pendingAlarmAndInactiveSensor_AlarmStatusToNoAlarm()
    {
        Set<Sensor> allSensors = setupSensors(2, false);
        /* get particular sensor in the set */
        sensor = allSensors.iterator().next();
        sensor.setActive(true);

        /* system in pending alarm */
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, false);
        /* the system must be pushed into NO ALARM status. */
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /**
     * Unit test of req 4: If alarm is active, change in sensor state should not affect the alarm state.
     * Note: The user can reproduce this behavior when at least two sensors are added.
     *       When going to the system in ARMED_HOME or ARMED_AWAY mode, if user activates
     *       the first sensor it causes the alarm to go to the PENDING_ALARM state,
     *       and when activating the second sensor, the system goes to the ALARM state.
     *       Now, any change in the sensor state should not change the status of the alarm from ALARM state.
     *       It makes sense because the system is already telling the user that they are in danger,
     *       so no change in sensors should stop this behavior.
     * 
     * @param sensorStatus sensor status
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void activeAlarm_ChangeSensorStateNotAffectAlarmState(boolean sensorStatus)
    {
        /* system in ALARM state */
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        if(!sensorStatus){
            sensor.setActive(true);
        }
        securityService.changeSensorActivationStatus(sensor, sensorStatus);
        /* the alarm status shall not be affected by the change of sensor state, so setAlarmStatus will not be called. */
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    /**
     * Unit test of req 5: If a sensor is activated while already active and the system is in pending state,
     *                     change it to alarm state.
     * @param armingStatus arming status
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void activateActiveSensorAndPendingAlarm_AlarmStatusToAlarm(ArmingStatus armingStatus)
    {
        /* set a sensor active */
        sensor.setActive(true);

        /* system in PENDING state */
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        /* alarm is armed because the system is in PENDING state */
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);

        securityService.changeSensorActivationStatus(sensor, true);
        /* the alarm status must be changed to ALARM */
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    /**
     * Unit test of req 6: If a sensor is deactivated while already inactive, make no changes to the alarm state.
     */
    @Test
    public void detivateActiveSensor_NoChangeToAlarmState()
    {
        /* deactie an inactie sensor */
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);
        /* no changes to the alarm state*/
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    /**
     * Unit test of req 7: If the image service identifies an image containing a cat while the system is armed-home,
     *                     put the system into alarm status.
     */
    @Test
    public void detectCatImageAndSystemIsArmedhome_AlarmStatusToAlarm()
    {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        /* system is armed-home */
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        /* detected cat image */
        when(imageServices.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(image);
        /* the alarm status must be changed to ALARM */
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    /**
     * Unit test of req 8: If the image service identifies an image that does not contain a cat, 
     *                     change the status to no alarm as long as the sensors are not active.
     */
    @Test
    public void detectNoCatImageAndNotActiveSensor_AlarmStatusToNoAlarm()
    {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        /* detected no cat image */
        when(imageServices.imageContainsCat(any(), anyFloat())).thenReturn(false);
        securityService.processImage(image);
        /* the alarm status must be changed to NO ALARM */
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /**
     * Unit test of req 9: If the system is disarmed, set the status to no alarm.
     */
    @Test
    public void disarmedSystem_AlarmStatusToNoAlarm()
    {
        /* system is disarmed */
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        /* the alarm status must be changed to NO ALARM */
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /**
     * Unit test of req 10: If the system is armed, reset all sensors to inactive.
     * Note: The user can reproduce this behavior by activating some sensors,
     *       and after clicking on the ARMED_HOME or ARMED_AWAY button user 
     *       will see that all sensors went to the inactive state.
     *
     * @param armingStatus arming status
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void armedSystem_resetAllSensorsToInactive(ArmingStatus armingStatus)
    {
        Set<Sensor> allSensors = setupSensors(2, false);

        /* get set of sensors */
        when(securityRepository.getSensors()).thenReturn(allSensors);
        /* arm the system */
        securityService.setArmingStatus(armingStatus);
        /* check if all the sensor are inactive */
        securityService.getSensors().forEach(sensor -> Assertions.assertFalse(sensor.getActive()));
    }

    /**
     * Unit test of req 11: If the system is armed-home while the camera shows a cat,
     *                      set the alarm status to alarm.
     * Note: This behavior can be reproduced when the user is in the system as ARMED_AWAY or DISARMED.
     *       In this system, when the scan button is clicked after adding the cat image,
     *       the result comes out to be positive. After that, when changing to ARMED_HOME,
     *       the alarm status should change to ALARM.
     * 
     * @param armingStatus arming status
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"DISARMED", "ARMED_AWAY"})
    public void armehomedSystemAndDetectCatImage_AlarmStatusToAlarm(ArmingStatus armingStatus)
    {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        /* first disarmed the system */
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        /* detected cat image */
        when(imageServices.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(image);
        /* the system is armed-home */
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        /* the alarm status must be changed to ALARM */
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    /* Additional test for code coverage */
    @Test
    public void CCOV_AddAndRemoveStatusListener()
    {
        securityService.addStatusListener(statusListener);
        securityService.removeStatusListener(statusListener);
    }

    @Test
    public void CCOV_AddAndRemoveSensor()
    {
        securityService.addSensor(sensor);
        securityService.removeSensor(sensor);
    }
}
