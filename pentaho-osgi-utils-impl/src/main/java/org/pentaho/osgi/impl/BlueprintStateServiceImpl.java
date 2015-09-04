package org.pentaho.osgi.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.karaf.bundle.core.BundleState;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.BlueprintListener;
import org.pentaho.osgi.api.BlueprintStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by bmorrise on 9/3/15.
 */

public class BlueprintStateServiceImpl implements BlueprintStateService, BlueprintListener {
  private static final Logger LOG = LoggerFactory.getLogger( BlueprintStateServiceImpl.class );
  private final Map<String, BlueprintEvent> states;
  private final List<String> bluePrints;

  public BlueprintStateServiceImpl( BundleContext bundleContext ) {
    states = new ConcurrentHashMap<String, BlueprintEvent>();
    bluePrints = new ArrayList<String>();

    for ( Bundle bundle : bundleContext.getBundles() ) {
      if ( bundle.getResource( "OSGI-INF/blueprint" ) != null ) {
        bluePrints.add( bundle.getLocation() );
      }
    }
  }

  @Override public Boolean hasBlueprint( String location ) {
    return bluePrints.contains( location );
  }

  @Override public BundleState getState( String location ) {
    BlueprintEvent event = states.get(location);
    return getState( event );
  }

  @Override
  public void blueprintEvent(BlueprintEvent blueprintEvent) {
    if (LOG.isDebugEnabled()) {
      BundleState state = getState(blueprintEvent);
      LOG.debug("Blueprint app state changed to " + state + " for bundle "
          + blueprintEvent.getBundle().getBundleId());
    }
    states.put(blueprintEvent.getBundle().getLocation(), blueprintEvent);
  }

  private BundleState getState(BlueprintEvent blueprintEvent) {
    if (blueprintEvent == null) {
      return BundleState.Unknown;
    }
    switch (blueprintEvent.getType()) {
      case BlueprintEvent.CREATING:
        return BundleState.Starting;
      case BlueprintEvent.CREATED:
        return BundleState.Active;
      case BlueprintEvent.DESTROYING:
        return BundleState.Stopping;
      case BlueprintEvent.DESTROYED:
        return BundleState.Resolved;
      case BlueprintEvent.FAILURE:
        return BundleState.Failure;
      case BlueprintEvent.GRACE_PERIOD:
        return BundleState.GracePeriod;
      case BlueprintEvent.WAITING:
        return BundleState.Waiting;
      default:
        return BundleState.Unknown;
    }
  }

}
