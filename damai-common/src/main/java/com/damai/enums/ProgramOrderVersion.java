package com.damai.enums;

public enum ProgramOrderVersion {
    /**
     * 版本
     * */
    V1_VERSION("v1","v1版本"),

    V2_VERSION("v2","v2版本"),

    V3_VERSION("v3","v3版本"),

    V4_VERSION("v4","v4版本"),
    ;

    private final String version;

    private final String msg;

    ProgramOrderVersion(String version, String msg) {
        this.version = version;
        this.msg = msg;
    }

    public String getVersion() {
        return version;
    }


    public String getMsg() {
        return this.msg == null ? "" : this.msg;
    }


    public static String getMsg(String version) {
        for (ProgramOrderVersion re : ProgramOrderVersion.values()) {
            if (re.version.equals(version)) {
                return re.msg;
            }
        }
        return "";
    }

    public static ProgramOrderVersion getRc(String version) {
        for (ProgramOrderVersion re : ProgramOrderVersion.values()) {
            if (re.version.equals(version)) {
                return re;
            }
        }
        return null;
    }
}
