package com.example.xml;

import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("apklst") 
public class Apklst {
	public int version;
	public Repository repository;
	
	@XStreamImplicit
	public List<Package> packages;
}
