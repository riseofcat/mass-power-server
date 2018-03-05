package com.riseofcat.lib_gwt;

public interface Coder<TCoded, TDecoded> {//todo maybe remove?
TCoded encode(TDecoded data);
TDecoded decode(TCoded code);
}
