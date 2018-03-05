package com.riseofcat.lib_gwt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonBasic implements Serializable {
public int redundant;
}
