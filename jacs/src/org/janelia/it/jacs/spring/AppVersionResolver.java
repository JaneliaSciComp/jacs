
package org.janelia.it.jacs.spring;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Comparator;

/**
 * @author Cristian Goina
 */
public class AppVersionResolver {

    static Logger logger = Logger.getLogger(AppVersionResolver.class.getName());

    static class AppVersion {
        private String majorNumber;
        private String minorNumber;
        private String revisionNumber;
        private String buildNumber;
    }

    static class AppVersionComparator implements Comparator<AppVersion> {

        public AppVersionComparator() {
        }

        public int compare(AppVersion v1, AppVersion v2) {
            if (v1 != null && v2 != null) {
                int compRes;
                compRes = compareVersionComponents(v1.majorNumber, v2.majorNumber);
                if (compRes != 0) {
                    return compRes;
                }
                compRes = compareVersionComponents(v1.minorNumber, v2.minorNumber);
                if (compRes != 0) {
                    return compRes;
                }
                compRes = compareVersionComponents(v1.revisionNumber, v2.revisionNumber);
                if (compRes != 0) {
                    return compRes;
                }
                else {
                    return compareVersionComponents(v1.buildNumber, v2.buildNumber);
                }
            }
            else if (v1 != null) {
                return 1;
            }
            else if (v2 != null) {
                return -1;
            }
            else {
                return 0;
            }
        }

        int compareVersionComponents(String vn1, String vn2) {
            if (vn1 != null && vn2 != null) {
                CharacterIterator cit1 = new StringCharacterIterator(vn1);
                CharacterIterator cit2 = new StringCharacterIterator(vn2);
                for (; ;) {
                    String region1 = getNextRegion(cit1);
                    String region2 = getNextRegion(cit2);
                    if (region1 != null && region2 != null) {
                        if (Character.isDigit(region1.charAt(0)) && Character.isDigit(region1.charAt(0))) {
                            int n1 = Integer.parseInt(region1);
                            int n2 = Integer.parseInt(region2);
                            if (n1 != n2) {
                                return n1 - n2;
                            }
                        }
                        else if (!Character.isDigit(region1.charAt(0)) && !Character.isDigit(region1.charAt(0))) {
                            int scompRes = region1.compareTo(region2);
                            if (scompRes != 0) {
                                return scompRes;
                            }
                        }
                        else if (Character.isDigit(region1.charAt(0)) && !Character.isDigit(region1.charAt(0))) {
                            // first argument starts with a digit while the second start with a non-digit
                            return -1;
                        }
                        else {
                            // first argument starts with a non-digit while the second start with a digit
                            return 1;
                        }
                    }
                    else if (region1 != null) {
                        return 1;
                    }
                    else if (region2 != null) {
                        return -1;
                    }
                    else {
                        return 0;
                    }
                }
            }
            else if (vn1 != null) {
                return 1;
            }
            else if (vn2 != null) {
                return -1;
            }
            else {
                return 0;
            }
        }

        private String getNextRegion(CharacterIterator cit) {
            char firstChar = CharacterIterator.DONE;
            StringBuffer buffer = new StringBuffer();
            for (char c = cit.current(); ; c = cit.next()) {
                if (c == CharacterIterator.DONE) {
                    break;
                }
                // skip whitespaces
                if (Character.isWhitespace(c)) {
                    continue;
                }
                if (firstChar == CharacterIterator.DONE) {
                    firstChar = c;
                    buffer.append(c);
                }
                else {
                    if (Character.isDigit(firstChar)) {
                        if (Character.isDigit(c)) {
                            buffer.append(c);
                        }
                        else {
                            break;
                        }
                    }
                    else {
                        if (Character.isDigit(c)) {
                            break;
                        }
                        else {
                            buffer.append(c);
                        }
                    }
                }
            }
            return buffer.length() == 0 ? null : buffer.toString();
        }

    }

    private String appVersionAsString;
    private String fullAppVersionAsString;
    private AppVersion appVersion;
    private long appVersionLatestTimestamp;
    private long appVersionCheckIntervalInMillis;
    private ComputeBeanRemote remoteAppVersionGetter;
    private final AppVersionComparator versionComparator = new AppVersionComparator();

    public void
    AppVersionResolver() {
    }

    public long getAppVersionCheckIntervalInMillis() {
        return appVersionCheckIntervalInMillis;
    }

    public void setAppVersionCheckIntervalInMillis(long appVersionCheckIntervalInMillis) {
        this.appVersionCheckIntervalInMillis = appVersionCheckIntervalInMillis;
    }

    public long getAppVersionLatestTimestamp() {
        return appVersionLatestTimestamp;
    }

