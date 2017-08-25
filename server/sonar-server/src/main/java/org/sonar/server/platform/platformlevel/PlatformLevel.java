/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform.platformlevel;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.config.Configuration;
import org.sonar.cluster.ClusterProperties;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.core.platform.Module;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public abstract class PlatformLevel {
  private final String name;
  @Nullable
  private final PlatformLevel parent;
  private final ComponentContainer container;

  public PlatformLevel(String name) {
    this.name = name;
    this.parent = null;
    this.container = createContainer(null);
  }

  public PlatformLevel(String name, @Nonnull PlatformLevel parent) {
    this.name = checkNotNull(name);
    this.parent = checkNotNull(parent);
    this.container = createContainer(parent.container);
  }

  public ComponentContainer getContainer() {
    return container;
  }

  public String getName() {
    return name;
  }

  /**
   * Intended to be override by subclasses if needed
   */
  protected ComponentContainer createContainer(@Nullable ComponentContainer parent) {
    if (parent == null) {
      return new ComponentContainer();
    }
    return parent.createChild();
  }

  public PlatformLevel configure() {
    configureLevel();

    List<Module> modules = container.getComponentsByType(Module.class);
    for (Module module : modules) {
      module.configure(container);
    }

    return this;
  }

  protected abstract void configureLevel();

  /**
   * Intended to be override by subclasses if needed
   */
  public PlatformLevel start() {
    container.startComponents();

    return this;
  }

  /**
   * Intended to be override by subclasses if needed
   */
  public PlatformLevel stop() {
    container.stopComponents();

    return this;
  }

  /**
   * Intended to be override by subclasses if needed
   */
  public PlatformLevel destroy() {
    if (parent != null) {
      parent.container.removeChild(container);
    }
    return this;
  }

  protected <T> T get(Class<T> tClass) {
    return requireNonNull(container.getComponentByType(tClass));
  }

  protected <T> List<T> getAll(Class<T> tClass) {
    return container.getComponentsByType(tClass);
  }

  protected <T> Optional<T> getOptional(Class<T> tClass) {
    return Optional.ofNullable(container.getComponentByType(tClass));
  }

  protected void add(Object... objects) {
    for (Object object : objects) {
      if (object != null) {
        container.addComponent(object, true);
      }
    }
  }

  /**
   * Add a component to container only if the web server is startup leader.
   *
   * @throws IllegalStateException if called from PlatformLevel1, when cluster settings are not loaded
   */
  AddIfStartupLeader addIfStartupLeader(Object... objects) {
    AddIfStartupLeader res = new AddIfStartupLeader(isStartupLeader());
    res.ifAdd(objects);
    return res;
  }

  /**
   * Add a component to container only if clustering is enabled.
   *
   * @throws IllegalStateException if called from PlatformLevel1, when cluster settings are not loaded
   */
  AddIfCluster addIfCluster(Object... objects) {
    AddIfCluster res = new AddIfCluster(isClusterEnabled());
    res.ifAdd(objects);
    return res;
  }

  private boolean isClusterEnabled() {
    Optional<Configuration> cluster = getOptional(Configuration.class);
    return cluster
      .map(c -> c.getBoolean(ClusterProperties.CLUSTER_ENABLED).orElse(Boolean.FALSE))
      .orElseThrow(() -> new IllegalStateException("Cluster settings not loaded yet"));
  }

  private abstract class AddIf {
    private final boolean condition;

    private AddIf(boolean condition) {
      this.condition = condition;
    }

    public void ifAdd(Object... objects) {
      if (condition) {
        PlatformLevel.this.add(objects);
      }
    }

    public void otherwiseAdd(Object... objects) {
      if (!condition) {
        PlatformLevel.this.add(objects);
      }
    }
  }

  public final class AddIfStartupLeader extends AddIf {
    private AddIfStartupLeader(boolean condition) {
      super(condition);
    }
  }

  public final class AddIfCluster extends AddIf {
    private AddIfCluster(boolean condition) {
      super(condition);
    }
  }

  private boolean isStartupLeader() {
    Optional<StartupLeader> cluster = getOptional(StartupLeader.class);
    checkState(cluster.isPresent(), "Cluster settings not loaded yet");
    return cluster.get().isStartupLeader();
  }

  protected void addAll(Collection<?> objects) {
    add(objects.toArray(new Object[objects.size()]));
  }

}
