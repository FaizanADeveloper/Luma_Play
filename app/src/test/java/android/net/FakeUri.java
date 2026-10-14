package android.net;

import android.os.Parcel;
import java.util.Collections;
import java.util.List;

/**
 * Pure test double for [android.net.Uri] allowing JVM unit tests to instantiate
 * [com.lumaplay.tv.data.model.AudioItem] without Robolectric, Android instrumentation,
 * or mocking frameworks.
 */
public class FakeUri extends Uri {
    private final String uriString;

    public FakeUri(String uriString) {
        this.uriString = uriString;
    }

    @Override
    public boolean isHierarchical() {
        return true;
    }

    @Override
    public boolean isRelative() {
        return false;
    }

    @Override
    public String getScheme() {
        return "content";
    }

    @Override
    public String getSchemeSpecificPart() {
        return "";
    }

    @Override
    public String getEncodedSchemeSpecificPart() {
        return "";
    }

    @Override
    public String getAuthority() {
        return "media";
    }

    @Override
    public String getEncodedAuthority() {
        return "media";
    }

    @Override
    public String getUserInfo() {
        return null;
    }

    @Override
    public String getEncodedUserInfo() {
        return null;
    }

    @Override
    public String getHost() {
        return "media";
    }

    @Override
    public int getPort() {
        return -1;
    }

    @Override
    public String getPath() {
        return uriString;
    }

    @Override
    public String getEncodedPath() {
        return uriString;
    }

    @Override
    public String getQuery() {
        return null;
    }

    @Override
    public String getEncodedQuery() {
        return null;
    }

    @Override
    public String getFragment() {
        return null;
    }

    @Override
    public String getEncodedFragment() {
        return null;
    }

    @Override
    public List<String> getPathSegments() {
        return Collections.emptyList();
    }

    @Override
    public String getLastPathSegment() {
        return null;
    }

    @Override
    public Builder buildUpon() {
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    @Override
    public int compareTo(Uri other) {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        return this.toString().equals(o.toString());
    }

    @Override
    public int hashCode() {
        return uriString != null ? uriString.hashCode() : 0;
    }

    @Override
    public String toString() {
        return uriString;
    }
}
