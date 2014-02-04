package com.guidewire.tarot;

import com.google.common.base.Optional;
import org.jclouds.ContextBuilder;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaApiMetadata;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.ServerExtendedStatus;

public class JCloudsUtil {
  public static NovaApi createNovaApi(String keystone, String identity, String password) {
    return
      ContextBuilder.newBuilder(new NovaApiMetadata())
        .endpoint(keystone)
        .credentials(identity, password)
        .buildApi(NovaApi.class)
    ;
  }

  public static boolean isInUse(Server server) {
    final Server.Status status = server.getStatus();
    final Optional<ServerExtendedStatus> extended = server.getExtendedStatus();
    return extended.isPresent() ? extended.get().getPowerState() == 1 && status == Server.Status.ACTIVE : status == Server.Status.ACTIVE;
  }
}
