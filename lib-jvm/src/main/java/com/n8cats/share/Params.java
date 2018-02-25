package com.n8cats.share;

import com.n8cats.lib_gwt.LibAllGwt;

public class Params {
public static final String TITLE = "mass-power.io";
public static final int DEFAULT_LATENCY_MS = 250;
public static final float DEFAULT_LATENCY_S = 250 / LibAllGwt.MILLIS_IN_SECCOND;
public static final int DELAY_TICKS = Params.DEFAULT_LATENCY_MS * 3 / Logic.UPDATE_MS + 1;//количество тиков для хранения действий //bigger delayed
public static final int REMOVE_TICKS = DELAY_TICKS * 3;//bigger removed
public static final int FUTURE_TICKS = DELAY_TICKS * 3;

}
