/*
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright 2015 Pentaho Corporation. All rights reserved.
 */

package org.pentaho.osgi.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.karaf.bundle.core.BundleState;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.pentaho.osgi.api.BlueprintStateService;
import org.pentaho.osgi.api.IKarafFeatureWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nbaker on 2/19/15.
 */
public class KarafFeatureWatcherImpl implements IKarafFeatureWatcher {
  private BundleContext bundleContext;
  private long timeout = 60 * 1000L;
  private Logger logger = LoggerFactory.getLogger( getClass() );

  public KarafFeatureWatcherImpl( BundleContext bundleContext ) {

    this.bundleContext = bundleContext;
  }

  @Override public void waitForFeatures() throws FeatureWatcherException {

    long entryTime = System.currentTimeMillis();

    ServiceTracker serviceTracker = new ServiceTracker( bundleContext, FeaturesService.class.getName(), null );
    serviceTracker.open();
    try {
      serviceTracker.waitForService( timeout );
    } catch ( InterruptedException e ) {
      logger.debug( "FeaturesService Service Tracker Interrupted" );
    }

    ServiceReference<FeaturesService> serviceReference = bundleContext.getServiceReference( FeaturesService.class );
    if ( serviceReference != null ) {
      FeaturesService featuresService = bundleContext.getService( serviceReference );

      ServiceReference<ConfigurationAdmin>
          serviceReference1 =
          bundleContext.getServiceReference(ConfigurationAdmin.class );
      ConfigurationAdmin configurationAdmin = bundleContext.getService( serviceReference1 );

      ServiceReference<BlueprintStateService>
          bundleInterfaceServiceReference =
          bundleContext.getServiceReference( BlueprintStateService.class );
      BlueprintStateService blueprintStateService = bundleContext.getService( bundleInterfaceServiceReference );

      try {
        Configuration configuration = configurationAdmin.getConfiguration( "org.apache.karaf.features" );
        String featuresBoot = (String) configuration.getProperties().get( "featuresBoot" );
        String[] requiredFeatures = featuresBoot.split( "," );

        // Loop through to see if features are all installed
        while ( true ) {
          List<String> uninstalledFeatures = new ArrayList<String>();
          Boolean blueprintLoaded = true;

          for ( String requiredFeature : requiredFeatures ) {
            requiredFeature = requiredFeature.trim();
            Feature feature = featuresService.getFeature( requiredFeature );

            if ( feature != null ) {
              for ( BundleInfo bundleInfo : feature.getBundles() ) {
                if ( blueprintStateService.hasBlueprint( bundleInfo.getLocation() ) ) {
                  BundleState state = blueprintStateService.getState( bundleInfo.getLocation() );
                  if ( state != BundleState.Active && state != BundleState.Failure ) {
                    blueprintLoaded = false;
                  }
                }
              }
              if ( !featuresService.isInstalled( feature ) ) {
                uninstalledFeatures.add( requiredFeature );
              }
            }
          }

          if ( uninstalledFeatures.size() > 0 || !blueprintLoaded ) {
            if ( System.currentTimeMillis() - timeout > entryTime ) {
              throw new FeatureWatcherException(
                  "Timed out waiting for Karaf features to install: " + StringUtils.join( uninstalledFeatures, "," ) );
            }
            logger.debug( "KarafFeatureWatcher is waiting for the following features to install: " + StringUtils
                .join( uninstalledFeatures, "," ) );
            Thread.sleep( 100 );
            continue;
          }
          break;
        }

      } catch ( IOException e ) {
        throw new FeatureWatcherException( "Error accessing ConfigurationAdmin", e );
      } catch ( Exception e ) {
        throw new FeatureWatcherException( "Unknown error in KarafWatcher", e );
      }
    }
  }
}
