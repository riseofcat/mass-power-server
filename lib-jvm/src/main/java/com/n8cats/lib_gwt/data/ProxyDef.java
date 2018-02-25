package com.n8cats.lib_gwt.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.n8cats.lib_gwt.Const;
import com.n8cats.lib_gwt.JsonBasic;

import java.util.Date;

public class ProxyDef extends JsonBasic {

public Const.ProxyType type;
public String host;
public String port;
public String login;
public String pass;
public String region;
volatile public int usages;
volatile public int ping;
volatile public Date previousCheck;
volatile public int successChecks;
volatile public int failedChecks;
volatile public int successTasks;
volatile public int failedTasks;
volatile public boolean success;

@JsonIgnore
public String getPreviousCheckStr() {
	if(previousCheck != null) {
		return previousCheck.toString();
	}
	return "not checked yet";
}

}
