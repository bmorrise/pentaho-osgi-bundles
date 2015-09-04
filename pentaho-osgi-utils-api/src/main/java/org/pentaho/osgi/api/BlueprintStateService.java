package org.pentaho.osgi.api;

import org.apache.karaf.bundle.core.BundleState;

/**
 * Created by bmorrise on 9/3/15.
 */
public interface BlueprintStateService
{
  BundleState getState( String location );
  Boolean hasBlueprint( String location );
}
