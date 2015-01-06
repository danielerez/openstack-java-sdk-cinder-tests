package org.openstack.cinder.tests;

import com.woorea.openstack.base.client.HttpMethod;
import com.woorea.openstack.base.client.OpenStackRequest;
import com.woorea.openstack.base.client.OpenStackTokenProvider;
import com.woorea.openstack.cinder.Cinder;
import com.woorea.openstack.keystone.model.Access;
import com.woorea.openstack.keystone.utils.KeystoneTokenProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

public class AbstractCinderTest {

    private static final String CINDER_URL = ""; // e.g. http://<FQDN>:8776
    private static final String AUTH_URL = ""; // e.g. http://<FQDN>:5000/v2.0

    private static final String USERNAME = "cinder";
    private static final String PASSWORD = "";
    private static final String TENANT_NAME = "services";
    private static final String API_VERSION = "/v2/";

    protected static final Log log = LogFactory.getLog(AbstractCinderTest.class);
    public static final long TIMEOUT = 1000 * 60;

    private Cinder client;
    KeystoneTokenProvider keystoneTokenProvider;

    @Test
    public void testConnection() {
        try {
            getClient("").execute(new OpenStackRequest<Cinder>(getClient(""), HttpMethod.GET, "", null, null));
        } catch (RuntimeException e) {
            log.error(e);
        }
    }

    protected Cinder getClient(String tenantId) {
        if (client == null) {
            client = new Cinder(CINDER_URL.concat(API_VERSION).concat(tenantId));
            client.setTokenProvider(getTokenProvider());
        }
        return client;
    }

    protected String getTenantId() {
        return getAccess().getToken().getTenant().getId();
    }

    private Access getAccess() {
        return getKeystoneTokenProvider().getAccessByTenant(TENANT_NAME);
    }

    private OpenStackTokenProvider getTokenProvider() {
        return getKeystoneTokenProvider().getProviderByTenant(TENANT_NAME);
    }

    private KeystoneTokenProvider getKeystoneTokenProvider() {
        if (keystoneTokenProvider == null) {
            keystoneTokenProvider = new KeystoneTokenProvider(AUTH_URL, USERNAME, PASSWORD);
        }
        return keystoneTokenProvider;
    }
}
