package com.woorea.openstack.cinder.test;

import com.woorea.openstack.base.client.OpenStackRequest;
import com.woorea.openstack.base.client.OpenStackResponseException;
import com.woorea.openstack.cinder.model.ConnectionInfo;
import com.woorea.openstack.cinder.model.Volume;
import com.woorea.openstack.cinder.model.VolumeForCreate;
import com.woorea.openstack.cinder.model.VolumeForUpdate;
import com.woorea.openstack.cinder.model.Volumes;
import org.apache.commons.httpclient.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import java.util.Timer;
import java.util.TimerTask;

public class CinderVolumesTest extends AbstractCinderTest {

    private static final String VOLUME_TYPE = "ceph";

    @Test
    public void testGetVolumes() {
        Volumes volumes = getVolumes();
        Assert.assertNotNull(volumes);
        for (Volume volume : volumes) {
            log.info(volume);
        }
    }

    @Test
    public void testCreateVolume() {
        Volume volume = createVolume("test_create");
        Assert.assertNotNull(volume);
        log.info(getVolumeById(volume.getId()));
    }

    @Test(timeout = TIMEOUT)
    public void testDeleteVolume() throws InterruptedException {
        final Thread thread = Thread.currentThread();
        final String newVolumeId = createVolume("test_delete").getId();

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                try {
                    Volume volume = getVolumeById(newVolumeId);
                    switch (CinderVolumeStatus.forValue(volume.getStatus())) {
                        case Available:
                            deleteVolume(newVolumeId);
                            break;
                        case Deleting:
                            log.info(String.format("Deleting volume %s", newVolumeId));
                            break;
                        case ErrorDeleting:
                            log.error(String.format("Failed to delete volume %s", newVolumeId));
                            cancel();
                            thread.interrupt();
                            break;
                    }
                } catch (OpenStackResponseException e) {
                    cancel();
                    Assert.assertEquals(e.getStatus(), HttpStatus.SC_NOT_FOUND);
                    thread.interrupt();
                }
            }
        }, 0, 1000);
        while (!thread.isInterrupted()) ;
    }

    @Test
    public void testUpdateVolume() {
        String volumeId = createVolume("test_update").getId();
        String newName = "test_update_new_name";
        String newDesc = "test_update_new_desc";

        updateVolume(volumeId, newName, newDesc);
        Volume volume = getVolumeById(volumeId);
        Assert.assertTrue(volume.getName().equals(newName) && volume.getDescription().equals(newDesc));
    }

    @Test(timeout = TIMEOUT)
    public void testExtendVolume() {
        final Thread thread = Thread.currentThread();
        final String volumeId = createVolume("test_extend").getId();

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                Volume volume = getVolumeById(volumeId);
                switch (CinderVolumeStatus.forValue(volume.getStatus())) {
                    case Available:
                        if (volume.getSize() == 1) {
                            extendVolume(volumeId, 2);
                        } else {
                            cancel();
                            Assert.assertEquals(volume.getSize(), Integer.valueOf(2));
                            thread.interrupt();
                        }
                        break;
                    case Extending:
                        log.info(String.format("Extending volume %s", volumeId));
                        break;
                    case ErrorExtending:
                        log.error(String.format("Error extending volume %s", volumeId));
                        break;
                }
            }
        }, 0, 1000);
        while (!thread.isInterrupted()) ;
    }

    @Test
    public void testInitializeConnectionForVolume() {
        String volumeId = createVolume("test_initialize_connection").getId();
        ConnectionInfo connectionInfo = initializeConnectionForVolume(volumeId);
        log.info(String.format("ConnectionInfo: %s", connectionInfo));
        Assert.assertNotNull(connectionInfo);
    }

    @Test
    public void testCloneVolume() {
        final Thread thread = Thread.currentThread();
        final String volumeId = createVolume("test_volume_to_clone").getId();

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                Volume volume = getVolumeById(volumeId);
                switch (CinderVolumeStatus.forValue(volume.getStatus())) {
                    case Available:
                        cancel();
                        Volume volumeClone = createVolume("test_cloned_volume", volumeId);
                        Assert.assertEquals(volumeClone.getSourceVolid(), volumeId);
                        thread.interrupt();
                        break;
                }
            }
        }, 0, 1000);
        while (!thread.isInterrupted()) ;
    }

    private Volumes getVolumes() {
        OpenStackRequest<Volumes> listRequest = getClient(getTenantId()).volumes().list(true);
        return listRequest.execute();
    }

    private Volume getVolumeById(String id) {
        return getClient(getTenantId()).volumes().show(id).execute();
    }

    private Volume createVolume(String name) {
        return createVolume(name, null);
    }

    private Volume createVolume(String name, String source_volid) {
        VolumeForCreate volumeForCreate = new VolumeForCreate();
        volumeForCreate.setName(name);
        volumeForCreate.setDescription("test_description");
        volumeForCreate.setSize(1);
        volumeForCreate.setVolumeType(VOLUME_TYPE);
        volumeForCreate.setSourceVolid(source_volid);
        return getClient(getTenantId()).volumes().create(volumeForCreate).execute();
    }

    private void deleteVolume(String volumeId) {
        getClient(getTenantId()).volumes().delete(volumeId).execute();
    }

    private void updateVolume(String id, String name, String description) {
        VolumeForUpdate volumeForUpdate = new VolumeForUpdate();
        volumeForUpdate.setName(name);
        volumeForUpdate.setDescription(description);
        getClient(getTenantId()).volumes().update(id, volumeForUpdate).execute();
    }

    private void extendVolume(String volumeId, int newSize) {
        getClient(getTenantId()).volumes().extend(volumeId, newSize).execute();
    }

    private ConnectionInfo initializeConnectionForVolume(String volumeId) {
        return getClient(getTenantId()).volumes().initializeConnection(volumeId).execute();
    }

}
