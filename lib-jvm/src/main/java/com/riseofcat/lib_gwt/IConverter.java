package com.riseofcat.lib_gwt;

public interface IConverter<From,To> {
To convert(From obj);
}
