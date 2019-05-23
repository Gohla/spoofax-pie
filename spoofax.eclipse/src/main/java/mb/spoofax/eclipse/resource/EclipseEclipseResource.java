package mb.spoofax.eclipse.resource;

import mb.resource.ReadableResource;
import mb.resource.Resource;
import mb.resource.ResourceKey;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

public class EclipseEclipseResource implements Resource, ReadableResource, WrapsEclipseResource {
    private final IResource resource;


    public EclipseEclipseResource(IResource resource) {
        this.resource = resource;
    }


    @Override public boolean exists() {
        return resource.exists();
    }

    @Override public boolean isReadable() {
        return true;
    }

    @Override public Instant getLastModifiedTime() {
        final long stamp = resource.getModificationStamp();
        if(stamp == IResource.NULL_STAMP) {
            return Instant.MIN;
        }
        return Instant.ofEpochMilli(stamp);
    }

    private IFileStore getStore() throws IOException {
        try {
            return EFS.getStore(resource.getLocationURI());
        } catch(CoreException e) {
            throw new IOException("Getting file store for resource '" + resource + "' failed unexpectedly", e);
        }
    }

    @Override public long getSize() throws IOException {
        return getStore().fetchInfo().getLength();
    }

    @Override public InputStream newInputStream() throws IOException {
        try {
            return getStore().openInputStream(EFS.NONE, null);
        } catch(CoreException e) {
            throw new IOException("Creating a new input stream for resource '" + resource + "' failed unexpectedly", e);
        }
    }


    @Override public ResourceKey getKey() {
        return new EclipseResourceKey(resource);
    }


    @Override public IResource getWrappedEclipseResource() {
        return resource;
    }


    @Override public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        final EclipseEclipseResource that = (EclipseEclipseResource) o;
        return resource.equals(that.resource);
    }

    @Override public int hashCode() {
        return resource.hashCode();
    }

    @Override public String toString() {
        return resource.toString();
    }
}