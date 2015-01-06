package org.openstack.cinder.tests;

import com.woorea.openstack.base.client.OpenStackRequest;
import com.woorea.openstack.base.client.OpenStackResponseException;
import com.woorea.openstack.cinder.model.VolumeType;
import com.woorea.openstack.cinder.model.VolumeTypeForCreate;
import com.woorea.openstack.cinder.model.VolumeTypes;
import org.apache.commons.httpclient.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class CinderVolumeTypesTest extends AbstractCinderTest {

    @Test
    public void testGetVolumeTypes() {
        VolumeTypes volumeTypes = getVolumeTypes();
        Assert.assertNotNull(volumeTypes);
        for (VolumeType volumeType : volumeTypes) {
            log.info(volumeType);
        }
    }

    @Test
    public void testCreateVolumeType() {
        VolumeType volumeType = createVolumeType("test_create");
        Assert.assertNotNull(volumeType);
        log.info(getVolumeTypeById(volumeType.getId()));
    }

    @Test
    public void testDeleteVolumeType() throws InterruptedException {
        final String newVolumeTypeId = createVolumeType("test_delete").getId();

        deleteVolumeType(newVolumeTypeId);
        try {
            getVolumeTypeById(newVolumeTypeId);
        } catch (OpenStackResponseException e) {
            Assert.assertEquals(e.getStatus(), HttpStatus.SC_NOT_FOUND);
        }
    }

    private VolumeTypes getVolumeTypes() {
        OpenStackRequest<VolumeTypes> listRequest = getClient(getTenantId()).volumeTypes().list();
        return listRequest.execute();
    }

    private VolumeType getVolumeTypeById(String id) {
        return getClient(getTenantId()).volumeTypes().show(id).execute();
    }

    private VolumeType createVolumeType(String name) {
        VolumeTypeForCreate volumeTypeForCreate = new VolumeTypeForCreate();
        volumeTypeForCreate.setName(name);

        Map<String, Object> extraSpecs = new HashMap<String, Object>();
        extraSpecs.put("volume_backend_name", "ceph");
        volumeTypeForCreate.setExtraSpecs(extraSpecs);

        return getClient(getTenantId()).volumeTypes().create(volumeTypeForCreate).execute();
    }

    private void deleteVolumeType(String volumeTypeId) {
        getClient(getTenantId()).volumeTypes().delete(volumeTypeId).execute();
    }

}
