package com.example.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("package") 
public class Package {
	public String name;
	public String path;
	public String ver;
	public int vercode;
	public String apkid;
	public String icon;
	public String date;
	public String md5h;
	public String hash;
	public String rat;
	public int minSdk;
	public int sz;
	//public String catg;
	//public String catg2;
}
