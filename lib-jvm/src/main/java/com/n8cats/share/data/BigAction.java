package com.n8cats.share.data;
import com.n8cats.share.Logic;

import org.jetbrains.annotations.Nullable;

public class BigAction implements Logic.InStateAction {//todo redundant because Json serialization
	@Nullable public NewCarAction n;
	@Nullable public PlayerAction p;
	public void act(State state, Logic.GetCarById getCar) {
		if(n != null) n.act(state, getCar);
		if(p != null) p.act(state, getCar);
	}
}
