/**
 * Copyright 2016 SAP SE
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.sap.cloud.commons.connectivity;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.sap.core.connectivity.api.configuration.ConnectivityConfiguration;
import com.sap.core.connectivity.api.configuration.DestinationConfiguration;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>DestinationUtils provides a convenience set of methods for access to properties defined in a particular Destination,
 * encapsulating all the boilerplate code required for that.</p>
 *
 * <div>Usage samples:</div>
 * <p>1. Fast-track to a particular property configured for destination with name "myDestination":<br>
 * <code>URL destinationUrl = DestinationUtils.builder().setDestinationName("myDestination").build().getProperty(URL.class, DestinationProperties.URL.toString());</code></p>
 *
 * <p>1. Get all properties configured for destination with name "myDestination":<br>
 * <code>Map&lt;String,String&gt; destinationProperties = DestinationUtils.builder().setDestinationName("myDestination").build().getDestinationConfiguration().getAllProperties();</code></p>
 *
 * <p>3. Lookup a particular connectivity configuration (in this case the default one):<br>
 * <code>ConnectivityConfiguration connCfg = DestinationUtils.lookupConnectivityConfiguration("java:comp/env/connectivityConfiguration");<BR>
 *
 * <p>4. Get all properties configured for destination with name "myDestination" from a particular connectivity configuration (looked up elsewhere):<br>
 * <code>Map&lt;String,String&gt; destinationProperties = DestinationUtils.getDestinationProperties(connCfg, "myDestination");</code></p>
 *
 */
public class DestinationUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(DestinationUtils.class);

  public enum DestinationProperties {
    URL("URL"), User("User"), Password("Password");

    private String text;

    private DestinationProperties(final String text) {
      this.text = text;
    }

    @Override
    public String toString() {
      return text;
    }
  }

  private String destinationName;
  private String connectivityCfgLookupName;
  private ConnectivityConfiguration connectivityConfiguration;

  private DestinationUtils() {}

  private DestinationUtils(final Builder destinationBuilder) {
    destinationName = destinationBuilder.destinationName;
    connectivityCfgLookupName = destinationBuilder.connectivityCfgLookupName;
  }

  public static Builder builder(){
	  return new Builder();
  }

  public String getDestinationName() {
    return destinationName;
  }

  public String getConnectivityConfigurationLookupName() {
	return connectivityCfgLookupName;
  }

  public DestinationConfiguration getDestinationConfiguration() throws IOException {
	final DestinationConfiguration destinationConfiguration = getConnectivityConfiguration().getConfiguration(destinationName);
    if (destinationConfiguration == null) {
      final String errorMessage =
          String.format("Destination %s is not found. Make sure to have the destination configured.", destinationName);
      LOGGER.error(errorMessage);
      throw new IOException(errorMessage);
    }
    return destinationConfiguration;
  }

  public ConnectivityConfiguration getConnectivityConfiguration() throws IOException {
	if (connectivityConfiguration == null) {
	  connectivityConfiguration = lookupConnectivityConfiguration(connectivityCfgLookupName);
	}
	return connectivityConfiguration;
  }

  public static ConnectivityConfiguration lookupConnectivityConfiguration(final String name) throws IOException {
    ConnectivityConfiguration cfg = null;
    try {
        final Context ctx = new InitialContext();
        cfg = (ConnectivityConfiguration) ctx.lookup(name);
    } catch (final NamingException e) {
      LOGGER.error("Could not lookup resource with name resource with name {}.", name, e);
      throw new IOException(e.getMessage(), e);
    }
    return cfg;
  }

  public static Map<String, String> getDestinationProperties(final ConnectivityConfiguration configuration, final String destinationName) throws ConfigurationException {
    final DestinationConfiguration destConfiguration = configuration.getConfiguration(destinationName);
    if (destConfiguration == null) {
      throw new ConfigurationException(String.format(
          "Destination [ %s ] not found. Hint: Make sure to have the destination configured.", destinationName));
    }
    return destConfiguration.getAllProperties();
  }

  @SuppressWarnings("unchecked")
  public <T> T getProperty(final Class<T> clazz, final String key) throws IOException, ConfigurationException {
    final Map<String, String> destinationProperties = getDestinationConfiguration().getAllProperties();
    final String value = destinationProperties.get(key);
    T instance = null;
    try {
      final Constructor<T> constructor = clazz.getDeclaredConstructor(String.class);
      constructor.setAccessible(true);
      instance = constructor.newInstance(value);
    } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
      LOGGER.error(e.getMessage(), e);
      throw new RuntimeException(String.format(
          "Could not instantiate object from type [ %s ] with argument [ %s ]. Maybe the given class does not specify a constructor taking such an argument.",
          clazz.getName(), value));
    }
    if (instance instanceof String) {
      instance = (T) ((String) instance).replaceAll("\\r|\\n", "");
    }
    return instance;
  }

  /**
   * Builds immutable DestinationUtils instances for the provided configuration properties.
   *
   */
  public static class Builder {

    private String destinationName;
    private String connectivityCfgLookupName = "java:comp/env/connectivityConfiguration";

    private Builder(){}

    /**
     * <p>
     * <div>Builds a new {@link DestinationUtils} instance with the state supplied by the setter methods.</div>
     * Throws an {@link IllegalStateException} if the method has been invoked prior to setting all required properties.
     * </p>
     * @return a new {@link DestinationUtils} instance
     */
    public DestinationUtils build() {
      if (destinationName == null) {
	  throw new IllegalStateException("The property destinationName value is null. Make sure to invoke setDestinationName(String) with a valid argument before the invocation of the build() method.");
      }
      return new DestinationUtils(this);
    }

    /**
     * Sets the <b>required</b> property <code>destinationName</code>, which identifies the destination for which a {@link DestinationUtils} instance is going to be built.
     * Always invoke this method, before invoking build().
     *
     * @param destinationName
     * @return a Builder instance for chaining, setup with this destinationName
     */
    public Builder setDestinationName(final String destinationName) {
      if(StringUtils.isEmpty(destinationName)) {
        throw new IllegalArgumentException("null is not a valid value for desitnationName");
      }
      this.destinationName = destinationName;
      return this;
    }

    /**
     * <em>Optionally</em>, set connectivity configuration lookup name using this method in case you would like to lookup configuration
     * other than the default.
     * The default configuration lookup name is <code>"java:comp/env/connectivityConfiguration"</code>.
     *
     * @param connectivityCfgLookupName
     * @return a Builder instance for chaining, setup with this <code>connectivityCfgLookupName</code>
     */
    public Builder setConnectivityConfigurationLookupName(final String connectivityCfgLookupName) {
      this.connectivityCfgLookupName = connectivityCfgLookupName;
      return this;
    }
  }

}