    public void setAppVersionLatestTimestamp(long appVersionLatestTimestamp) {
        this.appVersionLatestTimestamp = appVersionLatestTimestamp;
    }

    public void setRemoteAppVersionGetter(ComputeBeanRemote remoteAppVersionGetter) {
        this.remoteAppVersionGetter = remoteAppVersionGetter;
    }

    public void init() {
        checkAppVersion();
    }

    public String getAppVersion() {
        return getAppVersion(false);
    }

    public String getAppVersion(boolean getFullVersion) {
        long currentTimestamp = System.currentTimeMillis();
        if (appVersionLatestTimestamp == -1 ||
                (currentTimestamp - appVersionLatestTimestamp) > appVersionCheckIntervalInMillis) {
            checkAppVersion();
        }

        if (getFullVersion)
            return fullAppVersionAsString;
        else
            return appVersionAsString;
    }

    public void setAppVersion(String appVersion) {
        this.fullAppVersionAsString = appVersion;
        this.appVersionAsString = removeBuildNumber(fullAppVersionAsString);
        this.appVersion = parseVersion(appVersion);
    }

    String formatVersion(AppVersion version) {
        StringBuffer versionBuffer = new StringBuffer();
        if (version != null) {
            if (version.majorNumber != null) {
                versionBuffer.append(version.majorNumber);
            }
            else {
                // stop here
                return versionBuffer.toString();
            }
            versionBuffer.append('.');
            if (version.minorNumber != null) {
                versionBuffer.append(version.minorNumber);
            }
            else {
                // stop here
                return versionBuffer.toString();
            }
            versionBuffer.append('.');
            if (version.revisionNumber != null) {
                versionBuffer.append(version.revisionNumber);
            }
            else {
                // stop here
                return versionBuffer.toString();
            }
            versionBuffer.append('.');
            if (version.buildNumber != null) {
                versionBuffer.append(version.buildNumber);
            }
        }
        return versionBuffer.toString();
    }

    AppVersion parseVersion(String versionString) {
        AppVersion version = null;
        if (versionString != null) {
            version = new AppVersion();
            String unparsedVersion = versionString;
            int dotPosition;
            // set the major
            dotPosition = unparsedVersion.indexOf('.');
            if (dotPosition == -1) {
                version.majorNumber = unparsedVersion;
                return version;
            }
            else {
                version.majorNumber = unparsedVersion.substring(0, dotPosition);
                unparsedVersion = unparsedVersion.substring(dotPosition + 1);
            }
            // set the minor
            dotPosition = unparsedVersion.indexOf('.');
            if (dotPosition == -1) {
                version.minorNumber = unparsedVersion;
                return version;
            }
            else {
                version.minorNumber = unparsedVersion.substring(0, dotPosition);
                unparsedVersion = unparsedVersion.substring(dotPosition + 1);
            }
            // set the revision
            dotPosition = unparsedVersion.indexOf('.');
            if (dotPosition == -1) {
                version.revisionNumber = unparsedVersion;
                return version;
            }
            else {
                version.revisionNumber = unparsedVersion.substring(0, dotPosition);
                unparsedVersion = unparsedVersion.substring(dotPosition + 1);
            }
            // set the build
            version.buildNumber = unparsedVersion;
        }
        return version;
    }

    private void checkAppVersion() {
        AppVersion remoteAppVersion = null;
        try {
            String remoteAppVersionValue = remoteAppVersionGetter.getAppVersion();
            remoteAppVersion = parseVersion(remoteAppVersionValue);
        }
        catch (Exception ignore) {
            // if there's any kind of problem update the timestamp
            // so that it wouldn't try too often
            logger.warn("Could not retrieve the remote application version", ignore);
            appVersionLatestTimestamp = System.currentTimeMillis();
        }
        if (remoteAppVersion != null) {
            int compResult = versionComparator.compare(appVersion, remoteAppVersion);
            if (compResult < 0) {
                appVersion = remoteAppVersion;
                fullAppVersionAsString = formatVersion(appVersion);
                this.appVersionAsString = removeBuildNumber(fullAppVersionAsString);
            }
            // pondering whether we should update the timestamp
            // no matter whether the retrieval of the version was successfull or not
            appVersionLatestTimestamp = System.currentTimeMillis();
        }
    }

    private String removeBuildNumber(String fullVersion) {
        String[] parts = fullVersion.split("\\.");
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < parts.length && i < 3; i++) // no more then 3 elements to display
        {
            if (i > 0) sb.append(".");
            sb.append(parts[i]);
        }
        return sb.toString();
    }

}
