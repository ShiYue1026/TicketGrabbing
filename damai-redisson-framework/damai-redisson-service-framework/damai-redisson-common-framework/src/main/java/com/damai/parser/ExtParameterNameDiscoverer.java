package com.damai.parser;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.NativeDetector;

public class ExtParameterNameDiscoverer extends DefaultParameterNameDiscoverer {

    public ExtParameterNameDiscoverer() {
        super();
        if(!NativeDetector.inNativeImage()){
            addDiscoverer(new LocalVariableTableParameterNameDiscoverer());
        }
    }
}
