package org.openstack.cinder.tests;

import com.woorea.openstack.base.client.OpenStackRequest;
import com.woorea.openstack.base.client.OpenStackResponseException;
import com.woorea.openstack.cinder.model.Metadata;
import com.woorea.openstack.cinder.model.Snapshot;
import com.woorea.openstack.cinder.model.SnapshotForCreate;
import com.woorea.openstack.cinder.model.SnapshotForUpdate;
import com.woorea.openstack.cinder.model.Snapshots;
import com.woorea.openstack.cinder.model.Volume;
import com.woorea.openstack.cinder.model.VolumeForCreate;
import org.apache.commons.httpclient.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class CinderSnapshotsTest extends AbstractCinderTest {

    private static final String VOLUME_TYPE = "ceph";

    @Test
    public void testGetSnapshots() {
        Snapshots snapshots = getSnapshots();
        Assert.assertNotNull(snapshots);
        for (Snapshot snapshot : snapshots) {
            log.info(snapshot);
        }
    }

    @Test(timeout = TIMEOUT)
    public void testCreateSnapshot() {
        final String volumeId = createVolume("test_create").getId();

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                Volume volume = getVolumeById(volumeId);
                switch (CinderVolumeStatus.forValue(volume.getStatus())) {
                    case Available:
                        cancel();
                        Snapshot snapshot = createSnapshot("test_create", volume.getId());
                        Assert.assertNotNull(snapshot);
                        log.info(getSnapshotById(snapshot.getId()));
                        completedMap.put("testCreateSnapshot", true);
                        break;
                }
            }
        }, 0, 1000);
        while (completedMap.get("testCreateSnapshot") == null);
    }

    @Test(timeout = TIMEOUT)
    public void testDeleteSnapshot() throws InterruptedException {
        final String volumeId = createVolume("test_delete").getId();

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            Snapshot snapshot = null;

            public void run() {
                if (snapshot == null) {
                    Volume volume = getVolumeById(volumeId);
                    switch (CinderVolumeStatus.forValue(volume.getStatus())) {
                        case Available:
                            snapshot = createSnapshot("test_create", volume.getId());
                            break;
                    }
                } else {
                    try {
                        snapshot = getSnapshotById(snapshot.getId());
                        switch (CinderVolumeStatus.forValue(snapshot.getStatus())) {
                            case Available:
                                deleteSnaphsot(snapshot.getId());
                                break;
                        }
                    } catch (OpenStackResponseException e) {
                        cancel();
                        Assert.assertEquals(e.getStatus(), HttpStatus.SC_NOT_FOUND);
                        completedMap.put("testDeleteSnapshot", true);
                    }
                }
            }
        }, 0, 1000);
        while (completedMap.get("testDeleteSnapshot") == null);
    }

    @Test(timeout = TIMEOUT)
    public void testUpdateSnapshot() {
        final String volumeId = createVolume("test_update").getId();

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                Volume volume = getVolumeById(volumeId);
                switch (CinderVolumeStatus.forValue(volume.getStatus())) {
                    case Available:
                        cancel();
                        Snapshot snapshot = createSnapshot("test_update", volume.getId());
                        String newName = "test_update_new_name";
                        String newDesc = "test_update_new_desc";

                        updateSnapshot(snapshot.getId(), newName, newDesc);
                        snapshot = getSnapshotById(snapshot.getId());
                        Assert.assertTrue(snapshot.getName().equals(newName) && snapshot.getDescription().equals(newDesc));
                        completedMap.put("testUpdateSnapshot", true);
                        break;
                }
            }
        }, 0, 1000);
        while (completedMap.get("testUpdateSnapshot") == null);
    }

    @Test(timeout = TIMEOUT)
    public void testShowMetadata() throws InterruptedException {
        final String volumeId = createVolume("test_metadata").getId();

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                Volume volume = getVolumeById(volumeId);
                switch (CinderVolumeStatus.forValue(volume.getStatus())) {
                    case Available:
                        cancel();
                        Snapshot snapshot = createSnapshot("test_metadata", volumeId);

                        Map<String, String> map = new HashMap<String, String>();
                        map.put("test_key1", "test_value1");
                        map.put("test_key2", "test_value2");
                        Metadata metadata = new Metadata();
                        metadata.setMetadata(map);
                        updateSnapshotMetadata(snapshot.getId(), metadata);

                        Metadata updatedMetadata = getSnapshotMetadata(snapshot.getId());
                        log.info(updatedMetadata);
                        completedMap.put("testShowMetadata", true);
                        break;
                }
            }
        }, 0, 1000);
        while (completedMap.get("testShowMetadata") == null);
    }

    private Snapshots getSnapshots() {
        OpenStackRequest<Snapshots> listRequest = getClient(getTenantId()).snapshots().list(true);
        return listRequest.execute();
    }

    private Snapshot getSnapshotById(String id) {
        return getClient(getTenantId()).snapshots().show(id).execute();
    }

    private Snapshot createSnapshot(String name, String volumeId) {
        SnapshotForCreate snapshotForCreate = new SnapshotForCreate();
        snapshotForCreate.setName(name);
        snapshotForCreate.setDescription("test_description");
        snapshotForCreate.setVolumeId(volumeId);
        return getClient(getTenantId()).snapshots().create(snapshotForCreate).execute();
    }

    private void deleteSnaphsot(String snapshotId) {
        getClient(getTenantId()).snapshots().delete(snapshotId).execute();
    }

    private void updateSnapshot(String id, String name, String description) {
        SnapshotForUpdate snapshotForUpdate = new SnapshotForUpdate();
        snapshotForUpdate.setName(name);
        snapshotForUpdate.setDescription(description);
        getClient(getTenantId()).snapshots().update(id, snapshotForUpdate).execute();
    }

    private Metadata getSnapshotMetadata(String snapshotId) {
        return getClient(getTenantId()).snapshots().showMetadata(snapshotId).execute();
    }

    private void updateSnapshotMetadata(String snapshotId, Metadata metadata) {
        getClient(getTenantId()).snapshots().updateMetadata(snapshotId, metadata).execute();
    }

    private Volume getVolumeById(String id) {
        return getClient(getTenantId()).volumes().show(id).execute();
    }

    private Volume createVolume(String name) {
        VolumeForCreate volumeForCreate = new VolumeForCreate();
        volumeForCreate.setName(name);
        volumeForCreate.setDescription("test_description");
        volumeForCreate.setSize(1);
        volumeForCreate.setVolumeType(VOLUME_TYPE);
        return getClient(getTenantId()).volumes().create(volumeForCreate).execute();
    }
}
