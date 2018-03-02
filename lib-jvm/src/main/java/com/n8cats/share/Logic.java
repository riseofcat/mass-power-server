package com.n8cats.share;

import com.n8cats.lib_gwt.LibAllGwt;
import com.n8cats.share.data.Car;
import com.n8cats.share.data.PlayerId;
import com.n8cats.share.data.State;

public class Logic {
public static final int UPDATE_MS = 40;
public static final float UPDATE_S = UPDATE_MS / LibAllGwt.MILLIS_IN_SECCOND;
public static final int MIN_SIZE = 20;
public static final int FOOD_SIZE = 20;
public static final float MIN_RADIUS = 1f;
public static final int FOODS = 20;

public interface InStateAction {
	void act(State state, GetCarById getCar);
}
public interface GetCarById {
	Car getCar(PlayerId id);
}
}
