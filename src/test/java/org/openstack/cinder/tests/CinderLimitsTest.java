package org.openstack.cinder.tests;

import com.woorea.openstack.base.client.OpenStackRequest;
import com.woorea.openstack.cinder.model.Limits;
import org.junit.Assert;
import org.junit.Test;

public class CinderLimitsTest extends AbstractCinderTest {

    @Test
    public void testGetLimits() {
        Limits limits = getLimits();
        Assert.assertNotNull(limits);
        log.info(limits);
    }

    private Limits getLimits() {
        OpenStackRequest<Limits> listRequest = getClient(getTenantId()).limits().list();
        return listRequest.execute();
    }

}
