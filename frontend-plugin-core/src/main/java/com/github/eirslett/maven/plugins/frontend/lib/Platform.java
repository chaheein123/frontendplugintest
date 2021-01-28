package com.github.eirslett.maven.plugins.frontend.lib;

import java.io.File;

enum Architecture { x86, x64, ppc64le, s390x, arm64, armv7l;
    public static Architecture guess(){
        String arch = System.getProperty("os.arch");
        String version = System.getProperty("os.version");

        if (arch.equals("ppc64le")) {
            return ppc64le;
        } else if (arch.equals("aarch64")) {
            return arm64;
        } else if (arch.equals("s390x")) {
                return s390x;
        } else if (arch.equals("arm") && version.contains("v7")) {
                return armv7l;
        } else {
            return arch.contains("64") ? x64 : x86;
        }
    }
}

enum OS { Windows, Mac, Linux, SunOS;

    public static OS guess() {
        final String osName = System.getProperty("os.name");
        return  osName.contains("Windows") ? OS.Windows :
                osName.contains("Mac") ? OS.Mac :
                        osName.contains("SunOS") ? OS.SunOS :
                                OS.Linux;
    }

    public String getArchiveExtension(){
        if(this == OS.Windows){
          return "zip";
        } else {
          return "tar.gz";
        }
    }

    public String getCodename(){
        if(this == OS.Mac){
            return "darwin";
        } else if(this == OS.Windows){
            return "win";
        } else if(this == OS.SunOS){
            return "sunos";
        } else {
            return "linux";
        }
    }
}

class Platform {
    private final String nodeDownloadRoot;
    private final OS os;
    private final Architecture architecture;
    private final String classifier;

    public Platform(OS os, Architecture architecture) {
        this("https://nodejs.org/dist/", os, architecture, null);
    }

    public Platform(String nodeDownloadRoot, OS os, Architecture architecture, String classifier) {
        this.nodeDownloadRoot = nodeDownloadRoot;
        this.os = os;
        this.architecture = architecture;
        this.classifier = classifier;
    }

    public static Platform guess(){
        OS os = OS.guess();
        Architecture architecture = Architecture.guess();
        // The default libc is glibc, but Alpine uses musl. When not default, the nodejs download
        // (and path within it) needs a classifier in the suffix (ex. -musl).
        // We know Alpine is in use if the release file exists, and this is the simplest check.
        if (os == OS.Linux && new File("/etc/alpine-release").exists()) {
            return new Platform(
                // Currently, musl is Experimental. The download root can be overridden with config
                // if this changes and there's not been an update to this project, yet.
                // See https://github.com/nodejs/node/blob/master/BUILDING.md#platform-list
                "https://unofficial-builds.nodejs.org/download/release/",
                os, architecture, "musl");
        }
        return new Platform(os, architecture);
    }

    public String getNodeDownloadRoot(){
        return nodeDownloadRoot;
    }

    public String getArchiveExtension(){
        return os.getArchiveExtension();
    }

    public String getCodename(){
        return os.getCodename();
    }

    public boolean isWindows(){
        return os == OS.Windows;
    }

    public boolean isMac(){
        return os == OS.Mac;
    }

    public String getLongNodeFilename(String nodeVersion, boolean archiveOnWindows) {
        if(isWindows() && !archiveOnWindows){
            return "node.exe";
        } else {
            return "node-" + nodeVersion + "-" + this.getNodeClassifier();
        }
    }

    public String getNodeDownloadFilename(String nodeVersion, boolean archiveOnWindows) {
        if(isWindows() && !archiveOnWindows) {
            if(architecture == Architecture.x64){
                if (nodeVersion.startsWith("v0.")) {
                    return nodeVersion+"/x64/node.exe";
                } else {
                    return nodeVersion+"/win-x64/node.exe";
                }
            } else {
                if (nodeVersion.startsWith("v0.")) {
                	return nodeVersion + "/node.exe";
                } else {
                    return nodeVersion+"/win-x86/node.exe";
                }
            }
        } else {
            return nodeVersion + "/" + getLongNodeFilename(nodeVersion, archiveOnWindows) + "." + os.getArchiveExtension();
        }
    }

    public String getNodeClassifier() {
        final String result;
        if(isMac() && architecture == Architecture.arm64) { // this check is required to download the x64 binary until there is an arm64 version available for macOS (darwin).
            result = getCodename() + "-" + Architecture.x64.name();
        } else {
            result = getCodename() + "-" + architecture.name();
        }
        return classifier != null ? result + "-" + classifier : result;
    }
}
